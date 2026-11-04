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
