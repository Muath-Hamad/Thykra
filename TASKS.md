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
- [ ] Server: Media upload endpoints (photos, videos) with presigned URLs
- [ ] Server: Image processing pipeline (thumbnails, compression, EXIF extraction)
- [ ] Shared: Upload queue manager with retry logic
- [ ] Android: Photo/video picker and upload UI
- [ ] iOS: Photo/video picker and upload UI
- [ ] Web: Drag-and-drop / file picker upload UI
- [ ] Android: Album media grid view with lazy loading
- [ ] iOS: Album media grid view with lazy loading
- [ ] Web: Album media gallery with lazy loading
- [ ] Android: Full-screen media viewer (swipe, pinch-zoom)
- [ ] iOS: Full-screen media viewer
- [ ] Web: Lightbox media viewer

### 2.3 Offline Upload Queue
- [ ] Shared: Local queue that persists pending uploads
- [ ] Shared: Auto-sync when connectivity is restored
- [ ] Android: Background upload worker (WorkManager)
- [ ] iOS: Background upload task (BGTaskScheduler)
- [ ] Android/iOS: Upload progress indicator in UI

---

## Phase 3 — Social & Engagement

> Reactions, comments, and interactive features that make albums feel alive.

### 3.1 Reactions & Comments
- [ ] Server: Reaction endpoints (add/remove reaction to media)
- [ ] Server: Comment endpoints (CRUD on media items)
- [ ] Server: Travel-themed emoji/reaction set definition
- [ ] Shared: Reaction and comment data models
- [ ] Android: Reaction picker overlay on media
- [ ] iOS: Reaction picker overlay on media
- [ ] Web: Reaction picker on media
- [ ] Android: Comment thread UI per media item
- [ ] iOS: Comment thread UI per media item
- [ ] Web: Comment thread UI per media item

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
- [ ] Server: Recap generation service (select best media, order chronologically)
- [ ] Server: Video stitching pipeline (FFmpeg or cloud service)
- [ ] Shared: Recap data model and status tracking
- [ ] Android: Recap viewer (video playback)
- [ ] iOS: Recap viewer (video playback)
- [ ] Web: Recap viewer (video playback)
- [ ] Android/iOS: Share recap to external apps

---

## Phase 5 — Widgets (Signature Feature)

> Home-screen widgets that keep trip memories alive without opening the app.

### 5.1 Android Widgets
- [ ] Android: Glance widget — latest photo from recent album
- [ ] Android: Glance widget — recent reactions/comments feed
- [ ] Android: Widget configuration (select album, refresh interval)
- [ ] Android: Widget deep link into album on tap

### 5.2 iOS Widgets
- [ ] iOS: WidgetKit — latest photo from recent album
- [ ] iOS: WidgetKit — recent reactions/comments feed
- [ ] iOS: Widget configuration (select album)
- [ ] iOS: Widget deep link into album on tap

---

## Phase 6 — Privacy & Sharing

> Controls that let users decide who sees what.

### 6.1 Privacy Controls
- [ ] Server: Album visibility settings (private, link-shared)
- [ ] Server: Shareable invite link generation with expiry
- [ ] Server: Member removal and block functionality
- [ ] Shared: Permission checking logic
- [ ] Android: Album privacy settings UI
- [ ] iOS: Album privacy settings UI
- [ ] Web: Album privacy settings UI

### 6.2 Share to External
- [ ] Server: Public album read-only view endpoint
- [ ] Web: Public shared album page (no login required)
- [ ] Android: Share album link via system share sheet
- [ ] iOS: Share album link via system share sheet

---

## Phase 7 — Polish & Launch

> Performance, quality, and release readiness.

### 7.1 Performance & UX
- [ ] Image caching strategy (Coil on Android, platform caches)
- [ ] Pagination for all list endpoints (cursor-based)
- [ ] Loading states, error states, empty states across all screens
- [ ] Animations and transitions polish
- [ ] Accessibility audit (screen readers, contrast)

### 7.2 Testing
- [ ] Shared: Unit tests for business logic
- [ ] Server: Integration tests for all API endpoints
- [ ] Android: UI tests for critical flows
- [ ] iOS: UI tests for critical flows
- [ ] Web: E2E tests for critical flows

### 7.3 Deployment
- [ ] Server: Docker containerization
- [ ] Server: Production deployment (cloud provider)
- [ ] Android: Play Store listing and release
- [ ] iOS: App Store listing and release
- [ ] Web: Production deployment (CDN + hosting)

---

## Progress Summary

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Foundation & Auth | Done |
| 2 | Shared Albums | In Progress (2.1 Done) |
| 3 | Social & Engagement | Not Started |
| 4 | Smart Features | Not Started |
| 5 | Widgets | Not Started |
| 6 | Privacy & Sharing | Not Started |
| 7 | Polish & Launch | Not Started |
