# Aptoide Store Listing Package

Ready-to-upload assets and copy for every GoConsoleOS companion app on
[Aptoide](https://www.aptoide.com).

## Contents

Each app has its own folder under `./aptoide/<app>/`:

| File | Purpose | Spec |
|---|---|---|
| `icon.png` | App icon | 512×512 PNG |
| `icon-192.png` | Small icon / notification | 192×192 PNG |
| `icon-48.png` | Tiny icon | 48×48 PNG |
| `feature.png` | Store feature graphic (banner) | 1024×500 PNG |
| `description.md` | Short + full description copy | — |

## Regenerating assets

```powershell
powershell -ExecutionPolicy Bypass -File .\generate-assets.ps1
```

## Apps included

1. **GoConsoleOS Portable** — the flagship launcher that discovers your USB
   console on Wi-Fi and opens every tool.
2. **GoConsoleOS Link** — browse and launch your host's game library over LAN
   (Steam Link style).
3. **GoConsoleOS Cast** — screen-cast your device to the console/TV.
4. **USB Health** — SMART health reports for every portable USB game console.
5. **GoAI** — the offline gaming assistant.

> Note: all five are features of the single `GoConsoleOS-Portable.apk`
> (v1.2.0). They are listed separately here so you can publish one store
> listing per use-case — upload the same APK to each listing, or just the
> flagship one if you prefer a single entry point.

## Upload checklist (per listing)

1. Create the app on developer.aptoide.com.
2. Upload the release APK
   (`apps/portable/build/outputs/apk/release/portable-release.apk`).
3. Set the title, short description and full description from `description.md`.
4. Upload `icon.png` and `feature.png`.
5. Add phone screenshots (drop into each app's `screenshots/` folder if you
   capture them; slugs are reserved).
6. Set age/content rating and save.

Screenshots are intentionally not auto-generated — capture them from a test
device or emulator so the store shows real UI. Reserved file names:
`screenshots/shot-1.png` … `shot-N.png` (phone, 1080×1920 portrait).