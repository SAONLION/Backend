#!/usr/bin/env python3

import argparse
import boto3
from botocore.exceptions import ClientError


def fix_content_type(bucket: str, prefix: str, region: str):
    s3 = boto3.client("s3", region_name=region)

    paginator = s3.get_paginator("list_objects_v2")

    total = 0
    changed = 0
    skipped = 0
    failed = 0

    print(f"Bucket: {bucket}")
    print(f"Prefix: {prefix}")
    print("Starting...")
    print()

    for page in paginator.paginate(Bucket=bucket, Prefix=prefix):
        for obj in page.get("Contents", []):
            key = obj["Key"]

            if key.endswith("/"):
                continue

            total += 1

            try:
                head = s3.head_object(
                    Bucket=bucket,
                    Key=key
                )

                current_type = head.get("ContentType", "")

                if current_type == "image/webp":
                    skipped += 1
                    print(f"[SKIP] {key} (already image/webp)")
                    continue

                copy_args = {
                    "Bucket": bucket,
                    "Key": key,
                    "CopySource": {
                        "Bucket": bucket,
                        "Key": key
                    },
                    "MetadataDirective": "REPLACE",
                    "Metadata": head.get("Metadata", {}),
                    "ContentType": "image/webp",
                }

                # 기존 값이 실제로 존재하는 경우에만 전달
                for param in (
                        "CacheControl",
                        "ContentDisposition",
                        "ContentEncoding",
                        "ContentLanguage",
                        "Expires",
                        "WebsiteRedirectLocation",
                ):
                    value = head.get(param)
                    if value is not None:
                        copy_args[param] = value

                s3.copy_object(**copy_args)


                # 실제로 변경됐는지 즉시 확인
                verify = s3.head_object(
                    Bucket=bucket,
                    Key=key
                )

                if verify.get("ContentType") == "image/webp":
                    changed += 1
                    print(
                        f"[CHANGED] {key}: "
                        f"{current_type} -> image/webp"
                    )
                else:
                    failed += 1
                    print(
                        f"[FAILED] {key}: "
                        f"Content-Type is still {verify.get('ContentType')}"
                    )

            except ClientError as e:
                failed += 1
                print(f"[FAILED] {key}: {e}")

    print()
    print("===== RESULT =====")
    print(f"total   = {total}")
    print(f"changed = {changed}")
    print(f"skipped = {skipped}")
    print(f"failed  = {failed}")


def main():
    parser = argparse.ArgumentParser()

    parser.add_argument(
        "--bucket",
        required=True,
        help="S3 bucket name"
    )

    parser.add_argument(
        "--prefix",
        default="products/",
        help="S3 prefix to process"
    )

    parser.add_argument(
        "--region",
        default="ap-northeast-2",
        help="AWS region"
    )

    args = parser.parse_args()

    fix_content_type(
        args.bucket,
        args.prefix,
        args.region
    )


if __name__ == "__main__":
    main()
