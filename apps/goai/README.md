# GoAI (Android)

**GoAI — the AI for GoConsoleOS, by GoStudios** — the same app that ships as the
Windows/Electron build, packaged as a native Android app.

The full web app (chat, weather, images, code, research, currency, timers,
games, cities and GoStudios Cloud accounts) runs unmodified inside a WebView.
The web code uses only public HTTPS APIs and `localStorage`, so no Electron
APIs are needed on device.

## Support

- Android 12 – 16 (`minSdk 31`, `targetSdk 36`).
- Stability hardening in `MainActivity`:
  - memory-aware rendering (software/hardware layer),
  - WebView renderer-crash recovery (`onRenderProcessGone` restarts the app),
  - fallback page on load failure,
  - microphone + location permissions for voice chat and GPS weather,
  - back-button navigation inside the WebView,
  - large heap + config-change handling so rotation does not reload the app.

## Build

```sh
./gradlew :apps:goai:assembleRelease
# -> apps/goai/build/outputs/apk/release/...
```

Release builds sign with the keystore from `$ANDROID_APP_KEYSTORE` (falls back
to the debug key). `assembleDebug` builds an installable debug APK.

## Web assets

The app's web files live in `src/main/assets/www` (copied from the GoAI web
project). Keep `index.html`, `main.js`, `preload.js`, `package.json` and the
`assets/` folder in sync when the desktop app is updated.