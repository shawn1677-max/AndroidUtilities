# Play Store listing

Copy for the Google Play Console entry, plus the answers the review process
asks for. Nothing here is filled in speculatively — every claim matches what
the code actually does.

## App details

- **App name:** Utility Box
- **Short description (max 80 characters):**
  `30 offline tools: device info, level, ruler, QR, converters and more.`
- **Category:** Tools
- **Content rating:** Everyone (no ads, no purchases, no user content)
- **Contains ads:** No
- **In-app purchases:** No
- **Package name:** `com.utilitybox.app`

## Full description

> **One small app instead of thirty. Completely offline.**
>
> Utility Box collects the tools you actually reach for — a level, a ruler, a
> compass, a QR scanner, a unit converter, a stopwatch — into one app that
> works everywhere, including on a plane or down a basement with no signal.
>
> It has no internet permission. Not "we promise not to use it" — the app
> cannot connect to the internet at all. No ads, no accounts, no tracking, no
> analytics.
>
> **Know your device**
> • Device Info — model, chipset, RAM, screen, Android build, uptime
> • Battery — level, health, temperature, voltage, live current draw
> • Storage — what is using space across every volume
> • Network Info — connection type, IP addresses, DNS servers
> • Sensor Explorer — every sensor your phone has, with live readings
> • App Inventory — versions, sizes and install dates for your apps
>
> **Measure the world**
> • Compass with a proper drawn rose and calibration guidance
> • Bubble level for shelves, pictures and worktops
> • Screen ruler you can calibrate against a bank card
> • Sound meter with a rolling history graph
> • Flashlight with strobe and an SOS beacon
> • Stopwatch with laps, countdown timer, and a metronome with tap tempo
>
> **Test your hardware**
> • Screen test — eight patterns that reveal dead and stuck pixels
> • Touch test — find dead zones and check multi-touch
> • Vibration test — patterns and adjustable strength
> • Tone generator — 20 Hz to 20 kHz for checking speakers and earphones
>
> **Convert and encode**
> • Unit converter — 11 categories and more than 80 units
> • Number bases, colour codes with WCAG contrast, text transforms
> • MD5 and SHA hashes, Base64, Morse code
> • QR generator for text, links, phone numbers and Wi-Fi details
> • QR and barcode scanner that never opens a link without asking
>
> **Calculate**
> • Tip splitting, percentages, date arithmetic, BMI
> • Password and passphrase generator with honest strength estimates
> • Dice, coin flips and random picking
>
> **Privacy by construction**
> The camera is used only in the QR scanner, and the microphone only in the
> sound meter — both ask when you open that tool, and neither ever records
> anything. Everything else needs no permission at all.
>
> Free, no ads, no subscription, no catch.

## Data safety form

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **No** |
| Is all of the user data collected by your app encrypted in transit? | Not applicable — no data is transmitted |
| Do you provide a way for users to request that their data is deleted? | Not applicable — no data is collected |

Rationale to keep on file: the app declares no `INTERNET` permission, so it
cannot transmit data. Camera and microphone input is processed in memory and
discarded. The only persisted value is a screen-ruler calibration number in
the app's private preferences.

## Permission declarations

Neither of the runtime permissions is on Google's restricted list, so no
declaration form is required. If asked:

- **CAMERA** — used solely to decode QR codes and barcodes in the QR Scanner.
  Frames are analysed in memory and discarded; no image is ever captured,
  stored or transmitted.
- **RECORD_AUDIO** — used solely to compute a loudness figure in the Sound
  Meter. Audio buffers are converted to an RMS value and discarded; no
  recording is created.

The app deliberately does **not** use `QUERY_ALL_PACKAGES`, any location
permission, or any storage permission.

## Store assets checklist

These are the images Play requires. They are not in the repository because they
need to be captured from a real device.

- [ ] App icon, 512 × 512 PNG (source vector: `app/src/main/res/mipmap/ic_launcher.xml`)
- [ ] Feature graphic, 1024 × 500 PNG
- [ ] Phone screenshots, 2–8 images, minimum 1080 px on the short edge.
      Suggested set: home screen with search, Device Info, Compass, Bubble
      Level, Unit Converter, QR Generator, Sound Meter, Password Generator
- [ ] 7-inch and 10-inch tablet screenshots, if publishing for tablets

## Release checklist

1. Bump `versionCode` and `versionName` in `app/build.gradle.kts`.
2. `./gradlew testDebugUnitTest lintRelease` — both must pass.
3. `./gradlew bundleRelease` with `keystore.properties` present.
4. Upload `app/build/outputs/bundle/release/app-release.aab`.
5. Host `PRIVACY.md` at a public URL and paste that URL into the listing.
6. Roll out to internal testing first, then production.
