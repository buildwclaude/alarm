# Riddle Alarm

A plain, reliable Android alarm clock with three twists that make it distinct:

1. **The set-alarm screen looks like the time of day you're setting** — the background paints itself as night, dawn, morning, afternoon, evening, or dusk, so AM/PM confusion is impossible.
2. **You must solve a riddle to stop the alarm.**
3. **After the riddle, the screen flashes blue → red → green** before you can snooze or dismiss.

Built with Kotlin + Jetpack Compose + Material 3. Fully offline — no accounts, no analytics, no internet permission.

- **Target device:** Samsung Galaxy A52s 5G (One UI, Android 12–14)
- **minSdk 26 · targetSdk 35 · compileSdk 35**

> **You never need Android Studio or Gradle on your computer.** Every build runs on GitHub Actions and produces an installable APK attached to the [Releases](../../releases) page.

---

## 📲 How to download & install the app on your phone

1. On your phone, open the **[Releases page](../../releases/latest)** of this repo in your browser.
2. Under **Assets**, tap **`riddle-alarm-latest.apk`** to download it.
3. Open the downloaded file (tap the download notification, or find it in your **Files → Downloads**).
4. The first time, One UI will say installing from your browser is blocked. Tap **Settings** on that prompt, turn on **"Allow from this source"** for your browser (Chrome/Samsung Internet), then go back and tap **Install**.
5. Open **Riddle Alarm**.

Because every build keeps the same package name and is signed with the standard Android debug key, **installing a new APK updates the old one in place** — your alarms are kept.

### After installing — two taps I'll ask you for
- **Notifications:** allow them so the alarm can show over your lock screen.
- **Battery (important on Samsung):** One UI aggressively closes background apps, which can silence alarms. The app shows a one-time card that jumps you to **Settings → Battery → Riddle Alarm → set to "Unrestricted."** Please do this once.

---

## 🔨 How building works (for the curious)

There is nothing to install locally. The pipeline lives in
[`.github/workflows/build.yml`](.github/workflows/build.yml):

- Runs on every push to `main` (and on demand via **Actions → Build APK → Run workflow**).
- Uses Ubuntu + JDK 17 (Temurin) + a cached Gradle.
- **`build`** job compiles and assembles the **debug** APK. If the code doesn't compile, this fails loudly and nothing is published.
- **`lint`** job runs Android Lint in parallel.
- **`release`** job runs only after both pass, and publishes the APK to a rolling **`latest`** GitHub Release so the download link on your phone always points at the newest good build.

The APK is also saved as a **workflow artifact** on each run.

---

## 🔊 How to change the alarm sound

The bundled alarm tone lives at **`app/src/main/res/raw/alarm_default.ogg`**.

To swap it:
1. Replace that file with any `.ogg`, `.mp3`, or `.wav` file (keep a short, loud, loopable tone).
2. If you change the file extension, update the one reference in
   `app/src/main/java/com/buildwclaude/alarm/alarm/AlarmSound.kt`.
3. Commit and push — the next build picks it up.

The current tone is a simple rising two-tone chime generated as public-domain audio (see `tools/` if present). Adding *more* selectable sounds later is a one-line change in `AlarmSound.kt`.

---

## Project status

Built in stages, each a single commit with a green build:

- [x] **Step 1** — Project skeleton + CI producing an installable APK
- [x] **Step 2** — Room database + scheduling + reliable ringing
- [x] **Step 3** — Time-of-day adaptive picker
- [x] **Step 4** — Riddle-to-dismiss screen
- [x] **Step 5** — Blue/red/green flash sequence
- [x] **Step 6** — Settings + polish
