# SMS Forwarder

A simple Android app that forwards every incoming SMS to an email address via Gmail SMTP.

## Features

- **Setup wizard** with inline links to Google's app-password and 2-Step Verification pages
- **Test email button** verifies SMTP credentials before completing setup
- **Dashboard** with today's count vs. 500/day cap, status filters, and per-message detail
- **Failure tracking** — every send failure stores reason, attempt count, and next retry time
- **Retry policy** — 5 attempts with exponential backoff (1m → 5m → 30m → 2h → 6h)
- **Daily cap** — at 500 sends/day forwarding pauses with a banner; resets at local midnight
- **7-day history** auto-pruned by a daily background job
- **Encrypted credential storage** (Android Keystore-backed `EncryptedSharedPreferences`)

## Build prerequisites

- JDK 17
- Android SDK with `platform-tools`, `platforms;android-35`, `build-tools;35.0.0`
- Gradle wrapper (bootstrapped on first build)

If you're on Ubuntu 24.04, the `setup-ubuntu.sh` script installs everything and bootstraps the wrapper for you.

## Quick build (Ubuntu 24.04)

```bash
# One-time setup (installs JDK + Android SDK + bootstraps the Gradle wrapper)
./setup-ubuntu.sh

# Build a debug APK
./gradlew assembleDebug

# APK ends up at:
#   app/build/outputs/apk/debug/app-debug.apk
```

## Build manually (other systems)

1. Install JDK 17 (any distribution).
2. Install Android command-line tools, then run:
   ```bash
   sdkmanager --licenses
   sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
   ```
3. Create `local.properties` in the project root:
   ```
   sdk.dir=/absolute/path/to/your/android-sdk
   ```
4. Bootstrap the Gradle wrapper jar (one time only — needs a system Gradle ≥ 8.10):
   ```bash
   gradle wrapper --gradle-version 8.10.2
   ```
   After this, `./gradlew` is self-contained.
5. Build:
   ```bash
   ./gradlew assembleDebug
   ```

## Install on phone

**Via ADB** (developer options + USB debugging enabled):
```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

**Without ADB:** transfer `app-debug.apk` to the phone (USB, email, cloud), tap to install. You'll need to allow "install unknown apps" for whatever app you opened the file from.

## First-run setup in the app

1. Enter your **Gmail address** (the sender).
2. Create an **app password**:
   - Enable 2-Step Verification: https://myaccount.google.com/signinoptions/twosv
   - Generate an app password: https://myaccount.google.com/apppasswords
   - Paste the 16-character code into the app.
3. Enter the **recipient email** (any provider).
4. Tap **Send test email** — you should receive it in the recipient inbox within a few seconds.
5. Grant **SMS permission** when prompted.
6. (Optional but recommended) Tap **Disable battery optimization** — without it, Android may delay sending while the phone is idle.
7. Tap **Finish setup**.

## How it works

- A manifest-declared `BroadcastReceiver` listens for `SMS_RECEIVED` (the app does *not* need to be the default SMS app for this).
- Each incoming SMS is inserted into a Room database with status `PENDING` and a `WorkManager` job is enqueued.
- `SendWorker` checks the daily cap, sends via Gmail SMTP (`smtp.gmail.com:587`, STARTTLS), and updates the message row.
- On transient failure (network, rate limit) it schedules a retry with backoff. On hard failure (auth, invalid recipient) it marks the message permanently failed.
- A daily `RetentionWorker` prunes messages older than 7 days.

## Project structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/smsforwarder/app/
│   ├── SmsForwarderApp.kt        Application — schedules retention worker
│   ├── MainActivity.kt           Compose entry point
│   ├── data/                     Room entities, DAOs, ConfigStore, Repository
│   ├── sms/SmsReceiver.kt        SMS_RECEIVED broadcast receiver
│   ├── mail/GmailSender.kt       Jakarta Mail SMTP client + error classification
│   ├── work/                     SendWorker (with retry), RetentionWorker
│   └── ui/                       Compose screens (Onboarding, Dashboard, Settings)
└── res/                          Themes, launcher icon, backup rules
```

## Notes

- **Privacy**: all data stays on the device. Nothing is transmitted except the email send to Gmail SMTP.
- **Permissions used**: `RECEIVE_SMS`, `READ_SMS`, `INTERNET`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED` (so WorkManager can resume after reboot), `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`.
- **MMS is not supported.** Only standard SMS bodies are forwarded.
- **Google may show a security alert** the first time you sign in with the app password — that's normal; the test email step exists to surface this immediately.
