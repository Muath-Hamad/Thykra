# Brand — the Thykra mark

The chosen logo direction, kept here as source-of-truth masters.

**Nothing in the app references these files yet.** That is deliberate. Adoption across
Android, iOS and web is tracked in [THY-180](https://linear.app/thykra/issue/THY-180).

| File | What it is |
|---|---|
| `thykra-logo-stamp.svg` | 1024×1024 vector master. 7 paths, 2 colours, no gradients. |
| `thykra-logo-stamp.lottie.json` | Lottie v5.9.4, 1024×1024, 7 layers, 1 s at 120 fps. |

## The concept

The **Stamp** — a double-ruled passport-stamp frame around a geometric horizon, tilted
slightly off-axis, as if pressed by hand.

It was chosen over four other directions because it is the one motif the product already
uses: `ui/kit/Stamp.kt` draws the same double rule at the same slight rotation on invite,
share and public Recap surfaces. The logo is therefore not a new visual idea to teach
users — it is the mark they have already been meeting inside the app.

## Palette

Two colours, both lifted unchanged from the frozen Wanderlust Editions tokens:

| Role | Hex | Token |
|---|---|---|
| Clay | `#AA4324` | `--clay` / `Paper.warm` |
| Bone | `#FBF6ED` | `--bg` / `Paper.bg` |

There is no pure white in the mark, matching the rule that `#FFFFFF` exists in Thykra
only as `onPrimary`.

## Provenance

Generated with Recraft V3 (vector mode) from a prompt built on the design system's own
tokens. The SVG carries an embedded C2PA manifest recording the AI provenance; leave it
in place.

The wordmark is **not** part of these files, and should not be regenerated. Thykra owns
its type — set "Thykra" in Archivo Expanded SemiBold and the Arabic ذكرى in Readex Pro
Bold, both already bundled at `composeApp/src/commonMain/composeResources/font/`.
