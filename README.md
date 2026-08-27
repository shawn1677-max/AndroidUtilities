# Utility Box

An offline Android toolbox: 30 practical utilities in one small app, built with
Kotlin and Jetpack Compose.

The app has **no internet permission at all**. Every tool runs entirely on the
device, so nothing it measures, generates or reads can leave the phone.

## The tools

### Device
| Tool | What it does |
| --- | --- |
| Device Info | Model, SoC, RAM, display geometry, build fingerprint, uptime |
| Battery | Live level, health, temperature, voltage, current and charge counters |
| Storage | Usage for internal, system, shared and removable volumes |
| Network Info | Connection type, validation state, bandwidth estimate, IP addresses, DNS |
| Sensor Explorer | Every sensor the device reports, with live values |
| App Inventory | Launchable apps with version, APK size, target SDK and install dates |

### Measure
| Tool | What it does |
| --- | --- |
| Compass | Magnetic heading with a drawn rose and calibration guidance |
| Bubble Level | Pitch, roll, total tilt and builder's slope |
| Screen Ruler | Centimetres or inches, calibrated against a bank card |
| Sound Meter | Ambient level in dB with history graph and calibration offset |
| Flashlight | Steady torch, adjustable strobe and an SOS beacon (also a home screen widget) |
| Stopwatch | Millisecond timing with laps and splits (also a home screen widget) |
| Countdown Timer | Presets, fine adjustment and an alarm-stream alert |
| Metronome | 30–260 BPM, accented downbeat, tap tempo |

### Hardware tests
| Tool | What it does |
| --- | --- |
| Screen Test | Eight full-screen patterns for dead and stuck pixels |
| Touch Test | Multi-touch tracking and a coverage grid for dead zones |
| Vibration Test | Duration and amplitude control plus five patterns |
| Tone Generator | 20 Hz–20 kHz sine, per-channel output, logarithmic sweep |

### Convert and encode
| Tool | What it does |
| --- | --- |
| Unit Converter | 11 categories, 80+ units, with a full conversion table |
| Number Base | Binary, octal, decimal, hex and any base 2–36, arbitrary precision |
| Colour Tool | HEX, RGB, HSL, CMYK and WCAG contrast ratios |
| Text Tools | 12 transforms plus word, line and reading-time statistics |
| Hash Generator | MD5, SHA-1, SHA-256, SHA-384, SHA-512 |
| Base64 | Encode and decode, standard or URL-safe alphabet |
| Morse Code | Text to Morse and back, optionally flashed on the torch |
| QR Generator | Text, links, phone numbers and Wi-Fi credentials |
| QR Scanner | QR, Data Matrix, Aztec, EAN, UPC, Code 128, Code 39, ITF |

### Calculate
| Tool | What it does |
| --- | --- |
| Tip Calculator | Tip, total, bill splitting and round-up |
| Percentages | Percent of, what percent, change, discount and mark-up |
| Date Calculator | Days between dates, date arithmetic, working days |
| BMI Calculator | Metric or imperial, with the healthy range for your height |
| Password Generator | Random passwords and passphrases with honest entropy estimates |
| Random Picker | Numbers, dice, coins, list picking and shuffling |

## Home screen widgets

**Flashlight** — a one-cell widget that toggles the torch without opening the app.

Because the torch is a shared system resource — the in-app tool, the quick
settings tile and other apps can all change it — a tap reads the real torch
state from the camera service rather than trusting a remembered flag, then
toggles from that. A widget showing a stale icon therefore does the right
thing on the first tap instead of appearing to do nothing.

The widget adds no permissions: `CameraManager.setTorchMode` needs none.

**Stopwatch** — start, pause and reset from the home screen; tapping the
reading opens the full tool where the laps are.

It is the *same* stopwatch as the in-app one, not a second copy: both read and
write `StopwatchStore`, so starting it on the home screen and pausing it in the
app behaves as one timer.

A widget cannot be redrawn once a second, so the running reading is drawn by a
`Chronometer`, which ticks itself inside the launcher with no app process
involved — only start, pause and reset cost a redraw. That is also why the
widget shows whole seconds while the in-app screen shows hundredths.

## Settings

A gear icon in the home app bar opens Settings.

**Appearance** chooses between *Follow system*, *Light* and *Dark*. The choice
applies immediately, persists across restarts, and re-styles the status and
navigation bar icons so they stay legible when you override the system setting.
Dynamic colour still tracks the wallpaper on Android 12 and later in both
themes.

## Permissions

The app declares four permissions and nothing else:

| Permission | Used by | Notes |
| --- | --- | --- |
| `VIBRATE` | Vibration Test | Install-time permission |
| `ACCESS_NETWORK_STATE` | Network Info | Install-time permission, read-only |
| `CAMERA` | QR Scanner only | Requested when you open that tool |
| `RECORD_AUDIO` | Sound Meter only | Requested when you open that tool |

`INTERNET` is deliberately absent, which is the strongest possible guarantee
that no tool phones home. The App Inventory uses a manifest `<queries>` filter
for launcher activities rather than the restricted `QUERY_ALL_PACKAGES`
permission, and the Network Info tool omits Wi-Fi SSID and signal strength so
that no location permission is needed.

## Building

Requirements: JDK 17, Android SDK with platform 36.

```bash
./gradlew assembleDebug        # debug APK
./gradlew testDebugUnitTest    # unit tests
./gradlew lintRelease          # static analysis
./gradlew bundleRelease        # Play Store AAB
```

`local.properties` must point at your SDK:

```properties
sdk.dir=/path/to/android-sdk
```

### Release signing

Signing material is read from `keystore.properties` in the project root, which
is git-ignored. Create it once:

```properties
storeFile=/absolute/path/to/upload-keystore.jks
storePassword=…
keyAlias=upload
keyPassword=…
```

Generate the upload key with:

```bash
keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA \
        -keysize 2048 -validity 10000 -alias upload
```

Without that file the release variant still builds, just unsigned.

## Project layout

```
app/src/main/java/com/utilitybox/app/
├── MainActivity.kt          single activity, Compose only
├── nav/                     navigation graph and the searchable home screen
├── tools/
│   ├── ToolRegistry.kt      one entry per tool: id, title, icon, search keywords
│   ├── device/              device information tools
│   ├── measure/             measurement tools
│   ├── hardware/            hardware self-tests
│   ├── convert/             converters and encoders
│   └── calculate/           calculators
├── ui/common/               shared scaffold, cards, rows, permission gate
├── ui/settings/             settings screen (appearance, about)
├── widget/                  home screen widgets (flashlight, stopwatch)
├── ui/theme/                Material 3 theme, dynamic colour, theme preference
└── util/                    formatting, audio engines, screen-on helper
```

Adding a tool means adding an id to `ToolIds`, an entry to `ToolRegistry.all`,
a screen composable, and one line in `UtilityBoxNavHost`. A unit test asserts
that the id constants and the registry stay in step.

## Tech

- Kotlin 2.2, Jetpack Compose with Material 3 and dynamic colour
- Single activity, Navigation Compose, no third-party analytics or ad SDKs
- CameraX and ZXing for barcode scanning and generation (both used offline)
- minSdk 24, targetSdk 36, R8 with resource shrinking on release
- 59 unit tests covering unit conversion, Morse, formatting, theme resolution, stopwatch timing and the registry

## Licence

See [LICENSE](LICENSE).
