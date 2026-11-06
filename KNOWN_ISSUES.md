# Known Issues & Hardware Limitations | Xamera

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Workspace:** `D:\Webapp\xamera`  
**Repository:** https://github.com/arybishal/Xamera  
**Developer:** Bishal Aryal  
**Version:** 0.7.0  
**Date:** 2026-11-06  

---

## 1. Resolved Issues

### Issue 1: App Startup Crash on `CameraViewModel` Instantiation
- **Status:** **RESOLVED** (Phase 1)
- **Symptom:** Application automatically closed immediately upon launch.
- **Root Cause:** `CameraViewModel` defined default constructor parameters (`cameraEngine`, `mediaStoreManager`) without the `@JvmOverloads` annotation. When Android's `by viewModels()` (`AndroidViewModelFactory`) invoked reflection to find `clazz.getConstructor(Application.class)`, it threw `java.lang.NoSuchMethodException: com.bishal.adaptivecamera.ui.camera.CameraViewModel.<init> [class android.app.Application]`.
- **Fix:** Added `@JvmOverloads constructor(...)` to `CameraViewModel`, ensuring bytecode exposes the standard single-argument `(Application)` constructor expected by `AndroidViewModelFactory`. Verified on physical device: process PID is continuously active and focused.

### Issue 2: CameraX 1.4.0 RAW Stream Output Limitation
- **Status:** **RESOLVED** via Architecture Isolation (Phase 4)
- **Detail:** CameraX 1.4.0 `ImageCapture` only supports output formats `OUTPUT_FORMAT_JPEG` and `OUTPUT_FORMAT_JPEG_ULTRA_HDR`. It cannot bind `ImageFormat.RAW_SENSOR` or construct Adobe DNG files.
- **Mitigation:** Implemented a dedicated `Camera2ManualEngine` that manages `CameraDevice`, `CameraCaptureSession`, and dedicated `ImageReader(ImageFormat.RAW_SENSOR)` alongside `ImageFormat.JPEG`, using Android's native `DngCreator` for valid DNG packaging.

### Issue 3: Exposure Slider Opening Failure & Desynchronization
- **Status:** **RESOLVED** (Phase 4 Addendum)
- **Symptoms:** The exposure interface sometimes appeared and sometimes failed to open; the slider position could desynchronize from actual camera exposure; rapid sliding caused jitter or canceled operations.
- **Root Causes:**
  1. *Empty rendering failure:* The slider composable contained `if (minIndex >= maxIndex) return`. Before CameraX finished binding the camera or if the camera reported an empty range, the slider rendered nothing, appearing as a broken or failed click.
  2. *HAL Flooding & Stale State:* Dragging the slider continuously triggered dozens of `setExposureCompensationIndex` calls per second, flooding CameraX's ListenableFuture queue and throwing `CameraControl.OperationCanceledException`.
  3. *Unchecked Hardware Support:* `CameraConfiguration` previously assumed a hardcoded `[-4, 4]` range before querying the camera HAL, leading to illegal requests on cameras with non-standard ranges.
- **Fix:**
  1. Added dynamic CameraX `ExposureState` querying in `CameraXEngine` to read `isExposureCompensationSupported`, `exposureCompensationRange`, and `exposureCompensationStep`.
  2. Implemented readiness guards (`isExposureStateReady` and `isExposureControlAvailable`).
  3. Added visual loading and disabled states in `ExposureSlider` (e.g. "Exposure Fixed - Sensor does not support compensation") with a close button and reset button.
  4. Attached `future.addListener` to `setExposureCompensationIndex` on `cameraExecutor` to read actual applied indices and synchronize UI state with the HAL without flooding.

### Issue 11: Missing `android.permission.VIBRATE` Manifest Declaration
- **Status:** **RESOLVED** (Phase 5)
- **Symptom:** In Android 11+ runtime logs, `Vibrator` calls triggered `java.lang.SecurityException: vibrate: Neither user nor current process has android.permission.VIBRATE`.
- **Root Cause:** Android requires explicit `VIBRATE` permission in `AndroidManifest.xml` for apps invoking `Vibrator` or `VibratorManager` APIs for tactile feedback.
- **Fix:** Added `<uses-permission android:name="android.permission.VIBRATE" />` to `app/src/main/AndroidManifest.xml` and wrapped all `Vibrator` invocations inside safe exception handlers in `HapticEngineImpl` for graceful silent fallback if disabled by system policy. Verified on OnePlus Ace 5 / 13R.

