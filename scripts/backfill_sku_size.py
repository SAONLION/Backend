#!/usr/bin/env python3
"""Backfill sku.size from final_backdata/data/products.csv, matched by style_number.

Context: sku.style_number is already fully populated (632/632 on both local and EC2),
but sku.size ended up NULL/empty for some rows on EC2. products.csv has one row per
style_number with a `sizes` column (JSON array, e.g. ["LRG","MED"]) -- the same source
scripts/load_products.py and scripts/import_backdata.py already draw sku.size from.

This script is intentionally narrow and non-destructive:
  - Only UPDATEs the `size` column.
  - Only touches sku rows where size IS NULL or '' (never overwrites an existing value).
  - Matches on style_number, and the UPDATE's WHERE clause pins both `sku` and
    `style_number` together so a row is only ever touched if both agree with what we read.
  - Never touches sku_id, style_number, or any other column/table.

DB connection is read from the same DB_URL / DB_USERNAME / DB_PASSWORD env vars Spring's
application.yaml uses, so this always targets whatever DB those env vars point at
(point them at EC2's DB to run this against production).

Usage:
    DB_URL=... DB_USERNAME=... DB_PASSWORD=... python scripts/backfill_sku_size.py --dry-run
    DB_URL=... DB_USERNAME=... DB_PASSWORD=... python scripts/backfill_sku_size.py
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import re
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_CSV_PATH = REPO_ROOT / "final_backdata" / "data" / "products.csv"


def parse_jdbc_url(url: str) -> tuple[str, int, str]:
    match = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", url)
    if not match:
        raise ValueError(f"Cannot parse DB_URL: {url}")
    host, port, database = match.group(1), match.group(2) or "3306", match.group(3)
    return host, int(port), database


def load_size_by_style(csv_path: Path) -> dict[str, str]:
    size_by_style: dict[str, str] = {}
    with csv_path.open("r", encoding="utf-8-sig", newline="") as handle:
        for row in csv.DictReader(handle):
            style_number = row["style_number"]
            sizes = json.loads(row["sizes"] or "[]")
            size_value = ",".join(sizes)[:255] if sizes else None
            if size_value:
                size_by_style[style_number] = size_value
    return size_by_style


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--csv-path", type=Path, default=DEFAULT_CSV_PATH)
    parser.add_argument("--dry-run", action="store_true",
                         help="Print how many rows would be updated without touching the DB")
    args = parser.parse_args()

    size_by_style = load_size_by_style(args.csv_path)
    print(f"products.csv rows with a usable size: {len(size_by_style)}")

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
            cursor.execute(
                "SELECT sku, style_number FROM sku WHERE style_number IS NOT NULL "
                "AND (size IS NULL OR size = '')"
            )
            null_size_rows = cursor.fetchall()

            to_update = [(sku_id, style_number, size_by_style[style_number])
                         for sku_id, style_number in null_size_rows
                         if style_number in size_by_style]
            unmatched = [style_number for _, style_number in null_size_rows
                         if style_number not in size_by_style]

            print(f"sku rows with NULL/empty size: {len(null_size_rows)}")
            print(f"  -> matched in products.csv and will be updated: {len(to_update)}")
            print(f"  -> NULL/empty size but no match in products.csv (left untouched): {len(unmatched)}")

            if args.dry_run:
                for sku_id, style_number, size_value in to_update[:10]:
                    print(f"    [dry-run] sku={sku_id} style_number={style_number} -> size={size_value!r}")
                if len(to_update) > 10:
                    print(f"    ... and {len(to_update) - 10} more")
                return 0

            updated = 0
            for sku_id, style_number, size_value in to_update:
                cursor.execute(
                    "UPDATE sku SET size = %s WHERE sku = %s AND style_number = %s "
                    "AND (size IS NULL OR size = '')",
                    (size_value, sku_id, style_number),
                )
                updated += cursor.rowcount

        connection.commit()
        print(f"Updated {updated} sku row(s).")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
