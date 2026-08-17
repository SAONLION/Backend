#!/usr/bin/env python3
"""One-off migration: load the sku_image table from final_backdata/data/catalog.sqlite's
image_classifications (+ original_images) tables, joined against the S3 manifest CSV
produced by s3_upload_images.py.

Only 'product' and 'model' shot_type rows are loaded -- those are the only two
shot types the journey-card collage (and sku_image.shot_type) currently model.

Run s3_upload_images.py first. DB connection is read from the same DB_URL /
DB_USERNAME / DB_PASSWORD env vars Spring's application.yaml uses.
"""

from __future__ import annotations

import argparse
import csv
import os
import re
import sqlite3
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SQLITE_PATH = REPO_ROOT / "final_backdata" / "data" / "catalog.sqlite"
DEFAULT_MANIFEST_PATH = REPO_ROOT / "final_backdata" / "data" / "s3_manifest.csv"

SHOT_TYPE_MAP = {"product": "PRODUCT", "model": "MODEL"}


def parse_jdbc_url(url: str) -> tuple[str, int, str]:
    match = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", url)
    if not match:
        raise ValueError(f"Cannot parse DB_URL: {url}")
    host, port, database = match.group(1), match.group(2) or "3306", match.group(3)
    return host, int(port), database


def fetch_classified_images(sqlite_path: Path) -> list[dict]:
    connection = sqlite3.connect(str(sqlite_path))
    connection.row_factory = sqlite3.Row
    try:
        cursor = connection.execute("""
            SELECT o.style_number, o.position, c.shot_type, c.has_person
            FROM original_images o
            JOIN image_classifications c
              ON o.style_number = c.style_number AND o.position = c.position
            WHERE c.shot_type IN ('product', 'model')
        """)
        return [dict(row) for row in cursor.fetchall()]
    finally:
        connection.close()


def load_manifest(manifest_path: Path) -> dict[tuple[str, int], str]:
    urls_by_key = {}
    with manifest_path.open("r", encoding="utf-8") as handle:
        for row in csv.DictReader(handle):
            urls_by_key[(row["style_number"], int(row["position"]))] = row["image_url"]
    return urls_by_key


def build_rows(classified: list[dict], urls_by_key: dict[tuple[str, int], str]) -> list[dict]:
    rows = []
    missing_url = 0
    for image in classified:
        key = (image["style_number"], image["position"])
        image_url = urls_by_key.get(key)
        if image_url is None:
            missing_url += 1
            continue
        rows.append({
            "style_number": image["style_number"],
            "position": image["position"],
            "image_url": image_url,
            "shot_type": SHOT_TYPE_MAP[image["shot_type"]],
            "has_person": bool(image["has_person"]),
        })
    if missing_url:
        print(f"WARNING: {missing_url} classified image(s) have no matching manifest entry "
              f"(re-run s3_upload_images.py?)", file=sys.stderr)
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sqlite-path", type=Path, default=DEFAULT_SQLITE_PATH)
    parser.add_argument("--manifest-path", type=Path, default=DEFAULT_MANIFEST_PATH)
    parser.add_argument("--dry-run", action="store_true",
                         help="Parse and join without touching the DB")
    args = parser.parse_args()

    classified = fetch_classified_images(args.sqlite_path)
    urls_by_key = load_manifest(args.manifest_path)
    rows = build_rows(classified, urls_by_key)

    print(f"classified product/model images: {len(classified)}")
    print(f"rows ready to load into sku_image: {len(rows)}")

    if args.dry_run:
        return 0

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
            cursor.executemany(
                """
                INSERT INTO sku_image (style_number, position, image_url, shot_type, has_person,
                                        created_at, updated_at)
                VALUES (%(style_number)s, %(position)s, %(image_url)s, %(shot_type)s, %(has_person)s,
                        NOW(), NOW())
                ON DUPLICATE KEY UPDATE
                    image_url = VALUES(image_url),
                    shot_type = VALUES(shot_type),
                    has_person = VALUES(has_person),
                    updated_at = NOW()
                """,
                rows,
            )
        connection.commit()
        print(f"Loaded/updated {len(rows)} sku_image rows")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
