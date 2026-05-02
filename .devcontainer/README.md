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

## Authenticating Claude Code
Either:
- Export `ANTHROPIC_API_KEY=sk-ant-...` on the host before `docker compose up`, or
- Run `claude` once interactively and complete the OAuth flow — credentials are persisted in the `claude-config` named volume.

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
docker compose -f docker-compose.dev.yml down -v   # nukes Gradle/Android/npm/Claude volumes
```
