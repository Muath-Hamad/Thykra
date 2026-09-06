# Getting Thykra onto an iPhone

Three routes, cheapest first.

## 1. Xcode on a Mac, free account — 10 minutes, expires after 7 days

No CI, no IPA, no paid membership.

1. Open `iosApp/iosApp.xcodeproj`.
2. Select the `iosApp` target → Signing & Capabilities → tick **Automatically manage
   signing** and pick your personal team.
3. Set `THYKRA_API_BASE_URL` in `iosApp/Configuration/Config.xcconfig` to something the
   phone can reach — your Cloudflare tunnel, or `http://<your-mac>.local:8081` on the
   same Wi-Fi. **Not `localhost`: on a phone that is the phone.**
4. Plug the phone in, pick it as the destination, hit Run.

The build stops working after 7 days and you re-run it. Fine for looking at the redesign,
not for living with it.

## 2. TestFlight — needs the paid Apple Developer Program

Best if you want it on your phone for more than a week, or want to hand it to anyone else.

**One-time setup**

1. Join the Apple Developer Program ($99/yr) and note your **Team ID** (10 characters,
   under Membership).
2. App Store Connect → Users and Access → Integrations → **App Store Connect API** →
   generate a key with the **App Manager** role. You get:
   - a Key ID
   - an Issuer ID (a UUID)
   - a `AuthKey_XXXXXXXX.p8` file, **downloadable exactly once**
3. Register the bundle identifier `com.jameeli.thykra.Thykra` and create the app record in
   App Store Connect.
4. Enable the **App Groups** capability for `group.com.jameeli.thykra` on the app id — the
   token is shared with the widget extension through it.
5. Add these repository secrets (Settings → Secrets and variables → Actions):

   | Secret | Value |
   |---|---|
   | `APPLE_TEAM_ID` | your 10-character team id |
   | `APPLE_API_KEY_ID` | the Key ID from step 2 |
   | `APPLE_API_ISSUER_ID` | the Issuer ID from step 2 |
   | `APPLE_API_PRIVATE_KEY` | the whole contents of the `.p8`, newlines included |
   | `THYKRA_API_BASE_URL` | e.g. `https://api.thykra.com` |
   | `THYKRA_WEB_BASE_URL` | e.g. `https://thykra.com` |

**Each build**

Actions → **iOS IPA** → Run workflow → method `app-store`. Or push a `v*` tag.

The workflow fails fast and loudly if a secret is missing or if the API origin points at
localhost, rather than handing you an app that installs and then cannot talk to anything.

TestFlight processing takes 5–15 minutes after upload. Then install the TestFlight app on
the phone and the build appears.

## 3. Ad-hoc IPA — paid membership, and every device registered in advance

Actions → **iOS IPA** → Run workflow → method `ad-hoc`. Download the artifact.

Install with Apple Configurator, or drag the `.ipa` onto the device in Xcode's
Devices and Simulators window. Every target device's UDID has to be registered in the
developer portal *before* the build — an ad-hoc IPA only installs on devices baked into
its profile. Valid for a year.

## Why the API origin matters

`API_BASE_URL` is a build setting, not a constant:

- **Android** — `API_BASE_URL=` in `local.properties`, or `THYKRA_API_BASE_URL` in the
  environment. Defaults to `http://10.0.2.2:8081`, the emulator's loopback to the host.
- **iOS** — `THYKRA_API_BASE_URL` in `Config.xcconfig`, which lands in `Info.plist`.
  Empty means a simulator build and falls back to `http://localhost:8081`.

`Info.plist` allows plain http only to `.local` names and IP literals
(`NSAllowsLocalNetworking`), so a phone can reach a dev server on the same Wi-Fi while
anything deployed still has to be https.

## Known gap

`iosApp/ThykraWidgets/` has the Swift sources for the two home-screen widgets, but the
target is not in `iosApp.xcodeproj` — `grep ThykraWidgets project.pbxproj` returns
nothing. The iOS widgets are therefore not built or shipped by any of the routes above.
Adding the extension target is a manual step in Xcode; the Android Glance widgets are
unaffected.
