#!/usr/bin/env bash
# One-way export of host Claude config into the container's ~/.claude volume.
# Runs at container create + on demand (`bash .devcontainer/sync-claude-config.sh`).
# Skips writable-state dirs (sessions, history, projects, telemetry, cache, etc.)
# so container-side activity stays isolated and host activity is not overwritten.

set -euo pipefail

SRC="/host-claude"
AGENTS_SRC="/host-agents"
DST="$HOME/.claude"

if [ ! -d "$SRC" ]; then
  echo "[sync-claude-config] $SRC not mounted — skipping"
  exit 0
fi

mkdir -p "$DST"

# Items copied straight from ~/.claude
EXPORT_PATHS=(
  "plugins"
  "agents"
  "commands"
  "hooks"
  "settings.json"
  "settings.local.json"
  "keybindings.json"
  "CLAUDE.md"
)

for p in "${EXPORT_PATHS[@]}"; do
  if [ -e "$SRC/$p" ]; then
    echo "[sync-claude-config] exporting $p"
    rm -rf "$DST/$p"
    cp -rL "$SRC/$p" "$DST/$p" 2>/dev/null || cp -r "$SRC/$p" "$DST/$p"
  fi
done

# Skills: host has them as symlinks under ~/.claude/skills/<name> pointing to
# ~/.agents/skills/<name>. Symlinks don't resolve inside the container, so copy
# from ~/.agents/skills directly.
if [ -d "$AGENTS_SRC/skills" ]; then
  echo "[sync-claude-config] exporting skills from /host-agents/skills"
  rm -rf "$DST/skills"
  cp -rL "$AGENTS_SRC/skills" "$DST/skills" 2>/dev/null || cp -r "$AGENTS_SRC/skills" "$DST/skills"
fi

# Per-project memory: host stores it under projects/<host-slug>/memory.
# Container working dir is /workspace → slug is "-workspace". Remap so MEMORY.md
# is found at the path the runtime derives from cwd.
HOST_PROJECT_MEM="$SRC/projects/G--Projects-Thykra/memory"
CONTAINER_PROJECT_MEM="$DST/projects/-workspace/memory"
if [ -d "$HOST_PROJECT_MEM" ]; then
  echo "[sync-claude-config] exporting project memory → -workspace"
  mkdir -p "$CONTAINER_PROJECT_MEM"
  cp -r "$HOST_PROJECT_MEM/." "$CONTAINER_PROJECT_MEM/"
fi

echo "[sync-claude-config] done"
