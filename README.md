# ApkMint

Convert any **Website URL**, **HTML code**, or **HTML/CSS/JS `.zip` project** into a signed, installable **Android APK** directly on your phone — **100% on-device, no server required**.

---

## Features

- **Website to APK & HTML to APK**
  - Convert live website URLs into Android apps.
  - Paste/edit HTML code directly, import `.html` files, or import multi-file `.zip` web projects (`HTML + CSS + JS`).
  - Built-in **Quick HTML Starter Templates** (Bio/Portfolio, Calculator, Quiz App).
  - **Live WebView Preview** before building.
- **On-Device APK Patching & Signing**
  - Patches binary `AndroidManifest.xml` (`package`, `versionCode`, `versionName`, permissions/authorities).
  - Patches `resources.arsc` (`app_name`).
  - Generates adaptive & legacy PNG icons (`mdpi` to `xxxhdpi`) with zero white borders.
  - Packages 4-byte aligned APKs and signs them with **APK Signature Scheme v1, v2 & v3** (`apksig`) using an auto-generated per-device PKCS12 keystore.
- **Icon Customization**
  - Pick an icon from Gallery.
  - **Auto-fetch Website Icon** directly from the target URL.
  - **Text Icon Studio**: create custom text icons with font styles, shapes, sizes, solid/gradient backgrounds, and an interactive **HSV Color Picker**.
- **Design & Behavior**
  - **Dynamic Status Bar**: auto-matches website headers and live **Day/Night mode** toggles via `MutationObserver`.
  - **Floating Action Bubble**: optional draggable bubble (with dynamic theme colors & spring animations) for Back, Forward, Refresh, Home, and Share.
  - **Edge Swipe Navigation**: swipe from left/right screen edges for Back/Forward.
  - **Splash Screen**, **Orientation**, **Loading Progress Bar Color**, **Styled Day/Night Exit Dialog**.
  - **Custom CSS & JavaScript Injection**, **Desktop Site Mode**, **Keep Screen On**, **Block Screenshots (`FLAG_SECURE`)**, **Camera / Microphone / Location** permissions.
- **Project History**
  - Install, Share, Delete, or **Edit & Rebuild** previously created apps with auto-incremented `versionCode`.

---

## Architecture

ApkMint uses a clean dual-module Gradle setup:

1. **`:template` (`com.apkmint.template`)** — Lightweight WebView runtime module compiled to a release APK and embedded into `:app/src/main/assets/template.apk` automatically during Gradle build (`copyTemplateApk`).
2. **`:app` (`com.apkmint.creator`)** — Jetpack Compose Material 3 builder UI, binary AXML/ARSC patchers (`StringPoolCodec`, `AxmlPatcher`, `ArscPatcher`), `apksig` APK signer, and project manager.

---

## Build & Run

### Prerequisites
- Android Studio (with JDK 17+ / JBR)
- Android SDK (compileSdk 37, minSdk 24)

### Build via Command Line
```bash
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Gradle will automatically compile `:template:assembleRelease` and copy it into `app/src/main/assets/template.apk` before building `:app`.

---

## Author

- **Khorshed Alom**
- GitHub: [https://github.com/khor7ddota](https://github.com/khor7ddota)
- Email: `khor7d.a@gmail.com`

---

## License

This project is licensed under the **MIT License** — see the [LICENSE](LICENSE) file for details.
