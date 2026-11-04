# Test Report | Adaptive Camera

**Test Date:** 2026-11-04  
**Version:** 0.2.0  
**Phase:** Phase 1 (Camera Foundation & Startup Crash Investigation)  

---

## 1. Test Summary

| Test Category | Total | Passed | Failed | Skipped / Not Run | Result |
|---|:---:|:---:|:---:|:---:|:---:|
| **Unit Tests** | 19 | 19 | 0 | 0 | **PASS** |
| **Physical Device Verification** | 1 | 1 | 0 | 0 | **PASS** (Physical device attached: `3B15B800NRT00000`) |
| **Build Verification** | 1 | 1 | 0 | 0 | **PASS** |

---

## 2. Startup Crash Investigation & Verification

- **Reported Defect:** Immediate automatic app closure upon launch on the developer's OnePlus Ace 5 (running CPH2691IN firmware).
- **Logcat Stack Trace Captured:**
  ```text
  FATAL EXCEPTION: main
  Process: com.bishal.adaptivecamera.debug, PID: 2045
  java.lang.RuntimeException: Cannot create an instance of class com.bishal.adaptivecamera.ui.camera.CameraViewModel
  Caused by: java.lang.NoSuchMethodException: com.bishal.adaptivecamera.ui.camera.CameraViewModel.<init> [class android.app.Application]
  ```
- **Root Cause Identified:** Missing `@JvmOverloads` on `CameraViewModel` constructor prevented Java reflection in `AndroidViewModelFactory` from finding the `(Application)` constructor overload.
- **Verification After Fix:**
  - APK re-assembled and installed to device `3B15B800NRT00000`.
  - App launched via `am start`.
  - Process checked via `ps -A | grep com.bishal.adaptivecamera`: running continuously (`PID 5750`).
  - Window focus confirmed via `dumpsys window`: `mCurrentFocus=Window{... MainActivity}`.
  - Zero fatal exceptions or runtime crashes.

---

## 3. Unit Test Results

### `com.bishal.adaptivecamera.core.diagnostics.DeviceInfoTest`
- `testCPH2691ReportsUnconfirmedPhysicalIdentityDueToConvertedFirmware`: **PASS**
- `testPKG110ReportsChinaAce5ProbableIdentity`: **PASS**
- `testCPH2645ReportsGlobal13RProbableIdentity`: **PASS**
- `testGenericDeviceHasUnconfirmedIdentity`: **PASS**

### `com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfileTest`
- `testLensFacingMappingFollowsAndroidCameraMetadataConstants`: **PASS**
- `testCameraResolutionCalculations`: **PASS**
- `testExposureTimeFormatting`: **PASS**
- `testDeviceCapabilityReportSerializationWithAccurateFacing`: **PASS**

### `com.bishal.adaptivecamera.camera.engine.CameraSessionStateTest`
- `testOperationalStates`: **PASS**
- `testCapturePhotoGuards`: **PASS**
- `testRecordingGuards`: **PASS**
- `testCameraSwitchGuards`: **PASS**

### `com.bishal.adaptivecamera.camera.config.CameraConfigurationTest`
- `testCameraFacingToggle`: **PASS**
- `testFlashModeTransitions`: **PASS**
- `testPhotoTimerTransitions`: **PASS**
- `testExposureEvDisplayFormatting`: **PASS**
- `testZoomClampingLogic`: **PASS**

### `com.bishal.adaptivecamera.core.storage.MediaStoreManagerTest`
- `testGenerateMediaFilename`: **PASS**
- `testMediaItemFormatting`: **PASS**

---

## 4. Physical Device Verification

- **Serial:** `3B15B800NRT00000`
- **Reported Model:** `CPH2691`
- **Reported Device / Board:** `OP5D3BL1`
- **Reported Product:** `CPH2691IN`
- **Android OS:** Android 17 (API 37)
- **App Package Installed:** `com.bishal.adaptivecamera.debug`
- **Verification Execution:**
  - Installation: **PASS** (`Success - 64,330,752 bytes`)
  - Launch & Stability: **PASS** (Process `5750` active, window focused, zero crashes)
  - HAL Sensor Enumeration:
    - Camera ID 0: `LENS_FACING_BACK` (1) - Logical multi-camera referencing physical IDs `2` and `3`. Level: `LEVEL_3`.
    - Camera ID 1: `LENS_FACING_FRONT` (0) - Front selfie sensor. Level: `FULL`.
    - Camera ID 2: `LENS_FACING_BACK` (1) - Physical rear auxiliary sensor.
    - Camera ID 3: `LENS_FACING_BACK` (1) - Physical rear auxiliary sensor.

---

## 5. Build Artifact Verification

- **Task:** `./gradlew assembleDebug`
- **Result:** **BUILD SUCCESSFUL**
- **Output Artifact:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** 64,330,752 bytes (~61.3 MB)
- **SHA256:** `000B1035D3FB1DB069787D78871EA2F7967F55DEEA00968903DAF3D3A7796CBB`
