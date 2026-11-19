# Money Ledger

Personal finance app for Android: multi-account ledger, expense tracking, reports and Excel export.
Built with Kotlin, Jetpack Compose and Room (data is stored locally on the device).

## Download the app

Every push to `main` is built by GitHub Actions. Grab the newest APK from the
**Releases → latest** page of this repository and install it on your Android phone
(you may need to allow "Install unknown apps" for your browser).

## Build it yourself

Requirements: JDK 21 and the Android SDK (Android Studio installs both).

```bash
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

Or open the folder in Android Studio and press Run.

## Release builds

Release signing reads `KEYSTORE_PATH`, `STORE_PASSWORD` and `KEY_PASSWORD` from the environment
(key alias `upload`). Without them only debug builds are possible.
