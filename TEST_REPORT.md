# Test Report | Adaptive Camera

**Test Date:** 2026-11-04  
**Version:** 0.1.0  
**Phase:** Phase 1 (Camera Foundation & Diagnostic Correction)  

---

## 1. Test Summary

| Test Category | Total | Passed | Failed | Skipped / Not Run | Result |
|---|:---:|:---:|:---:|:---:|:---:|
| **Unit Tests** | 19 | 19 | 0 | 0 | **PASS** |
| **Physical Device Verification** | 1 | 1 | 0 | 0 | **PASS** (Physical device attached: `3B15B800NRT00000`) |
| **Build Verification** | 1 | 1 | 0 | 0 | **PASS** |

---

## 2. Unit Test Results

### `com.bishal.adaptivecamera.core.diagnostics.DeviceInfoTest`
- `testCPH2691ReportsUnconfirmedPhysicalIdentityDueToConvertedFirmware`: **PASS** (Verifies that Indian 13R firmware on OP5D3BL1 board yields UNCONFIRMED physical confidence and explains cross-flash ambiguity).
- `testPKG110ReportsChinaAce5ProbableIdentity`: **PASS** (Verifies Chinese native model SKU detection).
- `testCPH2645ReportsGlobal13RProbableIdentity`: **PASS** (Verifies Global 13R model SKU detection).
- `testGenericDeviceHasUnconfirmedIdentity`: **PASS** (Verifies generic devices like Pixel 8 Pro do not trigger Ace 5 profile).

### `com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfileTest`
- `testLensFacingMappingFollowsAndroidCameraMetadataConstants`: **PASS** (Verifies `CameraMetadata.LENS_FACING_BACK == 1` maps to `BACK`, `LENS_FACING_FRONT == 0` maps to `FRONT`, and `LENS_FACING_EXTERNAL == 2` maps to `EXTERNAL`).
- `testCameraResolutionCalculations`: **PASS** (Verifies 16:9, 4:3, 1:1 aspect ratios and MP calculation).
- `testExposureTimeFormatting`: **PASS** (Verifies nanoseconds conversion to fractional seconds).
- `testDeviceCapabilityReportSerializationWithAccurateFacing`: **PASS** (Verifies formatted text and JSON generation with accurate facing and identity evidence).

### `com.bishal.adaptivecamera.camera.engine.CameraSessionStateTest`
- `testOperationalStates`: **PASS** (Verifies `isOperating` for READY, CAPTURING, RECORDING).
- `testCapturePhotoGuards`: **PASS** (Ensures photo capture can only occur in READY state).
- `testRecordingGuards`: **PASS** (Ensures recording can start only in READY state and stop only in RECORDING state).
- `testCameraSwitchGuards`: **PASS** (Prevents camera switching during active capture or recording).

### `com.bishal.adaptivecamera.camera.config.CameraConfigurationTest`
- `testCameraFacingToggle`: **PASS** (Verifies BACK <-> FRONT toggle).
- `testFlashModeTransitions`: **PASS** (Verifies OFF -> AUTO -> ON -> TORCH -> OFF cycle).
- `testPhotoTimerTransitions`: **PASS** (Verifies OFF -> 3s -> 10s -> OFF cycle).
- `testExposureEvDisplayFormatting`: **PASS** (Verifies `+1.0 EV`, `-1.0 EV`, and `0.0 EV` formatting).
- `testZoomClampingLogic`: **PASS** (Verifies zoom ratio clamping within min/max bounds).

### `com.bishal.adaptivecamera.core.storage.MediaStoreManagerTest`
- `testGenerateMediaFilename`: **PASS** (Verifies `IMG_yyyyMMdd_HHmmss.jpg` and `VID_yyyyMMdd_HHmmss.mp4` naming).
- `testMediaItemFormatting`: **PASS** (Verifies size formatting and video duration `01:25` formatting).

---

## 3. Physical Device Verification

- **Serial:** `3B15B800NRT00000`
- **Reported Model:** `CPH2691`
- **Reported Device / Board:** `OP5D3BL1`
- **Reported Product:** `CPH2691IN`
- **Reported Fingerprint:** `OnePlus/CPH2691IN/OP5D3BL1:17/UKQ1.231108.001/V.R4T3.2d570f8-6d2927-6d2926:user/release-keys`
- **Display ID:** `CPH2691_17.0.0.105(EX01)`
- **Android OS:** Android 17 (API 37)
- **App Package Installed:** `com.bishal.adaptivecamera.debug`
- **Verification Execution:**
  - APK Installation: **PASS** (`Success - 64,243,980 bytes`)
  - App Launch: **PASS** (`am start -n com.bishal.adaptivecamera.debug/...` launched successfully with zero crashes)
  - HAL Sensor Enumeration:
    - Camera ID 0: `LENS_FACING_BACK` (1) - Logical multi-camera referencing physical IDs `2` and `3`. Level: `LEVEL_3`.
    - Camera ID 1: `LENS_FACING_FRONT` (0) - Front selfie sensor. Level: `FULL`.
    - Camera ID 2: `LENS_FACING_BACK` (1) - Rear physical auxiliary sensor.
    - Camera ID 3: `LENS_FACING_BACK` (1) - Rear physical auxiliary sensor.

---

## 4. Build Artifact Verification

- **Task:** `./gradlew assembleDebug`
- **Result:** **BUILD SUCCESSFUL**
- **Output Artifact:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** 64,243,980 bytes (~61.3 MB)
- **SHA256:** `A05FB11D9242D6841C538AF13ABA1BF472426FFA1F38AC7CF8855602A4FD344B`
