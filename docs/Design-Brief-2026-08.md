# Thykra — Web App UI Redesign Brief

> **Archived.** This is the *input* brief that commissioned the web redesign, written
> before it was built. The redesign shipped on 2026-08-06 as **Wanderlust Editions** —
> see [Wanderlust-Editions.md](./Wanderlust-Editions.md) for what was actually built.
>
> Sections 3–4 describe the **pre-redesign** web UI and no longer match the app;
> §3 "Wanderlust Bold" does still describe the shipped Android theme, and §6's DTO
> contract remains the data available to the UI. Kept for design history and rationale.

> **Purpose of this document.** This is a complete, self-contained briefing for a design-focused
> Claude session (or any designer) to **redesign the entire Thykra web app around the user
> journey**. It describes the product, the current UI screen by screen, the data available, the
> known problems, and ends with the actual design prompt. Scope is **web only** — Android/iOS
> exist but are not part of this effort.

---

## 1. What is Thykra?

**Thykra** (from Arabic *ذكرى* — "a memory") is a **shared photo album app for trips**.

The core insight, printed on the login screen: *"The best photos from a trip are never on just
one phone."* After a trip with friends, the photos are scattered across everyone's devices.
Thykra gives the group one shared album ("Trip") where everyone dumps their photos and videos,
then relives the trip together — browsing, reacting, and commenting.

- **Brand voice:** warm, adventurous, editorial. Tagline: *"Travel Together. Remember Forever."*
- **In-app terminology:** albums are called **"Trips"** in all user-facing copy.
- **Audience:** friend groups and families who travel together. Casual users, mobile-heavy.
- **Platforms:** Android + iOS (Compose Multiplatform) and this **React web app**. All share one
  Ktor API. The web app must remain the full-featured "home base" (admin, sharing, moderation).
- **Bilingual ambition:** English + Arabic (RTL). Currently only the marketing landing page is
  bilingual; the rest of the app is English-only. The redesign should be RTL-ready.

### Product pillars (all shipped and working)

1. **One-click access** — Google OAuth only. No passwords, no signup form.
2. **Trips (albums)** — create, edit, delete; cover image auto-set from the latest upload.
3. **People** — roles (`OWNER` / `CONTRIBUTOR` / `VIEWER`), invite links with expiry
   (1/7/30/90 days), join-by-link, remove and **block/unblock** members.
4. **Privacy** — per-album visibility: `PRIVATE` (members only) or `LINK_SHARED`
   (public read-only page at `/public/:albumId`, no sign-in needed).
5. **Media** — drag-and-drop multi-upload (photos + videos) with a retrying queue
   (presigned-URL flow), server-generated thumbnails, square-grid gallery, full-screen lightbox.
6. **Social (Phase 3, in progress)** — per-media **reactions** (8 travel-themed emoji) and
   **comments** (post/edit/delete, album-owner moderation). More social features are planned:
   an activity feed and "Recaps" (auto-generated trip highlights) — both currently appear as
   "Coming Soon" teasers in the UI and must have a home in the new design.

---

## 2. Tech constraints (what the redesign must fit into)

| Aspect | Constraint |
|---|---|
| Stack | React 18 + TypeScript + Vite. Client-side SPA, no SSR. |
| Routing | TanStack Router (code-based route tree in `src/router.tsx`). |
| Auth | `@react-oauth/google` renders Google's own button; keep it. JWT stored in localStorage; API returns `{ success, data?, error? }` envelopes. |
| UI libraries | **None today** — no component lib, no CSS framework, no icon package, no animation lib. All icons are hand-drawn inline SVG. The redesign *may* propose adding libraries, but lightweight/zero-runtime approaches are preferred. |
| Styling today | Design tokens in `src/theme.css` (CSS custom properties) + a scoped `<style>` string inside every component. This has caused drift and duplication — the redesign should propose a cleaner system. |
| API | Fixed. The design must work with the existing endpoints and DTOs (section 6). Small additive API changes are negotiable; redesigning the backend is out of scope. |
| Media | Images/videos served by the API (thumbnails ~400px JPEG). Full-size originals available at `url`. |
| Fonts | Currently Google Fonts: **Nunito** (display, up to 900 weight), **Inter** (body), **Noto Kufi Arabic** (Arabic). Changeable. |

