#!/usr/bin/env bash
# LocalStack runs this once after S3 is ready (per INIT_SCRIPTS_DIR=/etc/localstack/init/ready.d).
# Creates the media bucket the server expects.
set -euo pipefail

BUCKET="${S3_BUCKET:-thykra-media}"
REGION="${AWS_DEFAULT_REGION:-us-east-1}"

awslocal s3api create-bucket \
    --bucket "$BUCKET" \
    --region "$REGION" \
    ${REGION:+--create-bucket-configuration "LocationConstraint=$REGION"} \
    2>/dev/null \
    || echo "Bucket $BUCKET already exists, continuing."

# Wide-open public-read for dev convenience. Real prod uses a CDN + signed URLs.
awslocal s3api put-bucket-acl --bucket "$BUCKET" --acl public-read 2>/dev/null || true

echo "LocalStack S3 bucket ready: $BUCKET"
