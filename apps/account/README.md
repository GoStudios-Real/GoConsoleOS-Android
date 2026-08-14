# GoConsoleOS Account Portal (Android)

A native Kotlin app that connects to a GoConsoleOS console on your LAN and opens the
Account Center web portal in a WebView.

It uses the repo's `:shared` SDK to auto-discover consoles via the UDP beacon, and also
accepts a manual IP address. The web portal (sign-in, devices, map, wallet, Game Pass
subscriptions & gift cards, friends, GoAI) is served by the console on port 39210.

## Build

```bash
gradle :apps:account:assembleDebug
```

The debug APK is written to `apps/account/build/outputs/apk/debug/`.

## Install & use

1. Install the APK on your phone.
2. Open it — it scans Wi-Fi for a GoConsoleOS console and prefills the IP. Otherwise enter
   the console's IP manually (port 39210).
3. Sign in with your GoConsoleOS account and manage your devices, wallet and Game Pass
   from anywhere on your network.

## Limitations

- Requires a GoConsoleOS console on your LAN (port 39210) — it is a remote client, not a
  full console emulator. The Windows WPF shell cannot run on Android.