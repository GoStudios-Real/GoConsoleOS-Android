# GoConsoleOS Link / Cast transport

Version 1.2.0 — shared wireless LAN transport between the Windows GoConsoleOS
host and the Android companion app.

## Ports

| Port | Protocol | Purpose |
|---|---|---|
| 39100 | UDP | Discovery beacons. Phone sends `GCS` hello to 255.255.255.255; a GoConsoleOS host replies with a host beacon. |
| 39101 | TCP | Control + media stream. |

## UDP discovery

Client sends on loopback & broadcast every 2s:

```json
{"id":"GCS","version":"1.2.0",kind":"hello"}
```

Host replies (unicast to the sender):

```json
{
  "id": "GCS",
  "kind": "console.os",
  "name": "GoConsoleOS",
  "address": "192.168.1.20",
  "port": 39101,
  "version": "1.7.0",
  "features": ["link","usb","cast"],
  "os": "GoConsoleOS 2.0"
}
```

The mobile app latches the reply and shows the host in the launcher.

## TCP stream (`LinkClient`)

After connecting, the client sends a control line:

```
{"type":"hello","id":"GCS","version":"1.2.0","client":"gs"}\n
```

The stream mixes two kinds of records:

### 1. Control lines (JSON, LF-terminated)

```json
{"type":"usb.list"\n}
{"type":"usb.list","devices":[{...}]}\n
{"type":"games.list","games":["Snake","Breakout","Pong","Tetris","Dino"]}\n
{"type":"games.launch","title":"Snake"}\n
{"type":"cast.start"}\n
{"type":"cast.stop"}\n
{"type":"pair","action":"open-usb-installer"}\n
{"type":"tools.list"}\n
{"type":"tools.run","tool":"goai"}\n
```

`tools.list` replies with the Portable App tool catalogue the host supports:

```json
{"type":"tools.list","tools":[{"id":"usb-installer","name":"GoUsbMaker","desc":"..."},{"id":"goai","name":"GoAI","desc":"..."}]}
```

`tools.run` triggers that tool on the host (`usb-installer`, `usb-health`,
`cast`, `goai`, `store`, `screenshot`) and replies
`{"type":"tools.run","ok":true,"tool":"..."}`.

### 2. Binary frames

5-byte header: `1 byte type` + `4 byte big-endian length`, then payload.

| Type byte | Meaning | Payload |
|---|---|---|
| 0 | hello/handshake echo | JSON |
| 1 | screenshot JPEG | JPEG bytes |
| 2 | USB health report array | JSON of `UsbDeviceInfo[]` |
| 3 | Cast video frame | JPEG bytes |
| 4 | Cast audio chunk | raw PCM |
| 5 | input (from phone → host) | 4-byte button bitmask |

### Input frame (phone → host, type 5)

4-byte big-endian bitmask matches the Windows `ControllerButtons` enum:

```
Guide=1  Back=2  Start=4  A=8  B=16
X=32  Y=64  LeftShoulder=128  RightShoulder=256
DPadUp=512  DPadDown=1024  DPadLeft=2048  DPadRight=4096
```

## USB health report

A `usb.list` reply carries an array of `UsbDeviceInfo`:

```json
{
  "type": "usb.list",
  "devices": [
    {"id":"F:","label":"GOCONSOLEOS","vendor":"Lexar","product":"USB Flash",
     "serial":"", health":"ok","healthScore":100,"total":7859077120,
     "free":3335000000,"interface":"USB","issue":"","mounted":true}
  ]
}
```

Health values: `ok` / `fair` / `poor` / `unknown`.

## Cast

A Cast session is started by the phone:

1. `MediaProjectionActivity` requests screen capture.
2. `CastService` captures frames and sends type-3 JPEG frames over the same TCP.
3. Host displays them fullscreen on the connected screen.
4. `{"type":"cast.stop"}` ends the session.

## Notes

- Same-Wi-Fi only (no NAT traversal). Host and phone must share a subnet.
- Android 13 (API 33) and Android 16 (API 36) are the supported targets; the
  app runs on API 23+.
- Cleartext TCP is fine on the LAN; the host binds only to the LAN interface.