---

## 3. Current design language — "Wanderlust Bold"

The existing theme, for reference (the redesign may evolve or replace it, but should keep the
warm, travel-editorial spirit rather than going generic/corporate):

| Token | Value | Use |
|---|---|---|
| Sky Blue | `#1B7FCC` | Primary actions, links, accents |
| Ocean Blue | `#4FA3E0` | Hover states |
| Sunrise Orange | `#F47820` | Secondary accent (rare — landing CTA, "Soon" badges) |
| Sandy | `#F5E6C8` | Card surfaces, inputs |
| Warm White | `#FDF8EF` | Page background (warm paper, not pure white) |
| Deep Navy | `#1A1A2E` | Text, dark overlays, lightbox backdrop |
| Muted Slate | `#5C6B7A` | Secondary text |
| Soft Red | `#E05252` | Errors, destructive |

- Typography style: huge Nunito headlines (weight 800–900) with *italic lighter-weight accent
  words in blue* (e.g. "Good morning, *Muath.*"); uppercase letter-spaced micro-labels.
- Shadows are blue-tinted; radii 8/12/16/24px; spacing scale 4→64px.
- Motion: staggered fade-up entrances (~0.8s ease), image zoom-on-hover, spinners.
- **No dark mode anywhere.**

---

## 4. Current information architecture

| Route | Auth | Screen |
|---|---|---|
| `/1` | public | Marketing landing (bilingual EN/AR, RTL toggle) — **note the odd path** |
| `/login` | public | Split-screen login (cinematic Unsplash photo + Google sign-in card) |
| `/` | required | Dashboard: greeting, 3 stat cards, album grid, create-trip modal |
| `/albums` | required | "All Trips" — album grid + create modal (near-duplicate of `/`) |
| `/albums/:albumId` | required | Album detail: upload zone, media grid, lightbox, privacy, members, blocked list, invite link — all one long page |
| `/profile` | required | Profile card (edit name), logout, 2 "Coming Soon" teasers (Recaps, Trip Activity) |
| `/invite/:token` | mixed | Invite consumption: auto-join if signed in, else stash token → login → resume |
| `/public/:albumId` | public | Read-only shared album: hero, owner row, grid, own lightbox (no reactions/comments) |

Global chrome (authenticated pages): fixed 64px translucent `AppNav` — wordmark, "Trips" link,
disabled "Recaps · Soon" item, user chip → profile, Logout, hamburger menu on mobile.

---

## 5. User journeys (design around these)

### J1 — The Organizer starts a trip *(first-run journey)*
1. Hears about Thykra, opens the site. **Today:** the real landing page lives at `/1`; hitting
   `/` while signed out bounces to `/login` — first impression is a login wall.
2. Signs in with Google (one click, works well).
3. Lands on an empty dashboard → "Create Your First Trip" → modal (title + description).
4. Opens the new trip → drags photos in → watches the upload queue → grid fills up.
5. Wants friends in: scrolls past the gallery to the bottom of the page, picks link expiry,
   generates an invite link, copies it into the group chat.

**Friction today:** landing page unreachable organically; invite generation is buried at the
bottom of a long page; no onboarding hints; upload feedback is a plain text list without
progress bars; nothing celebrates the "first trip created" moment.

### J2 — The Friend joins *(viral loop — most important journey)*
1. Taps an invite link in WhatsApp → `/invite/:token`.
2. If signed out: "You've been invited" card → Sign in → Google → **auto-resumes** the invite
   (token stashed in localStorage) → joins → redirected into the album.
3. Browses the grid, opens the lightbox, reacts 🏖️, comments, uploads their own photos.

**Friction today:** the invite page says "You've been invited" but shows **nothing about the
trip** — no album name, cover, who invited you, or member faces. Zero temptation. This is the
app's growth loop and it's the blandest screen in the product.

### J3 — Reliving the trip *(retention journey)*
1. Returning user opens the dashboard → sees trip cards (cover photo, member avatars).
2. Opens a trip → scans the square grid → lightbox → arrow-keys through photos, reacts,
   reads/writes comments in the sidebar.

