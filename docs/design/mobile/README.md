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

| # | Step | Section | Status |
|---|---|---|---|
| 01 | Tokens + theme | §1 | done |
| 02 | Component kit + icons | §2 | done |
| 03 | Chrome + navigation | §7 | todo |
| 04 | Trips list + Landing + Create trip | §08 | todo |
| 05 | Trip with day chapters | §06 | todo |
| 06 | Upload dock + share-to-Thykra | §06 | todo |
| 07 | Viewer restyle | §07 | todo |
| 08 | Invite preview + joined | §05 | todo |
| 09 | Trip settings, owner and member | §09 | todo |
| 10 | Activity + Recaps + reader | §10 | todo |
| 11 | Me + widgets restyle | §11, §13 | todo |
| 12 | Arabic + RTL pass | — | todo |

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

- `AlbumDto.videoCount` and `InvitePreviewDto.videoCount` (steps 4, 8)
- Upload size limit in `/api/config` (step 6)
- A shareable single-media URL (step 7)
- Soft delete for Undo (step 7)
- Recap rebuild/delete semantics (step 10)
- Confirm one-reaction-per-person-per-media (step 7)
