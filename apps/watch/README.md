# GoConsoleOS Watch Companion (Wear OS)

A Wear OS companion app that discovers your GoConsoleOS console on Wi-Fi and shows
connection status, Game Pass tier, and game count — glanceable from your wrist.

## Features

- **Auto-discover** your GoConsoleOS console on the local network (UDP beacon)
- **Connection status** at a glance
- **Game Pass tier** indicator (Free / Pro / Plus / Premium / Ultimate)
- **Game count** from your library
- **One-tap disconnect** to return to discovery

## Build

Prerequisites: .NET 9 SDK (for shared SDK) and Android SDK with Wear OS platform.

```bash
gradle :apps:watch:assembleDebug
```

The debug APK is written to `apps/watch/build/outputs/apk/debug/`.

## Install & use

1. Install the APK on your Wear OS watch (via ADB or Play Store).
2. Open it — it scans Wi-Fi for a GoConsoleOS console.
3. Tap a console to see its status.
4. Disconnect to return to discovery.

## Requirements

- Wear OS 3.0+ (API 30+)
- A GoConsoleOS console on your LAN (port 39100 for discovery)