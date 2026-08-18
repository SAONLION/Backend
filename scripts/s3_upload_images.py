#!/usr/bin/env python3
"""One-off migration: upload final_backdata/images_original/** to S3 and write a
manifest CSV (style_number, position, image_url) mapping each classified image
to its uploaded S3 URL. Run this once before load_sku_image.py.

Source file list comes from final_backdata/data/catalog.sqlite's original_images
table (style_number, position, local_path), not a directory walk, so the
manifest only ever covers images the crawler actually classified.

Credentials: uses boto3's default credential chain (env vars, ~/.aws/credentials
profile, or an EC2 instance IAM role) -- never hardcode keys here or pass them
on the command line.
"""

from __future__ import annotations

import argparse
import csv
import mimetypes
import os
import sqlite3
import sys
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SQLITE_PATH = REPO_ROOT / "finale_backdata" / "data" / "catalog.sqlite"
DEFAULT_IMAGES_ROOT = REPO_ROOT / "finale_backdata"
DEFAULT_MANIFEST_OUT = REPO_ROOT / "finale_backdata" / "data" / "s3_manifest.csv"


def fetch_image_rows(sqlite_path: Path) -> list[dict]:
    connection = sqlite3.connect(str(sqlite_path))
    connection.row_factory = sqlite3.Row
    try:
        cursor = connection.execute(
            "SELECT style_number, position, local_path FROM original_images ORDER BY style_number, position"
        )
        return [dict(row) for row in cursor.fetchall()]
    finally:
        connection.close()


def build_s3_key(prefix: str, style_number: str, local_path: str) -> str:
    filename = Path(local_path).name
    return f"{prefix}/{style_number}/{filename}"


def upload_all(rows: list[dict], images_root: Path, bucket: str, region: str, prefix: str,
                force: bool, dry_run: bool) -> list[dict]:
    import boto3
    from botocore.exceptions import ClientError

    s3 = boto3.client("s3", region_name=region)
    manifest_rows = []
    uploaded = skipped_existing = missing = 0

    for i, row in enumerate(rows, start=1):
        local_path = images_root / row["local_path"]
        s3_key = build_s3_key(prefix, row["style_number"], row["local_path"])
        image_url = f"https://{bucket}.s3.{region}.amazonaws.com/{s3_key}"

        if not local_path.exists():
            print(f"WARNING: missing local file, skipping: {local_path}", file=sys.stderr)
            missing += 1
            continue

        if not dry_run:
            if not force:
                try:
                    s3.head_object(Bucket=bucket, Key=s3_key)
                    skipped_existing += 1
                    manifest_rows.append({**row, "s3_key": s3_key, "image_url": image_url})
                    continue
                except ClientError as e:
                    if e.response["Error"]["Code"] not in ("404", "NoSuchKey"):
                        raise

            content_type = mimetypes.guess_type(str(local_path))[0] or "application/octet-stream"
            s3.upload_file(str(local_path), bucket, s3_key, ExtraArgs={"ContentType": content_type})
            uploaded += 1

        manifest_rows.append({**row, "s3_key": s3_key, "image_url": image_url})

        if i % 200 == 0:
            print(f"  ...{i}/{len(rows)} processed", file=sys.stderr)

    print(f"uploaded={uploaded} already_existed={skipped_existing} missing_local_file={missing} "
          f"total_in_manifest={len(manifest_rows)}")
    return manifest_rows


def write_manifest(manifest_rows: list[dict], manifest_out: Path) -> None:
    manifest_out.parent.mkdir(parents=True, exist_ok=True)
    with manifest_out.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=["style_number", "position", "s3_key", "image_url"])
        writer.writeheader()
        for row in manifest_rows:
            writer.writerow({
                "style_number": row["style_number"],
                "position": row["position"],
                "s3_key": row["s3_key"],
                "image_url": row["image_url"],
            })


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--sqlite-path", type=Path, default=DEFAULT_SQLITE_PATH)
    parser.add_argument("--images-root", type=Path, default=DEFAULT_IMAGES_ROOT,
                         help="Directory local_path values (e.g. images_original/XXX/01-....webp) are relative to")
    parser.add_argument("--manifest-out", type=Path, default=DEFAULT_MANIFEST_OUT)
    parser.add_argument("--bucket", default=os.environ.get("S3_BUCKET_NAME"))
    parser.add_argument("--region", default=os.environ.get("AWS_REGION", "ap-northeast-2"))
    parser.add_argument("--prefix", default="products", help="S3 key prefix images are uploaded under")
    parser.add_argument("--force", action="store_true", help="Re-upload even if the S3 key already exists")
    parser.add_argument("--dry-run", action="store_true",
                         help="Only print counts and write the manifest with computed URLs; skip S3 calls")
    args = parser.parse_args()

    if not args.bucket:
        sys.exit("S3 bucket is required: pass --bucket or set S3_BUCKET_NAME")

    rows = fetch_image_rows(args.sqlite_path)
    print(f"{len(rows)} classified image rows found in {args.sqlite_path}")

    manifest_rows = upload_all(rows, args.images_root, args.bucket, args.region, args.prefix,
                                args.force, args.dry_run)
    write_manifest(manifest_rows, args.manifest_out)
    print(f"manifest written: {args.manifest_out}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())