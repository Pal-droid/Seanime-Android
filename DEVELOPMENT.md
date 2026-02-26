# Pal-droid Development Guide

A cursed but functional Android port of Seanime. The app runs the full Seanime Go backend as a foreground service and wraps the React web frontend in a WebView.

## How It Works

```
┌─────────────────────────────────────────────┐
│              Android APK                     │
│                                              │
│  ┌─────────────────────────────────────────┐ │
│  │  Kotlin Shell (MainActivity + Service)  │ │
│  │  - Foreground Service lifecycle         │ │
│  │  - WebView loading localhost:43211      │ │
│  │  - Permission management                │ │
│  │  - Shutdown notification button         │ │
│  └────────────┬────────────────────────────┘ │
│               │ runs binary                  │
│  ┌────────────▼────────────────────────────┐ │
│  │  libseanime.so (Go binary)              │ │
│  │  - Full Seanime server                  │ │
│  │  - Embedded web assets                  │ │
│  │  - SQLite database                      │ │
│  │  - Torrent client                       │ │
│  │  - AniList API, extensions, etc.        │ │
│  └─────────────────────────────────────────┘ │
└─────────────────────────────────────────────┘
```

The Go binary is compiled for Android and placed in `app/src/main/jniLibs/` as `libseanime.so`. Android extracts it at install time, the foreground service runs it directly, and the app opens a WebView pointed at `localhost:43211`.

---

## Building the binary

Follow these steps in order.

### 1. Setting up

Clone the official seanime repo and cd into it

```bash
git clone https://github.com/5rahim/seanime
cd ~/seanime
```

### 2. Build the Go Binary

```bash
GOOS=android GOARCH=arm64 CGO_ENABLED=0 go build -tags android -ldflags="-s -w" -o seanime-server .
```

Then rename it to `libseanime.so` and place it in the correct JNI folder in the `seanime-android` repo.

#### Multi-Architecture Support

To build for other architectures, change `GOARCH` and place the binary in the corresponding folder:

| Architecture | GOARCH | Folder |
|---|---|---|
| ARM64 (most modern phones) | `arm64` | `jniLibs/arm64-v8a/` |
| ARM 32-bit | `arm` + `GOARM=7` | `jniLibs/armeabi-v7a/` |
| x86_64 (emulators) | `amd64` | `jniLibs/x86_64/` |

Gradle will automatically bundle the right binary for each device at install time.

### 3. Build the APK

Open the project in **Android Studio** or **CodeAssist** (on-device) and build from there. Manual Gradle builds are possible but brittle and not recommended.

---

## Permissions

| Permission | Reason |
|---|---|
| `INTERNET` | Network access for API calls and streaming |
| `FOREGROUND_SERVICE` | Keep server running in background |
| `FOREGROUND_SERVICE_MEDIA_PLAYBACK` | Classifies the foreground service type |
| `POST_NOTIFICATIONS` | Foreground service notification + shutdown button |

`usesCleartextTraffic="true"` is also set in the manifest to allow the WebView to reach the Go server over `http://localhost`.

---

## Features

| Feature | Status | Notes |
|---|---|---|
| Web UI | ✅ | Mobile-optimized via `SEA_PUBLIC_PLATFORM="mobile"` |
| Torrent client | ✅ | Pure Go, works natively |
| SQLite database | ✅ | Pure Go SQLite (glebarez/sqlite) |
| File scanner | ✅ | Works on internal storage |
| AniList API / Extensions | ✅ | |
| Online streaming playback | ✅ | HLS.js in WebView, mobile gestures supported |
| Fullscreen | ✅️ | Fullscreen button non-functional in WebView |
| Torrent streaming video | ❌ | Requires MPV — potential future fix via libmpv |
| System tray | ❌ | Not applicable on Android |
| Discord RPC | ❌ | No named pipe IPC on Android |
| Desktop notifications | ❌ | Could be added via Android notifications |
| Self-updater | ❌ | Manual APK update required |

---

## Debugging

### Logs

```bash
# All Seanime-related logs
adb logcat | grep -i seanime

# Specific tags
adb logcat SeanimeService:D MainActivity:D *:S
```

### Server Logs

```bash
adb shell run-as com.seanime.app ls files/logs
adb shell run-as com.seanime.app cat files/logs/
```

### WebView Debugging

WebView debugging is not enabled by default. To enable it, add this to `MainActivity.kt` inside `setupWebView()`:

```kotlin
WebView.setWebContentsDebuggingEnabled(true)
```

Then open `chrome://inspect/#devices` in Chrome.

### Server Not Responding

The app waits 2 seconds before loading `http://127.0.0.1:43211` and retries every 2 seconds if the server isn't up yet. If it never loads:

```bash
# Check if server started
adb logcat | grep "Go server started"

# Test server directly
adb shell curl http://127.0.0.1:43211
```

---

## Performance

- **RAM**: ~150–300MB (Go server + WebView)
- **Storage**: ~50MB binary, data varies
- **Battery**: Moderate impact due to foreground service

---

## Future Improvements

- [ ] Android-native notifications
- [ ] File picker for external storage
- [ ] Embedded MPV via libmpv for native torrent streaming video
- [ ] Android TV support
- [ ] Auto-update mechanism
- [ ] Split APKs per architecture for smaller downloads

---

## License

Same as the main [Seanime](https://github.com/5rahim/seanime) project.