**Friction today:** the grid is a flat 3-column wall of squares — no date grouping, no "day 1 /
day 2" narrative, no videos vs photos distinction beyond a play icon, no zoom, no slideshow, no
"jump to where I left off". Reactions/comments exist only inside the lightbox; nothing on the
album page signals *activity* ("Sara added 12 photos yesterday").

### J4 — Sharing outward *(the show-off journey)*
1. Owner flips the album to "Anyone with link" in Privacy → Share button copies
   `/public/:albumId`.
2. Grandma opens it with no account: hero cover, title, "Shared by Muath · 84 photos", grid,
   lightbox. A "Sign in" pill nudges signup.

**Friction today:** functional but flat — no reactions/comments visible (even read-only), weak
conversion nudge, no Open Graph meta so the link unfurls with nothing in chat apps.

### J5 — Housekeeping *(the admin journey)*
Owner renames the trip, changes visibility, removes or blocks a member, unblocks someone,
regenerates an invite. **Today** all of this shares one page with the photo gallery, pushing
the gallery and admin into each other's way. Destructive actions use `window.confirm`.

---

## 6. Data available to the UI (fixed contract)

Condensed DTO shapes — everything the design can display:

```ts
AlbumDto        { id, ownerId, title, description?, coverUrl?, visibility: 'PRIVATE'|'LINK_SHARED',
                  memberCount, previewMembers: {userId, displayName, avatarUrl?}[] /* max 4 */, createdAt }
AlbumMemberDto  { userId, displayName, avatarUrl?, role: 'OWNER'|'CONTRIBUTOR'|'VIEWER', joinedAt }
InviteLinkDto   { albumId, token, expiresAt }
BlockedMemberDto{ userId, displayName, avatarUrl?, blockedAt }

MediaDto        { id, albumId, uploaderId, type: 'PHOTO'|'VIDEO', url, thumbnailUrl?,
                  filename, fileSize, width?, height?, durationMs?, takenAt?, uploadedAt }

CommentDto      { id, mediaId, authorId, authorDisplayName, authorAvatarUrl?,
                  body /* ≤2000 chars */, createdAt, updatedAt }

ReactionType    'LOVE' ❤️ | 'WOW' 😮 | 'LAUGH' 😂 | 'WISH_I_WAS_THERE' ✈️ |
                'WANDERLUST' 🌍 | 'BEACH' 🏖️ | 'MOUNTAIN' ⛰️ | 'FOOD' 🍜
MediaReactions  { summary: {type, count, reactedByMe}[], reactions: ReactionDto[] }

PublicAlbumView { album: { title, description?, coverUrl?, ownerDisplayName, ownerAvatarUrl?,
                           mediaCount, createdAt }, media: PublicMediaDto[] }

UploadState     { filename, status: 'QUEUED'|'UPLOADING'|'CONFIRMING'|'DONE'|'FAILED', attempt /*≤3*/ }

User (auth)     { id, displayName, email, avatarUrl? }
```

Notable: `takenAt` (EXIF capture date) and `width`/`height` exist on media but are **unused** by
the current UI — they enable date-grouped timelines and true-aspect-ratio masonry layouts.
There is **no byte-level upload progress** (status enum only) unless we add it client-side.

---

## 7. Known UX/UI debt (fix these in the redesign)

**Structure**
- `/` (dashboard) and `/albums` are ~90% the same screen; the nav doesn't even link to
  `/albums`. Merge or differentiate.
- Marketing landing is parked at `/1` and unreachable; signed-out visitors to `/` hit the login
  wall. The landing should *be* the front door.
- Album detail mixes gallery + privacy + members + blocked + invites in one scroll. Needs
  separation (tabs, drawer, or a settings surface) with owner/member views thought through.
- "Recaps" and "Trip Activity" teasers exist (nav + profile) with no designed destination.

**Screens & components**
- Media grid: fixed 3 columns at every viewport, square crops only, no date grouping, ignores
  `takenAt`/aspect data. Empty states are one-liners.
- Lightbox: comments sidebar always visible (45vh on mobile), no zoom/slideshow/download/share
  of a single photo; the info available (uploader, date, filename) isn't shown.
