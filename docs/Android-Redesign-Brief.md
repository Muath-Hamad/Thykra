# Thykra — Mobile App (Compose) Redesign Brief

*Written 2026-09-05. Status: input for Claude Design. Linear: THY-166 (Phase 9).*

This brief is the product and technical context for redesigning the **Compose
Multiplatform app** in `composeApp/` so it matches the **Wanderlust Editions**
design system the web app shipped in Phase 8. Android is the target platform for
the design work; the same `commonMain` code renders on iOS, so every decision
here is made with both in mind but expressed in Material 3 terms first.

The companion prompt for Claude Design is `docs/Android-Redesign-Prompt.md`.
Attach this file, `webApp/src/styles/tokens.css`, and
`docs/Wanderlust-Editions.md` to that session.

---

## 1. What is Thykra?

**Thykra** (Arabic *ذكرى*, "a memory") is a shared photo album app for trips.
After a trip with friends, the photos are scattered across everyone's phones.
Thykra gives the group one shared **Trip** (the in-app word for an album) where
everyone dumps photos and videos, then relives the trip together: browsing by
day, reacting, commenting, and sharing outward.

- **Brand voice:** warm, adventurous, editorial. Tagline *"Travel Together.
  Remember Forever."*
- **Audience:** friend groups and families who travel together. Mobile-heavy.
  The phone is where the photos live, so the mobile app is the **capture and
  contribute** surface; the web app is the **home base** for admin and sharing.
- **Bilingual ambition:** English + Arabic (RTL). Web has full Arabic parity;
  mobile is English-only today and must become RTL-ready.

Product pillars, all shipped on the server and web: Google/Apple one-tap sign-in;
Trips with roles (`OWNER`/`CONTRIBUTOR`/`VIEWER`); multi-use invite links with
expiry, join-by-link, remove/block; per-trip visibility (`PRIVATE` /
`LINK_SHARED` public page); photo + video upload through a retrying queue with
server thumbnails and EXIF dates; reactions (8 travel emoji) and comments;
activity feeds; Recaps (auto-built photo sequences, shareable publicly).
Full context: `docs/Design-Brief-2026-08.md` §1, §5, §6.

---

## 2. What exists on Android today

The app works end to end but is styled in the **old "Wanderlust Bold"** theme
(light only) and is missing everything the web gained in Phase 8.

### 2.1 Screens (`composeApp/src/commonMain/.../ui/`)

| Screen | Route object | State today |
|---|---|---|
| Landing | `LoginScreen` | 3-column auto-scrolling Unsplash travel images, gradient, one sign-in button. Good bones. |
| Trips list | `AlbumListScreen` | Portrait 3:4 cards, cover = latest upload, title + avatar stack, create-trip dialog, upload-progress chip in the top bar. |
| Trip | `AlbumDetailScreen(albumId)` | One long screen: upload button, square media grid, then privacy toggle, members, blocked list, invite-link generator, share. Owner and member views are the same layout with actions hidden. |
| Media viewer | `MediaViewerScreen(albumId, initialMediaId)` | Full-screen pager, edge-to-edge, pinch-zoom photos, ExoPlayer video with custom controls, reactions picker overlay, comments bottom sheet. Orientation-aware. Solid. |
| Profile | `ProfileScreen` | Name edit, avatar, sign out. |
| Home | `HomeScreen` | Placeholder ("Welcome to Thykra!") — dead screen, not in the main flow. |

Shared UI pieces: `ReactionPicker`, `CommentsSheet`, `ThykraIcons` (hand-drawn
vector paths, because no Material Icons artifact exists for Compose
Multiplatform 1.10), `ThykraTheme` (custom `lightColorScheme` + `Typography`,
system sans-serif with weight mapping; Nunito/Inter were never bundled).

### 2.2 Platform capabilities already built (keep them)

- **Photo Picker** (`PlatformMediaPicker`) for multi-select photos and videos.
- **Upload queue** in `shared` (`UploadQueueManager`): persisted to disk,
  waits for connectivity, 3-attempt backoff, **byte progress as a StateFlow**;
  Android runs it in a WorkManager `CoroutineWorker` so uploads survive the app
  being backgrounded.
