# Changelog | Adaptive Camera

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0] - Phase 0 Foundation - 2026-11-04

### Added
- **Project Foundation:** Initialized Gradle Kotlin DSL project targeting Android API 35 with minSdk 29 (Android 10).
- **Architecture Skeleton:** Established clean modular architecture structure conforming to Part 4 specifications (`core`, `camera`, `intelligence`, `photography`, `video`, `lut`, `graphics`, `gallery`, `data`, `ui`).
- **Device Identification:** Added `DeviceInfo` model with detection logic specifically tailored for the primary test target, **OnePlus Ace 5 (China)** (`PKG110`/`PJR110`), separating it from the global OnePlus 13R (`CPH2645`).
- **Hardware Capability Models:**
  - `CapabilityStatus` enum (`SUPPORTED`, `UNSUPPORTED`, `UNAVAILABLE`, `NOT_VERIFIED`, `TEMPORARILY_UNAVAILABLE`).
  - `CameraHardwareLevel` enum mapping Camera2 hardware levels (`LIMITED`, `FULL`, `LEVEL_3`, `LEGACY`, `EXTERNAL`).
  - `CameraCapabilityProfile` capturing sensor resolution, FPS ranges, focal lengths, manual focus, manual sensor exposure (ISO & shutter), RAW, HDR, and stabilization.
  - `DeviceCapabilityReport` with formatted text and JSON export capabilities.
- **Hardware Intelligence Engine Prototype:** Implemented `CameraCapabilityScanner` utilizing Android Camera2 `CameraManager` and `CameraCharacteristics`.
- **UI Diagnostics Screen:** Interactive Jetpack Compose diagnostics interface featuring:
  - Device and firmware identification cards with primary target highlighting.
  - Camera hardware summary and discovered sensors inventory.
  - Diagnostics warnings and notices.
  - Action buttons to copy report as JSON or formatted text, or share via Android system share sheet.
- **Testing:** Unit test suite covering device identification, resolution calculations, exposure formatting, capability status usability, and report serialization.
- **Build Verification:** Verified clean build producing `app/build/outputs/apk/debug/app-debug.apk` (SHA256: `837865C30AD7510BF52C2A7E90679A4BD15F8A1CA945908C1BF83C53114BCAF7`).
- **Documentation:** Created `README.md`, `ARCHITECTURE.md`, `ROADMAP.md`, `TASKS.md`, `CHANGELOG.md`, `TEST_REPORT.md`, `DEVICE_COMPATIBILITY.md`, `KNOWN_ISSUES.md`, `LICENSES.md`, and `reports/phase-00-foundation.md`.

---

## [0.2.0] - Phase 1 Camera Foundation & Diagnostic Correction - 2026-11-04

### Added
- **Camera Foundation:** Complete CameraX engine implementation (`CameraXEngine`) providing lifecycle-aware live preview, photo capture, video recording, front/rear camera switching, zoom, tap focus, and exposure compensation.
- **Session State Machine:** Observable `CameraSessionState` (`CLOSED`, `OPENING`, `READY`, `CAPTURING`, `RECORDING`, `SWITCHING`, `ERROR`) with concurrent conflict guards.
- **Scoped Media Storage:** `MediaStoreManager` integrating with Android scoped storage (`DCIM/AdaptiveCamera`) for automatic photo/video indexing with `IS_PENDING` safety.
- **Local Media Gallery:** 3-column thumbnail browser with native hardware thumbnail loading, full-screen image viewer, integrated video player with transport controls, metadata inspector, and delete confirmation dialogs.
- **Camera Viewfinder UI:** Dark-first professional camera interface featuring top bar (flash, timer, EV, diagnostics shortcut), bottom capture controls (mode tabs, shutter button with recording pulse, gallery thumbnail, camera switcher), animated tap-to-focus ring, zoom chips, and vertical EV slider.
- **Hardware Diagnostic Correction:**
  - Audited device classification to separate reported models, firmware identities, hardware platform families, and physical confidence (`CONFIRMED`, `PROBABLE`, `UNCONFIRMED`, `CONFLICTING`).
  - Corrected `LENS_FACING` constants to accurately map `CameraMetadata.LENS_FACING_FRONT == 0` and `CameraMetadata.LENS_FACING_BACK == 1`, resolving the inverted "1 BACK, 3 FRONT" camera layout.
  - Discovered true HAL mapping: Camera ID 0 is Back (Logical multi-camera), Camera ID 1 is Front (Physical), Camera ID 2 is Back (Physical), Camera ID 3 is Back (Physical).
- **Unit Testing:** 19 unit tests passing covering device classification, camera facing, exposure formatting, session state transitions, zoom bounds, and MediaStore naming.
- **Physical Device Verification:** Installed and verified on physical `OnePlus Ace 5 / CPH2691IN` (`3B15B800NRT00000`) running Android 17 (API 37).
