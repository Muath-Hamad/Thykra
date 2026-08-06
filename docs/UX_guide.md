# THYKRA
## UI/UX Design Specification — MVP v1.0
*Travel Together. Remember Forever.*

> **⚠️ Superseded for Web (2026-08-06):** the web app now follows the
> **Wanderlust Editions** redesign — an editorial print direction
> (Paper/Darkroom themes, Archivo/Readex type, day chapters, justified
> rows, the Stamp) that replaces the "Wanderlust Bold" visual identity
> described below. See [Wanderlust-Editions.md](./Wanderlust-Editions.md).
> This document remains the reference for the **Android and iOS apps** and
> as historical context for the original MVP.

---

| Field | Details |
|---|---|
| Document Type | UI/UX Design Specification |
| Product | Thykra — Shared Travel Memory App |
| Stage | Early MVP |
| Platforms | iOS, Android, Web (Desktop + Mobile Browser) |
| Audience | B2C — Friends, Couples, Families, Communities |
| Primary Flow | Trip Recap / Highlight Reel |
| Visual Direction | Wanderlust Bold — Sky Blue, Sandy Neutrals, Orange |
| Competitor Reference | Google Photos + BeReal |
| Version | 1.0 — Initial Specification |

---

## Table of Contents

1. [Product Overview](#1-product-overview)
2. [Design Principles](#2-design-principles)
3. [Visual Identity System](#3-visual-identity-system)
4. [Core Component Library](#4-core-component-library)
5. [Key Screen Specifications](#5-key-screen-specifications)
6. [Priority MVP Flow: Trip Recap](#6-priority-mvp-flow-trip-recap)
7. [Widget Experience](#7-widget-experience)
8. [Accessibility Requirements](#8-accessibility-requirements)
9. [Platform-Specific Considerations](#9-platform-specific-considerations)
10. [Design Token Reference](#10-design-token-reference)
11. [MVP Scope & Design Priorities](#11-mvp-scope--design-priorities)
12. [Document Notes](#12-document-notes)

---

## 1. Product Overview

Thykra is a mobile-first shared travel memory app that transforms group trips into collaborative social scrapbooks. It bridges the gap between cloud storage (Google Photos, iCloud) and social broadcasting (Instagram, TikTok) by providing a private, interactive, memory-focused experience for groups.

The defining experience is the home-screen widget — users stay connected to their trips passively, seeing new uploads and reactions without ever opening the app. This drives habit formation and emotional engagement long after the trip ends.

### 1.1 Design Positioning

> **Design Philosophy:** Thykra should feel like opening a beautifully worn travel journal — warm, personal, and full of life. The interface must be immediately familiar to users of Google Photos and BeReal, while feeling distinctly more alive and emotionally resonant.

| Reference | What We Borrow | Strategic Intent |
|---|---|---|
| Google Photos | Familiar grid layouts, album organization, search patterns, photo viewer behavior | Adopt for core album browsing and navigation structure |
| BeReal | Dual-capture authenticity, real-time group notification, unfiltered social sharing | Adopt for upload prompts, group activity feed feel, and spontaneous capture UX |
| Thykra Unique | Widget-first engagement, travel-themed reactions, private group storytelling | Differentiate here — do not dilute with generic social patterns |

---

## 2. Design Principles

These six principles govern every design decision made for Thykra. When in doubt, refer back to these.

### 2.1 Warm Over Cold

The interface must never feel clinical, sterile, or corporate. Every screen should feel like it belongs in a travel magazine or a postcard shop. Soft gradients, warm whites, sandy backgrounds, and sun-soaked oranges take precedence over neutral grays and cold whites.

### 2.2 Familiar First, Delightful Second

Core interactions — browsing photos, adding to albums, viewing a grid — must feel instantly familiar to Google Photos users. Delight and novelty are layered on top: travel-themed reactions, widget animations, recap cinematics. Never sacrifice familiarity for novelty in navigation.

### 2.3 Private by Default

Every album, every reaction, every comment exists within a trusted group. The UI must constantly signal privacy without making it feel like a warning. Invite flows, sharing controls, and group membership must feel warm and intentional, not bureaucratic.

### 2.4 Alive Without Effort

Users should feel their trip is living and breathing without needing to open the app. Widgets, notifications, and auto-generated recaps must reduce friction to zero. The product does the storytelling work — users just show up.

### 2.5 Vibrant but Not Overwhelming

Bold color is used with intention. The primary blue and orange palette creates energy without visual noise. Dark mode is explicitly out of scope for MVP. Backgrounds stay light. Typography stays readable. Color is accent, not wallpaper.

### 2.6 Emotional Resonance

Every feature should ask: does this make someone feel something about their trip? Grid layouts should evoke a scrapbook, not a file folder. Recaps should feel like a mini-movie, not a slideshow. Reactions should feel playful, not transactional.

---

## 3. Visual Identity System

### 3.1 Color Palette

The palette is anchored in Wanderlust Bold — a combination inspired by open skies, desert sand, and golden-hour light. It is vivid and inviting without leaning dark or moody.

| Color Role | Hex Value | Usage |
|---|---|---|
| Sky Blue (Primary) | `#1B7FCC` | Primary actions, CTAs, navigation, links, key UI accents |
| Ocean Blue (Secondary) | `#4FA3E0` | Hover states, secondary buttons, progress indicators |
| Sunrise Orange (Accent) | `#F47820` | Notifications, reactions, highlights, section headers |
| Sandy Neutral (Background) | `#F5E6C8` | Card backgrounds, album containers, subtle page sections |
| Warm White (Base) | `#FDF8EF` | Primary app background, modal backgrounds |
| Deep Navy (Text) | `#1A1A2E` | Body text, headings, primary readable content |
| Muted Slate (Secondary Text) | `#5C6B7A` | Captions, timestamps, secondary labels |

> ⚠️ **Critical Constraint:** Dark backgrounds, dark modes, and near-black UI surfaces are explicitly excluded from MVP scope. If a screen feels heavy, lighten it. The product must feel like sunlight.

### 3.2 Typography

Thykra uses a two-family system: a geometric sans-serif for UI chrome and a slightly warmer rounded sans-serif for content and marketing copy. Both must render cleanly at all sizes across iOS, Android, and web.

| Type Role | Specification |
|---|---|
| Display / Marketing Headings | Nunito (Bold, ExtraBold) — warm, rounded, friendly |
| UI Navigation & Labels | Inter (Medium, SemiBold) — clean, legible, neutral |
| Body & Captions | Inter (Regular, Light) — high readability at small sizes |
| Album & Trip Titles (User-set) | Nunito SemiBold — personal feel for user-generated content |
| Minimum Body Size (Mobile) | 14px / 28sp — never go below this for body text |
| Line Height | 1.5x base — generous spacing for a relaxed, warm reading feel |

### 3.3 Iconography

Icons follow the rounded style — no sharp corners. Use SF Symbols on iOS and Material Symbols (Rounded variant) on Android, mapped to the same semantic meaning. On web, use Lucide or Phosphor (Regular weight, rounded).

Travel-specific icons (camera, map pin, passport, compass, airplane) should be slightly illustrated/outlined rather than flat fills — this adds warmth and personality without requiring a custom icon library.

### 3.4 Imagery & Photo Treatment

User-generated photos are the hero of every screen. UI chrome should recede. Photo containers use soft rounded corners (12–16px radius on mobile). Album covers use a subtle warm vignette overlay to make titles readable without heavy text-shadow.

Empty states and onboarding use original illustrated scenes: open roads, sunlit beaches, maps with dotted routes, polaroid-style photo frames. Illustrations must match the color palette and feel handcrafted, not generic.

---

## 4. Core Component Library

### 4.1 Buttons

| Button Type | Specification |
|---|---|
| Primary CTA | Sky Blue fill (`#1B7FCC`), white label, 48px height mobile / 44px web, 24px border radius, Nunito SemiBold |
| Secondary | Orange fill (`#F47820`), white label — used for emotional/reactive actions (e.g., "Create Recap") |
| Ghost / Outline | Sky Blue border, Sky Blue label, transparent fill — for tertiary actions |
| Destructive | Soft red (`#E05252`), white label — only for delete/remove actions, always preceded by a confirmation |
| Disabled State | 40% opacity of primary, non-interactive, never gray — disabled should still feel warm |
| Minimum Touch Target | 44×44px on mobile regardless of visual size (WCAG AA compliance) |

### 4.2 Cards & Albums

Album cards are the primary content unit across the app. They must feel like physical photographs or travel postcards.

| Component | Specification |
|---|---|
| Album Card (Grid) | 16:9 cover photo, 12px border radius, trip title in Nunito Bold over warm vignette, member avatar stack (max 4 + overflow count), photo count badge |
| Album Card (List) | Square thumbnail left, trip title + date range + member count right, last activity timestamp in muted slate |
| Memory Moment Card | Full-bleed square photo, reaction strip below, commenter avatar + first comment preview, timestamp |
| Recap Card | Cinematic 16:9 still from highlight reel, play button overlay in orange, duration badge |
| Card Shadow | `0 4px 16px rgba(27, 127, 204, 0.12)` — a blue-tinted shadow that feels sky-like, not gray |

### 4.3 Navigation

Navigation patterns are directly borrowed from Google Photos to maximize familiarity. Deviations are clearly called out.

| Navigation Element | Specification |
|---|---|
| Mobile Bottom Nav | 5 tabs: Home, Trips, Upload (+), Recaps, Profile. Orange dot on Upload tab as visual anchor. Active state in Sky Blue. |
| Web Top Nav | Horizontal nav bar: Thykra logo left, Trips / Recaps / Explore center, Search + Avatar right. Sticky on scroll. |
| Mobile Web | Collapsible hamburger menu. Bottom nav hidden on mobile web — replaced with floating Upload FAB in orange. |
| Tab Active State | Sky Blue icon + label, no underline (Google Photos pattern). Inactive in Muted Slate. |
| Back Navigation | Always top-left chevron (iOS: system back, Android: system gesture + visual chevron). Never modal-only flows without escape. |

### 4.4 Reactions & Social Elements

Reactions are a core differentiator. They must feel playful and travel-specific, borrowing the quick-tap accessibility of BeReal reactions but with Thykra's personality.

| Social Element | Specification |
|---|---|
| Reaction Strip | 6 travel-themed emoji reactions (e.g., Camera, Heart, Map Pin, Flame, Laugh, Wow). Tap to react, hold to see who reacted. Horizontal scrollable on overflow. |
| Reaction Animation | Gentle bounce scale (1.0 → 1.3 → 1.0) on tap. Floating emoji burst for first reaction on a photo. |
| Comment Preview | First comment shown inline under photo. Tap to expand thread. BeReal-style compact threading. |
| Activity Indicator | Orange dot on album card for unseen activity. Disappears on scroll-past (not tap required). |
| Group Presence | Member avatar stack on album cover. Tapping opens member list with last-seen timestamps. |

---

## 5. Key Screen Specifications

### 5.1 Onboarding Flow

Onboarding is 4 screens maximum. No account required to explore — users can view a sample trip before signing up (reduces friction significantly). Sign-up is triggered on first attempt to create or join a trip.

| Screen | Content | Visual Treatment |
|---|---|---|
| Screen 1 — Welcome | Animated illustrated hero (open road, camera in hand). Tagline: "Travel Together. Remember Forever." Two CTAs: "Start a Trip" (Primary) and "See How It Works" (Ghost) | Sky Blue gradient top wash, Warm White base |
| Screen 2 — How It Works | 3-step carousel: Create Album → Invite Friends → Relive Together. Icon + short copy per step. Skip link top-right. | Sandy Neutral cards, Orange step indicators |
| Screen 3 — Permissions | Camera, Photos, Notifications, Location. Each permission has a warm illustrated reason why. "Not Now" always available — no guilt. | Clean Warm White, Sky Blue permission icons |
| Screen 4 — Profile Setup | Name + avatar (photo or emoji picker). Optional — can skip and set later. "Let's Go" primary CTA in orange. | Sandy background, playful avatar emoji grid |

### 5.2 Home Screen

Home is the emotional anchor of the app. It should feel like opening a travel journal — alive with recent activity from your trips.

| Element | Specification |
|---|---|
| Layout | Vertical scroll. Pinned hero banner for most active current trip. Album card grid (2-up) below. "Memories From This Day" section if past trip photos exist for today's date. |
| Empty State | Illustrated open road scene. Headline: "Your next adventure starts here." Two CTAs: Create Trip / Join Trip. |
| Header | Thykra logo left, Search icon right, Notification bell with orange dot badge. No user avatar in header (lives in tab bar). |
| Hero Trip Banner | Full-bleed 16:9 album cover, trip name in Nunito ExtraBold, member avatar stack, last activity time, "Open Trip" CTA in white ghost button over vignette. |
| Activity Feed Strip | Horizontal scrollable strip of recent moments across all trips. Each moment is a 1:1 photo card with reactor avatar overlay. Positioned between hero and album grid. |

### 5.3 Trip Album View

The album view is directly modeled on Google Photos' album UX — the grid is the familiar anchor. Thykra adds social layers on top without cluttering the grid.

| Element | Specification |
|---|---|
| Grid Layout | 3-up square grid (default, matches Google Photos). Toggle to 2-up for larger previews. Pinch-to-zoom between grid sizes (Google Photos pattern). |
| Header | Trip cover photo top, trip name, date range, member count, location (if available). Share button top-right. |
| Upload FAB | Persistent orange circular FAB bottom-right. Tap: camera or library pick. Hold: multi-select. Shows offline queue count badge if no connection. |
| Photo Viewer | Full-screen, swipe left/right to navigate (Google Photos pattern). Reaction strip anchored bottom. Comment panel slides up from bottom on tap. Share / Download in top-right menu. |
| Date Dividers | Sticky date headers while scrolling (Google Photos pattern). Format: "Saturday, June 14" in Muted Slate, small caps. |
| Contributor Badge | Small avatar badge on each photo cell showing who uploaded it. Familiar from Google Photos Shared Albums. |

---

## 6. Priority MVP Flow: Trip Recap

> **Why This Flow First:** The Trip Recap / Highlight Reel is Thykra's most emotionally powerful feature and the one most likely to drive word-of-mouth sharing. A user who watches their recap and feels something will share it — and that share is the most effective acquisition channel. Nail this first.

### 6.1 Recap Generation Entry Points

The recap must feel discovered, not searched for. It should surface as a delightful surprise.

| Entry Point | Behavior |
|---|---|
| Auto-Prompt (Primary) | Push notification 24–48 hours after a trip ends: "Your [Trip Name] recap is ready! ✈️". Tapping opens directly to the Recap Preview screen. |
| Album Page CTA | Persistent "Create Recap" button (orange) pinned in album header. Available once 5+ photos exist in the album. |
| Home Memories Section | If today matches a past trip date, a "Year Ago" recap prompt surfaces in the Home memories strip. |
| Recap Tab | Dedicated Recaps tab in bottom nav. Lists all generated recaps chronologically. "Generate New" CTA at top. |

### 6.2 Recap Generation Screen

While the recap is being generated (10–30 seconds), the user sees an engaging loading experience rather than a spinner.

| Element | Specification |
|---|---|
| Loading State | Animated collage of trip photos assembling like polaroids being laid on a table. Warm Sandy background. Progress text: "Collecting your memories..." → "Finding the highlights..." → "Almost ready!" |
| Duration | Target under 20 seconds. If longer, offer "Notify Me When Ready" option to not block the user. |
| Background Processing | Recaps can be queued to generate in background. User receives notification when complete. |
| Customization Before Generate | Optional pre-generation screen: pick music mood (Upbeat / Chill / Cinematic), recap length (30s / 60s / 90s), highlight preference (Best reactions / Chronological / Random). Can always skip to auto-generate. |

### 6.3 Recap Playback Screen

The playback screen is the emotional climax. It should feel cinematic — not like a slideshow, like a memory.

| Element | Specification |
|---|---|
| Layout | Full-bleed video player. No top/bottom bars during playback. Status bar hidden on iOS. Immersive experience. |
| Controls | Tap to pause/play. Swipe up for details. Swipe down to dismiss (sheet behavior). Scrubber visible on pause only. |
| Music | Background music (licensed library). Music selector accessible via settings icon top-right during pause. |
| Moment Credits | During playback: contributor avatar + name fades in on each photo. "Photo by [Name]" — gives credit, drives emotional connection. |
| End Card | After recap: trip name, date range, member count, total photos stat. Four actions: Save to Camera Roll / Share with Group / Share Externally / Regenerate. |
| Sharing | External share exports as video with Thykra watermark (subtle, bottom-right corner, Sand color). "Made with Thykra" on end card. This is the viral acquisition loop. |

### 6.4 Recap Settings & Customization (Post-Generation)

| Control | Behavior |
|---|---|
| Edit Music | Swipe up on player to reveal settings sheet. Music selector shows 4 mood options with preview snippets. |
| Edit Photo Selection | Toggle individual photos in/out of recap. Minimum 5 photos required. Grid view of included/excluded photos. |
| Trim Length | Scrubber to set start/end point of recap. Simplified — not a full editor. |
| Title Card | Editable trip title and dates on the opening card. Tap to edit in-place. |
| Regenerate | One-tap to regenerate with same settings but different photo order/transitions. |

---

## 7. Widget Experience

The widget is Thykra's core habit-forming mechanism. It must feel alive and emotionally warm on the user's home screen, without requiring any app interaction.

### 7.1 Widget Sizes & Content

| Widget Size | Content & Behavior |
|---|---|
| Small (2×2) | Latest photo from most active trip. Trip name label. Member avatar of uploader. Taps open the specific photo in album view. |
| Medium (4×2) | Latest photo left + 2 recent activity items right (reaction received, new upload, new comment). Taps open album. |
| Large (4×4) | 3-photo collage from latest trip. Activity feed strip. Trip name + member count. "Open Recap" quick action if recap available. |
| Background Color | Sandy Neutral (`#F5E6C8`) with Sky Blue trip name. Never white or dark. Matches Thykra's warmth on any home screen wallpaper. |
| Update Frequency | Refreshes every 15–30 minutes or on new group activity (push-triggered refresh). |

### 7.2 Widget Setup Flow

Widget setup must be zero-friction. The widget should work immediately after install with no additional configuration — it auto-selects the most active current trip. Advanced users can long-press to configure which trip to pin.

---

## 8. Accessibility Requirements

Thykra targets WCAG 2.1 AA compliance at MVP. The warm, high-contrast palette (Sky Blue on White, Orange on White) naturally supports strong color contrast. The following requirements are mandatory for launch.

| Requirement | Specification |
|---|---|
| Color Contrast | All body text: minimum 4.5:1 ratio. Large text and UI components: minimum 3:1. Sky Blue (`#1B7FCC`) on Warm White (`#FDF8EF`) meets AA at all sizes. |
| Touch Targets | Minimum 44×44px on all interactive elements (iOS HIG and Material Design both require this). |
| Screen Reader | All photos must have auto-generated alt text (leveraging on-device ML). User-uploaded captions used when available. |
| Dynamic Type | All UI text responds to system font size settings. Test at 3 size increments above default. |
| Reduced Motion | All animations (recap transitions, reaction bursts, widget updates) respect iOS/Android Reduce Motion setting. Crossfade fallback for all animated transitions. |
| Focus States | Keyboard navigation on web. All interactive elements have visible focus ring in Sky Blue (3px, 2px offset). |

---

## 9. Platform-Specific Considerations

### 9.1 iOS

- Follow Human Interface Guidelines for navigation patterns, sheet presentations, and haptic feedback.
- Use SF Symbols (Rounded) for all system icons. Custom icons must match SF Symbol optical weight.
- Support Dynamic Island / Live Activity for active trip uploads (stretch goal — log for post-MVP).
- Photo Library integration via `PHPickerViewController` — no legacy `UIImagePickerController`.
- Widget built with WidgetKit. Complication added for Apple Watch (post-MVP).

### 9.2 Android

- Follow Material Design 3 guidelines. Use Material You dynamic color only as an accent layer — do not override the Thykra brand palette.
- Use Material Symbols (Rounded) for all system icons.
- Support Android predictive back gesture on API 33+.
- Widget built with Glance (Jetpack Compose). Minimum Android 8.0 (API 26) for widget support.
- Photo picker via Photo Picker API (API 33+) with fallback to MediaStore for older versions.

### 9.3 Web (Desktop)

- Minimum supported resolution: 1280px width. Optimal: 1440px. Max content width: 1200px centered.
- Responsive breakpoints: 1280px (desktop), 768px (tablet), 375px (mobile web).
- Photo upload via drag-and-drop on desktop. Fallback to file picker.
- Recap playback via native HTML5 video player with custom controls overlay.
- Widgets are not available on web — surface equivalent content in a persistent sidebar panel.

### 9.4 Web (Mobile Browser)

- Mobile web is a reduced-feature companion, not the primary experience. Users are nudged toward the native app via a smart banner on first visit.
- Core flows available: view trip, upload photo, view/share recap. Reactions and comments available.
- Bottom navigation replaced by sticky top nav + floating orange Upload FAB.
- Camera capture via native browser media APIs — quality will be lower than native. Display "For the best experience, use the app" tooltip on first camera attempt.

---

## 10. Design Token Reference

These tokens should be implemented in the design system (Figma variables) and mirrored in code (CSS custom properties / iOS design tokens / Android resource values).

### 10.1 Spacing Scale

| Token | Value & Usage |
|---|---|
| `space-1` | 4px — micro spacing, icon padding |
| `space-2` | 8px — tight element spacing |
| `space-3` | 12px — compact list items |
| `space-4` | 16px — standard component padding (base unit) |
| `space-6` | 24px — card internal padding, section gaps |
| `space-8` | 32px — section breaks, modal padding |
| `space-12` | 48px — large section dividers, hero padding |
| `space-16` | 64px — screen-level top padding (iOS safe area reference) |

### 10.2 Border Radius Scale

| Token | Value & Usage |
|---|---|
| `radius-sm` | 8px — input fields, small badges |
| `radius-md` | 12px — photo cards, buttons |
| `radius-lg` | 16px — album cards, bottom sheets |
| `radius-xl` | 24px — modals, large cards |
| `radius-full` | 9999px — avatars, reaction pills, FABs |

### 10.3 Shadow Scale

| Token | Value & Usage |
|---|---|
| `shadow-sm` | `0 2px 8px rgba(27, 127, 204, 0.08)` — subtle card lift |
| `shadow-md` | `0 4px 16px rgba(27, 127, 204, 0.12)` — standard card elevation |
| `shadow-lg` | `0 8px 32px rgba(27, 127, 204, 0.18)` — modals, bottom sheets |
| `shadow-orange` | `0 4px 12px rgba(244, 120, 32, 0.24)` — orange FAB, orange CTAs |

---

## 11. MVP Scope & Design Priorities

> **MVP Design Principle:** Every screen that is not in MVP scope should have a placeholder state rather than being absent. Empty tabs, "Coming Soon" states, and locked feature indicators must still look polished and on-brand. A half-finished UI is worse than a beautiful placeholder.

### 11.1 In Scope for MVP

- Onboarding (4-screen flow)
- Home screen with album grid and activity strip
- Trip Album view with photo grid, upload, and photo viewer
- Trip Recap generation, playback, and sharing **(Priority Flow)**
- Invite flow (link-based invite, no phone number required)
- Basic privacy controls (album visibility: Group Only / Share via Link)
- Home screen widget (Small and Medium sizes — iOS and Android)
- User profile (name, avatar, trips count)
- Offline upload queue (photos queued, uploaded when connection returns)
- Push notifications (new upload, new reaction, recap ready)

### 11.2 Explicitly Out of Scope for MVP

- Dark mode
- Map view / trip route visualization
- Smart trip auto-detection (location-based)
- Apple Watch complication
- Android tablet optimized layout
- In-app video editor / manual recap editor
- Third-party integrations (Instagram import, Google Photos sync)
- Explore / Discovery tab (public trips)
- Monetization / premium tier

---

## 12. Document Notes

This specification covers UI/UX design direction for Thykra MVP v1.0. It is a living document and should be updated as design validation, user testing, and engineering constraints inform decisions.

Design files should be maintained in Figma with components, variables, and auto-layout matching the token system defined in Section 10. All component states (default, hover, pressed, disabled, loading) must be specified before engineering handoff.

> **Next Steps**
>
> 1. Validate onboarding and home screen flows with 5 target users before high-fidelity design begins.
> 2. Prototype the Recap playback experience in Figma and test emotional resonance.
> 3. Confirm music licensing approach with legal before committing to in-app music feature.
> 4. Establish Figma component library with all tokens from Section 10 before screen design begins.

---

*— Thykra Design Specification v1.0 —*