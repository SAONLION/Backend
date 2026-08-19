#!/usr/bin/env python3
"""Backfill the 10 sku columns added by V18__sku_catalog_detail_fields.sql
(description, short_description, body_material, trim_material, country_of_origin,
dimensions_text, storage_text, lining_care_text, strap_length, handle_drop) from
final_backdata/data/catalog.sqlite's products table, matched by style_number.

Context: scripts/load_products.py already computes these same 10 fields when it does a
fresh --truncate load (see its module docstring / extract_material / extract_dimensions_text
/ extract_feature_sentences / extract_attribute helpers, written for the P2-4/P2-5/P2-8 API
field additions). This script reuses those exact functions (imported from load_products.py,
not reimplemented) to fill in the same columns on sku rows that already exist, without
touching anything else about those rows.

This script is intentionally narrow and non-destructive, same principles as
scripts/backfill_sku_size.py:
  - Only UPDATEs the 10 columns listed above -- never INSERT/DELETE/TRUNCATE.
  - Only touches a given column on a given row when it is NULL/'' there; a column that
    already has a value is never overwritten.
  - Matches on style_number.
  - Every UPDATE's WHERE clause pins `sku` AND `style_number` together, plus an
    "IS NULL OR = ''" guard repeated for every column being set in that statement, so a
    row is only touched if it still matches everything we read moments earlier.
  - Never touches sku_id, style_number, name, category, or any other existing column/table.

DB connection is read from the same DB_URL / DB_USERNAME / DB_PASSWORD env vars Spring's
application.yaml uses, so this always targets whatever DB those env vars point at.

Usage:
    DB_URL=... DB_USERNAME=... DB_PASSWORD=... python scripts/backfill_sku_catalog_fields.py --dry-run
    DB_URL=... DB_USERNAME=... DB_PASSWORD=... python scripts/backfill_sku_catalog_fields.py
"""

from __future__ import annotations

import argparse
import os
import re
import sqlite3
import sys
from collections import Counter
from pathlib import Path

import load_products as loader

TARGET_COLUMNS = [
    "description",
    "short_description",
    "body_material",
    "trim_material",
    "country_of_origin",
    "dimensions_text",
    "storage_text",
    "lining_care_text",
    "strap_length",
    "handle_drop",
]


def parse_jdbc_url(url: str) -> tuple[str, int, str]:
    match = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", url)
    if not match:
        raise ValueError(f"Cannot parse DB_URL: {url}")
    host, port, database = match.group(1), match.group(2) or "3306", match.group(3)
    return host, int(port), database


def is_blank(value) -> bool:
    return value is None or value == ""


def fetch_sqlite_products(sqlite_path: Path) -> dict[str, dict]:
    connection = sqlite3.connect(str(sqlite_path))
    connection.row_factory = sqlite3.Row
    try:
        rows = connection.execute(
            "SELECT style_number, description, short_description, long_description, "
            "materials_json, country_of_origin, dimensions_json, features_json, "
            "product_attributes_json FROM products"
        )
        return {row["style_number"]: dict(row) for row in rows}
    finally:
        connection.close()


def compute_source_values(product_row: dict) -> dict[str, str | None]:
    """Same field derivation scripts/load_products.py uses for a fresh load, reused here
    so a backfill against existing rows can never drift from what a full reload would
    have produced."""
    materials_json = product_row.get("materials_json")
    features_json = product_row.get("features_json")
    attributes_json = product_row.get("product_attributes_json")
    return {
        "description": product_row.get("description") or product_row.get("long_description"),
        "short_description": product_row.get("short_description"),
        "body_material": loader.extract_material(materials_json, "body"),
        "trim_material": loader.extract_material(materials_json, "trim"),
        "country_of_origin": product_row.get("country_of_origin"),
        "dimensions_text": loader.extract_dimensions_text(product_row.get("dimensions_json")),
        "storage_text": loader.extract_feature_sentences(features_json, ("포켓", "수납")),
        "lining_care_text": loader.extract_feature_sentences(features_json, ("안감",)),
        "strap_length": loader.extract_attribute(attributes_json, "strap_length"),
        "handle_drop": loader.extract_attribute(attributes_json, "handle_drop"),
    }


