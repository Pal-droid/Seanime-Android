# Development Guide

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

The Go binary is compiled for Android (ARM64) and placed in `app/src/main/jniLibs/arm64-v8a/` as `libseanime.so`. Android extracts it at install time, and the foreground service runs it directly. The app then opens a WebView pointed at `localhost:43211`.

---

## Project Structure

```
/
├── app/                        # Android Kotlin project
│   └── src/main/
│       ├── java/com/seanime/app/
│       │   ├── MainActivity.kt        # WebView host
│       │   ├── SeanimeService.kt      # Foreground service
│       │   └── SeanimeApplication.kt  # App class
│       ├── jniLibs/                   # Prebuilt binaries (not in repo)
│       │   ├── arm64-v8a/libseanime.so
│       │   └── ...
│       └── AndroidManifest.xml
└── DEVELOPMENT.md
```

---

## Building the Binary

The binary is compiled from the [Seanime](https://github.com/5rahim/seanime) source with Android-specific build tags.

### Prerequisites

- Go 1.22+
- Seanime source code

### Build Command

```bash
GOOS=android GOARCH=arm64 CGO_ENABLED=0 \
  go build -tags netgo,android \
  -ldflags="-extldflags=-static -s -w" \
  -o ~/Seanime-Android .
```

Then rename and place it:

```bash
mv seanime-server app/src/main/jniLibs/arm64-v8a/libseanime.so
```

### Multi-Architecture Support

To support other architectures, change `GOARCH` and place the output in the corresponding folder:

| Architecture | GOARCH | Folder |
|---|---|---|
| ARM64 (most modern phones) | `arm64` | `jniLibs/arm64-v8a/` |
| ARM 32-bit | `arm` + `GOARM=7` | `jniLibs/armeabi-v7a/` |
| x86_64 (emulators) | `amd64` | `jniLibs/x86_64/` |

Gradle will automatically bundle the right binary for each device at install time.

---

## Building the APK

For building and signing the APK, use a proper IDE such as **Android Studio** or **CodeAssist** (Android). Manual Gradle builds are possible but brittle and not recommended.

---

## Permissions

Declared in `AndroidManifest.xml`:

- `INTERNET` — Network access for API calls and streaming
- `FOREGROUND_SERVICE` — Keep server running in background
- `FOREGROUND_SERVICE_MEDIA_PLAYBACK` — Classifies the foreground service as media playback
- `POST_NOTIFICATIONS` — Foreground service notification + shutdown button

`usesCleartextTraffic="true"` is also set to allow the WebView to access the Go server over `http://localhost`.

---

## Features

| Feature | Status | Notes |
|---|---|---|
| Web UI | ✅ | Mobile-optimized via `SEA_PUBLIC_PLATFORM="mobile"` |
| Torrent client | ✅ | Pure Go, works natively |
| SQLite database | ✅ | Pure Go SQLite (glebarez/sqlite) |
| File scanner | ✅ | Works on internal storage |
| Online streaming playback | ✅ | Fullscreen button doesn't work. |
| Torrent streaming video | ❌ | Requires MPV, stubbed out — potential future fix via libmpv |
| AniList API / Extensions | ✅ | |
| System tray | ❌ | Not applicable on Android |
| Discord RPC | ❌ | No named pipe IPC on Android |
| External MPV/VLC | ❌ | Stubbed out, potential future improvement |
| Desktop notifications | ❌ | Could be added via Android notifications |
| Self-updater | ❌ | Manual APK update required |

---

## Debugging

### View Logs

```bash
# All Seanime-related logs
adb logcat | grep -i seanime

# Specific tags
adb logcat SeanimeService:D MainActivity:D *:S
```

### WebView Debugging

1. Enable in `MainActivity.kt`:
   ```kotlin
   WebView.setWebContentsDebuggingEnabled(true)
   ```
2. Open `chrome://inspect/#devices` in Chrome

### Server Logs

```bash
adb shell run-as com.seanime.app ls files/logs
adb shell run-as com.seanime.app cat files/logs/<logfile>
```

### Server Not Responding

```bash
# Check if server started
adb logcat | grep "Go server started"

# Test server directly
adb shell curl http://localhost:43211
```

---

## Performance

- **RAM**: ~150–300MB (Go server + WebView)
- **Storage**: ~50MB binary, data varies
- **Battery**: Moderate impact due to foreground service + WAKE_LOCK

---

## Future Improvements

- [ ] Android-native notifications (replace no-ops)
- [ ] File picker for external storage
- [ ] Embedded MPV via libmpv for native video playback
- [ ] Android TV support
- [ ] Auto-update mechanism
- [ ] Split APKs per architecture for smaller downloads

---

## License

Same as the main [Seanime](https://github.com/5rahim/seanime) project.
