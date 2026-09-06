# Wanderlust Editions — mobile

The signed-off mobile design system for the Compose Multiplatform app (Android + iOS),
exported from Claude Design project `988a7e63-4444-4956-b78a-a4995ea4d782`.

Open any `.dc.html` file in a browser — `support.js` sits beside them and renders the boards.

| Document | Deliverables | Implements |
|---|---|---|
| [01-concept-theme](01-concept-theme.dc.html) | Concept note, Material 3 theme spec | `ui/theme/` |
| [02-component-kit](02-component-kit.dc.html) | 30-icon set, 20-part component kit | `ui/theme/ThykraIcons.kt`, `ui/kit/` |
| [03-screen-specs](03-screen-specs.dc.html) | Twelve routes, every state | `ui/screens/` |
| [04-widgets-motion-build](04-widgets-motion-build.dc.html) | Glance widgets, motion + haptics table, build order | `widget/`, `ui/theme/ThykraMotion.kt` |

## Build order (§15 of part 4)

Twelve steps, each shippable with the old screens still in place. Each card's
"Done when" in part 4 is its acceptance criterion.

| # | Step | Section | Status | Issue |
|---|---|---|---|---|
| 01 | Tokens + theme | §1 | done | THY-168 |
| 02 | Component kit + icons | §2 | done | THY-169 |
| 03 | Chrome + navigation | §7 | done | THY-170 |
| 04 | Trips list + Landing + Create trip | §08 | done | THY-171 |
| 05 | Trip with day chapters | §06 | done | THY-172 |
| 06 | Upload dock + share-to-Thykra | §06 | **partial** | THY-173 |
| 07 | Viewer restyle | §07 | done | THY-174 |
| 08 | Invite preview + joined | §05 | done | THY-175 |
| 09 | Trip settings, owner and member | §09 | done | THY-176 |
| 10 | Activity + Recaps + reader | §10 | done | THY-177 |
| 11 | Me + widgets restyle | §11, §13 | done | THY-178 |
| 12 | Arabic + RTL pass | — | **partial** | THY-179 |

### What the two partials still need

**Step 06** — the dock is built, hosted by the Scaffold, and driven by a real batch model
with byte-level progress, retry, skip and the celebration. Still outstanding: the
share-to-Thykra target and its trip picker, the Android foreground-service notification,
the local EXIF read that lets a placeholder plate land in the right chapter before the
server answers, and those placeholder plates themselves.

**Step 12** — the theme half is done and tested: the Arabic type instance, the display
fallback to Readex, zero tracking, Arabic-Indic chapter numerals, the Stamp sign flip,
and a clean RTL audit (no hardcoded Left/Right anywhere in `composeApp`). What is missing
is the string table. Design part 4 assumed steps 01–11 would use one from the start; they
did not, so every user-facing string is still an inline English literal. Extracting them
across ~25 files is mechanical, but the Arabic copy needs a translator rather than a
machine — that is the gate on this step, not the code.

## Acceptance, repeated on every step

Both themes · 393 and 360 widths · 100% and 200% font scale · TalkBack pass on the new
screen · reduced motion on · airplane mode: no blank screen · no literal colour, size or
duration outside `ui/theme/` and `ui/kit/` · iOS build compiles and the screen renders.

## Corrections to the boards found while implementing

The design is the source of truth for intent; these are places where a stated
figure did not survive being executed. In every case the token is right and the
number in the document is what needs updating.

| Where | Part 1 says | Measures | Effect |
|---|---|---|---|
| Paper, white on clay `#AA4324` | 7.1:1 | 5.8:1 | Still clears AA for body text |
| Darkroom, ink on blue `#5AA6EA` | 8.1:1 | 7.2:1 | Still clears AA |
| Darkroom, ink on clay `#E4794C` | 6.6:1 | 6.3:1 | Still clears AA |

`ThykraContrastTest` pins the measured values, so a token that drops below AA
fails the build.

Part 2 calls the icon set "exactly 30" and then names 31 glyphs. All 31 are
drawn, plus the three filled twins the tab bar needs, for 34 vectors in
`ThykraIcons`.

`MediaDto.durationMs` — listed as server work for step 05 — already exists on the
wire, so the video duration pill is live rather than deferred.

## Open server work

None of it blocks a step; each degrades gracefully as specified.

- ~~`AlbumDto.videoCount`~~ — done, along with `mediaCount` and `lastActivityAt` (THY-171)
- `InvitePreviewDto.videoCount` (step 8) — the preview reads the album's own counts for now
- Upload size limit in `/api/config` (step 6)
- A shareable single-media URL (step 7)
- Soft delete for Undo (step 7)
- Recap rebuild/delete semantics (step 10)
- Confirm one-reaction-per-person-per-media (step 7)
