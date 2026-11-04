# Known Issues & Limitations | Adaptive Camera

## Phase 1 Baseline

### 1. Resolved: App Startup Crash on `CameraViewModel` Instantiation
- **Status:** **RESOLVED**
- **Symptom:** Application automatically closed immediately upon launch.
- **Root Cause:** `CameraViewModel` defined default constructor parameters (`cameraEngine`, `mediaStoreManager`) without the `@JvmOverloads` annotation. When Android's `by viewModels()` (`AndroidViewModelFactory`) invoked reflection to find `clazz.getConstructor(Application.class)`, it threw `java.lang.NoSuchMethodException: com.bishal.adaptivecamera.ui.camera.CameraViewModel.<init> [class android.app.Application]`.
- **Fix:** Added `@JvmOverloads constructor(...)` to `CameraViewModel`, ensuring bytecode exposes the standard single-argument `(Application)` constructor expected by `AndroidViewModelFactory`. Verified on physical device: process PID is continuously active and focused.

### 2. Physical Identity in Cross-Flashed / Converted ROM Environments
- **Status:** Documented / Analyzed
- **Detail:** On devices running cross-flashed regional firmware (e.g. Indian OnePlus 13R `CPH2691IN` on board `OP5D3BL1`), `Build.MODEL`, `Build.PRODUCT`, and `Build.FINGERPRINT` report the operating system's target SKU rather than physical PCB component provenance.
- **Mitigation:** The application never claims confirmed physical identity solely based on software properties. It reports `firmwareIdentity` separately from `probableHardwareFamily`, sets `physicalIdentityConfidence` to `UNCONFIRMED`, and presents explainable identity evidence factors.

### 3. Auxiliary Physical Camera Access in Standard CameraX Engine
- **Status:** Documented / Handled
- **Detail:** On devices with logical multi-camera HALs (such as Camera 0 on the Ace 5 / 13R), CameraX binds to the logical camera ID, allowing Qualcomm's multi-camera compositor to dynamically switch between physical sub-lenses (IDs 2 and 3) during optical and digital zoom. Direct uncomposited streaming from individual physical sub-cameras is not exposed by standard CameraX selectors.
- **Mitigation:** Physical IDs are accurately discovered, audited, and displayed in Diagnostics. Advanced direct physical sensor binding is architected for Phase 2 and Phase 4 via Camera2 extensions.

---

## Phase 4 Pro Photography

### 4. CameraX 1.4.0 RAW Stream Output Limitation
- **Status:** **RESOLVED** via Architecture Isolation
- **Detail:** CameraX 1.4.0 `ImageCapture` only supports output formats `OUTPUT_FORMAT_JPEG` and `OUTPUT_FORMAT_JPEG_ULTRA_HDR`. It cannot bind `ImageFormat.RAW_SENSOR` or construct Adobe DNG files.
- **Mitigation:** Implemented a dedicated `Camera2ManualEngine` that manages `CameraDevice`, `CameraCaptureSession`, and dedicated `ImageReader(ImageFormat.RAW_SENSOR)` alongside `ImageFormat.JPEG`, using Android's native `DngCreator` for valid DNG packaging.

### 5. Fixed Focus on Front Camera
- **Status:** Documented / Handled
- **Detail:** The front-facing selfie camera (Camera ID 1) reports `LENS_INFO_MINIMUM_FOCUS_DISTANCE = 0.0f` (fixed focus infinity).
- **Mitigation:** The manual focus slider is automatically disabled and labeled "Fixed Focus (Front)" when the front camera is active, preventing non-functional controls from misleading the user.

### 6. Manual Sensor Overrides AE Control
- **Status:** Handled per Photographic Standards
- **Detail:** When manual ISO sensitivity or manual shutter speed is requested, `CONTROL_AE_MODE` must be set to `CONTROL_AE_MODE_OFF`. In this state, hardware AE compensation (`CONTROL_AE_EXPOSURE_COMPENSATION`) is ignored by the camera HAL.
- **Mitigation:** The application clearly displays whether manual sensor control is active via `ProAppliedTelemetryBadge` and disables the AE compensation slider during manual exposure to prevent conflicting requests.

