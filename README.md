# Adaptive Camera (Xamera)

[![Platform](https://img.shields.io/badge/Platform-Android-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-blue.svg)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202024.10.01-blue.svg)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-29%20(Android%2010)-orange.svg)](https://developer.android.com/about/versions/10)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-brightgreen.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey.svg)](LICENSES.md)

**Adaptive Camera** is a professional Android camera application engineered from the ground up to intelligently inspect device camera hardware capabilities and adapt its photography and videography toolset accordingly.

Unlike generic camera apps that assume arbitrary camera capabilities or crash when OEM features are inaccessible, Adaptive Camera discovers what the physical sensors, lenses, and image signal processors actually support—delivering a tuned, zero-crash, high-fidelity capture experience.

---

## 🌟 Core Pillars

1. **Hardware Intelligence Engine:** Deep Camera2 and platform capability inspection across all accessible logical and physical cameras.
2. **Device-Adaptive UI & Controls:** Advanced manual controls (ISO, Shutter, Focus, RAW) are unlocked only when the hardware truly supports them.
3. **Real-Time 3D LUT Color Studio:** Live preview and captured photo/video grading using OpenGL ES 3.0 shaders with `.cube` LUTs.
4. **Smart Auto Photography:** Rule-based scene evaluation, lighting analysis, and lens recommendation.
5. **Completely On-Device & Offline:** 100% free, open-source technology stack with no accounts, cloud processing, or subscription requirements.

---

## 📱 Device Compatibility Target

* **Primary Test Target:** **OnePlus Ace 5 (China)** (Model `PKG110` / `PJR110`).
  > *Note:* The Chinese OnePlus Ace 5 is closely related to the global OnePlus 13R, but their physical camera configurations and firmware exposure to third-party Camera2 APIs are not identical. Adaptive Camera treats them as distinct hardware profiles.
* **Secondary Targets:** OnePlus 13R (Global), OnePlus 12R, Google Pixel, Samsung Galaxy, and generic Android 10+ devices.

---

## 🛠️ Technology Stack

| Layer | Technology |
|---|---|
| **Language** | Kotlin 2.0.21 |
| **UI Framework** | Jetpack Compose + Material 3 |
| **Camera Framework** | CameraX 1.4.0 (Standard) + Camera2 (Pro/Manual/RAW) |
| **Graphics Engine** | OpenGL ES 3.0 / EGL |
| **Video Encoding** | MediaCodec + MediaMuxer |
| **Data Persistence** | Room 2.6.1 + DataStore Preferences |
| **Media Storage** | MediaStore (Android Scoped Storage) |
| **Build System** | Gradle 8.10.2 + Android Gradle Plugin 8.7.2 |

---

## 🏗️ Architecture

The project follows a clean, modular architecture separating hardware, graphics, intelligence, and presentation:

```text
com.bishal.adaptivecamera
├── core/            # System utilities, device info, dispatchers, permissions
├── camera/          # Hardware capability scanner, profiles, Camera2/CameraX sessions
├── intelligence/    # Scene detection, lighting analysis, smart auto policies
├── photography/     # Photo pipelines: Auto, Pro, RAW, HDR, Night, Portrait
├── video/           # Video pipelines: Standard, Pro Video, High-Speed
├── lut/             # .cube parser, 3D LUT texture generators, preset management
├── graphics/        # EGL context, OpenGL ES 3.0 renderers, frame pipelines
├── gallery/         # MediaStore repository, local photo/video browser
├── data/            # Room database and DataStore preferences
└── ui/              # Compose screens (Camera, Pro, LUT Drawer, Diagnostics)
```

See [ARCHITECTURE.md](ARCHITECTURE.md) for detailed structural specifications.

---

## 🚀 Building & Running

### Prerequisites
- JDK 17 (Microsoft OpenJDK or Android Studio JBR)
- Android SDK (API 35, Build-Tools 35.0.0+)
- Gradle 8.10.2 (included via `./gradlew`)

### Build Debug APK
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Run Unit Tests
```bash
./gradlew test
```

---

## 👤 Developer

* **Developer:** Bishal Aryal
* **Website:** [https://bishalaryal.com.np](https://bishalaryal.com.np)
* **GitHub:** [arybishal](https://github.com/arybishal)

---

## 📄 License

Adaptive Camera is open-source software licensed under the Apache License, Version 2.0. See [LICENSES.md](LICENSES.md).