### Issue 12: Continuous Kelvin White Balance Requires Camera2 RggbChannelVector and CONTROL_AWB_MODE_OFF
- **Status:** **RESOLVED** (Phase 6)
- **Symptom:** Setting standard Camera2 `CONTROL_AWB_MODE` only permits coarse discrete presets (e.g. Daylight, Cloudy, Incandescent) and cannot achieve smooth continuous Kelvin color temperature (2000K–10000K) or green/magenta tint bias.
- **Root Cause:** In Camera2 HAL architecture, fine continuous chromaticity control requires disabling auto white balance (`CONTROL_AWB_MODE_OFF`), enabling transform matrix color correction (`COLOR_CORRECTION_MODE_TRANSFORM_MATRIX`), and passing precise 4-channel chromatic gains via `CaptureRequest.COLOR_CORRECTION_GAINS`.
- **Fix:** Implemented `KelvinConverter` modeling Planckian locus blackbody radiation to compute normalized `RggbChannelVector(r, gEven, gOdd, b)` with green/magenta tint offset. Conversely, real-time HAL telemetry extracts `CaptureResult.COLOR_CORRECTION_GAINS` and computes dynamic live CCT estimation (`rgbGainsToKelvin`) displayed directly in the pro telemetry badge. Verified on OnePlus Ace 5 / 13R.

### Issue 13: Single CaptureRequest Multi-Surface Synchronization for Synchronized RAW + JPEG
- **Status:** **RESOLVED** (Phase 7)
- **Symptom:** Disconnected asynchronous capture requests for RAW and JPEG output frames result in divergent exposure timestamps, differing motion blur, and mismatched subject positioning between the RAW negative and the processed companion.
- **Root Cause:** If RAW and JPEG surfaces are submitted in separate `session.capture` requests or via distinct capture builders, Android Camera2 HAL processes them as two distinct sensor exposure events with varying total exposure duration, ISO sensitivity, and nanosecond timestamps.
- **Fix:** Architected `Camera2ManualEngine` to attach both `jpegImageReader.surface` and `rawImageReader.surface` to a single unified `CaptureRequest.Builder(CameraDevice.TEMPLATE_STILL_CAPTURE)`. Both buffers originate from the exact same exposure cycle, sharing an identical `TotalCaptureResult.SENSOR_TIMESTAMP`.

### Issue 14: Manual Exposure Modulation During Auto Exposure Bracketing (AEB)
- **Status:** **RESOLVED** (Phase 7)
- **Symptom:** In Pro manual mode (`isManualIso || isManualShutter`), Camera2 HAL ignores `CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION` because `CONTROL_AE_MODE` is disabled (`OFF`).
- **Root Cause:** Photographic exposure bracketing in manual mode cannot rely on auto-exposure indices and must calculate direct sensor duration and sensitivity modulation.
- **Fix:** Implemented `AebExposureCoordinator.buildManualBracketSequence`, which steps `SENSOR_EXPOSURE_TIME` by powers of 2 ($2^{\Delta EV}$). If physical sensor limits are exceeded (e.g. at 30s upper bound), residual EV bias is seamlessly compensated via `SENSOR_SENSITIVITY` (ISO). An invariant restoration block ensures the user's initial manual shutter and ISO settings are restored upon sequence completion. Verified with 7-frame sequences.

---

## 2. Hardware Limitations & Device Compatibility (OnePlus Ace 5 / 13R Platform)

### Issue 4: Physical Identity in Cross-Flashed / Converted ROM Environments
- **Status:** Documented / Handled via Explainable Classification
- **Detail:** On devices running cross-flashed regional firmware (e.g. Indian OnePlus 13R `CPH2691IN` on Chinese OnePlus Ace 5 board `OP5D3BL1`), `Build.MODEL`, `Build.PRODUCT`, and `Build.FINGERPRINT` report the operating system's target SKU rather than physical PCB component provenance.
- **Mitigation:** The application never claims confirmed physical identity solely based on software properties. It reports `firmwareIdentity` separately from `probableHardwareFamily`, sets `physicalIdentityConfidence` to `UNCONFIRMED`, and presents explainable identity evidence factors.

