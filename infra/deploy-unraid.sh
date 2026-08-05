#!/usr/bin/env bash
# Deploy Thykra to an Unraid server over SSH.
#
#   bash infra/deploy-unraid.sh
#
# Streams the sources this stack needs to the server via tar-over-SSH (Unraid
# has no git and Git Bash has no rsync), then builds and starts the Compose
# stack there. Re-running it is a redeploy: images rebuild, volumes persist.
#
# Requires .env.unraid to exist locally (copy from .env.unraid.example).
set -euo pipefail

SSH_HOST="${SSH_HOST:-root@tower.local}"
REMOTE_DIR="${REMOTE_DIR:-/mnt/user/appdata/thykra/src}"
ENV_FILE="${ENV_FILE:-.env.unraid}"
COMPOSE_FILE="docker-compose.unraid.yml"
COMPOSE_VERSION="${COMPOSE_VERSION:-5.4.0}"

cd "$(dirname "$0")/.."

[ -f "$ENV_FILE" ] || {
    echo "ERROR: $ENV_FILE not found. Run: cp .env.unraid.example $ENV_FILE" >&2
    exit 1
}

echo "==> Checking SSH access to $SSH_HOST"
ssh -o BatchMode=yes -o ConnectTimeout=10 "$SSH_HOST" 'echo ok' >/dev/null

echo "==> Verifying Docker + Compose on the server"
# Unraid ships Docker but not Compose, and its root filesystem is tmpfs — so the
# plugin vanishes on every reboot. Reinstall it on demand instead of keeping a
# ~60MB binary on the FAT32 USB stick.
ssh "$SSH_HOST" "
    set -e
    if docker compose version >/dev/null 2>&1; then exit 0; fi
    echo '    Compose missing (tmpfs reset?) — installing v${COMPOSE_VERSION}'
    mkdir -p /usr/libexec/docker/cli-plugins
    curl -fsSL -o /usr/libexec/docker/cli-plugins/docker-compose \
        'https://github.com/docker/compose/releases/download/v${COMPOSE_VERSION}/docker-compose-linux-x86_64'
    chmod +x /usr/libexec/docker/cli-plugins/docker-compose
    docker compose version >/dev/null
"
COMPOSE_CMD="docker compose"
echo "    using: $COMPOSE_CMD"

echo "==> Syncing sources to $SSH_HOST:$REMOTE_DIR"
ssh "$SSH_HOST" "mkdir -p '$REMOTE_DIR'"

# Only what the two images actually need. Build outputs and dependency dirs are
# excluded — they are regenerated inside the images and would bloat the stream.
tar -cz \
    --exclude='node_modules' \
    --exclude='build' \
    --exclude='.gradle' \
    --exclude='.kotlin' \
    --exclude='dist' \
    --exclude='data' \
    gradlew gradlew.bat gradle settings.gradle.kts build.gradle.kts gradle.properties \
    shared server webApp infra \
    "$COMPOSE_FILE" .dockerignore "$ENV_FILE" \
  | ssh "$SSH_HOST" "tar -xz -C '$REMOTE_DIR'"

echo "==> Building and starting the stack (this takes a while on first run)"
ssh "$SSH_HOST" "cd '$REMOTE_DIR' && DOCKER_BUILDKIT=1 COMPOSE_DOCKER_CLI_BUILD=1 \
    $COMPOSE_CMD -f '$COMPOSE_FILE' --env-file '$ENV_FILE' up -d --build"

echo "==> Container status"
ssh "$SSH_HOST" "cd '$REMOTE_DIR' && $COMPOSE_CMD -f '$COMPOSE_FILE' --env-file '$ENV_FILE' ps"

echo
echo "Done. Tail logs with:"
echo "  ssh $SSH_HOST \"cd $REMOTE_DIR && $COMPOSE_CMD -f $COMPOSE_FILE --env-file $ENV_FILE logs -f\""