- **Video** via Media3 ExoPlayer with lazy prepare per pager page.
- **Images** via Coil 3 (with `coil-video` for video frames).
- **Glance home-screen widgets**: latest photo, recent reactions/comments feed,
  per-widget config (choose trip), deep link into the trip on tap.
- **Deep links** `thykra://albums`, `thykra://album/{id}`,
  `thykra://album/{id}/media/{mediaId}` through `DeepLinkBus`.
- **System share sheet** for trip links; clipboard copy.
- **Navigation Compose 2.9.2** with type-safe `@Serializable` routes.

### 2.3 Missing versus the web (the redesign's scope)

- No **invite preview / join** screen: an invite link opened on the phone has
  no in-app landing (the web has a 9-state `/invite/:token`).
- No **trip settings** surface: privacy, members, blocked, invite links, leave
  trip are all inlined under the gallery.
- No **day chapters**: the grid is a flat wall of squares that ignores
  `takenAt`, `width`/`height`, and photo-vs-video beyond a play badge.
- No **activity** (global or per-trip) and no **Recaps** anywhere.
- No **upload dock**: a top-bar chip only; no per-file rows, retry, or
  batch celebration.
- No **dark theme**, no **Arabic/RTL**, no bundled brand fonts, no skeletons,
  spinner-only loading, one-line empty states, `AlertDialog` confirms.
- Member actions are a menu, not a bottom sheet (flagged in
  `docs/Wanderlust-Editions.md` §7).
- Mobile still assumes the old **single-use** invite semantics; DTOs are
  additive so nothing breaks, but join counts and invite previews are unused.

---

## 3. Tech constraints the design must fit

| Constraint | Detail |
|---|---|
| UI framework | Compose Multiplatform 1.10.0, Material 3, Kotlin 2.3. All screens live in `commonMain` and compile for Android **and** iOS. Platform code goes through `expect`/`actual`. |
| Theme | Must be expressible as a Material 3 `ColorScheme` + `Typography` + `Shapes`. Dynamic color (Material You) is **off** — the brand palette wins, per `docs/UX_guide.md` §9.2. |
| Icons | No `material-icons` dependency. Icons are either hand-drawn vector paths (`ThykraIcons.kt`) or a bundled icon font. Keep the icon set small (≈30) and name every icon the design uses. |
| Fonts | Must be bundled via `composeResources/font`. Web uses **Archivo Expanded** (display) and **Readex Pro** (text + Arabic display). Both are OFL and can ship in the APK; total budget ≈ 600 KB. |
| Images | Coil 3. Thumbnails are server-generated 400 px JPEG; full-size originals load on demand. Videos show a Coil-decoded first frame in grids. |
| Navigation | Navigation Compose, type-safe routes. Predictive back on API 33+. Bottom navigation or nav rail must be a real `NavigationBar`/`NavigationRail` for accessibility. |
| Insets | Edge-to-edge is already on. Every screen handles `safeDrawing` insets; the viewer uses `WindowInsets.safeDrawing.only(Top + Horizontal)` in landscape. |
| Uploads | Presigned-URL flow: request → PUT bytes → confirm. Byte progress exists per file. Background continuation via WorkManager. |
| Widgets | Glance 1.1.1; widgets have their own restricted layout system (no arbitrary Compose). Design them as a separate, simpler surface. |
| Data | Only the DTOs in `shared/src/commonMain/.../model/` (summarised in `docs/Design-Brief-2026-08.md` §6, plus §5 below). Anything else needs a server change — flag it, don't assume it. |
| Accessibility | 48 dp minimum touch targets, TalkBack content descriptions on every image and control, dynamic type up to 200 %, `reduce motion` honoured, WCAG AA contrast. |
| RTL | Compose mirrors layouts automatically with `LayoutDirection.Rtl`; the design must use start/end semantics, not left/right, and specify what does **not** mirror (photos, video controls, the Stamp rotation flips sign). |
| Performance | Lists are `LazyColumn`/`LazyVerticalGrid`; grids must virtualise. No blur-behind on lists (cost on mid-range phones). |

---

## 4. The design system to inherit: Wanderlust Editions

