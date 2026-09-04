# Wanderlust Editions — Web Redesign Implementation

*Implemented 2026-08-06 on branch `redesign/wanderlust-editions`.*

The web app (`webApp/`) was rebuilt from scratch against the four-document
"Wanderlust Editions" design spec exported from Claude Design. This document
records what was built, the design system as implemented, the new server APIs,
the product decisions made along the way, and the verification that backs it.

---

## 1. Design system

**Editorial print direction** — the app reads like a travel journal being
typeset, not a cloud drive.

- **Themes**: *Paper* (light, warm bone background) and *Darkroom* (dark),
  switched via `[data-theme="dark"]`. Theme choice (`system | light | dark`)
  persists in `localStorage` and is applied pre-mount to avoid flashes. The
  lightbox always renders in Darkroom regardless of theme.
- **Type**: Archivo Expanded for display, Readex Pro for Arabic display.
  A clamp()-interpolated type scale between 480–1024px viewports. Arabic
  rules: no italics, no uppercase, no tracking, 0.92× display size, 1.32
  line-height, Western digits except the big day numerals (٠١٢…).
- **Two accents only**: blue `--accent` for system/actions, clay `--warm`
  reserved for *people* (avatars, the invite stamp, member moments).
- **Media is "the plate"**: photos never get border-radius, cards do.
- **Tokens-only styling**: no hex literals in component CSS; everything flows
  from `webApp/src/styles/tokens.css` (surfaces, inks, rules, spacing, radii,
  motion durations/eases, z-layers).
- **RTL wholesale**: logical properties throughout; the Stamp rotates −3° in
  LTR, +3° in RTL; lightbox arrows mirror.

## 2. Signature mechanics

- **Day chapters**: trip photos group into day chapters from `takenAt`
  (undated media falls into an "Added later" chapter). Each chapter opens
  with an oversized day numeral, a 2px ink rule, and a lead plate capped at
  62vh; headers pin and shrink to a hairline bar on scroll.
- **Justified rows**: gallery rows are laid out by a target-row-height
  algorithm (150/180/220/260 by viewport, final-row 1.5× rule) in
  `src/lib/justified.ts` — pure and unit-tested, as is the chapter grouping
  in `src/lib/chapters.ts`.
- **Contact sheet**: alternate uniform-square view, 5/4/3/2 columns.
- **The Stamp**: rotated clay stamp marking who invited you / who shared —
  the human fingerprint on invite and public pages.
- **Honest uploads**: XHR byte progress per file; indeterminate sweeps only
  for QUEUED/CONFIRMING; a dock with per-file rows, retry, and batch
  celebration once per trip.
- **Darkroom lightbox**: zoom/pan, slideshow (disabled under reduced
  motion), 2.5s idle chrome, session-persistent comments panel, optimistic
  reactions/comments with rollback + retry, filmstrip, full keyboard map
  (`← → Home End Esc + − 0 Space C R D`), shared-element open animation.

## 3. Information architecture

Public: `/` (landing), `/login`, `/invite/:token` (9 states),
`/s/:albumId` (public trip), `/r/:shareToken` (public recap reader).
Authed: `/trips`, `/trips/:albumId` (`?m=` deep-links the lightbox),
`/trips/:albumId/settings`, `/trips/:albumId/activity`,
`/trips/:albumId/recaps`, `/activity`, `/recaps`, `/me`.
Legacy aliases (`/albums`, `/albums/:id`, `/profile`, `/1`) redirect. A 401
from any authed call clears the session and returns to `/login?next=…`
(token refresh is attempted first, deduplicated across concurrent calls).

## 4. New server surface (Ktor)

All endpoints use the existing `ApiResponse(success, data, error)` envelope
and additive-only DTO changes (defaults) so mobile clients are unaffected.

| Endpoint | Purpose |
|---|---|
| `GET /api/invites/{token}/preview` | Anonymous invite preview; status precedence REVOKED > BLOCKED > ALREADY_MEMBER > EXPIRED > VALID; BLOCKED leaks nothing |
| `GET /api/albums/{id}/invites`, `DELETE /api/albums/{id}/invites/{token}` | Invite management (list active links + join counts, revoke) |
| `GET /api/activity`, `GET /api/albums/{id}/activity`, `POST /api/activity/seen` | Aggregated activity feeds (30-minute windows, cursor pagination) + seen marker |
| `GET/POST /api/recaps`… + public `GET /api/r/{shareToken}` | Recap create/list/read + builder (≥12-photo gate, ≤24 photos round-robin across days, engagement-ranked) |
| `GET /api/public/albums/{id}` additions | Public reaction summaries |
| `DELETE /api/albums/{id}/members/{userId}` | Now permits self-removal ("leave trip") for non-owners |

## 5. Product decisions made (flagged for review)

1. **Invite links are multi-use** within their expiry — group-chat
   semantics. Joins are tracked per link (`InviteJoinsTable`, `joinCount`);
   the legacy single-use `usedBy` column remains but no longer consumes the
   link. Revocation and expiry still gate joining.
2. **Members can leave a trip themselves**; the owner cannot leave their own
   album (delete or — future — transfer instead).
3. **Recaps built beyond the spec's pre-launch state**: full build + reader
   + public share page shipped (photo-sequence recap; no video stitching).
4. **Role-change UI omitted** — there is no server endpoint for changing a
   member's role; see `Actions.md`.

## 6. Verification

- **Server**: 39/39 integration tests (invite preview/multi-use, activity,
  recaps, public reactions, member-leave, plus the original suite). Tests
  run per-class in forked JVMs; Exposed's default database is pinned after
  connect (see `DatabaseFactory.kt`) — un-scoped transactions otherwise
  bind to the *first* registered database in the JVM.
- **Web unit**: 44/44 vitest tests over the pure libs (chapters, justified
  layout, formatting, derived activity); `tsc --noEmit` clean; production
  build green.
- **E2E**: 18/18 Playwright checks against the real stack (H2 + dev-login):
  create trip → upload → chapters → lightbox → invite → two-friend
  multi-use join → public share → settings → dark theme → RTL → legacy
  redirects → 404 → zero page errors.
- **Adversarial review**: an independent code review hunted for demonstrable
  bugs and produced 19 confirmed findings — all fixed and re-verified
  (commit `42c5b10`), including a token-refresh race, a dead upload-retry
  path, an open redirect via `?next=//…`, and the impossible leave-trip.

## 7. Known degradations / follow-ups

- Per-trip OG unfurls need host-level HTML injection (static floor shipped
  in `index.html`).
- Web display-name changes show stale in the header until reload.
- Mobile member actions use a menu, not a bottom sheet.
- Public recap reader captions are date-only (public payload has no
  uploader).
- The web app's dev stack expects `ALLOW_DEV_LOGIN=true` for the test
  backdoor; never enable it in production.
