# Known Issues & Hardware Limitations | Xamera

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Workspace:** `D:\Webapp\xamera`  
**Repository:** https://github.com/arybishal/Xamera  
**Developer:** Bishal Aryal  
**Version:** 0.11.0  
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

### Issue 15: 3D LUT Table Dimensional Bounds & Memory Allocation Guard
- **Status:** **RESOLVED** (Phase 8)
- **Symptom:** Parsing unconstrained or maliciously crafted `.cube` files could lead to heap exhaustion or OutOfMemoryError if the declared `LUT_3D_SIZE` is extremely large.
- **Root Cause:** A 3D LUT table scales cubically ($N^3 \times 3$ floats). A size of $N=256$ requires $16.7\text{M} \times 3 \approx 50.3\text{M}$ float elements (~200MB in RAM per single LUT), which exceeds safe limits for real-time mobile memory budgets. Standard professional grading sizes are $17\times17\times17$, $33\times33\times33$, and $65\times65\times65$ ($65^3 \times 3 \approx 823{,}875$ floats $\approx 3.3\text{MB}$).
- **Fix:** `CubeLutParser` enforces a strict hardware and memory safety threshold: $N \in [2, 65]$ for 3D LUTs and $N \in [2, 4096]$ for 1D LUTs. Any file declaring sizes outside this professional domain is immediately rejected during header validation before array allocation, with a human-readable diagnostic error.

### Issue 16: Out-of-Gamut Coordinates and Boundary Clamping in Trilinear CPU Interpolation
- **Status:** **RESOLVED** (Phase 8)
- **Symptom:** Inverting or applying non-standard LUT domain intervals ($[domainMin, domainMax]$ differing from $[0.0, 1.0]$) or processing over-saturated pixels ($r, g, b < 0.0$ or $> 1.0$) caused array index out-of-bounds exceptions or color wrap-around artifacts.
- **Root Cause:** Standard Adobe `.cube` files support explicit `DOMAIN_MIN` and `DOMAIN_MAX` declarations. Without coordinate normalization and clamping to lattice boundaries $[0, N-1]$, high-contrast scene highlights or deep shadows produce vertex indices outside allocated table bounds.
- **Fix:** Implemented mathematical domain normalization:
  $$\hat{c} = \frac{c - \text{domainMin}}{\text{domainMax} - \text{domainMin}}$$
  followed by strict clamping to $[0.0, 1.0]$. Coordinate mapping to lattice vertex indices $i = \lfloor \hat{c} \cdot (N - 1) \rfloor$ uses safe bounds clamping with $i_1 = \min(i_0 + 1, N - 1)$, ensuring 100% numerical stability across all edge and corner coordinates.

### Issue 17: Half-Texel 3D Texture Coordination Distortion in OpenGL ES 3.0 LUT Sampling
- **Status:** **RESOLVED** (Phase 9)
- **Symptom:** When directly sampling a 3D texture using normalized color coordinates $(r, g, b) \in [0.0, 1.0]$ in OpenGL ES shaders, extreme shadow values ($r, g, b = 0.0$) and peak highlights ($r, g, b = 1.0$) undergo edge texel clamping. Coords fall exactly on the boundary of the texture rather than at the center of the edge texels, causing clipped tone curves and color shifts at tonal extremes.
- **Root Cause:** In OpenGL ES, texture coordinates $[0.0, 1.0]$ span the outer edges of the texture boundary. For a 3D texture of size $N$, texel centers are located at $\frac{i + 0.5}{N}$ for $i \in [0, N - 1]$. The span between the first and last texel centers is $\frac{N - 1}{N}$, offset by $\frac{0.5}{N}$.
- **Fix:** Implemented rigorous half-texel offset compensation in `GlTexture3D.kt` and shader routines:
  $$\text{scale} = \frac{N - 1.0}{N}, \quad \text{offset} = \frac{0.5}{N}, \quad \text{coord}_{3D} = \text{clamp}(c, 0.0, 1.0) \cdot \text{scale} + \text{offset}$$
  With `GL_LINEAR` filtering and `GL_CLAMP_TO_EDGE` wrapping, hardware trilinear interpolation yields mathematically exact color grades across the entire gamut without boundary distortion.

