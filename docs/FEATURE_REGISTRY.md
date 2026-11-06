# Xamera Feature Registry & Deduplication Matrix

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Workspace:** `D:\Webapp\xamera`  
**Repository:** https://github.com/arybishal/Xamera  
**Developer:** Bishal Aryal  
**Reference Platform:** OnePlus Ace 5 / OnePlus 13R Platform (`OP5D3BL1` / `CPH2691IN` / Snapdragon 8 Gen 3)  
**Date:** 2026-11-06  
**Status:** Living Canonical Registry

---

## 1. Architectural Deduplication Rules

To eliminate fragmented state, conflicting HAL requests, and duplicated processing pipelines, all features in Xamera must strictly conform to these nine canonical architectural contracts:

1. **Exposure Architecture (Single Controller Contract):**
   - A single unified exposure state model governs both auto-exposure bias and manual sensor control.
   - When manual ISO (`SENSOR_SENSITIVITY`) or manual shutter speed (`SENSOR_EXPOSURE_TIME`) is locked, the HAL hardware mode transitions to `CONTROL_AE_MODE_OFF`.
   - In `CONTROL_AE_MODE_OFF`, EV compensation sliders are automatically disabled in the UI to prevent illegal requests.
   - Reverting to Auto re-enables `CONTROL_AE_MODE_ON` and re-attaches exposure compensation dynamically clamped to the active sensor's `ExposureState.exposureCompensationRange`.

2. **Histogram & Photometric Monitoring (Single Stream Engine):**
   - A single downsampled preview analysis pipeline (`ImageFormat.YUV_420_888` / GPU compute pass) provides real-time photometric data.
   - Luminance and RGB distributions are display modes of the same underlying 64-bin per-channel histogram engine.
   - Live Zebra Striping (IRE thresholding) and Full-Spectrum False Color monitoring consume the identical luminance buffer/shader stages without creating duplicate preview frame subscriptions.

3. **Focus Architecture (Unified Focus Controller):**
   - Tap-to-focus, Continuous Autofocus (`CONTROL_AF_MODE_CONTINUOUS_PICTURE`), AF Lock, and Manual Diopter Focus (`CONTROL_AF_MODE_OFF` with `LENS_FOCUS_DISTANCE`) run through one shared focus controller.
   - Focus Peaking is an analytical visualization overlay that monitors the high-frequency spatial gradients of the active viewfinder stream without modifying camera focus state.
   - Focus Magnifier / Loupe applies a digital viewport crop on the viewfinder preview without altering the underlying capture stream.

4. **Zoom & Lens Switching (Single Controller Pathway):**
   - Touch pinch-to-zoom, quick-step buttons (`0.6x`, `1x`, `2x`, etc.), smooth zoom dial dragging, and multi-lens camera switching all update a single `zoomRatio` state.
   - The HAL logical camera compositing layer automatically arbitrates physical sub-sensor hand-offs (e.g. from 0.6x ultra-wide to 1.0x wide).
   - In manual physical lens override mode, direct physical camera binding uses the same parameter normalization.

5. **Aspect Ratio & Framing (Single Framing Configuration):**
   - One aspect ratio state (`4:3`, `16:9`, `1:1`, `FULL`, `2.39:1`) governs both viewfinder framing masks and captured output matrix transformations.
   - Prevents WYSIWYG mismatch between the captured image and on-screen framing guides.

6. **RAW Capture (Unified Capture Engine):**
   - RAW-only (`DNG`), JPEG/HEIC-only, and RAW+JPEG/HEIC are output configurations of a single `takePhoto()` dispatch.
   - The underlying capture session binds `ImageFormat.RAW_SENSOR` and `ImageFormat.JPEG` simultaneously, feeding `DngCreator` and `MediaStoreManager` through a coordinated transaction.

7. **Color Science & GPU Shaders (Unified Render Graph):**
   - All real-time color processing runs through a single OpenGL ES 3.0 shader graph.
   - The pipeline stages sequentially: `OES Camera Texture -> Color Space / Log Curve -> 3D LUT Sampling (Trilinear) -> Film Grain -> Halation Glow -> Output Target`.
   - The same GL context and shader programs serve live preview grading, graded photo capture (PBO/Bitmap), and graded video recording (`MediaCodec` input surface) without redundant code.

8. **Video Architecture (Unified Recording Model):**
   - Video resolution, frame rates, bitrate targets, shutter angle, stabilization mode, and audio routing are encapsulated in one `VideoRecordingConfiguration`.
   - Time-lapse and Slow Motion are specialized capture/playback rate profiles operating inside the shared video pipeline.

