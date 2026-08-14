# GoConsoleOS Account Portal (Android)

A lightweight Android client for the GoConsoleOS Account Center. It wraps the console's
web portal in a WebView and connects to a running GoConsoleOS console over your local
network.

> This is a **.NET MAUI** app, so it does **not** build through the Gradle project in this
> repo. It is kept here as source alongside the native Kotlin apps. Build it separately
> with the .NET SDK (see below).

## Features

- Sign in / create GoConsoleOS accounts
- Profile, devices, console map, security (2FA), wallet
- GoConsole Game Pass subscriptions (Pro / Plus / Premium / Ultimate)
- Gift card redemption and generation
- Friends and recent activity
- GoAI assistant

## Build

Prerequisites: .NET 9 SDK with the `maui-android` workload and the Android SDK.

```bash
dotnet publish apps/account-portal/GoConsoleOS.Mobile.csproj -f net9.0-android -c Release
```

The signed APK is written to
`apps/account-portal/bin/Release/net9.0-android/com.gostudios.goconsoleos-Signed.apk`.

## Install & use

1. Copy the APK to your phone and install it (allow unknown sources).
2. Open the app and enter your console's IP address (port defaults to 39210).
3. Sign in with your GoConsoleOS account and manage your devices, wallet and Game Pass
   from anywhere on your network.

## Limitations

- Requires a GoConsoleOS console on your LAN (port 39210) - it is a remote client, not a
  full console emulator. The Windows WPF shell cannot run on Android.