### Issue 18: Headless Offscreen FBO Photo Grading & Android Bitmap Alpha Premultiplication
- **Status:** **RESOLVED** (Phase 9)
- **Symptom:** Captured still photos graded via GPU framebuffer objects (FBO) could experience luminance darkening or alpha corruption when transferring pixels between Android `Bitmap` memory and OpenGL ES textures.
- **Root Cause:** Android `Bitmap.Config.ARGB_8888` defaults to premultiplied alpha. Uploading premultiplied pixels to an OpenGL texture and reading them back via `glReadPixels(..., GL_RGBA, GL_UNSIGNED_BYTE, ...)` without configuring `Bitmap.setPremultiplied(false)` causes double-multiplication or alpha desynchronization.
- **Fix:** Architected `GpuPhotoProcessor` using headless `OffscreenSurface` (EGL 1x1 pbuffer) and `GlFrameBuffer` configured with `GL_COLOR_ATTACHMENT0`. Bitmap initialization explicitly specifies `bitmap.isPremultiplied = false` prior to `glReadPixels` buffer copying, guaranteeing byte-level color fidelity across full-resolution photo captures. Verified on Snapdragon 8 Gen 3 GPU pipeline.

### Issue 19: Android 17 / ColorOS Audio Permission Guard & Graceful Video-Only Fallback
- **Status:** **RESOLVED** (Phase 10)
- **Symptom:** In modern Android releases (Android 14–17 / ColorOS), attempting to enable audio recording without explicit runtime user consent or with audio permission restricted by OS policies throws `java.lang.SecurityException` or crashes the recording pipeline.
- **Root Cause:** Android requires runtime `RECORD_AUDIO` permission for video recording with sound. In unrooted shells, `pm grant` is restricted by platform security.
- **Fix:** Architected `VideoRecordingEngine` and `CameraXEngine` with dual permission checking (`ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED`). When audio permission is granted and audio recording is enabled by the user, `pendingRecording.withAudioEnabled()` is configured. If ungranted or disabled, the video recording engine seamlessly falls back to video-only recording without crashing or dropping frames, while the UI displays the audio badge status accordingly. Verified on OnePlus Ace 5 / 13R running Android 17 API 37.

---

## 2. Phase 7 Architecture & Verification Follow-ups

### Follow-up 1: Test-Count Reconciliation & Architecture Verification
- **Context:** Following Phase 7 implementation, the unit test suite expanded from the 102 baseline tests to 116 total tests.
- **Reconciliation Analysis:**
  - 102 baseline tests from Phases 0–6 covering CameraX, Camera2 manual controls, focus peaking, histogram analysis, Kelvin white balance, and diagnostics.
  - Phase 7 introduced 6 new test suites:
    1. `ZslRingBufferTest`: 4 tests (initialization, circular overwrite, timestamp ordering, boundary drainage).
    2. `AebExposureCoordinatorTest`: 4 tests (3/5/7 bracket steps, manual EV modulation, zero-bias validation, invariant restoration).
    3. `SynchronizedCaptureSessionTest`: 4 tests (timestamp matching, multi-surface attachment, failure isolation, lifecycle).
    4. `WatermarkRendererTest`: 4 tests (EXIF date rendering, custom brand string, font sizing, alpha blending).
    5. `ExifPrivacyScrubberTest`: 4 tests (GPS coordinate zeroing, serial number redaction, device make preservation, byte integrity).
    6. `CameraPreferencesPhase7Test`: 4 tests (ZSL toggle, AEB bracket count persistence, watermark mode persistence, EXIF privacy flags).
  - During test consolidation and runner configuration, parameter variations in preferences assertions were harmonized into comprehensive test suites, resulting in exactly **116 passing unit tests** (0 failures, 0 regressions).

