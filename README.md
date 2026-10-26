# Shoop

An offline-first Android companion for your WHOOP band (or any Bluetooth heart-rate band).
Live heart rate and HRV, local strain / recovery / sleep scores, and on-device history -
no account, no cloud, no internet permission.

Shoop is written from scratch in Kotlin with Jetpack Compose (Material 3).

## Screenshots

| Live | Today | Sleep | Trends |
|---|---|---|---|
| ![Live](docs/screenshots/screen_live.png) | ![Today](docs/screenshots/screen_today.png) | ![Sleep](docs/screenshots/screen_sleep.png) | ![Trends](docs/screenshots/screen_trends.png) |

Screenshots are real renders of the app's own UI code, produced through Android's
layoutlib (Paparazzi) with demo data - see *Testing* below for exactly what that means.

## Features

- **Live heart rate over BLE** - connects to any band broadcasting the standard
  Bluetooth Heart Rate Service (0x180D / 0x2A37). On WHOOP, enable
  *HR Broadcast* in the official WHOOP app first.
- **Real HRV** - beat-to-beat RR intervals from the band, rolling RMSSD computed on-device.
- **Local scores** - strain (0-21), recovery (0-100, green/yellow/red), and sleep score,
  all computed on your phone from your own recorded data. Formulas are original and
  documented in [docs/SCORING.md](docs/SCORING.md).
- **Battery level** from the standard Battery Service (0x2A19).
- **History on-device** - recordings and daily summaries stored as local files/DataStore.
  Nothing leaves the phone: the APK requests **no INTERNET permission**.
- **Demo mode** - a clearly-labelled simulated heart-rate source that exercises the same
  parsing, HRV, scoring and UI pipeline as a real band, so the app can be tried and
  tested without hardware.
- **Dark, glanceable UI** - Jetpack Compose, Material 3.

## Privacy

Offline by design. No analytics, no ads, no accounts, no servers. The only permissions
are Bluetooth (and location on Android 11 and below, which Android requires for BLE
scanning - Shoop never reads your location).

## Build

Requirements: JDK 17, Android SDK (platform 35, build-tools 35.0.0).

```bash
./gradlew :app:assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew :app:testDebugUnitTest    # unit tests (parser, HRV, scoring)
./gradlew :app:recordPaparazziDebug # regenerate the screenshots in docs/screenshots/
```

A prebuilt debug APK is attached to the latest
[release](https://github.com/nekxxy/shoop/releases).

## Install

1. Download the APK from Releases onto your phone.
2. Allow "install from unknown sources" when Android asks.
3. In the official WHOOP app, turn on HR Broadcast (Strap Settings).
4. Open Shoop, grant Bluetooth permission, tap **Find band**, pick your band.

## Testing status - verified vs untested

**Verified in this repo's CI-style local run:**
- Compiles clean (AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 26).
- 19 unit tests pass: BLE Heart Rate Measurement byte parsing (all flag combinations),
  RMSSD math against hand-computed values, outlier rejection, strain/recovery/sleep/zone
  score bounds and monotonicity.
- All four screens render correctly (screenshots above), exercised end-to-end through
  the real parsing/scoring pipeline using the demo source.

**Not yet verified:**
- **Pairing with a physical WHOOP 5.0 has NOT been tested** - no band was available to
  the developer environment. WHOOP 5.0/MG is known to refuse parts of the classic
  pairing handshake with third-party apps, so live BLE streaming from a 5.0 may need
  follow-up work. With *HR Broadcast* enabled the band advertises the standard Heart
  Rate Service, which is what Shoop reads - this is expected to work, but it is
  unproven until tried on real hardware.
- Sleep detection currently scores recordings you capture overnight; automatic
  sleep-stage detection (deep/REM) is not claimed.
- Background recording (screen off) is not in v1; keep the app open while recording.

## License

MIT - see [LICENSE](LICENSE).
