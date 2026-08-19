#!/usr/bin/env python3
"""Soft-delete the images (and, where every image of a style disappears, the sku itself)
listed in a delete.md handoff file, without ever touching an existing primary key.

delete.md format (pipe table, header + separator + data rows):
    | style_number   | file_name            | reviewer | note | updated_at |
    | MWHFSTA10BK001 | 03-5778f2ba4f54.webp | ...      |      | ...        |

file_name's leading number is the image's position (verified 1:1 against
finale_backdata/data/catalog.sqlite's original_images.position for every row before
this script existed -- see scripts/soft_delete_from_delete_md.py's commit message).

This does NOT truncate/reinsert sku or sku_image (scripts/load_products.py's approach) --
sku.sku is the numeric ID printed on physical NFC/QR tags in-store, so a reload that
reassigns ids would silently point already-deployed tags at the wrong product. Instead:

  1. sku_image rows matching (style_number, position) from delete.md -> is_deleted = 1.
  2. Any style_number whose product/model sku_image rows are now *all* deleted ->
     its sku row also gets is_deleted = 1 (the "style removed from catalog entirely" case).

Both are UPDATEs, guarded by "AND is_deleted = 0" so re-running is a no-op the second time.
No row is ever inserted or removed, and no primary key changes. See migration
V20__sku_soft_delete.sql and the repository methods ending in *AndIsDeletedFalse* for the
read-side half of this (they're what makes soft-deleted rows actually disappear from the
API).

DB connection is read from the same DB_URL / DB_USERNAME / DB_PASSWORD env vars Spring's
application.yaml uses, so this always targets whatever DB the app itself is configured for.

Usage:
    DB_URL=... DB_USERNAME=... DB_PASSWORD=... python scripts/soft_delete_from_delete_md.py --dry-run
    DB_URL=... DB_USERNAME=... DB_PASSWORD=... python scripts/soft_delete_from_delete_md.py
"""

from __future__ import annotations

import argparse
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_DELETE_MD_PATH = REPO_ROOT.parent / "delete.md"


def parse_jdbc_url(url: str) -> tuple[str, int, str]:
    match = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", url)
    if not match:
        raise ValueError(f"Cannot parse DB_URL: {url}")
    host, port, database = match.group(1), match.group(2) or "3306", match.group(3)
    return host, int(port), database


def parse_delete_md(path: Path) -> list[tuple[str, int]]:
    lines = path.read_text(encoding="utf-8").splitlines()
    rows: list[tuple[str, int]] = []
    for line in lines:
        line = line.strip()
        if not line.startswith("|"):
            continue
        cells = [c.strip() for c in line.strip("|").split("|")]
        if len(cells) < 2:
            continue
        style_number, file_name = cells[0], cells[1]
        if not style_number or style_number == "style_number":
            continue
        if set(style_number) == {"-"}:
            continue
        prefix = file_name.split("-", 1)[0]
        if not prefix.isdigit():
            print(f"WARNING: cannot parse position from file_name {file_name!r} (style {style_number}), skipping",
                  file=sys.stderr)
            continue
        rows.append((style_number, int(prefix)))
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--delete-md-path", type=Path, default=DEFAULT_DELETE_MD_PATH)
    parser.add_argument("--dry-run", action="store_true",
                         help="Print how many rows would be soft-deleted without touching the DB")
    args = parser.parse_args()

    image_keys = parse_delete_md(args.delete_md_path)
    distinct_styles = sorted({style for style, _ in image_keys})
    print(f"delete.md rows parsed: {len(image_keys)} image(s) across {len(distinct_styles)} style(s)")

    db_url = os.environ.get("DB_URL")
    db_username = os.environ.get("DB_USERNAME")
    db_password = os.environ.get("DB_PASSWORD")
    if not db_url or db_username is None or db_password is None:
        sys.exit("DB_URL / DB_USERNAME / DB_PASSWORD env vars are required (same values as application.yaml)")

    import pymysql

    host, port, database = parse_jdbc_url(db_url)
    connection = pymysql.connect(host=host, port=port, user=db_username, password=db_password,
                                  database=database, charset="utf8mb4", autocommit=False)
    try:
        with connection.cursor() as cursor:
            images_already_deleted = 0
            images_not_found = 0
            images_to_delete = []
            new_deletion_count_by_style: dict[str, int] = {}
            for style_number, position in image_keys:
                cursor.execute(
                    "SELECT image_id, is_deleted FROM sku_image WHERE style_number = %s AND position = %s",
                    (style_number, position),
                )
                row = cursor.fetchone()
                if row is None:
                    images_not_found += 1
                    continue
                _, is_deleted = row
                if is_deleted:
                    images_already_deleted += 1
                    continue
                images_to_delete.append((style_number, position))
                new_deletion_count_by_style[style_number] = new_deletion_count_by_style.get(style_number, 0) + 1

            print(f"  -> already soft-deleted (no-op): {images_already_deleted}")
            print(f"  -> not present in sku_image (never loaded, e.g. detail/lifestyle shots): {images_not_found}")
            print(f"  -> will be soft-deleted now: {len(images_to_delete)}")

            fully_removed_styles = []
            for style_number in distinct_styles:
                cursor.execute(
                    "SELECT COUNT(*) FROM sku_image WHERE style_number = %s AND is_deleted = 0",
                    (style_number,),
                )
                currently_visible = cursor.fetchone()[0]
                remaining_after = currently_visible - new_deletion_count_by_style.get(style_number, 0)
                if remaining_after <= 0:
                    cursor.execute(
                        "SELECT sku, is_deleted FROM sku WHERE style_number = %s", (style_number,)
                    )
                    sku_row = cursor.fetchone()
                    if sku_row is not None and not sku_row[1]:
                        fully_removed_styles.append(style_number)

            print(f"  -> style(s) whose product/model images all disappear "
                  f"(their sku will be soft-deleted too): {len(fully_removed_styles)}")
            for style_number in fully_removed_styles:
                print(f"     {style_number}")

            if args.dry_run:
                return 0

            for style_number, position in images_to_delete:
                cursor.execute(
                    "UPDATE sku_image SET is_deleted = 1, updated_at = NOW() "
                    "WHERE style_number = %s AND position = %s AND is_deleted = 0",
                    (style_number, position),
                )

            for style_number in fully_removed_styles:
                cursor.execute(
                    "UPDATE sku SET is_deleted = 1, updated_at = NOW() "
                    "WHERE style_number = %s AND is_deleted = 0",
                    (style_number,),
                )

        connection.commit()
        print(f"Soft-deleted {len(images_to_delete)} sku_image row(s) and "
              f"{len(fully_removed_styles)} sku row(s).")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
