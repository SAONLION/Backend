#!/usr/bin/env python3
"""Load the MCM crawl snapshot (backdata/data/*.ndjson, *.json) into our own
product / sku / product_image ERD tables (see src/main/resources/db/migration).

This intentionally does NOT use backdata/mysql/schema.sql or import_mysql.py —
those build the crawler's own separate schema and are kept only as reference.
Here we map the raw crawl files onto tables our Spring Boot app's JPA entities
(Product, Sku, ProductImage) already expect:

  product_groups.json      -> product        (one row per variant_group_id)
  products.ndjson          -> sku             (one row per style_number/color)
  original_images.ndjson   -> product_image   (one row per style_number+position)
  category_products.ndjson -> product.category (lowest category_position wins)

DB connection is read from the same DB_URL / DB_USERNAME / DB_PASSWORD env vars
Spring's application.yaml uses, so this always targets whatever DB the app itself
is configured for.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import sys
from pathlib import Path

DATA_DIR = Path(__file__).resolve().parent.parent / "backdata" / "data"

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


def load_primary_category_by_style() -> dict[str, str]:
    best: dict[str, tuple[int, str]] = {}
    for row in read_ndjson(DATA_DIR / "category_products.ndjson"):
        style = row["style_number"]
        position = row.get("category_position")
        position = position if position is not None else 10**9
        slug = row.get("category_slug") or "etc"
        current = best.get(style)
        if current is None or position < current[0]:
            best[style] = (position, slug)
    return {style: slug for style, (_, slug) in best.items()}


def format_materials(materials: dict | None) -> str | None:
    if not materials:
        return None
    lines = []
    for key, value in materials.items():
        if not value:
            continue
        label = MATERIAL_LABELS.get(key, key)
        lines.append(f"{label}: {value}")
    return "\n".join(lines) if lines else None


def build_product_rows(groups: list[dict], products_by_style: dict[str, dict],
                        category_by_style: dict[str, str]) -> list[dict]:
    rows = []
    for group in groups:
        variant_group_id = group["variant_group_id"]
        candidate_styles = [variant_group_id, *group.get("listed_styles", []),
                             *group.get("all_variant_styles", [])]
        representative = next((s for s in candidate_styles if s in products_by_style), None)
        rep = products_by_style.get(representative, {})

        category = category_by_style.get(representative) or category_by_style.get(variant_group_id) or "etc"
        rows.append({
            "variant_group_id": variant_group_id,
            "name": (group.get("name") or rep.get("name") or variant_group_id)[:100],
            "category": category[:50],
            "material_desc": format_materials(rep.get("materials")),
            "heritage_desc": rep.get("description") or rep.get("long_description"),
        })
    return rows


def build_sku_rows(products: list[dict], known_group_ids: set[str]) -> list[dict]:
    rows = []
    skipped = 0
    for product in products:
        variant_group_id = product.get("variant_group_id") or product["style_number"]
        if variant_group_id not in known_group_ids:
            skipped += 1
            continue
        sizes = product.get("sizes") or []
        size_value = ",".join(sizes)[:255] if sizes else None
        rows.append({
            "style_number": product["style_number"],
            "variant_group_id": variant_group_id,
            "color": (product.get("current_color") or None),
            "size": size_value,
            "price": product.get("price"),
        })
    if skipped:
        print(f"WARNING: skipped {skipped} sku rows with no matching product group", file=sys.stderr)
    return rows


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--dry-run", action="store_true",
                         help="Parse the crawl files and print counts without touching the DB")
    parser.add_argument("--truncate", action="store_true",
                         help="Truncate product/sku/product_image before loading (destructive, local dev only)")
    args = parser.parse_args()

    groups = json.loads((DATA_DIR / "product_groups.json").read_text(encoding="utf-8"))
    products = list(read_ndjson(DATA_DIR / "products.ndjson"))
    images = list(read_ndjson(DATA_DIR / "original_images.ndjson"))
    category_by_style = load_primary_category_by_style()

    products_by_style = {p["style_number"]: p for p in products}
    known_group_ids = {g["variant_group_id"] for g in groups}

    product_rows = build_product_rows(groups, products_by_style, category_by_style)
    sku_rows = build_sku_rows(products, known_group_ids)
    sku_style_set = {row["style_number"] for row in sku_rows}
    image_rows = [img for img in images if img["style_number"] in sku_style_set]

    print(f"product rows to insert:       {len(product_rows)}")
    print(f"sku rows to insert:           {len(sku_rows)}")
    print(f"product_image rows to insert: {len(image_rows)} (of {len(images)} raw image records)")

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

            if args.truncate:
                cursor.execute("SET FOREIGN_KEY_CHECKS=0")
                for table in ("product_image", "sku", "product"):
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

            style_to_sku_id: dict[str, int] = {}
            next_sku_id = 1
            for row in sku_rows:
                product_id = variant_group_to_product_id[row["variant_group_id"]]
                sku_id = next_sku_id
                next_sku_id += 1
                cursor.execute(
                    "INSERT INTO sku (sku, product_id, color, size, price, stock_qty, created_at, updated_at) "
                    "VALUES (%s, %s, %s, %s, %s, NULL, NOW(), NOW())",
                    (sku_id, product_id, row["color"], row["size"], row["price"]),
                )
                style_to_sku_id[row["style_number"]] = sku_id

            next_image_id = 1
            inserted_images = 0
            for image in image_rows:
                sku_id = style_to_sku_id[image["style_number"]]
                image_id = next_image_id
                next_image_id += 1
                cursor.execute(
                    "INSERT INTO product_image (image_id, sku, image_url, sort_order, created_at, updated_at) "
                    "VALUES (%s, %s, %s, %s, NOW(), NOW())",
                    (image_id, sku_id, image["source_url"], image["position"]),
                )
                inserted_images += 1

        connection.commit()
        print(f"Loaded: product={len(variant_group_to_product_id)}, "
              f"sku={len(style_to_sku_id)}, product_image={inserted_images}")
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return 0


if __name__ == "__main__":
    raise SystemExit(main())