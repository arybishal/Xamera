# Test Report | Adaptive Camera

**Test Date:** 2026-11-04  
**Version:** 0.1.0  
**Phase:** Phase 0 (Foundation and Hardware Research)  

---

## 1. Test Summary

| Test Category | Total | Passed | Failed | Skipped / Not Run | Result |
|---|:---:|:---:|:---:|:---:|:---:|
| **Unit Tests** | 8 | 8 | 0 | 0 | **PASS** |
| **Instrumentation Tests** | 0 | 0 | 0 | 0 | *Queued for Phase 1* |
| **Physical Device Verification** | 1 | 0 | 0 | 1 | **NOT RUN** (No device attached over ADB) |
| **Build Verification** | 1 | 1 | 0 | 0 | **PASS** |

---

## 2. Unit Test Results

### `com.bishal.adaptivecamera.core.diagnostics.DeviceInfoTest`
- `testDetectsOnePlusAce5ChinaVariant`: **PASS** (Correctly matches `PKG110`, `Ace 5`, `PJR110` identifiers).
- `testDistinguishesFromGlobalOnePlus13R`: **PASS** (Ensures global `CPH2645` is not conflated with China Ace 5).
- `testGenericDevicesAreNotAce5`: **PASS** (Ensures Pixel, Galaxy, and generic devices do not trigger Ace 5 profile).

### `com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfileTest`
- `testCameraResolutionCalculations`: **PASS** (Verifies 16:9, 4:3, 1:1 aspect ratios and MP calculation).
- `testExposureTimeFormatting`: **PASS** (Verifies nanoseconds conversion to fractional seconds e.g. `1/10000s to 30.00s`).
- `testDeviceCapabilityReportSerialization`: **PASS** (Verifies formatted text and JSON generation).
- `testCapabilityStatusUsability`: **PASS** (Verifies `isUsable` contract for `SUPPORTED`).
- `testHardwareLevelMapping`: **PASS** (Verifies Camera2 integer constants map correctly to enum values).

---

## 3. Physical Device Testing

- **Target Device:** OnePlus Ace 5 (China)
- **Status:** **NOT RUN**
- **Note:** `adb devices -l` was executed and reported 0 connected devices at build time. Per project guidelines, no physical tests are fabricated. The app is ready for immediate deployment and capability validation as soon as the test device is plugged in or connected over wireless ADB.

---

## 4. Build Artifact Verification

- **Task:** `./gradlew assembleDebug`
- **Result:** **BUILD SUCCESSFUL**
- **Output Artifact:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** 63,839,121 bytes (~60.8 MB)
- **SHA256:** `837865C30AD7510BF52C2A7E90679A4BD15F8A1CA945908C1BF83C53114BCAF7`