- Upload: no progress bars, no cancel/retry/dismiss, error text never displayed.
- Invite landing shows no album context (see J2 — highest-impact fix in the app).
- Destructive flows use `window.confirm`; success feedback is inline text ("Copied!") with no
  toast system.
- Loading is spinner-only everywhere; no skeletons. Copy-tone varies screen to screen.

**System**
- Styling is a per-component `<style>` string with duplicated CSS (the album card is
  implemented twice), hardcoded off-token colors (`#D4C5A9`, `#2e9e50`), and four different
  breakpoints (700/768/900/none). No shared Button/Modal/Card/Input components exist.
- No dark mode. No favicon, no OG/Twitter meta (share links unfurl blank). Accessibility is
  partial (some ARIA on reactions; plain-div click targets in grids; focus management in
  modals/lightbox is ad hoc).
- Arabic/RTL exists only on the landing page despite an `--font-arabic` token.

---

## 8. Redesign goals

1. **Journey-first IA.** One coherent flow per journey in section 5, with J2 (friend joins) and
   J3 (reliving) treated as the product's heart.
2. **Photos are the hero.** Chrome recedes; media leads. Use `takenAt` to tell the trip as a
   story (days, chapters), not a wall of squares.
3. **Make it feel alive.** Surface people and activity: who's here, who added what, recent
   reactions/comments — on cards and album pages, not just inside the lightbox.
4. **One design system.** Shared tokens + a small component kit (Button, Card, Modal, Toast,
   Avatar, Input, Skeleton, EmptyState), consistent breakpoints, mobile-first.
5. **Keep the warmth.** Evolve "Wanderlust Bold" (warm paper, bold editorial type, travel
   accents) — do not regress to a generic white SaaS look. Dark mode is welcome as an addition.
6. **Ready for what's next.** Leave designed slots for Recaps and Activity (Phase 3) so they
   land without another restructure.
7. **Accessible + RTL-ready.** Keyboard paths for lightbox/modals/menus, visible focus, WCAG AA
   contrast, layouts that mirror cleanly.

**Out of scope:** backend/API redesign, native apps (they will follow the web's lead later),
payments/monetization, offline support.

---

## 9. The design prompt

> **You are redesigning the Thykra web app end-to-end.** Everything above is your product
> context. Work journey-by-journey, not screen-by-screen: J1 organizer first-run, J2 friend
> join loop, J3 reliving, J4 outward sharing, J5 admin.
>
> **Deliver, in order:**
> 1. **Design concept** — a named direction evolving "Wanderlust Bold": mood, personality,
>    3–5 signature visual moves (e.g. how covers, type, and color behave), and how light/dark
>    modes relate.
> 2. **Revised IA** — final route map with what moves, merges, or appears (landing at `/`,
>    dashboard vs. all-trips resolution, where settings/members/invites live, where Recaps and
>    Activity will slot in). A simple flow diagram per journey.
> 3. **Design tokens** — full palette (light + dark), type scale and font choices (must include
>    an Arabic-capable pairing), spacing/radius/shadow/motion tokens.
> 4. **Component kit** — spec the shared components (Button variants, Card, Modal, Toast,
>    Avatar + AvatarStack, Input, Tabs/Segmented, Skeleton, EmptyState, Lightbox controls,
>    Upload progress) with states: default/hover/focus/disabled/loading/error.
> 5. **Screen-by-screen specs** for every route in the new IA — layout description (regions,
>    hierarchy, responsive behavior at mobile/tablet/desktop), content mapped to the real DTO
>    fields in section 6, all states (loading/empty/error/success), and interactions including
>    keyboard. Give the invite landing (J2) and album view (J3) the deepest treatment.
> 6. **Motion & microinteraction guidelines** — entrances, hovers, upload feedback, reaction
>    bursts; respectful of `prefers-reduced-motion`.
> 7. **Build order** — a pragmatic sequence for implementing the redesign incrementally in the
>    existing React/Vite codebase (tokens → kit → chrome → screens), so the app never breaks.
>
> **Rules:** use only data that exists in section 6 (flag any nice-to-have that needs a small
> API addition, e.g. per-album activity); respect the tech constraints in section 2; write
> specs concrete enough that a developer can implement without seeing your mockups.
