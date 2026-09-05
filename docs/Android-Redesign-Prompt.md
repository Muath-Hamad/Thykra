# Claude Design prompt — Thykra mobile app (Android first)

*Paste the block below into Claude Design. Attach these three files from the
repo first: `docs/Android-Redesign-Brief.md`, `webApp/src/styles/tokens.css`,
`docs/Wanderlust-Editions.md`. If the original Wanderlust Editions web canvas is
still in your Claude Design projects, start from it so the components and tokens
are inherited rather than re-derived.*

---

> **You are redesigning the Thykra mobile app (Compose Multiplatform, Android
> first) so it matches the Wanderlust Editions design system that already ships
> on the web app.** The attached `Android-Redesign-Brief.md` is your complete
> product and technical context; `tokens.css` is the source of truth for
> colour, type, spacing, radius, and motion; `Wanderlust-Editions.md` records
> how the web implemented the system. Read all three before drawing anything.
>
> **The product in one line:** Thykra is a shared photo album for trips — friends
> pool photos into one Trip, relive it by day, react, comment, and share
> outward. The phone is where the photos live, so mobile is the capture-and-
> contribute surface; web is the admin home base.
>
> **Non-negotiables, from the brief §4.1:** two themes, Paper and Darkroom; two
> accents only (blue for system, clay strictly for people); media is the plate
> and never gets a corner radius; ink-tinted elevation, none in Darkroom; every
> value comes from a token; Archivo Expanded for display, Readex Pro for text
> and all Arabic. Everything must be expressible as a Material 3 `ColorScheme`,
> `Typography`, and `Shapes` — Material You dynamic colour is off.
>
> **Work journey by journey, not screen by screen** (brief §6): J1 organizer
> first run, J2 friend joins via invite link (the growth loop — make this the
> best screen in the app), J3 reliving by day chapters, J4 sharing outward, J5
> housekeeping in trip settings, J6 capture-and-contribute with the honest
> upload dock (mobile-only, treat it as a first-class journey), J7 home-screen
> widgets, J8 offline and poor network.
>
> **Deliver, in this order:**
>
> 1. **Concept note** — how Wanderlust Editions translates to a phone in hand:
>    what changes at 393 dp, what the thumb reaches, how Paper and Darkroom
>    behave on OLED, what stays identical to web so the two read as one product.
> 2. **Material 3 theme spec** — fill every M3 colour role for Paper and
>    Darkroom using the mapping in brief §4.2; the type scale from §4.3 with sp
>    sizes; shapes; extended tokens (success, warning, numeral, scrim, on-scrim,
>    motion durations and eases). Output as a table a developer can transcribe
>    into Kotlin.
> 3. **Component kit** — every component in brief §9.3 with all states
>    (default, pressed, focused, disabled, loading, error), in both themes.
>    Name every icon you use; the set must stay at or under 30 because icons are
>    hand-drawn vector paths in this codebase.
> 4. **Screen specs** for every route in brief §7: one artboard per screen per
>    theme at 393×852 dp (Pixel 8), plus a 360×780 dp check for the three
>    densest screens, plus landscape for the media viewer only. For each screen:
>    regions and hierarchy, content mapped to real DTO fields (brief §5 —
>    nothing else exists), every state (loading with skeletons, empty, error,
>    offline, success), interactions (tap, long-press, swipe, back, pull to
>    refresh), TalkBack reading order, and RTL notes (what mirrors, what does
>    not). Give the deepest treatment to **invite preview**, **trip with day
>    chapters**, and the **upload dock**.
> 5. **Widgets** — latest-photo and recent-activity widgets at 2×2, 4×2, and
>    4×4 in both themes, using only what Glance can lay out (boxes, rows,
>    columns, text, images, no custom drawing).
> 6. **Motion and haptics** — screen entrances, chapter header pin and shrink,
>    reaction bursts, upload celebration, viewer chrome auto-hide; the
>    reduced-motion fallback for each; where haptics fire and at what strength.
> 7. **Build order** — confirm or adjust the sequence in brief §10 so each step
>    ships on its own without breaking the app.
>
> **Rules:** use only data listed in brief §5 and flag anything that would need
> a server change; respect the constraints in brief §3; design Android-first in
> Material 3 terms but never rely on an element that cannot exist on iOS,
> because the screen code is shared; keep copy warm and editorial, in the
> voice of the web app; write every spec concrete enough that a developer can
> build it without opening the canvas.
>
> **Start with the concept note and the theme spec, then stop and show me
> before drawing screens.**

---

## After the session

1. Export the canvas as the design spec documents and save them under
   `docs/design/mobile/` in the repo.
2. Reply on Linear THY-166 with the export location; the build sub-issues get
   created from the build order the design confirmed.