def plan_updates(sku_rows: list[tuple], products_by_style: dict[str, dict]) -> tuple[list, int, int]:
    """sku_rows: (sku, style_number, *TARGET_COLUMNS current values), one row per sku
    that has at least one blank target column. Returns (planned_updates, unmatched_style,
    no_new_values) where planned_updates is [(sku_id, style_number, {col: new_value})]."""
    planned_updates = []
    unmatched_style = 0
    no_new_values = 0

    for row in sku_rows:
        sku_id, style_number, *current_values = row
        current = dict(zip(TARGET_COLUMNS, current_values))

        product_row = products_by_style.get(style_number)
        if product_row is None:
            unmatched_style += 1
            continue

        source_values = compute_source_values(product_row)
        columns_to_set = {
            col: source_values[col]
            for col in TARGET_COLUMNS
            if is_blank(current[col]) and not is_blank(source_values.get(col))
        }
        if not columns_to_set:
            no_new_values += 1
            continue

        planned_updates.append((sku_id, style_number, columns_to_set))

    return planned_updates, unmatched_style, no_new_values


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sqlite-path", type=Path, default=loader.DEFAULT_SQLITE_PATH)
    parser.add_argument("--dry-run", action="store_true",
                         help="Print how many rows/cells would be updated without touching the DB")
    args = parser.parse_args()

    products_by_style = fetch_sqlite_products(args.sqlite_path)
    print(f"catalog.sqlite products rows: {len(products_by_style)}")

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
        column_list = ", ".join(f"`{col}`" for col in TARGET_COLUMNS)
        blank_check = " OR ".join(f"(`{col}` IS NULL OR `{col}` = '')" for col in TARGET_COLUMNS)

        with connection.cursor() as cursor:
            cursor.execute(
                f"SELECT sku, style_number, {column_list} FROM sku "
                f"WHERE style_number IS NOT NULL AND ({blank_check})"
            )
            sku_rows = cursor.fetchall()

        planned_updates, unmatched_style, no_new_values = plan_updates(sku_rows, products_by_style)

        print(f"sku rows with at least one blank target column: {len(sku_rows)}")
        print(f"  -> style_number not found in catalog.sqlite (left untouched): {unmatched_style}")
        print(f"  -> matched but catalog.sqlite has no new value either (left untouched): {no_new_values}")
        print(f"  -> will be updated: {len(planned_updates)}")

        column_counts = Counter()
        for _, _, columns_to_set in planned_updates:
            column_counts.update(columns_to_set.keys())
        print("  per-column cells to fill:")
        for col in TARGET_COLUMNS:
            print(f"    {col}: {column_counts.get(col, 0)}")

        if args.dry_run:
            for sku_id, style_number, columns_to_set in planned_updates[:10]:
                preview = ", ".join(f"{col}={value!r}" for col, value in columns_to_set.items())
                print(f"    [dry-run] sku={sku_id} style_number={style_number} -> {preview}")
            if len(planned_updates) > 10:
                print(f"    ... and {len(planned_updates) - 10} more")
            return 0

        with connection.cursor() as cursor:
            updated_rows = 0
            updated_cells = 0
            for sku_id, style_number, columns_to_set in planned_updates:
                set_clause = ", ".join(f"`{col}` = %s" for col in columns_to_set)
                guard_clause = " AND ".join(f"(`{col}` IS NULL OR `{col}` = '')" for col in columns_to_set)
                params = [*columns_to_set.values(), sku_id, style_number]
                cursor.execute(
                    f"UPDATE sku SET {set_clause} WHERE sku = %s AND style_number = %s "
                    f"AND ({guard_clause})",
                    params,
                )
                if cursor.rowcount:
                    updated_rows += 1
                    updated_cells += len(columns_to_set)

        connection.commit()
        print(f"Updated {updated_rows} sku row(s), {updated_cells} cell(s) total.")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
