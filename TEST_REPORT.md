# Test Report | Adaptive Camera

**Test Date:** 2026-11-05  
**Version:** 0.5.1  
**Phase:** Phase 4 Addendum (Camera Settings, Hardware Diagnostics & Exposure Reliability)  

---

## 1. Test Summary

| Test Category | Total | Passed | Failed | Skipped / Not Run | Result |
|---|:---:|:---:|:---:|:---:|:---:|
| **Unit Tests** | 61 | 61 | 0 | 0 | **PASS** |
| **Physical Device Verification** | 1 | 1 | 0 | 0 | **PASS** (Physical hardware profile verified: `3B15B800NRT00000`) |
| **Build Verification** | 1 | 1 | 0 | 0 | **PASS** |

---

## 2. Phase 4 Addendum Exposure & Settings Unit Test Results

### `com.bishal.adaptivecamera.camera.engine.ExposureReliabilityAndSettingsTest`
- `testDefaultExposureStateIsSafeAndNotReady`: **PASS** (Default state safe; control reported unavailable prior to HAL query)
- `testExposureControlAvailableWhenBoundAndSupported`: **PASS** (Available only when session ready, hardware supported, min < max)
- `testExposureControlUnavailableWhenHardwareUnsupported`: **PASS** (Graceful fallback when sensor does not support compensation)
- `testExposureControlUnavailableWhenNotReady`: **PASS** (Guards against desync during camera session recreation)
- `testExposureControlUnavailableWhenRangeFlat`: **PASS** (Flat min==max range treated as unavailable)
- `testExposureEvDisplayFormatting`: **PASS** (Locale-safe positive `+1.0 EV`, negative `-1.5 EV`, and zero formatting)
- `testExposureClampingLogic`: **PASS** (Strict boundary clamping within dynamic HAL limits)
- `testCameraFacingResetBehavior`: **PASS** (Camera switch resets ready state and index to safe baseline)

---

## 3. Phase 4 Pro Photography Unit Test Results

### `com.bishal.adaptivecamera.photography.pro.ProPhotographyPhase4Test`
- `testIsoRangeClamping`: **PASS** (Clamping to min/max sensor sensitivity range: 100 to 6400)
- `testShutterSpeedFormatting`: **PASS** (Photographic shutter fractions 1/8000s through 10s+)
- `testShutterSpeedPresetsWithinHardwareRange`: **PASS** (Presets generated and clamped strictly within device limits)
- `testFocusDistanceFormatting`: **PASS** (Diopters to real-world focus distance display: ∞, 100cm, 10cm)
- `testProWhiteBalanceMappings`: **PASS** (Auto, Cloudy, Daylight, Fluorescent, Incandescent, Shade to Camera2 AWB modes)
- `testProConfigurationStateTransitions`: **PASS** (Independent manual locking and clean reset to auto controls)
- `testHistogramAnalyzerWithWhiteFrame`: **PASS** (Highlight clipping ratio > 0.90, peak in bin 63 across Luma/R/G/B)
- `testHistogramAnalyzerWithBlackFrame`: **PASS** (Shadow clipping ratio > 0.90, peak in bin 0)
- `testFocusPeakingOnUniformFrame`: **PASS** (Uniform frame yields 0 edge peaking points)
- `testFocusPeakingOnHighContrastEdge`: **PASS** (High contrast spatial edge detected with high intensity and normalized coordinates)
- `testDngHeaderValidation`: **PASS** (Valid Little-Endian TIFF magic header `0x49492A00` accepted; non-TIFF / empty rejected)
- `testProAppliedStateFormatting`: **PASS** (Applied ISO, Shutter, Focus, Aperture, and Focal length telemetry formatting)
- `testSmartAutoCompatibilityDoesNotOverrideManualSettings`: **PASS** (Smart Auto recommendations remain purely advisory when manual settings are locked)

---

## 3. Phase 3 Smart Auto Unit Test Results

