#!/bin/sh
# Bootstraps the MinIO bucket the server expects. Runs once as a short-lived
# init container (see the `minio-init` service), then exits 0.
#
# Idempotent: safe to re-run on every `docker compose up`.
set -eu

BUCKET="${S3_BUCKET:-thykra-media}"
MINIO_URL="${MINIO_INTERNAL_URL:-http://minio:9000}"

echo "Waiting for MinIO at $MINIO_URL ..."
until mc alias set thykra "$MINIO_URL" "$MINIO_ROOT_USER" "$MINIO_ROOT_PASSWORD" >/dev/null 2>&1; do
    sleep 2
done
echo "MinIO reachable."

if mc ls "thykra/$BUCKET" >/dev/null 2>&1; then
    echo "Bucket $BUCKET already exists."
else
    mc mb "thykra/$BUCKET"
    echo "Created bucket $BUCKET."
fi

# The web/mobile clients load images and video with plain <img>/<video> tags
# against the public base URL, so objects must be anonymously readable.
#
# Deliberately NOT `mc anonymous set download` — that also grants s3:ListBucket,
# which lets anyone enumerate every key in the bucket. Combined with anonymous
# GetObject that would expose every photo in every album to anyone who can reach
# the port. Objects are addressed by unguessable UUID keys, so GetObject alone
# is what the app actually needs. Writes still require a presigned URL signed
# with the server's credentials.
cat > /tmp/anon-policy.json <<JSON
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "AWS": ["*"] },
      "Action": ["s3:GetObject"],
      "Resource": ["arn:aws:s3:::$BUCKET/*"]
    }
  ]
}
JSON
mc anonymous set-json /tmp/anon-policy.json "thykra/$BUCKET"
echo "Anonymous GetObject (no listing) enabled on $BUCKET."

echo "MinIO bucket ready: $BUCKET"
