# Thykra — Development Task Tracker

> **Platforms:** Android, iOS, Web (React/TS), Ktor Server
> **Architecture:** Kotlin Multiplatform shared logic + platform-specific UIs

**Legend:** `[ ]` Pending | `[~]` In Progress | `[x]` Done

---

## Phase 1 — Foundation & Auth

> Core project setup, authentication, and user management. Everything else depends on this.

### 1.1 Project Setup
- [x] Configure KMP shared module (targets: Android, iOS, JS)
- [x] Set up Ktor server project with environment config
- [x] Set up Android Compose app shell with navigation
- [x] Set up iOS Compose app shell with navigation
- [x] Set up Web (React + TS + Vite) app shell with routing
- [ ] Set up CI/CD pipeline (build + test for all targets)

### 1.2 Database & Storage
- [x] Design database schema (users, albums, media, reactions, comments, invites)
- [x] Set up PostgreSQL with Exposed on Ktor
- [ ] Set up cloud storage (S3/GCS) for media uploads
- [x] Define API contract (shared DTO module)

### 1.3 Authentication
- [x] Server: Auth endpoints (OAuth login, refresh, logout)
- [x] Server: JWT token generation and validation
- [x] Server: OAuth2 (Google, Apple Sign-In) integration
- [x] Shared: Auth token storage and refresh logic
- [x] Android: Login screen with Google Sign-In
- [x] iOS: Login screen with Apple Sign-In
- [x] Web: Login page with Google Sign-In

### 1.4 User Profile
- [x] Server: Profile CRUD endpoints (name, avatar)
- [x] Shared: Profile data models
- [x] Android: Profile screen (view + edit)
- [x] iOS: Profile screen (view + edit)
- [x] Web: Profile page (view + edit)

---

## Phase 2 — Shared Albums (Core Feature)

> The heart of Thykra — creating, joining, and contributing to shared trip albums.

### 2.1 Album Management
- [x] Server: Album CRUD endpoints (create, read, update, delete)
- [x] Server: Album invitation system (invite by link / user search)
- [x] Server: Album membership roles (owner, contributor, viewer)
- [x] Shared: Album data models and state management
- [x] Android: Create album flow (name, cover, date range, invite members)
- [x] iOS: Create album flow
- [x] Web: Create album flow
- [x] Android: Album list / home feed screen
- [x] iOS: Album list / home feed screen
- [x] Web: Album list / home feed page

### 2.2 Media Upload & Display
- [x] Server: Media upload endpoints (photos, videos) with presigned URLs
- [x] Server: Image processing pipeline (thumbnails, compression, EXIF extraction)
- [x] Shared: Upload queue manager with retry logic
- [x] Android: Photo/video picker and upload UI
- [x] iOS: Photo/video picker and upload UI
- [x] Web: Drag-and-drop / file picker upload UI
- [x] Android: Album media grid view with lazy loading
- [x] iOS: Album media grid view with lazy loading
- [x] Web: Album media gallery with lazy loading
- [x] Android: Full-screen media viewer (swipe, pinch-zoom)
- [x] iOS: Full-screen media viewer
- [x] Web: Lightbox media viewer

### 2.3 Offline Upload Queue
- [x] Shared: Local queue that persists pending uploads
- [x] Shared: Auto-sync when connectivity is restored
- [x] Android: Background upload worker (WorkManager)
- [ ] iOS: Background upload task (BGTaskScheduler)
- [x] Android/iOS: Upload progress indicator in UI

---

## Phase 3 — Social & Engagement

> Reactions, comments, and interactive features that make albums feel alive.

### 3.1 Reactions & Comments
- [x] Server: Reaction endpoints (add/remove reaction to media)
- [x] Server: Comment endpoints (CRUD on media items)
- [x] Server: Travel-themed emoji/reaction set definition
- [x] Shared: Reaction and comment data models
- [x] Android: Reaction picker overlay on media
- [x] iOS: Reaction picker overlay on media
- [x] Web: Reaction picker on media
- [x] Android: Comment thread UI per media item
- [x] iOS: Comment thread UI per media item
- [x] Web: Comment thread UI per media item

### 3.2 Notifications
- [ ] Server: Push notification service (FCM for Android, APNs for iOS)
- [ ] Server: Notification triggers (new upload, reaction, comment, invite)
- [ ] Android: Push notification handling and deep linking
- [ ] iOS: Push notification handling and deep linking
- [ ] Web: Browser notification support (optional)
- [ ] Shared: In-app notification feed model
- [ ] Android: Notification feed screen
- [ ] iOS: Notification feed screen
- [ ] Web: Notification feed page

---

## Phase 4 — Smart Features

> Location intelligence and auto-generated content that make Thykra feel magical.

### 4.1 Smart Trip Detection
- [ ] Server: Trip detection logic (cluster media by location + time)
- [ ] Server: Reverse geocoding for location names
- [ ] Shared: Location data models
- [ ] Android: Location permission flow and GPS tracking
- [ ] iOS: Location permission flow and GPS tracking
- [ ] Android/iOS: Auto-suggest album creation based on detected trip
- [ ] Android: Map view showing media pins per album
- [ ] iOS: Map view showing media pins per album
- [ ] Web: Map view (Mapbox/Leaflet) with media pins

