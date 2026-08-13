# GoConsoleOS Android

Companion apps for **GoConsoleOS** — the controller-first gaming console shell for Windows USB.

This repository contains full Android Studio source for the **GoConsoleOS** companion apps:

| App | Module | What it does |
|---|---|---|
| 🎮 **USB Controller** | `apps/touchscreen` | Touch gamepad for GoConsoleOS: two analog sticks, D-pad, A/B/X/Y, shoulder/trigger pads and Start/Back, streamed to the host over the LAN as input frames |
| 🖥️ **GoConsoleOS Mobile** | `apps/portable` | Bundles Portable USB, Link, USB Health and Cast on one device |

The **GoConsoleOS Mobile** app features:

| Feature | What it does |
|---|---|
| 🖥️ **Portable USB** | Find your GoConsoleOS USB host on Wi-Fi and open the **Portable App** panel — a full launcher for host tools: GoUsbMaker, USB Health, Cast, GoAI, GoStore and Screenshot (`tools.list` / `tools.run`) |
| 📡 **GoConsoleOS Link** | LAN transport to browse and launch the host's game library over the same Wi-Fi (Steam Link style) |
| 🛡️ **USB Health** | Local on-device volume & health check plus the host's deep SMART report for portable USB drives (Android 13/16) |
| 📺 **GoConsoleOS Cast** | Screen-cast the device to the host / TV via the same transport |
| 🖧 **On-device server** | The device hosts its own **ACC** account API + **GoAI** assistant on HTTP port **39210** (`/api/acc/*`, `/api/goai`, `/api/info`) — same as the desktop console |

## Project layout

```
apps/portable/       # GoConsoleOS Mobile (Portable, Link, USB Health, Cast)
apps/touchscreen/    # GoConsoleOS USB Controller — touch gamepad app
shared/              # SDK: discovery, LAN transport, models, input packer (zero third-party deps)
docs/                # PROTOCOL.md, HOST.md
.github/workflows/   # CI that builds both APKs on every push
```

The SDK (`/shared`) depends only on the Android framework and `org.json`
(part of the OS), so it compiles with the stock Android toolchain. It also
contains `ControllerInput`, the `FRAME_INPUT` payload packer shared by the
touch gamepad.

## Protocol summary

```
UDP :39100   discovery    GCS hello → hosts reply with a JSON beacon
TCP :39101   streaming    control lines + length-prefixed binary frames (JPEG, USB, cast)
```

Control messages include `hello`, `games.list`, `games.launch`, `usb.list`,
`pair`, `cast.start`/`cast.stop`, and the Portable App panel's `tools.list`
(fetch the remote tool catalogue) and `tools.run` (run a tool by id).

See [docs/PROTOCOL.md](docs/PROTOCOL.md) for the wire format and
[docs/HOST.md](docs/HOST.md) for the Windows host service.

## Building

Open this folder in Android Studio and Run, or on any machine with the Android
SDK + JDK 11:

```bash
./gradlew :apps:portable:assembleDebug :apps:touchscreen:assembleDebug
# -> apps/portable/build/outputs/apk/debug/...
# -> apps/touchscreen/build/outputs/apk/debug/...
```

CI builds both debug APKs on every push to `main`.

## Windows host side

The matching host component lives in the **GoConsoleOS** main repo under
`src/GoConsoleOSOS.Link/` — it answers discovery beacons, serves the game
library, streams health and accepts Casts through the same socket transport.

## License

Proprietary GoStudios software · © 2026 GoStudios.