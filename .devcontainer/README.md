# Thykra Docker Dev Environment

A self-contained Linux dev box for the Kotlin Multiplatform + Ktor + React project, with **Claude Code preconfigured to bypass permission prompts** (safe because everything runs in an isolated container).

## What's inside
- Eclipse Temurin **JDK 17** (compiles the project's JVM 11 target, supports Gradle 8.14)
- **Node.js 20** + npm (for `webApp/`)
- **Android SDK** with platforms 34/35 + build-tools (licenses pre-accepted)
- **Git**, build tools, common CLI utilities
- **Claude Code** CLI (`claude`) plus a `cc` shortcut that always passes `--dangerously-skip-permissions`
- Non-root `dev` user with passwordless sudo
- Persistent volumes for Gradle, Android SDK caches, npm modules, and Claude config
- Host Claude config (skills, plugins, settings, agents, commands, hooks, project memory) one-way exported into the container at create time

> iOS targets cannot run inside this container — Apple toolchains require macOS. Server, web, shared (JVM/JS), and Android builds all work.

## Option A — VS Code Dev Container (recommended)
1. Install the **Dev Containers** extension.
2. `Ctrl+Shift+P` → **Dev Containers: Reopen in Container**.
3. After build, open a terminal and run `cc` to launch Claude Code without permission prompts.

## Option B — Plain Docker Compose
```bash
# from project root
docker compose -f docker-compose.dev.yml up -d --build
docker compose -f docker-compose.dev.yml exec dev bash

# inside the container
cc                                       # Claude Code (skip-permissions)
./gradlew :server:run                    # http://localhost:8081
(cd webApp && npm install && npm run start)   # http://localhost:8080
./gradlew :composeApp:assembleDebug      # Android APK
```

## Claude config sharing
Your container's `~/.claude` is a named volume (`thykra-claude-config`), seeded at create time from your host config via `.devcontainer/sync-claude-config.sh`. Two read-only binds source the export:
- `~/.claude` → `/host-claude` (settings, plugins, agents, commands, hooks, project memory)
- `~/.agents` → `/host-agents` (actual skill files — host `~/.claude/skills/*` are symlinks into `~/.agents/skills/*` and would be dead inside Linux)

Container-side state (sessions, history, telemetry, OAuth tokens) stays isolated in the volume — host config is never mutated.

To re-pull changes from the host without rebuilding:
```bash
bash .devcontainer/sync-claude-config.sh
```

**Auth**: a one-time `claude` login is required after each volume reset. On Windows, host OAuth tokens live in Credential Manager (DPAPI) and can't be read from Linux, so they don't transfer. Alternatively, export `ANTHROPIC_API_KEY=sk-ant-...` on the host before `docker compose up` to use API-key auth.

> Project IDs are derived from the working directory path (`G--Projects-Thykra` on host vs `-workspace` in container), so chat histories don't collide. The export script remaps the project memory dir between the two slugs.

## About `--dangerously-skip-permissions`
The `cc` wrapper bypasses every Claude Code permission prompt. This is **only safe inside a disposable, isolated container** like this one — never run it on your host machine. The container has no access to anything outside `/workspace` and the named volumes.

## Ports
| Port | Service        |
|------|----------------|
| 8080 | Vite dev server |
| 8081 | Ktor server     |
| 5005 | JVM remote debug |

## Resetting caches
```bash
docker compose -f docker-compose.dev.yml down -v   # nukes Gradle/Android/npm volumes (host ~/.claude is untouched)
```