### 4.2 Trip Recaps (Auto-Generated Highlight Reels)
- [x] Server: Recap generation service (select best media, order chronologically) — photo-sequence recaps: ≥12-photo gate, ≤24 selected round-robin across days, engagement-ranked
- [ ] Server: Video stitching pipeline (FFmpeg or cloud service)
- [x] Shared: Recap data model and status tracking (`RecapModels.kt`)
- [ ] Android: Recap viewer (video playback)
- [ ] iOS: Recap viewer (video playback)
- [x] Web: Recap viewer — photo-sequence reader + public `/r/:token` share page (video playback pending stitching)
- [ ] Android/iOS: Share recap to external apps

---

## Phase 5 — Widgets (Signature Feature)

> Home-screen widgets that keep trip memories alive without opening the app.

### 5.1 Android Widgets
- [x] Android: Glance widget — latest photo from recent album
- [x] Android: Glance widget — recent reactions/comments feed
- [x] Android: Widget configuration (select album, refresh interval)
- [x] Android: Widget deep link into album on tap

### 5.2 iOS Widgets
- [x] iOS: WidgetKit — latest photo from recent album
- [x] iOS: WidgetKit — recent reactions/comments feed
- [x] iOS: Widget configuration (select album)
- [x] iOS: Widget deep link into album on tap

---

## Phase 6 — Privacy & Sharing

> Controls that let users decide who sees what.

### 6.1 Privacy Controls
- [x] Server: Album visibility settings (private, link-shared)
- [x] Server: Shareable invite link generation with expiry
- [x] Server: Member removal and block functionality
- [x] Shared: Permission checking logic
- [x] Android: Album privacy settings UI
- [x] iOS: Album privacy settings UI
- [x] Web: Album privacy settings UI

### 6.2 Share to External
- [x] Server: Public album read-only view endpoint
- [x] Web: Public shared album page (no login required)
- [x] Android: Share album link via system share sheet
- [x] iOS: Share album link via system share sheet

---

## Phase 7 — Polish & Launch

> Performance, quality, and release readiness.

### 7.1 Performance & UX
- [ ] Image caching strategy (Coil on Android, platform caches)
- [~] Pagination for all list endpoints (cursor-based) — activity feeds are cursor-paginated; older list endpoints pending
- [~] Loading states, error states, empty states across all screens — **Web: done** (delayed skeletons, error + empty states on every screen); mobile pending
- [~] Animations and transitions polish — **Web: done** (chapter stagger, plate-resolve, shared-element lightbox, reduced-motion floor); mobile pending
- [~] Accessibility audit (screen readers, contrast) — **Web: done** (focus-visible rings, roving tabindex gallery, focus-trapped modals, AA contrast via tokens); mobile pending

### 7.2 Testing
- [~] Shared: Unit tests for business logic — web pure libs covered (44 vitest tests: chapters, justified layout, formatting, derived activity)
- [x] Server: Integration tests for all API endpoints — 39 tests (auth'd flows, invites, activity, recaps, public pages, member-leave)
- [ ] Android: UI tests for critical flows
- [ ] iOS: UI tests for critical flows
- [x] Web: E2E tests for critical flows — 18-check Playwright smoke against the real stack (H2 + dev-login; see Wanderlust-Editions.md §6)

### 7.3 Deployment
- [ ] Server: Docker containerization
- [ ] Server: Production deployment (cloud provider)
- [ ] Android: Play Store listing and release
- [ ] iOS: App Store listing and release
- [ ] Web: Production deployment (CDN + hosting)

---

## Phase 8 — Wanderlust Editions Web Redesign (2026-08-06)

> Complete rebuild of `webApp/` against the Wanderlust Editions design spec,
> plus the server APIs it needed. Details: [Wanderlust-Editions.md](./Wanderlust-Editions.md).

- [x] Design system: Paper/Darkroom themes, tokens-only CSS, editorial type scale, Arabic type rules
- [x] Day-chapter gallery (big numerals, pinned headers, 62vh lead plate) + justified rows + contact sheet
- [x] Darkroom lightbox: zoom/pan, slideshow, comments, reactions, filmstrip, full keyboard map
- [x] Upload manager: real byte progress, dock, retry semantics, batch celebration
- [x] Screens: landing, login, invite (9 states), trips, trip, settings, activity ×2, recaps ×3, public trip, /me, 404
- [x] Full Arabic i18n (401-key parity) + wholesale RTL
- [x] Server: invite preview, multi-use invite links + management, activity feeds + seen, recaps (build/read/public), public reaction summaries, member self-leave
- [x] Adversarial code review — 19 confirmed findings, all fixed
- [x] Verification: 39/39 server, 44/44 web unit, 18/18 Playwright E2E
- [ ] Follow-ups → tracked in [Actions.md](./Actions.md) (OAuth client id, CI, per-trip OG unfurls, role-change endpoint)

---

## Known Bugs

- [x] **Android: Albums not displaying correctly** — Fixed: ViewModels were recreated on every recomposition in `AppNavHost.kt` (not wrapped in `remember`), causing loaded state to be discarded. Also added error state to `AlbumListViewModel`.

---

## Progress Summary

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Foundation & Auth | Done |
| 2 | Shared Albums | In Progress (2.1 Done, 2.2 Done all platforms, 2.3 mostly done — iOS BGTask remaining) |
| 3 | Social & Engagement | In Progress (3.1 Done — 3.2 Notifications pending) |
| 4 | Smart Features | In Progress (4.2 Recaps done on server + web as photo sequences; video stitching + mobile viewers pending) |
| 5 | Widgets | Done (compile-verified; on-device runtime + Xcode target wiring pending) |
| 6 | Privacy & Sharing | Done |
| 7 | Polish & Launch | In Progress (web polish, server integration tests, and web E2E done; mobile + deployment pending) |
| 8 | Wanderlust Editions Web Redesign | Done (follow-ups in Actions.md) |
