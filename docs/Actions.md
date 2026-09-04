# Actions — things only you can do

Items Claude cannot complete from inside the repo: credentials, hosting,
merges, and product calls. Check items off as you complete them; keep the
template at the bottom for future entries.

_Last updated: 2026-08-06 (Wanderlust Editions redesign)._

## Now — required for the redesign to fully work

- [ ] **Review & merge `redesign/wanderlust-editions`** — 4 commits on the
  branch; no PR was opened (open one when you're ready). The web app is
  rebuilt and the server gained new endpoints; see
  [Wanderlust-Editions.md](./Wanderlust-Editions.md) for the summary and
  the flagged product decisions (multi-use invite links, self-leave,
  recaps beyond spec).
- [ ] **Set the Google OAuth client id for the web app** — the login and
  landing pages render the real `<GoogleLogin>` button only when
  `VITE_GOOGLE_CLIENT_ID` is set at build time (and the server needs the
  matching `GOOGLE_CLIENT_ID`). Add your web client id to the deployment
  env and to `.env.prod`.
- [ ] **Approve the invite-semantics change** — invite links are now
  multi-use within expiry (previously one-shot). If you want single-use
  links back, say so and the join gate can re-consume `usedBy`.

## Soon — infrastructure

- [ ] **CI needs `dl.google.com`** — the sandbox here blocked it, so full
  multiplatform builds (Android Gradle Plugin) couldn't run; only JVM/server
  and the web app were built. Make sure your CI can reach
  `dl.google.com`, and add jobs for: `:server:test`,
  `webApp: tsc + vitest + vite build`.
- [ ] **Per-trip link unfurls (OG tags)** — a static SPA can't vary
  `<meta og:*>` per URL. A site-wide static floor is shipped in
  `webApp/index.html`; for per-trip previews in WhatsApp/iMessage you need
  edge injection (Cloudflare Worker / Vercel Edge / server-side render of
  `index.html` for `/s/:id` and `/r/:token`).

## Later — product decisions

- [ ] **Member role changes** — the settings screen omits "change role"
  because no endpoint exists (`PATCH /api/albums/{id}/members/{userId}`).
  Decide whether roles should be editable post-join; the UI slot is ready.
- [ ] **Owner leave/transfer** — owners currently cannot leave their own
  trip (by design). Decide if ownership transfer should exist.
- [ ] **Mobile apps and the new endpoints** — Android/iOS still use the old
  single-use join flow semantics. DTO changes were additive-only so nothing
  breaks, but the apps won't show join counts, invite previews, activity
  feeds, or recaps until they adopt the new endpoints.

---

## Template for future entries

```markdown
- [ ] **<Short imperative title>** — what needs doing, why it can't be done
  from the repo, and where the relevant code/config lives. Owner: <you/name>.
  Due: <date or "when convenient">.
```

Sections: **Now** (blocking or user-facing), **Soon** (infrastructure),
**Later** (product decisions). Move completed items to a dated
"Done" section at the bottom rather than deleting them.

## Done

*(nothing yet)*