The web redesign (Phase 8) replaced "Wanderlust Bold" with **Wanderlust
Editions**: *an editorial print direction — the app reads like a travel journal
being typeset, not a cloud drive.* The mobile app adopts it wholesale. Source of
truth: `webApp/src/styles/tokens.css` and `docs/Wanderlust-Editions.md` §1–2.

### 4.1 Non-negotiable rules

1. **Two themes**: *Paper* (light, warm bone) and *Darkroom* (dark, warm
   near-black). Follows the system setting with an in-app override. The media
   viewer is **always Darkroom**.
2. **Two accents only**: blue `accent` for system and actions; clay `warm`
   reserved for *people* (avatars, the invite Stamp, member moments). Never use
   clay for a button that isn't about a person.
3. **Media is "the plate"**: photos and videos get **0 dp** corner radius,
   always. Cards, sheets, chips, and buttons are soft (6–28 dp).
4. **Ink-tinted elevation**: shadows are tinted with the ink colour, never
   black or blue. Darkroom has no shadows except the top layer.
5. **Tokens only**: every colour, size, radius, and duration in a component
   references a token. A literal colour in a screen file is a review failure.
6. **Type**: Archivo Expanded for display, Readex Pro for text and for all
   Arabic. Arabic rules: no italics, no uppercase, no tracking, 0.92× display
   size, 1.32 line height, Western digits except oversized day numerals.

### 4.2 Colour tokens → Material 3 roles

| Token (web) | Paper | Darkroom | Material 3 role |
|---|---|---|---|
| `--bg` | `#FBF6ED` | `#121114` | `background`, `surface` |
| `--bg-raised` | `#F4EDE0` | `#1B191E` | `surfaceContainer`, `surfaceContainerHigh` |
| `--bg-sunken` | `#EAE1D1` | `#0C0B0E` | `surfaceContainerLowest` / `surfaceDim` |
| `--text` | `#151726` | `#F2EBDF` | `onBackground`, `onSurface` |
| `--text-2` | `#3B4453` | `#CFC7BC` | `onSurfaceVariant` |
| `--text-3` | `#5C6B7A` | `#ABA3A0` | `outline` (for meta text) |
| `--rule` | ink @ 10 % | bone @ 12 % | `outlineVariant` |
| `--rule-strong` | ink @ 20 % | bone @ 24 % | `outline` |
| `--accent` | `#1B6FBE` | `#5AA6EA` | `primary` |
| `--accent-hover` | `#155C9F` | `#7FBCF2` | pressed state of primary |
| `--accent-soft` | `#E4EFF9` | accent @ 14 % | `primaryContainer` |
| `--accent-text` | `#124F8C` | `#8CC3F5` | `onPrimaryContainer` |
| `--warm` | `#AA4324` | `#E4794C` | `tertiary` (people only) |
| `--warm-soft` | `#F7E7DF` | warm @ 14 % | `tertiaryContainer` |
| `--warm-text` | `#8F3A1E` | `#F0A183` | `onTertiaryContainer` |
| `--good` / `--good-soft` | `#1F7A4D` / `#DFF0E6` | `#4FBE85` / good @ 16 % | custom `success` pair (extend the scheme) |
| `--warn` / `--warn-soft` | `#8A5D12` / `#F6EBD4` | `#E0AC4C` / warn @ 16 % | custom `warning` pair |
| `--bad` / `--bad-soft` | `#C33A32` / `#FBE7E4` | `#F0736A` / bad @ 16 % | `error`, `errorContainer` |
| `--on-accent`, `--on-warm` | `#FFFFFF` | `#121114` | `onPrimary`, `onTertiary` |
| `--scrim` | ink @ 72 % | black @ 82 % | `scrim` |
| `--on-scrim` | `#F2EBDF` | `#F2EBDF` | text over media and scrims (bone in both modes) |

`secondary` is unused in the brand; map it to a muted ink so Material components
that reach for it don't introduce a third colour.

### 4.3 Type scale (mobile sizes, sp)

Web sizes clamp between 480 and 1024 px; mobile takes the **lower bound**.

