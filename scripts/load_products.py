#!/usr/bin/env python3
"""Load final_backdata/data/catalog.sqlite's product_groups / products / product_variants
into our own product / sku ERD tables (see src/main/resources/db/migration).

Supersedes scripts/import_backdata.py's product/sku loading: sources from catalog.sqlite
instead of the scattered *.ndjson/*.json files, and additionally back-fills
sku.style_number, which import_backdata.py never set. sku.style_number is what lets
sku_image (loaded separately by scripts/load_sku_image.py) join to a scanned sku.

  product_groups (sqlite)   -> product        (one row per variant_group_id)
  products (sqlite)         -> sku            (one row per style_number)
  product_variants (sqlite) -> sku.color cross-check (products.current_color is used;
                                both are identical for all 632 rows as of this writing)
  category_products.ndjson  -> product.category (lowest category_position wins)

catalog.sqlite's own categories/product_categories tables only carry Korean display
names, not the English slug already stored in product.category (e.g.
"lifestyle_home_decor"), so category still comes from the ndjson file kept alongside
catalog.sqlite in final_backdata/data/.

Out of scope: product_image and sku_image. product_image already exists (loaded by
import_backdata.py) and is keyed by the *previous* sku ids -- if you --truncate here,
those rows will silently point at the wrong skus until product_image is reloaded too.
sku_image is unaffected by sku id churn since it joins on style_number, not sku id.

DB connection is read from the same DB_URL / DB_USERNAME / DB_PASSWORD env vars Spring's
application.yaml uses, so this always targets whatever DB the app itself is configured
for.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sqlite3
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SQLITE_PATH = REPO_ROOT / "final_backdata" / "data" / "catalog.sqlite"
DEFAULT_CATEGORY_NDJSON_PATH = REPO_ROOT / "final_backdata" / "data" / "category_products.ndjson"

MATERIAL_LABELS = {
    "body": "바디",
    "trim": "트림",
    "lining": "안감",
    "hardware": "하드웨어",
}


def parse_jdbc_url(url: str) -> tuple[str, int, str]:
    match = re.match(r"jdbc:mysql://([^:/]+)(?::(\d+))?/([^?]+)", url)
    if not match:
        raise ValueError(f"Cannot parse DB_URL: {url}")
    host, port, database = match.group(1), match.group(2) or "3306", match.group(3)
    return host, int(port), database


def read_ndjson(path: Path):
    with path.open("r", encoding="utf-8") as handle:
        for line in handle:
            line = line.strip()
            if line:
                yield json.loads(line)


def load_primary_category_by_style(path: Path) -> dict[str, str]:
    best: dict[str, tuple[int, str]] = {}
    for row in read_ndjson(path):
        style = row["style_number"]
        position = row.get("category_position")
        position = position if position is not None else 10**9
        slug = row.get("category_slug") or "etc"
        current = best.get(style)
        if current is None or position < current[0]:
            best[style] = (position, slug)
    return {style: slug for style, (_, slug) in best.items()}


def format_materials(materials_json: str | None) -> str | None:
    if not materials_json:
        return None
    materials = json.loads(materials_json)
    if not materials:
        return None
    lines = []
    for key, value in materials.items():
        if not value:
            continue
        label = MATERIAL_LABELS.get(key, key)
        lines.append(f"{label}: {value}")
    return "\n".join(lines) if lines else None


def fetch_sqlite_rows(sqlite_path: Path) -> tuple[list[dict], list[dict], list[dict]]:
    connection = sqlite3.connect(str(sqlite_path))
    connection.row_factory = sqlite3.Row
    try:
        groups = [dict(row) for row in connection.execute(
            "SELECT variant_group_id, name, listed_styles_json, all_variant_styles_json "
            "FROM product_groups"
        )]
        products = [dict(row) for row in connection.execute(
            "SELECT style_number, variant_group_id, price, current_color, sizes_json, "
            "materials_json, description, long_description FROM products"
        )]
        variants = [dict(row) for row in connection.execute(
            "SELECT variant_group_id, style_number, color FROM product_variants"
        )]
        return groups, products, variants
    finally:
        connection.close()


def build_product_rows(groups: list[dict], products_by_style: dict[str, dict],
                        category_by_style: dict[str, str]) -> list[dict]:
    rows = []
    for group in groups:
        variant_group_id = group["variant_group_id"]
        listed_styles = json.loads(group["listed_styles_json"] or "[]")
        all_variant_styles = json.loads(group["all_variant_styles_json"] or "[]")
        candidate_styles = [variant_group_id, *listed_styles, *all_variant_styles]
        representative = next((s for s in candidate_styles if s in products_by_style), None)
        rep = products_by_style.get(representative, {})

        category = category_by_style.get(representative) or category_by_style.get(variant_group_id) or "etc"
        rows.append({
            "variant_group_id": variant_group_id,
            "name": (group.get("name") or rep.get("name") or variant_group_id)[:100],
            "category": category[:50],
            "material_desc": format_materials(rep.get("materials_json")),
            "heritage_desc": rep.get("description") or rep.get("long_description"),
        })
    return rows


def build_sku_rows(products: list[dict], variant_color_by_style: dict[str, str],
                    known_group_ids: set[str]) -> list[dict]:
    rows = []
    skipped = 0
    mismatched_color = 0
    for product in products:
        variant_group_id = product.get("variant_group_id") or product["style_number"]
        if variant_group_id not in known_group_ids:
            skipped += 1
            continue
        style_number = product["style_number"]
        if variant_color_by_style.get(style_number) != product.get("current_color"):
            mismatched_color += 1
        sizes = json.loads(product["sizes_json"] or "[]")
        size_value = ",".join(sizes)[:255] if sizes else None
        rows.append({
            "style_number": style_number,
            "variant_group_id": variant_group_id,
            "color": product.get("current_color"),
            "size": size_value,
            "price": product.get("price"),
        })
    if skipped:
        print(f"WARNING: skipped {skipped} sku rows with no matching product group", file=sys.stderr)
    if mismatched_color:
        print(f"WARNING: {mismatched_color} sku row(s) where products.current_color "
              f"differs from product_variants.color", file=sys.stderr)
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--sqlite-path", type=Path, default=DEFAULT_SQLITE_PATH)
    parser.add_argument("--category-ndjson-path", type=Path, default=DEFAULT_CATEGORY_NDJSON_PATH)
    parser.add_argument("--dry-run", action="store_true",
                         help="Parse catalog.sqlite and print counts without touching the DB")
    parser.add_argument("--truncate", action="store_true",
                         help="Truncate product/sku before loading (destructive; local dev only -- "
                              "see module docstring about product_image going stale)")
    args = parser.parse_args()

    groups, products, variants = fetch_sqlite_rows(args.sqlite_path)
    category_by_style = load_primary_category_by_style(args.category_ndjson_path)

    products_by_style = {p["style_number"]: p for p in products}
    variant_color_by_style = {v["style_number"]: v["color"] for v in variants}
    known_group_ids = {g["variant_group_id"] for g in groups}

    product_rows = build_product_rows(groups, products_by_style, category_by_style)
    sku_rows = build_sku_rows(products, variant_color_by_style, known_group_ids)

    print(f"product rows to insert: {len(product_rows)}")
    print(f"sku rows to insert:     {len(sku_rows)} (all with style_number set)")

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
            cursor.execute("SELECT COUNT(*) FROM product")
            existing = cursor.fetchone()[0]
            if existing and not args.truncate:
                sys.exit(f"product table already has {existing} row(s). "
                         f"Re-run with --truncate to replace, or clear it manually first.")

            cursor.execute("SELECT COUNT(*) FROM product_image")
            existing_images = cursor.fetchone()[0]
            if existing and args.truncate and existing_images:
                print(f"WARNING: product_image has {existing_images} existing row(s) tied to the sku ids "
                      f"being replaced -- those rows will now point at the wrong skus. Truncate/reload "
                      f"product_image too (scripts/import_backdata.py) before relying on it.",
                      file=sys.stderr)

            if args.truncate:
                cursor.execute("SET FOREIGN_KEY_CHECKS=0")
                for table in ("sku", "product"):
                    cursor.execute(f"TRUNCATE TABLE `{table}`")
                cursor.execute("SET FOREIGN_KEY_CHECKS=1")

            variant_group_to_product_id: dict[str, int] = {}
            for row in product_rows:
                cursor.execute(
                    "INSERT INTO product (name, category, material_desc, heritage_desc, created_at, updated_at) "
                    "VALUES (%s, %s, %s, %s, NOW(), NOW())",
                    (row["name"], row["category"], row["material_desc"], row["heritage_desc"]),
                )
                variant_group_to_product_id[row["variant_group_id"]] = cursor.lastrowid

            next_sku_id = 1
            inserted_skus = 0
            for row in sku_rows:
                product_id = variant_group_to_product_id[row["variant_group_id"]]
                sku_id = next_sku_id
                next_sku_id += 1
                cursor.execute(
                    "INSERT INTO sku (sku, product_id, style_number, color, size, price, stock_qty, "
                    "created_at, updated_at) VALUES (%s, %s, %s, %s, %s, %s, NULL, NOW(), NOW())",
                    (sku_id, product_id, row["style_number"], row["color"], row["size"], row["price"]),
                )
                inserted_skus += 1

        connection.commit()
        print(f"Loaded: product={len(variant_group_to_product_id)}, sku={inserted_skus}")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())