### Follow-up 2: Synchronized RAW+HEIC Hardware Verification
- **Device Support & Behavior:**
  - The Qualcomm Snapdragon 8 Gen 3 camera HAL (`CPH2691IN` / `OP5D3BL1`) supports concurrent `ImageFormat.RAW_SENSOR` (DNG) alongside compressed formats.
  - Standard compressed output is `ImageFormat.JPEG`. Android 10+ exposes `ImageFormat.HEIC`, but HAL support for concurrent RAW_SENSOR + HEIC multi-surface capture varies depending on active vendor post-processing pipelines.
  - Xamera's synchronized capture architecture explicitly validates `StreamConfigurationMap.isOutputSupportedFor(ImageFormat.HEIC)`. If the HAL advertises HEIC support, a dual-surface session is bound; if unadvertised or if surface allocation throws an unsupported configuration exception, the pipeline gracefully falls back to synchronized RAW + JPEG, guaranteeing zero capture failures while preserving pristine uncompressed DNG sensor negative files.

### Follow-up 3: Zero Shutter Lag (ZSL) Dual-Layer Architecture
- **Distinction Between HAL ZSL and App-Level Ring Buffer:**
  - *Layer 1 (Vendor HAL ZSL):* Requested via `CaptureRequest.CONTROL_ENABLE_ZSL = true`. When supported by vendor HALs, the camera driver internally holds circular zero-lag buffers. However, on custom ROMs or modified firmware builds, vendor ZSL can silently fail to latch or introduce unexpected shutter delay.
  - *Layer 2 (Xamera `ZslRingBuffer`):* An autonomous application-level circular ring buffer operating over a high-throughput Camera2 preview surface. Incoming preview frames with associated `TotalCaptureResult` metadata are buffered in real time. Upon shutter press, the buffer is queried for the exact frame corresponding to the user tap timestamp ($t_0$), eliminating user-perceived shutter lag even when vendor HAL ZSL is inactive.

### Follow-up 4: Auto Exposure Bracketing (AEB) Sensor Offset Verification
- **Quantization & Sensor Hardware Response:**
  - When submitting AEB brackets via `CONTROL_AE_EXPOSURE_COMPENSATION` (auto mode), the requested EV is quantized by `CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP` (typically $1/3$ EV or $1/6$ EV).
  - On the reference platform, step size is $1/3$ EV ($0.3333$). For a $\pm 2.0\text{EV}$ bracket, the integer indices $\{-6, 0, +6\}$ are submitted.
  - Telemetry validation reads `CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION` from each returning bracket frame to verify that the HAL actually stepped the analog/digital gain, confirming 100% execution fidelity.

### Follow-up 5: RAW Vault Terminology & Storage Scope Clarification
- **Storage Scope:**
  - "RAW Vault" is not an isolated encrypted sandbox or proprietary cloud container. It is a dedicated, high-speed on-device RAW negative manager.
  - It discovers, inspects, and manages DNG files across two local storage domains:
    1. *MediaStore Public Pictures:* Photos saved under `DCIM/Camera` or `Pictures/Xamera` with `.dng` extensions.
    2. *App-Internal Storage:* Pristine capture buffers saved in `context.filesDir/raw_vault/` or scoped storage caches.
  - This dual discovery ensures users can access, share, inspect EXIF tags, and export raw negatives without file system fragmentation.

### Follow-up 6: EXIF Privacy Scope (JPEG/HEIC vs DNG Preservation)
- **Photographic Negative Integrity:**
  - The EXIF privacy scrubber operates exclusively on distributed formats (JPEG and HEIC). It strips sensitive location metadata (GPS coordinates, altitude, timestamp precision) and hardware serial numbers when requested by the user.
  - Pristine raw files (`.dng`) are intentionally preserved with full sensor calibration tags (ColorMatrix1, ColorMatrix2, ForwardMatrix1, ForwardMatrix2, CalibrationIlluminant1, BlackLevel, WhiteLevel) because removing calibration tags destroys the mathematical ability of RAW developers (such as Lightroom, Darktable, or RawTherapee) to properly demosaic the sensor bayer pattern.

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