9. **Hardware Intelligence (Single Repository of Truth):**
   - All hardware capabilities are scanned once at runtime by `CameraCapabilityScanner` and persisted in `CameraCapabilityRepository`.
   - Viewfinder controls, pro sliders, and settings inspect this centralized repository. Settings screens provide user-facing shortcuts and diagnostic visibility but never instantiate independent camera controllers.

---

## 2. Feature Implementation & Verification Status Legend

- **Implemented and tested:** Feature is implemented in production code and covered by automated unit/instrumentation tests.
- **Implemented but not physically verified:** Implemented in code and passing unit tests, but awaiting confirmation on physical reference hardware.
- **Partially implemented:** Foundation or initial iteration exists (e.g. basic presets), but requested expansion is pending.
- **UI-only:** User interface elements exist, but underlying camera HAL binding or processing is not yet wired.
- **Unsupported by the reference device:** Feature is architected with graceful fallback because physical reference hardware lacks the underlying sensor/HAL capability.
- **Not implemented:** Scheduled for future implementation phases.

---

## 3. Master Feature Registry Matrix

| Feature ID | Canonical Name | Description | Module | UI Location | Shared Dependencies | Hardware Requirements | Current Status | Verification Status | Phase | Related Features |
|---|---|---|---|---|---|---|:---:|:---:|:---:|---|
| **FEAT-VIEW-01** | Immersive Viewfinder | Fullscreen viewfinder hiding system bars with gesture swipe restore | `ui.camera` | Main Viewfinder | `WindowInsetsControllerCompat` | Any Android 10+ screen | Implemented and tested | Verified (Unit + Device) | Phase 4A | FEAT-VIEW-04, FEAT-VIEW-07 |
| **FEAT-VIEW-02** | Composition Grids | Rule of thirds, golden ratio, golden spiral, center crosshairs overlays | `ui.camera.components` | Viewfinder Overlay | `CameraConfiguration` | None (Compose Canvas) | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-03, FEAT-VIEW-04 |
| **FEAT-VIEW-03** | Virtual Horizon Level | Dual-axis pitch and roll leveling indicator with zero-angle cyan shift | `core.sensors` | Viewfinder Overlay | `SensorManager` (Rotation Vector / Accel) | Accelerometer + Gyroscope | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-02, FEAT-VIEW-08 |
| **FEAT-VIEW-04** | Aspect Ratio Selector | Interactive 4:3 (3:4 portrait), 16:9, 1:1, Full, and 2.39:1 framing masks and cropping | `camera.config` | Viewfinder & Top Bar | `AspectRatioConfig`, `MediaStoreManager` | None | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-01, FEAT-VID-08 |
| **FEAT-VIEW-05** | Interactive Zoom Dial | Rotary tactile zoom dial with logarithmic scaling and step snapping | `camera.controls` | Viewfinder Lower Edge | `ZoomController`, `HapticEngine` | Camera2 Zoom Ratio | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-06, FEAT-FOC-06 |
| **FEAT-VIEW-06** | Pinch & Step Zoom | Multi-touch pinch gestures and discrete lens magnification pills (0.6x, 1x, 2x, etc.) | `camera.engine` | Viewfinder Center / Lower | `CameraEngine`, `ZoomCapabilityInfo` | Camera2 Zoom Ratio | Implemented and tested | Verified (Unit + Device) | Phase 1/5 | FEAT-VIEW-05, FEAT-FOC-06 |
| **FEAT-VIEW-07** | Left-Handed UI Mode | Mirrored UI layout positioning shutter button, pro controls, and tabs for left thumb | `ui.camera` | Root Viewfinder Layout | `PreferencesDataStore` | None | Partially implemented | Layout Dependency (Phase 15) | Phase 15 | FEAT-VIEW-09, FEAT-VIEW-05 |
| **FEAT-VIEW-08** | Haptic Feedback Engine | Tactile vibration feedback on shutter trip, dial ticks, level lock, and mode switches | `core.haptics` | Global UI Events | `Vibrator` / `VibrationEffect` | Device Haptic Actuator | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-05, FEAT-VIEW-03 |
| **FEAT-VIEW-09** | Floating Movable Shutter | Secondary draggable shutter button placeable anywhere on screen for one-hand shooting | `ui.camera.components` | Movable Viewfinder Layer | `CameraViewModel`, `PointerInput` | Touchscreen | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-07, FEAT-INT-06 |
| **FEAT-VIEW-10** | Quick Launch Intent | Support for lock screen camera shortcuts and double-tap power button intents | `core.lifecycle` | Android Manifest Activity | `MainActivity`, Intent Filter | Hardware Key / OS Trigger | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-VIEW-01 |
| **FEAT-VIEW-11** | Front Screen Flash | Fullscreen high-brightness warm white display fill light for selfie illumination | `ui.camera.components` | Fullscreen Overlay | `WindowManager.LayoutParams` | Front Facing Camera | Implemented and tested | Verified (Unit + Device) | Phase 5 | FEAT-EXP-01, FEAT-COL-01 |
| **FEAT-VIEW-12** | Countdown Photo Timer | Off, 3-second, and 10-second self-timer with animated central countdown graphic | `ui.camera.components` | Top Bar & Viewfinder | `CameraConfiguration`, Coroutines | None | Implemented and tested | Verified (Unit + Device) | Phase 1 | FEAT-VIEW-13, FEAT-EXP-08 |
| **FEAT-VIEW-13** | Shutter Flash Animation | Realistic high-speed white frame blink confirming photo capture completion | `ui.camera` | Viewfinder Surface | Jetpack Compose Animations | None | Implemented and tested | Verified (Unit + Device) | Phase 1 | FEAT-EXP-08 |
| **FEAT-EXP-01** | Auto Exposure (EV) Bias | Dynamic hardware exposure compensation slider with HAL step negotiation | `camera.engine` | Top Bar & Side Slider | `CameraControl`, `ExposureState` | AE Compensation Supported | Implemented and tested | Verified (Unit + Device) | Phase 1/4A | FEAT-EXP-02, FEAT-EXP-03 |
| **FEAT-EXP-02** | Manual Sensor ISO & Shutter | Explicit sensor sensitivity (ISO) and exposure time (shutter ns) with hardware clamping | `photography.pro` | Pro Control Bar (Tabs) | `Camera2ManualEngine` | `REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR` | Implemented and tested | Verified (Unit + Device) | Phase 4 | FEAT-EXP-01, FEAT-EXP-04 |
| **FEAT-EXP-03** | AE / AF Lock Engine | Independent or unified lock holding exposure and focus targets indefinitely | `camera.controls` | Viewfinder Tap & Pro Bar | `CaptureRequest.CONTROL_AE_LOCK` | AE / AF Lock Supported | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-EXP-01, FEAT-FOC-01 |
| **FEAT-EXP-04** | Live 4-Channel Histogram | Real-time 64-bin Luminance and RGB distributions with shadow/highlight clipping | `photography.pro` | Viewfinder Top Right | `PhotometryAnalyzer`, Canvas Graph | YUV_420_888 Preview Stream | Implemented and tested | Verified (Unit + Device) | Phase 4/6 | FEAT-EXP-05, FEAT-EXP-06 |
| **FEAT-EXP-05** | Live Zebra Striping | Viewfinder highlight diagonal zebra overlay flagging overexposed regions (70-100% IRE) | `photography.pro` | Viewfinder Live Overlay | `PhotometryAnalyzer`, `ZebraStripingOverlay` | YUV_420_888 Preview Stream | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-EXP-04, FEAT-EXP-06 |
| **FEAT-EXP-06** | Full-Spectrum False Color | 9-zone broadcast IRE photometric exposure heatmap rendering color-coded exposure zones | `photography.pro` | Viewfinder Live Overlay | `PhotometryAnalyzer`, `FalseColorOverlay` | YUV_420_888 Preview Stream | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-EXP-04, FEAT-EXP-05 |
| **FEAT-EXP-07** | Auto Exposure Bracketing | Rapid burst of 3, 5, or 7 frames stepped across EV biases (e.g. -2, 0, +2 EV) for HDR | `photography.capture` | Pro Capture Menu & Settings | `AebExposureCoordinator`, `Camera2ManualEngine` | Hardware AE / Manual Shutter | Implemented and tested | Verified (Unit + Device) | Phase 7 | FEAT-CMP-08, FEAT-RAW-01 |
| **FEAT-EXP-08** | Zero Shutter Lag (ZSL) | Circular preview buffer capturing the exact sensor timestamp of shutter touch | `photography.capture` | Still Capture Engine / Settings | `ZslRingBuffer`, `CameraXEngine`, `Camera2ManualEngine` | High-bandwidth HAL Memory | Implemented and tested | Verified (Unit + Device) | Phase 7 | FEAT-EXP-07, FEAT-CMP-06 |
| **FEAT-FOC-01** | Tap-to-Focus & Metering | Single-tap 3A focus and metering bounding box with auto-reset timer | `camera.engine` | Viewfinder Surface | `FocusMeteringAction`, `CameraControl` | Continuous AF Supported | Implemented and tested | Verified (Unit + Device) | Phase 1 | FEAT-FOC-02, FEAT-FOC-03 |
| **FEAT-FOC-02** | Autofocus Lock (AF Lock) | Dedicated toggle or long-tap freezing lens voice-coil motor position | `camera.controls` | Viewfinder Tap & Pro Bar | `CaptureRequest.CONTROL_AF_MODE` | AF Lock Supported | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-FOC-01, FEAT-EXP-03 |
| **FEAT-FOC-03** | Manual Focus (Diopters) | Fine-grained manual lens focus distance slider with front-camera fixed focus guard | `photography.pro` | Pro Control Bar (Focus Tab) | `Camera2ManualEngine`, Diopter Calc | `LENS_INFO_MINIMUM_FOCUS_DISTANCE > 0` | Implemented and tested | Verified (Unit + Device) | Phase 4 | FEAT-FOC-04, FEAT-FOC-05 |
| **FEAT-FOC-04** | Spatial Focus Peaking | 2D spatial gradient edge detection overlay highlighting in-focus planes in neon green | `photography.pro` | Viewfinder Live Overlay | `FocusPeakingAnalyzer`, Overlay | YUV Preview Frame Analyzer | Implemented and tested | Verified (Unit + Device) | Phase 4 | FEAT-FOC-03, FEAT-FOC-05 |
| **FEAT-FOC-05** | Focus Assist / Magnifier | 2x or 4x digital preview center crop loupe assisting micro-focus adjustments | `ui.camera.pro` | Viewfinder Overlay Loupe | `FocusMagnifierOverlay` | None (Preview Crop) | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-FOC-03, FEAT-FOC-04 |
| **FEAT-FOC-06** | Multi-Lens Direct Selector | Direct sensor binding for Wide (Main), Ultra-Wide, and Macro sub-cameras | `camera.discovery` | Viewfinder Lens Switcher | `CameraManager`, Logical Mapping | Multi-camera Physical IDs | Partially implemented | Verified (Unit only) | Phase 14 | FEAT-VIEW-06, FEAT-FOC-07 |
| **FEAT-FOC-07** | Dedicated Macro Mode | Macro focusing profile locking minimum focus distance or routing to physical macro | `photography.auto` | Scene Mode / Lens Menu | `LensRecommendationPolicy` | Macro lens or min diopter > 5 | Partially implemented | Verified (Unit only) | Phase 12 | FEAT-FOC-03, FEAT-FOC-06 |
| **FEAT-FOC-08** | Variable Physical Aperture | Hardware aperture step toggling on supported lenses; disabled with clear notice on fixed | `camera.capability` | Pro Bar / Settings | `CaptureRequest.LENS_APERTURE` | Dual/Variable Physical Iris | Unsupported by reference device | Hardware Incompatible | Phase 13 | FEAT-EXP-02, FEAT-CMP-05 |
| **FEAT-COL-01** | White Balance Presets | Photographic AWB presets (Daylight, Cloudy, Shade, Fluorescent, Incandescent, Twilight) | `photography.pro` | Pro Control Bar (WB Tab) | `Camera2ManualEngine`, AWB Modes | `CONTROL_AWB_AVAILABLE_MODES` | Implemented and tested | Verified (Unit + Device) | Phase 4 | FEAT-COL-02, FEAT-COL-03 |
| **FEAT-COL-02** | Kelvin Manual White Balance | Continuous color temperature slider (2000K–10000K) with green/magenta tint offset | `photography.pro` | Pro Control Bar (WB Tab) | `KelvinConverter`, `RggbChannelVector` | `COLOR_CORRECTION_MODE` | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-COL-01, FEAT-COL-03 |
| **FEAT-COL-03** | Auto White Balance Lock | Lock current chromaticity gains against dynamic scene illumination changes | `camera.controls` | Pro Control Bar / Viewfinder | `CaptureRequest.CONTROL_AWB_LOCK` | AWB Lock Supported | Implemented and tested | Verified (Unit + Device) | Phase 6 | FEAT-COL-01, FEAT-COL-02 |
| **FEAT-COL-04** | 3D LUT Library & Parser | Adobe `.cube` 3D/1D LUT parser (17, 33, 64), validation & Room DB, SAF import | `lut.parser`, `lut.repository` | Settings → LUT Library Screen | Room DB, DataStore, SAF | Device Storage Access | Implemented and tested | Verified (Unit + Architecture) | Phase 8 | FEAT-COL-05, FEAT-COL-07 |
| **FEAT-COL-05** | Live Viewfinder GPU LUT | Trilinear 3D texture sampling shader applying real-time color grading at 60fps | `graphics.opengl` | Live Viewfinder Surface | `EGLManager`, OpenGL ES 3.0 | OpenGL ES 3.0 | Not implemented | Unverified | Phase 9 | FEAT-COL-04, FEAT-COL-06 |
| **FEAT-COL-06** | Graded Photo Capture | Dual-capture engine rendering graded JPEG matching viewfinder grade + raw untouched | `photography.processing`| Shutter Button Pipeline | OpenGL PBO / Bitmap, MediaStore | GPU ES 3.0, Scoped Storage | Not implemented | Unverified | Phase 9 | FEAT-COL-05, FEAT-RAW-03 |
| **FEAT-COL-07** | Log Color Profiles & Color Science | Flat and logarithmic gamma tone curves (S-Log3, Log-C, HLG, PQ), 5 mathematical presets | `lut.presets`, `lut.model` | LUT Library & Presets | Trilinear Interpolator, Color Transfer Functions | Mathematical Color Engine | Implemented and tested | Verified (Unit + Architecture) | Phase 8 | FEAT-COL-04, FEAT-VID-07 |
| **FEAT-COL-08** | Procedural Film Grain | Real-time GPU noise generator simulating 35mm / 16mm organic photographic grain | `graphics.shaders` | Shaders / Creative Menu | OpenGL ES 3.0 Fragment Shader | OpenGL ES 3.0 | Not implemented | Unverified | Phase 9 | FEAT-COL-05, FEAT-COL-09 |
| **FEAT-COL-09** | Photochemical Halation | Photochemical red-orange edge scattering glow around high-contrast specular highlights | `graphics.shaders` | Shaders / Creative Menu | Two-pass Blur & Blend Shader | OpenGL ES 3.0 | Not implemented | Unverified | Phase 9 | FEAT-COL-05, FEAT-COL-08 |
| **FEAT-RAW-01** | RAW_SENSOR Capture | Native uncompressed sensor bayer stream capture via Camera2 `ImageFormat.RAW_SENSOR` | `photography.raw` | Pro Mode Capture | `Camera2ManualEngine`, ImageReader | `REQUEST_AVAILABLE_CAPABILITIES_RAW` | Implemented and tested | Verified (Unit + Device) | Phase 4 | FEAT-RAW-02, FEAT-RAW-03 |
| **FEAT-RAW-02** | Adobe DNG Generation | Compliant TIFF/DNG container synthesis with lens shading, color matrices via `DngCreator`| `photography.raw` | Storage / Background Worker | `DngCreator`, MediaStore | `RAW_SENSOR` Stream | Implemented and tested | Verified (Unit + Device) | Phase 4 | FEAT-RAW-01, FEAT-RAW-05 |
| **FEAT-RAW-03** | RAW+JPEG / RAW+HEIC | Simultaneous capture saving uncompressed DNG alongside compressed JPEG or HEIF | `photography.capture` | Pro Settings / Pro Bar / Shutter | Dual `ImageReader` Synchronization | Dual Stream Processing | Implemented and tested | Verified (Unit + Device) | Phase 4/7 | FEAT-RAW-01, FEAT-COL-06 |
| **FEAT-RAW-04** | Image Watermarking | Customizable stamp overlay (Date/Time, Text, SAF custom PNG logo, original preservation) | `photography.watermark`| Settings & Pro Engine | `PhotoWatermarkCompositor` | Canvas Bitmap Compositor | Implemented and tested | Verified (Unit + Device) | Phase 7 | FEAT-RAW-05, FEAT-RAW-06 |
| **FEAT-RAW-05** | EXIF Management & Privacy | Full EXIF tag population, author metadata injection, and "Strip EXIF" privacy toggle | `photography.exif` | Settings Screen & Pro Engine | `ExifPrivacyManager` | In-place ExifInterface Stripping | Implemented and tested | Verified (Unit + Device) | Phase 7 | FEAT-RAW-02, FEAT-RAW-04 |
| **FEAT-RAW-06** | Storage Destination Selector | Choose between internal app storage, public `DCIM`, custom directory, or SD card | `core.storage` | Settings Screen & Vault | Storage Access Framework (SAF) | Storage Access Permissions | Implemented and tested | Verified (Unit + Device) | Phase 7 | FEAT-RAW-07, FEAT-INT-08 |
| **FEAT-RAW-07** | Secure RAW Vault | Dedicated local DNG management, MediaStore indexing, thumbnail preview, metadata inspection, sharing & SAF export | `ui.vault` | Settings / Main Nav / Vault Screen | `RawVaultManager`, `RawVaultScreen` | High-speed Storage I/O | Implemented and tested | Verified (Unit + Device) | Phase 7 | FEAT-RAW-02, FEAT-INT-08 |
| **FEAT-VID-01** | Pro Video Frame Rates & Res | 4K UHD (24, 30, 60 fps), 1080p FHD (24, 30, 60, 120 fps), 720p HD (240 fps) | `video.profiles` | Video Control Bar | `VideoCapture`, `CamcorderProfile` | Snapdragon 8 Gen 3 Video HAL | Partially implemented | Verified (Unit only) | Phase 10 | FEAT-VID-02, FEAT-VID-03 |
| **FEAT-VID-02** | Video Bitrate & Intra-Frame | Standard, High (100 Mbps), and Intra-frame (All-I) H.264/H.265 encoding profiles | `video.encoder` | Video Settings Menu | `MediaCodec`, `MediaFormat` | Hardware Video Encoder | Not implemented | Unverified | Phase 10 | FEAT-VID-01, FEAT-VID-07 |
| **FEAT-VID-03** | Cinematic 180° Shutter Angle| Automatic exposure time calculation matching photographic shutter angle ($1/(2 \times FPS)$) | `video.recording` | Video Pro Bar | `Camera2` Shutter Calculator | Manual Sensor in Video | Not implemented | Unverified | Phase 10 | FEAT-EXP-02, FEAT-VID-01 |
| **FEAT-VID-04** | Multi-Mode Video Stabilization| Standard Optical (OIS), Electronic (EIS), and Gyro-Assisted Super Steady toggles | `video.recording` | Top Bar / Video Menu | `CaptureRequest.CONTROL_VIDEO_STABILIZATION_MODE` | OIS / EIS HAL Support | Partially implemented | Verified (Unit only) | Phase 10 | FEAT-VIEW-03, FEAT-VID-01 |
| **FEAT-VID-05** | Professional Audio Routing | Selectable audio input: Built-in Array, USB-C External Interface, Bluetooth Mic | `video.audio` | Video Settings / Top Bar | `AudioManager`, `AudioDeviceInfo` | Audio Input Peripherals | Not implemented | Unverified | Phase 10 | FEAT-VID-06, FEAT-INT-06 |
| **FEAT-VID-06** | Real-Time Audio VU Meters | Dual-channel real-time peak decibel level meters (-40dBFS to 0dBFS) with clipping alarm | `video.audio` | Viewfinder Recording Header | `AudioRecord` RMS Analysis | Microphone Permission | Not implemented | Unverified | Phase 10 | FEAT-VID-05, FEAT-VID-01 |
| **FEAT-VID-07** | Real-Time Video GPU LUT | Direct GPU-to-Encoder video pipeline recording graded 3D LUT video to MP4 | `video.encoder` | Video Mode Engine | `EGLSurface`, `MediaCodec`, `MediaMuxer` | GLES 3.0 + Hardware Codec | Not implemented | Unverified | Phase 11 | FEAT-COL-05, FEAT-COL-07 |
| **FEAT-VID-08** | Anamorphic De-Squeeze | 1.33x, 1.55x, and 1.8x live preview de-squeeze and final video export aspect scaling | `video.recording` | Video Aspect Menu | GL Viewport & Surface Scaling | OpenGL ES 3.0 | Not implemented | Unverified | Phase 11 | FEAT-VIEW-04, FEAT-INT-07 |
| **FEAT-VID-09** | Time-Lapse Intervalometer | Configurable frame interval (0.5s to 60s) rendering stabilized 4K/1080p time-lapse | `video.recording` | Mode Selector (TIME-LAPSE) | Frame Accumulator, `MediaMuxer` | Hardware Timer / Video HAL | Not implemented | Unverified | Phase 11 | FEAT-VID-01, FEAT-EXP-02 |
| **FEAT-VID-10** | High-Speed Slow Motion | 120fps / 240fps high-frame-rate sensor stream recorded for 24fps smooth playback | `video.recording` | Mode Selector (SLOW-MO) | Constrained High Speed Session | High Speed FPS Streams | Not implemented | Unverified | Phase 11 | FEAT-VID-01, FEAT-VID-02 |
| **FEAT-VID-11** | Dual-Camera / Director's View | Simultaneous front and rear camera capture in Picture-in-Picture or Split layout | `video.recording` | Mode Selector (DUAL) | Multi-Camera Concurrent HAL | `CameraManager.getConcurrentCameraIds()` | Unsupported by reference device | Hardware Incompatible | Phase 11 | FEAT-FOC-06, FEAT-VID-01 |
| **FEAT-CMP-01** | Smart Auto Scene Classifier | Rule-based scene categorization (Portrait, Landscape, Night, Macro, Backlit, Movement) | `intelligence.scene` | Viewfinder Suggestion Chip | `FrameMetricsAnalyzer`, Y-plane stats | YUV Preview Stream | Implemented and tested | Verified (Unit + Device) | Phase 3 | FEAT-CMP-02, FEAT-EXP-01 |
| **FEAT-CMP-02** | Smart Auto Configuration | Dynamic exposure bias, shutter compensation, and lens recommendation policies | `intelligence.policies` | Viewfinder Suggestion Chip | `SmartConfigurationGenerator` | Real-time Frame Metrics | Implemented and tested | Verified (Unit + Device) | Phase 3 | FEAT-CMP-01, FEAT-FOC-06 |
| **FEAT-CMP-03** | Multi-Frame Night Mode | Handheld low-light multi-frame alignment, optical flow motion de-ghosting, de-noising | `photography.night` | Mode Selector (NIGHT) | OpenCV / RenderEffect Alignment | Fast Burst Memory Buffer | Not implemented | Unverified | Phase 12 | FEAT-CMP-04, FEAT-EXP-02 |
| **FEAT-CMP-04** | Astrophotography Mode | Long-exposure star stacking (15-30s frames over 2-5 min) with celestial star tracking | `photography.night` | Mode Selector (ASTRO) | Gyroscope Tracking, Bayer Stacking | Tripod Stability, Manual Sensor | Not implemented | Unverified | Phase 12 | FEAT-CMP-03, FEAT-EXP-02 |
| **FEAT-CMP-05** | Portrait Bokeh Simulator | Synthetic depth map extraction with variable aperture blur simulation (f/1.4–f/16) | `photography.processing`| Mode Selector (PORTRAIT) | Dual-Pixel Depth / ML Kit Segmentation| Dual Camera or NPU | Not implemented | Unverified | Phase 12 | FEAT-FOC-08, FEAT-CMP-01 |
| **FEAT-CMP-06** | High-Speed Burst Mode | High-frequency sequential capture (15-30 fps) with AI-powered best shot selector | `camera.capture` | Long-press Shutter | Circular Memory Buffer, Sharpness Score| High-bandwidth RAM | Not implemented | Unverified | Phase 12 | FEAT-EXP-08, FEAT-INT-08 |
| **FEAT-CMP-07** | Sweep Panorama Mode | Gyro-guided horizontal/vertical sweep panorama with live alignment stitching | `photography.processing`| Mode Selector (PANO) | Gyroscope Sensor, Cylindrical Warper | Gyroscope + Hardware Accelerated| Not implemented | Unverified | Phase 12 | FEAT-VIEW-03, FEAT-VIEW-04 |
| **FEAT-CMP-08** | Multi-Frame HDR Fusion | High dynamic range de-ghosting and radiance fusion from bracketed AEB exposures | `photography.hdr` | Auto / Pro HDR Pipeline | Exposure Alignment, Tone Mapper | AEB Multi-frame Capture | Not implemented | Unverified | Phase 13 | FEAT-EXP-07, FEAT-COL-07 |
| **FEAT-CMP-09** | Macro Focus Stacking | Depth-bracketed sequential focus slices merged into all-in-focus sharp macro | `photography.processing`| Pro Macro Feature | Voice-coil Stepper, Laplacian Fusion | Precision AF Voice Coil | Not implemented | Unverified | Phase 13 | FEAT-FOC-03, FEAT-FOC-07 |
| **FEAT-CMP-10** | Sub-Pixel Super-Resolution | High-frequency spatial detail reconstruction utilizing natural hand micro-tremor | `photography.processing`| Pro 50MP Reconstruct | Sub-pixel Registration, Interpolator | Multi-frame High-bandwidth RAM | Not implemented | Unverified | Phase 13 | FEAT-EXP-08, FEAT-CMP-08 |
| **FEAT-CMP-11** | Action Stroboscopic Merge | Multi-exposure motion tracking blending a moving subject across a fixed background | `photography.processing`| Creative Mode Menu | Background Separation, Motion Masking | Steady Camera Mount / Handheld | Not implemented | Unverified | Phase 13 | FEAT-CMP-05, FEAT-CMP-06 |
| **FEAT-INT-01** | Hardware Capability Scanner | Comprehensive Camera2 characteristics parser discovering 16 capability dimensions | `camera.capability` | Background System | `CameraManager`, `CameraCharacteristics` | Android Camera2 API | Implemented and tested | Verified (Unit + Device) | Phase 2 | FEAT-INT-02, FEAT-INT-03 |
| **FEAT-INT-02** | Capability Diagnostic Screen | Centralized "Check Hardware Capability" inspection screen with JSON/TXT copy & share | `ui.settings` | Settings Screen | `DiagnosticsViewModel`, Settings UI | None | Implemented and tested | Verified (Unit + Device) | Phase 4A | FEAT-INT-01, FEAT-INT-03 |
| **FEAT-INT-03** | Explainable Identity Engine | Clear differentiation between flashed firmware identity (`CPH2691`) and physical board | `core.diagnostics` | Settings / Diagnostics | `DeviceInfo`, Hardware Model Heuristics | None | Implemented and tested | Verified (Unit + Device) | Phase 0/4A | FEAT-INT-01, FEAT-INT-02 |
| **FEAT-INT-04** | Live Document Scanner | Real-time rectangular boundary detection, perspective quadrilateral rectification | `intelligence.analysis` | Mode Selector (DOC) | ML Kit / OpenCV Edge Detector | Fast YUV Camera Stream | Not implemented | Unverified | Phase 12 | FEAT-INT-05, FEAT-RAW-06 |
| **FEAT-INT-05** | Live QR / Barcode Scanner | Sub-frame latency QR and linear barcode detection with instant link/text preview | `intelligence.analysis` | Viewfinder Auto Overlay | ML Kit Barcode Scanning | Camera Frame Stream | Not implemented | Unverified | Phase 12 | FEAT-INT-04, FEAT-VIEW-01 |
| **FEAT-INT-06** | Bluetooth Remote & Gimbal | Bluetooth HID shutter trigger support and DJI/Zhiyun gimbal joystick/wheel protocol | `core.peripherals` | Settings / Peripherals | Bluetooth LE, HID Device Receiver | Bluetooth Hardware | Not implemented | Unverified | Phase 14 | FEAT-VIEW-09, FEAT-VIEW-05 |
| **FEAT-INT-07** | Embedded FFmpeg Transcoder | On-device FFmpeg binaries for LUT baking, anamorphic stretching, and stream remuxing | `video.export` | Background Worker / Gallery | Mobile FFmpeg / C++ NDK Wrapper | High-performance CPU/GPU | Not implemented | Unverified | Phase 14 | FEAT-VID-07, FEAT-VID-08 |
| **FEAT-INT-08** | Scoped Media Gallery | In-app MediaStore gallery grid, full-screen zoomable viewer, video player, DNG badges | `gallery.ui` | Bottom Left Thumbnail | `MediaStore`, Room DB, Video Player | Scoped Storage Access | Implemented and tested | Verified (Unit + Device) | Phase 1/14 | FEAT-RAW-02, FEAT-RAW-07 |