| Role | Family | Size / line height | M3 slot |
|---|---|---|---|
| Display XL | Archivo Expanded 700 | 44 / 0.98 | `displayLarge` |
| Display L | Archivo Expanded 700 | 34 / 1.0 | `displayMedium` |
| Display M | Archivo Expanded 700 | 28 / 1.04 | `displaySmall` |
| Display S | Archivo Expanded 600 | 23 / 1.1 | `headlineMedium` |
| Day numeral | Archivo Expanded 700 | 40 / 1.0 | custom `numeral` |
| Title | Readex Pro 600 | 18 / 1.3 | `titleLarge` |
| Body L | Readex Pro 400 | 16 / 1.5 | `bodyLarge` |
| Body | Readex Pro 400 | 15 / 1.5 | `bodyMedium` |
| Body S | Readex Pro 400 | 13.5 / 1.45 | `bodySmall` |
| Meta | Readex Pro 500 | 12.5 / 1.4 | `labelMedium` |
| Label (caps, tracked) | Readex Pro 600 | 11 / 1.2, +0.08 em | `labelSmall` |

Arabic: display falls back to Readex Pro; drop caps/tracking.

### 4.4 Spacing, radius, motion

- **Spacing**: 4 dp base — 4, 8, 12, 16, 20, 24, 32, 40, 48, 64, 96. Screen
  gutter 16 dp on phones, 24 dp on tablets.
- **Radius**: plate 0 · xs 6 · sm 10 · md 14 · lg 20 · xl 28 · pill.
- **Motion**: 120 / 200 / 320 / 520 ms; ease-out `(.2,.8,.28,1)`, in-out
  `(.5,0,.3,1)`, spring `(.2,1.25,.3,1)`; stagger 40 ms. Reduced motion → cross-fades.
- **Chrome heights**: top bar 56 dp, bottom bar 60 dp + nav-bar inset.

### 4.5 Signature mechanics to carry over (adapt, don't copy)

- **Day chapters**: trip media grouped by `takenAt` into day chapters (undated
  → "Added later"). Each chapter opens with an oversized numeral, a 2 dp ink
  rule, and a lead plate; headers pin and shrink on scroll.
- **Justified rows** on web become a **true-aspect staggered grid** on phones
  (2 columns portrait, 3–4 landscape/tablet) with an alternate uniform-square
  contact sheet. `width`/`height` are on every `MediaDto`.
- **The Stamp**: rotated clay stamp marking who invited you / who shared.
  −3° in LTR, +3° in RTL.
- **Honest uploads**: per-file byte progress, indeterminate only while
  QUEUED/CONFIRMING, retry per file, one celebration per trip per batch.
- **Darkroom viewer**: always dark, chrome auto-hides, filmstrip, optimistic
  reactions and comments with rollback. Mobile already has most of this; the
  redesign restyles it and adds the filmstrip + info sheet (uploader, taken
  date, dimensions).

---

## 5. Data available to the UI

Everything in `docs/Design-Brief-2026-08.md` §6 (`AlbumDto`, `AlbumMemberDto`,
`InviteLinkDto`, `BlockedMemberDto`, `MediaDto`, `CommentDto`, `ReactionType`,
`MediaReactions`, `PublicAlbumView`, `UploadState`, `User`) **plus** the Phase 8
additions (`docs/Wanderlust-Editions.md` §4):

| Endpoint | What the mobile app can now show |
|---|---|
| `GET /api/invites/{token}/preview` | Anonymous invite preview: trip title, cover, inviter, member faces, status `VALID / EXPIRED / REVOKED / ALREADY_MEMBER / BLOCKED` (BLOCKED leaks nothing). |
| `GET /api/albums/{id}/invites`, `DELETE …/invites/{token}` | Active invite links with `joinCount`, revoke. |
| `GET /api/activity`, `GET /api/albums/{id}/activity`, `POST /api/activity/seen` | Aggregated activity in 30-minute windows (who added N photos, reacted, commented, joined), cursor-paginated, with a seen marker. |
| `GET/POST /api/recaps`, `GET /api/recaps/{id}`, public `GET /api/r/{shareToken}` | Recaps: ≥ 12-photo gate, ≤ 24 photos round-robin across days, engagement-ranked. **Photo sequences only — no video stitching yet.** |
| `DELETE /api/albums/{id}/members/{userId}` (self) | "Leave trip" for non-owners. Owners cannot leave. |

