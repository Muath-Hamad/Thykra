# Thykra

*Travel together. Remember forever.* — a shared trip photo-album app for
friends, couples, and families. Kotlin Multiplatform targeting Android, iOS,
Web, and a Ktor server.

> **The web app was rebuilt (2026-08-06)** as **Wanderlust Editions** — an
> editorial, print-inspired redesign with Paper/Darkroom themes, day-chapter
> galleries, a Darkroom lightbox, full Arabic/RTL support, and new server
> APIs (invite previews, multi-use links, activity feeds, recaps).
> See [docs/Wanderlust-Editions.md](./docs/Wanderlust-Editions.md).

## Documentation

| Doc | What it covers |
|---|---|
| [docs/Wanderlust-Editions.md](./docs/Wanderlust-Editions.md) | The web redesign: design system, IA, new APIs, decisions, verification |
| [docs/Actions.md](./docs/Actions.md) | Action items that need a human (credentials, hosting, merges, product calls) |
| [docs/TASKS.md](./docs/TASKS.md) | Development task tracker across all phases and platforms |
| [docs/UX_guide.md](./docs/UX_guide.md) | Original MVP UI/UX spec — still current for Android/iOS, superseded for web |
| [docs/pitch.md](./docs/pitch.md) | Product pitch |
| [docs/Design-Brief-2026-08.md](./docs/Design-Brief-2026-08.md) | _Archived_ — the brief that commissioned the web redesign, kept for design rationale |

## Repository layout

* [`/webApp`](./webApp) — the React + TypeScript web app (Vite, TanStack
  Router, CSS Modules). Rebuilt as Wanderlust Editions; talks to the server
  via the shared DTO contract.
* [`/server`](./server/src/main/kotlin) — the Ktor server (Exposed +
  PostgreSQL, JWT auth, presigned media uploads, invites, activity, recaps).
* [`/shared`](./shared/src) — Kotlin Multiplatform shared code; the DTO
  module is the API contract for all clients (additive-only changes).
* [`/composeApp`](./composeApp/src) — Compose Multiplatform code for the
  Android and iOS apps.
* [`/iosApp`](./iosApp/iosApp) — the iOS entry point / SwiftUI shell.

## Build and run

### Server

```shell
./gradlew :server:run
```

Configuration via environment (see `server/src/main/resources/application.yaml`):
`DATABASE_URL`, `JWT_SECRET`, `GOOGLE_CLIENT_ID`, storage settings.
`ALLOW_DEV_LOGIN=true` enables a dev-only login backdoor for local testing —
never set it in production.

### Web app

```shell
cd webApp
npm install
npm run start      # dev server (proxies /api to localhost:8081)
npm run build      # production build (typecheck + vite)
```

Set `VITE_GOOGLE_CLIENT_ID` for the Google sign-in button to render.

### Android

```shell
./gradlew :composeApp:assembleDebug
```

### iOS

Open [`/iosApp`](./iosApp) in Xcode and run, or use the IDE run configuration.

## Tests

```shell
./gradlew :server:test            # 39 server integration tests (H2 in-memory)
cd webApp && npx vitest run       # 44 unit tests (chapters, layout, formatting)
cd webApp && npx tsc --noEmit     # typecheck
./gradlew :composeApp:testDebugUnitTest   # headless Compose UI tests (Robolectric)
```

An 18-check Playwright E2E smoke (create trip → upload → invite → join →
share → RTL) runs against the real stack; see
[docs/Wanderlust-Editions.md §6](./docs/Wanderlust-Editions.md) for what it
covers.

> **CI note:** full multiplatform builds need `dl.google.com` reachable
> (Android Gradle Plugin). Server and web targets build without it.

## Production-like local stack (Docker)

Brings up Postgres, LocalStack S3, and the Ktor server, all wired together
via Docker Compose.

```shell
cp .env.prod.example .env.prod
# edit .env.prod — set JWT_SECRET, DATABASE_PASSWORD, GOOGLE_CLIENT_ID
docker compose -f docker-compose.prod.yml --env-file .env.prod up --build
```

The server boots on `http://localhost:${SERVER_PORT:-8081}` and writes media
to a LocalStack-hosted S3 bucket reachable at
`http://localhost:4566/thykra-media`. State persists across `up`/`down`
cycles in named Docker volumes (`thykra_postgres_data`,
`thykra_localstack_data`); `docker compose down -v` wipes everything.

For real production, swap `STORAGE_TYPE=s3` to point at a real AWS S3
bucket — leave `S3_ENDPOINT` unset, drop the `localstack` service, and use
the AWS default credential chain (instance role) by clearing
`S3_ACCESS_KEY`/`S3_SECRET_KEY`.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…
