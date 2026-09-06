# Screenshots

Device captures from the emulator, kept as a running history. Nothing here is
referenced by the build — it is a scrapbook you can prune whenever you like.

## The one rule: `KEEP-` means do not delete

A filename starting with `KEEP-` is a screenshot something still depends on —
evidence attached to a Linear issue, a before/after pair for a fix, or the proof
for an acceptance test that has not been signed off yet. Everything else is
disposable; delete it whenever the folder gets noisy.

```
KEEP-2026-09-06-j6-photos-in-their-days.png   ← keep: acceptance evidence
2026-09-06-trips-launch.png                   ← disposable
```

## Naming

```
[KEEP-]<yyyy-mm-dd>-<what-it-shows>.png
```

Date first so the folder sorts chronologically, and a description rather than a
screen name — "nav-bar-clipped-before" tells you why it was taken, "TripsScreen"
does not.

## Taking one

```bash
ADB=~/AppData/Local/Android/Sdk/platform-tools/adb.exe
"$ADB" exec-out screencap -p > screenshots/2026-09-06-whatever-it-shows.png
```

Useful knobs when capturing for a specific check:

```bash
"$ADB" shell settings put system font_scale 1.5      # large-text pass
"$ADB" shell settings put system font_scale 1.0      # put it back
"$ADB" shell am start -n com.jameeli.thykra/.MainActivity
```

## Not in git

The PNGs are gitignored — they are large, they change constantly, and a repo is
a poor photo album. The folder and this README are tracked so the convention
survives a fresh clone.
