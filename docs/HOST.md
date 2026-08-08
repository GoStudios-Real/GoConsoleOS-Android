# Connecting the Android app to the Windows host

The Android apps talk to a small **GoConsoleOS Link server** that lives in the
main Windows project (`GoConsoleOS` repo, `src/GoConsoleOS.Link/`). This doc
describes how that component should behave.

## Fifty-thousand foot view

```
[Android: GoConsoleOS Mobile]
        │   UDP :39100 hello
        ▼
[Windows host: GoConsoleOS.Link service]
        │   replies beacon, then:
        │   TCP :39101  control + JPEG/health/cast
        ▼
[GoConsoleOS shell on the USB]  →  socket transport library
```

## Responsibilities of the Link server

1. **Discovery** — listen for the UDP `hello`, reply with the host beacon
   (name, port 39101, version, features, os label).
2. **Game catalogue** — answer `games.list` with the titles the shell's
   library scan discovered, and handle `games.launch` (tells the shell to
   launch the app / game).
3. **USB health** — on `usb.list`, run the shell's existing USB health WMI
   query (`UsbDeviceHealthView`) and serialize the result as `UsbDeviceInfo[]`.
4. **Pair / tools** — on `pair.action=open-usb-installer`, opens the bundled
   `GoUsbMaker.exe` on the host machine.
5. **Cast sink** — accept type-3 frames and render them in an overlay window;
   accept type-4 audio toward the output device.

## Implementing (C# sketch)

The full implementation ships in the main repo at `src/GoConsoleOS.Link/` and
hangs off the same `MainWindow` lifecycle we already use for the controller
engine. Key responsibilities to wire up:

```csharp
TcpListener discoveryListener = new TcpListener(IPAddress.Any, Protocol.DISCOVERY_PORT);
// reply to hello with a JSON beacon

TcpListener media = new TcpListener(IPAddress.Any, Protocol.LINK_PORT);
// accept, read control lines / frames, route to shell actions
```

## Security notes

- Bind only to the LAN adapter, never `0.0.0.0`.
- Allowed-cleartext is fine on a private network; production would move to a
  per-device pairing code (the `pair` message is that hook).
- No secrets are placed inside the APK.