Client-side extras: **byte-level upload progress** per file (mobile has it,
web fakes it), and **offline queueing** (uploads persist across app restarts and
resume on connectivity).

Not available (flag if the design wants it): role changes after join, owner
transfer, push notifications, per-media view counts, video recaps.

---

## 6. Journeys, mobile edition

Design journey-by-journey. J2 and J3 are the heart; J6 is what makes mobile
different from web.

- **J1 — Organizer starts a trip.** Landing → one-tap sign-in → empty Trips
  screen → create trip (title, optional description) → trip screen → add
  photos from the picker → watch the dock → invite friends from a prominent
  place (not the bottom of a long scroll).
- **J2 — Friend joins (growth loop).** Taps `https://thykra.com/invite/{token}`
  in WhatsApp. If the app is installed (Android App Links / iOS Universal
  Links) it opens the **invite preview** with the trip's cover, title, who
  invited them, member faces, and a single Join button; signed-out users see
  the same preview, sign in, and land in the trip. All 9 web states apply.
- **J3 — Reliving.** Trips list → trip → day chapters → viewer → react,
  comment, swipe through the day. Activity is visible on the trip ("Sara added
  12 photos yesterday"), not only inside the viewer.
- **J4 — Sharing outward.** Share sheet with the public link and a Recap link;
  the Recap reader plays inside the app for members and opens the web reader
  for outsiders.
- **J5 — Housekeeping.** Trip settings: rename, visibility, members (bottom
  sheet actions: remove, block), blocked list (unblock), invite links (list,
  join counts, revoke, create with expiry), leave trip, delete trip. Owner and
  member variants are distinct screens, not hidden buttons.
- **J6 — Capture and contribute (mobile-only).** Back from a trip day, the
  user opens a trip and adds 40 photos and 3 videos in one go. The dock shows
  honest progress; the app can be backgrounded and uploads finish. On reopen,
  new photos are already in their chapters. Also: share-to-Thykra from the
  system share sheet (design the target picker: which trip?).
- **J7 — Glance from the home screen.** Widgets show the latest photo or
  recent activity for a chosen trip and deep-link into it. Restyle in
  Wanderlust Editions within Glance's limits.
- **J8 — Offline / poor network.** Cached trips and thumbnails stay browsable;
  actions queue or fail honestly with retry; no blank screens.

---

## 7. Target information architecture (mobile)

Mirror the web's authed routes; mobile chrome is a 4-tab bottom bar.

| Tab / route | Web equivalent | Notes |
|---|---|---|
| **Trips** `trips` | `/trips` | Card grid, create trip, upload dock summary. Default tab. |
| `trips/{albumId}` | `/trips/:id` | Day chapters, contact-sheet toggle, per-trip activity strip, add photos, share, settings entry. `?m=` deep-links the viewer. |
| `trips/{albumId}/viewer/{mediaId}` | lightbox | Always Darkroom, full-screen. |
| `trips/{albumId}/settings` | `/trips/:id/settings` | Owner vs member variants. |
| `trips/{albumId}/activity` | `/trips/:id/activity` | Per-trip feed. |
| `trips/{albumId}/recaps` | `/trips/:id/recaps` | Build + list per trip. |
| **Activity** `activity` | `/activity` | Global feed with seen marker; badge on the tab. |
| **Recaps** `recaps` | `/recaps` | All recaps; reader `recaps/{id}`. |
| **Me** `me` | `/me` | Profile, theme (system/paper/darkroom), language, sign out, app version. |
| `invite/{token}` | `/invite/:token` | Full-screen, outside the tab bar, reachable signed-out. |
| `landing` | `/`, `/login` | Signed-out entry. |
| Public trip `s/{albumId}` | `/s/:id` | Opens the web page; no in-app public mode for v1 (flag if the design disagrees). |

Delete `HomeScreen`. Map deep links: `https://thykra.com/trips/{id}`,
`/invite/{token}`, `/r/{shareToken}` as App Links, keeping the `thykra://`
scheme for widgets.

---

## 8. Redesign goals

1. **One design system across web and mobile.** Same tokens, same names, same
   two accents, same plate rule. A screenshot of the trip page on web and on
   Android should read as the same product.
2. **Photos are the hero.** Chrome recedes; day chapters tell the trip as a
   story; the grid respects aspect ratios.
3. **The growth loop works on the phone.** Invite preview is the best screen
   in the app.
4. **Make it feel alive.** People and activity on the trips list and trip
   screen, not only inside the viewer.
5. **Honest, resilient contribution.** The upload dock, background uploads,
   and offline behaviour are designed, not incidental.
6. **Darkroom from day one.** System-following dark theme with an override.
7. **Accessible and RTL-ready.** TalkBack, 48 dp targets, dynamic type,
   reduced motion, mirrored layouts, Arabic type rules.
8. **Incrementally buildable.** Every screen must be shippable on its own so
   the app never breaks mid-redesign (see §10).

**Out of scope:** server/API changes (flag needs), iOS-specific chrome (the
Compose code is shared; iOS adaptations come after), payments, video recaps.

---

## 9. What Claude Design should deliver

In this order, each concrete enough to implement without seeing the mockups:

1. **Concept note** — how Wanderlust Editions translates to a phone in hand:
   what changes at 393 dp wide, what the thumb reaches, how Paper and Darkroom
   behave on OLED.
2. **Material 3 theme spec** — the full `ColorScheme` for Paper and Darkroom
   from §4.2 (fill every M3 role), `Typography` from §4.3, `Shapes`, and the
   extended tokens (success/warning, numeral style, scrim, on-scrim, motion).
3. **Component kit** — Button (filled/tonal/outlined/text, people variant in
   warm), Card (trip card, activity card, recap card), Sheet (member actions,
   comments, share), Dialog (destructive confirm), Toast/Snackbar, Avatar +
   AvatarStack, Stamp, Input, Segmented control (chapters/contact sheet),
   Chip, Skeleton, EmptyState, UploadDock (+ row), ReactionBar + Picker,
   ChapterHeader (pinned/expanded), ViewerChrome. States: default, pressed,
   focused, disabled, loading, error. Every icon named.
4. **Screen specs** for every route in §7, Paper and Darkroom, portrait
   (393×852 dp, Pixel 8) and small (360×780 dp); landscape for the viewer
   only. Each: regions and hierarchy, content mapped to DTO fields, all states
   (loading, empty, error, offline, success), interactions (tap, long-press,
   swipe, back), TalkBack order, RTL notes. Deepest treatment: **invite
   preview (J2)**, **trip with day chapters (J3)**, **upload dock (J6)**.
5. **Widgets** — latest-photo and activity widgets at 2×2, 4×2, 4×4 in both
   themes, within Glance's layout limits.
6. **Motion and haptics** — entrances, chapter pin/shrink, reaction bursts,
   upload celebration, viewer chrome; reduced-motion fallbacks; where haptics
   fire.
7. **Build order** — the sequence in §10, with any adjustments the design
   implies.

Rules: use only data from §5 and flag anything else; respect §3; design for
Android first but never with an element that cannot exist on iOS (no
Android-only widgets in shared screens); keep the icon set ≤ 30 and list it.

---

## 10. Build order (engineering)

Each step ships independently behind the existing navigation.

1. **Tokens + theme** — bundle Archivo Expanded and Readex Pro; replace
   `ThykraTheme` with Paper/Darkroom schemes, typography, shapes, extended
   colours; theme preference in `Me`. Old screens keep working under the new
   theme.
2. **Component kit** — the components in §9.3, previewed in a debug gallery
   screen.
3. **Chrome + navigation** — 4-tab bottom bar, new route graph, App Links,
   delete `HomeScreen`.
4. **Trips list** → **Trip with day chapters** → **Viewer restyle** →
   **Invite preview** → **Trip settings** → **Upload dock** → **Activity** →
   **Recaps** → **Me**.
5. **Widgets restyle.**
6. **Arabic + RTL** pass with a bundled string table.

Each step becomes a sub-issue under THY-166 with the design section it
implements, acceptance criteria, and the platform label.