### `com.bishal.adaptivecamera.photography.auto.SmartAutoPhase3Test`
- `testFrameMetricsAnalyzerOnLowLightFrame`: **PASS** (Low ambient luminance, high shadow ratio detection)
- `testFrameMetricsAnalyzerOnBacklitHighContrastFrame`: **PASS** (High RMS contrast, dynamic range span)
- `testFrameMetricsAnalyzerTemporalMotionDelta`: **PASS** (Inter-frame variance & motion delta calculation)
- `testSceneClassifierNightDetection`: **PASS** (Night scene classification and confidence scoring)
- `testSceneClassifierBacklitDetection`: **PASS** (Backlit HDR scene classification and shadow rationale)
- `testSceneClassifierMacroDetection`: **PASS** (Close focus diopter mapping to cm distance)
- `testSceneClassifierActionMovementDetection`: **PASS** (High frame-rate motion detection)
- `testSceneClassifierFrontFacingPortraitDetection`: **PASS** (Selfie camera orientation and portrait framing)
- `testSceneClassifierLandscapeDetection`: **PASS** (Expansive landscape composition and steady framing)
- `testSmartConfigurationGeneratorSuggestions`: **PASS** (ISO, high shutter speed, EV compensation, and HDR suggestions)
- `testLensRecommendationPolicy`: **PASS** (Ultra-wide 1.68mm vs Main 5.59mm f/1.88 lens switching advice)
- `testSmartAutoOrchestratorPipeline`: **PASS** (End-to-end processing pipeline from frame metrics to auto state)

---

## 3. Phase 2 Hardware Intelligence Unit Test Results

### `com.bishal.adaptivecamera.camera.capability.HardwareIntelligencePhase2Test`
- `testCameraRelationshipInfoProperties`: **PASS** (Logical multi-camera vs physical reference mapping)
- `testDynamicRangeInfoEvaluation`: **PASS** (10-bit HLG10/HDR10 profile evaluation)
- `testStreamConfigurationModels`: **PASS** (StreamSizeInfo, FormatStreamConfiguration, StreamCombinationCapability)
- `testVideoCapabilityInfoProperties`: **PASS** (VideoQualityProfile, EIS, and Preview Stabilization flags)
- `testManualControlInfoAndClamping`: **PASS** (Manual sensor, focus distance, AE/AWB lock status)
- `testZoomCapabilityInfo`: **PASS** (Optical focal lengths and zoom ratio range verification)
- `testRawCapabilityInfo`: **PASS** (RAW_SENSOR stream format and resolution parsing)
- `testHardwareValidationSummaryCounters`: **PASS** (Total advertised vs validated vs verified feature counters)
- `testSensorAndLensInfoParsing`: **PASS** (Sensor active array, CFA, white level, black pattern, and lens OIS specs)

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

## 3. Physical Device Verification (Live Hardware Telemetry)

- **Device Serial:** `3B15B800NRT00000`
- **Reported Model:** `CPH2691` (OnePlus 13R India Regional Firmware)
- **Reported Device / Board:** `OP5D3BL1`
- **Platform:** Qualcomm Snapdragon 8 Gen 3 (`SM8650` / `qcom`)
- **Android OS:** Android 17 (API 37)
- **Live Capability Discovery Results:**
  - `Camera ID 0` (Logical Back): LEVEL_3, 4096x3072 active array, 5.59mm f/1.88, sub-cameras `[2, 3]`, zoom 0.6x-10.0x, ISO 100-6400, shutter 78µs-74.35s, 10-bit HDR (HLG10/HDR10/HDR10+), 4K UHD, EIS & Preview Stabilization.
  - `Camera ID 1` (Front): LEVEL_3, 2304x1728 active array, 3.23mm f/2.4, fixed focus, ISO 100-1600, shutter 35µs-23.22s, 1080p FHD, EIS & Preview Stabilization.
  - `Camera ID 2` (Physical Sub-Sensor): LEVEL_3, 4096x3072, 5.59mm f/1.88, physical reference of `0`.
  - `Camera ID 3` (Physical Sub-Sensor): LEVEL_3, 3264x2448, 1.68mm f/2.2, physical reference of `0` (provides 0.6x ultra-wide perspective).
- **Cache Persistence:** Verified `hardware_capability_cache.json` (25,500 bytes) and `cache_meta.json` (139 bytes) created under app internal files directory.

---

## 4. Build Artifact Verification

- **Task:** `./gradlew assembleDebug`
- **Result:** **BUILD SUCCESSFUL**
- **Output Artifact:** `app/build/outputs/apk/debug/app-debug.apk`
- **File Size:** 64,444,068 bytes (~61.4 MB)