---

## 4. Hardware Limit & Compatibility Assessment (OnePlus Ace 5 / 13R Reference)

Based on direct hardware extraction from the physical Qualcomm Snapdragon 8 Gen 3 platform (`CPH2691IN` on `OP5D3BL1`):

1. **Logical vs Physical Camera Binding (`FEAT-FOC-06`):**
   - Camera ID `0` is a `LOGICAL_MULTI_CAMERA` comprising physical sensors `2` (Wide 5.59mm) and `3` (Ultra-wide 1.68mm).
   - In standard CameraX modes, Qualcomm's multi-camera compositor manages optical switching.
   - For direct physical sub-camera access (`FEAT-FOC-06`), direct Camera2 sessions must bind to physical IDs `2` or `3` explicitly.

2. **Fixed Focus on Front & Ultra-Wide Sensors (`FEAT-FOC-03`, `FEAT-FOC-07`):**
   - Front Camera (Camera `1`): `LENS_INFO_MINIMUM_FOCUS_DISTANCE = 0.0f` (Fixed focus infinity). Manual focus diopter controls must be disabled with visual label `"Fixed Focus (Front)"`.
   - Ultra-Wide Sub-Camera (Camera `3`): `LENS_INFO_MINIMUM_FOCUS_DISTANCE = 0.0f` (Fixed hyperfocal distance at 1.74m). Macro mode must utilize Main Camera `0` / `2` with `minimumFocusDistance = 10.0` diopters (~10cm boundary).

3. **Variable Physical Aperture (`FEAT-FOC-08`):**
   - All sensors on the OnePlus Ace 5 / 13R report a single fixed aperture:
     - Camera `0` / `2`: Fixed $f/1.88$
     - Camera `1`: Fixed $f/2.40$
     - Camera `3`: Fixed $f/2.20$
   - Hardware status: **Unsupported by reference device**. The UI will display `"Fixed Aperture (f/1.88)"` in telemetry and disable physical iris adjustments.

4. **Concurrent Multi-Camera Streaming (`FEAT-VID-11`):**
   - Android Camera2 API `CameraManager.getConcurrentCameraIds()` returns empty sets on this platform due to ISP concurrency limitations.
   - Dual-camera recording (`FEAT-VID-11`) cannot stream simultaneous full-resolution frames from Camera 0 and Camera 1. Status: **Unsupported by reference device**. A clear fallback message will be displayed.

5. **Dynamic Range & 10-Bit Profiles:**
   - Supported dynamic range profiles include `HLG10`, `HDR10`, and `HDR10+` on Camera `0`, `1`, `2`, and `3`.
   - 10-bit video encoding requires compatible `MediaCodec` surface bindings in Phase 10.