### Issue 5: Auxiliary Physical Camera Access in Standard CameraX Engine
- **Status:** Documented / Handled via Direct Camera2 Engine
- **Detail:** On devices with logical multi-camera HALs (such as Camera 0 on the Ace 5 / 13R), CameraX binds to the logical camera ID, allowing Qualcomm's multi-camera compositor to dynamically switch between physical sub-lenses (IDs 2 and 3) during optical and digital zoom. Direct uncomposited streaming from individual physical sub-cameras is not exposed by standard CameraX selectors.
- **Mitigation:** Physical IDs are accurately discovered, audited, and displayed in Diagnostics. Advanced direct physical sensor binding is architected for Phase 4 (PRO mode) and Phase 14 via Camera2 physical stream mapping.

### Issue 6: Fixed Focus on Front Camera
- **Status:** Documented / Handled via UI Guards
- **Detail:** The front-facing selfie camera (Camera ID 1) reports `LENS_INFO_MINIMUM_FOCUS_DISTANCE = 0.0f` (fixed focus infinity).
- **Mitigation:** The manual focus slider is automatically disabled and labeled "Fixed Focus (Front)" when the front camera is active, preventing non-functional controls from misleading the user.

### Issue 7: Fixed Focus on Ultra-Wide Sub-Camera & Macro Strategy
- **Status:** Documented / Handled via Lens Routing
- **Detail:** Physical sub-camera 3 (Ultra-Wide, 1.68mm f/2.2) reports `LENS_INFO_MINIMUM_FOCUS_DISTANCE = 0.0f` (fixed hyperfocal focus at 1.74m). It cannot mechanically focus at close macro distances.
- **Mitigation:** Dedicated Macro mode (`FEAT-FOC-07`) routes to Main Camera 0 / 2 (5.59mm f/1.88), which provides `LENS_INFO_MINIMUM_FOCUS_DISTANCE = 10.0f` diopters (~10cm boundary), ensuring sharp close-up imagery without relying on fixed-focus ultra-wide optics.

### Issue 8: Variable Physical Aperture Hardware Incompatibility
- **Status:** Documented / Handled via Graceful UI Notice
- **Detail:** The OnePlus Ace 5 / 13R camera modules feature fixed physical apertures:
  - Camera 0 / 2 (Main): Fixed $f/1.88$
  - Camera 1 (Front): Fixed $f/2.40$
  - Camera 3 (Ultra-wide): Fixed $f/2.20$
- **Mitigation:** The application audits `CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES`. If the array contains only a single aperture float, `VariableApertureController` (`FEAT-FOC-08`) flags the capability as `UNSUPPORTED BY HARDWARE`, clearly displays `"Fixed Aperture (f/1.88)"` in the pro telemetry badge, and disables manual iris adjustments without crashing.

### Issue 9: Concurrent Multi-Camera Streaming HAL Restrictions
- **Status:** Documented / Handled via Graceful Fallback
- **Detail:** Android 11+ `CameraManager.getConcurrentCameraIds()` returns an empty set on the Snapdragon 8 Gen 3 `CPH2691IN` / `OP5D3BL1` platform. The vendor camera HAL does not expose concurrent ISP streaming pipelines for simultaneous front and rear 4K/1080p recording.
- **Mitigation:** In Dual-Camera / Director's View mode (`FEAT-VID-11`), the app verifies concurrency support before attempting session creation. On incompatible devices, it displays a clear advisory: `"Concurrent Dual-Camera streaming is not supported by device hardware"` and offers simulated single-session picture-in-picture alternatives.

### Issue 10: Manual Sensor Overrides AE Control
- **Status:** Handled per Photographic Standards
- **Detail:** When manual ISO sensitivity or manual shutter speed is requested, `CONTROL_AE_MODE` must be set to `CONTROL_AE_MODE_OFF`. In this state, hardware AE compensation (`CONTROL_AE_EXPOSURE_COMPENSATION`) is ignored by the camera HAL.
- **Mitigation:** The application clearly displays whether manual sensor control is active via `ProAppliedTelemetryBadge` and disables the AE compensation slider during manual exposure to prevent conflicting requests.
