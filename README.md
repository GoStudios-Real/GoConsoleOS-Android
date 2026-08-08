# GoConsoleOS Android

Companion apps for **GoConsoleOS** — the controller-first gaming console shell for Windows USB.

This repository contains full Android Studio source for a single APK, **GoConsoleOS Mobile**,
which bundles four companion services on one device:

| Feature | What it does |
|---|---|
| 🖥️ **Portable USB** | Find your GoConsoleOS USB host on Wi-Fi, launch it, open the USB Installer |
| 📡 **GoConsoleOS Link** | LAN transport to browse and launch the host's game library over the same Wi-Fi (Steam Link style) |
| 🛡️ **USB Health** | Local on-device volume & health check plus the host's deep SMART report for portable USB drives (Android 13/16) |
| 📺 **GoConsoleOS Cast** | Screen-cast the device to the host / TV via the same transport |

## Project layout

```
apps/portable/       # the single mobile app (GoConsoleOS-Portable.apk)
shared/              # SDK: discovery, LAN transport, models (zero third-party deps)
docs/                # PROTOCOL.md, HOST.md
.github/workflows/   # CI that builds the APK on every push
```

The SDK (`/shared`) depends only on the Android framework and `org.json`
(part of the OS), so it compiles with the stock Android toolchain.

## Protocol summary

```
UDP :39100   discovery    GCS hello → hosts reply with a JSON beacon
TCP :39101   streaming    control lines + length-prefixed binary frames (JPEG, USB, cast)
```

See [docs/PROTOCOL.md](docs/PROTOCOL.md) for the wire format and
[docs/HOST.md](docs/HOST.md) for the Windows host service.

## Building

Open this folder in Android Studio and Run, or on any machine with the Android
SDK + JDK 11:

```bash
./gradlew :apps:portable:assembleDebug
# -> apps/portable/build/outputs/apk/debug/...
```

CI builds the debug APK on every push to `main`.

## Windows host side

The matching host component lives in the **GoConsoleOS** main repo under
`src/GoConsoleOSOS.Link/` — it answers discovery beacons, serves the game
library, streams health and accepts Casts through the same socket transport.

## License

Proprietary GoStudios software · © 2026 GoStudios.