# Tasks & Work Breakdown | Xamera

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Workspace:** `D:\Webapp\xamera`  
**Repository:** https://github.com/arybishal/Xamera  
**Developer:** Bishal Aryal  
**Version:** 0.6.0  
**Date:** 2026-11-06  

---

## Phase 0: Foundation and Hardware Research
- [x] Inspect development environment (JDK, SDK, Gradle, ADB, Git, GitHub).
- [x] Configure Gradle Kotlin DSL project with Version Catalog (`gradle/libs.versions.toml`).
- [x] Configure Jetpack Compose BOM, Material 3, CameraX, Room, DataStore, and Coroutines.
- [x] Set up package identity: `com.bishal.adaptivecamera`.
- [x] Establish modular package structure per architecture specifications.
- [x] Define DeviceInfo model and OnePlus Ace 5 / 13R target platform identifier heuristics.
- [x] Implement CapabilityStatus enum and CameraHardwareLevel mapping.
- [x] Implement CameraCapabilityProfile and DeviceCapabilityReport data models.
- [x] Build prototype CameraCapabilityScanner using Android Camera2 CameraManager.
- [x] Create Diagnostics Compose UI with real-time capability inspection and JSON/Text export.
- [x] Write unit tests for DeviceInfo, CameraResolution, CameraCapabilityProfile, and CapabilityStatus (12 tests).
- [x] Assemble and verify debug APK (`app/build/outputs/apk/debug/app-debug.apk`).
- [x] Document environment inspection and produce Phase 0 Completion Report.

---

## Phase 1: Camera Foundation
- [x] Runtime permission manager for Camera and Audio.
- [x] CameraX Preview use case with Viewfinder integration (`PreviewView`).
- [x] Photo capture pipeline via `ImageCapture`.
- [x] Video recording pipeline via `VideoCapture` and `Recorder`.
- [x] Camera lens switcher (Front / Rear).
- [x] Pinch-to-zoom and zoom ratio controls with step pills (`0.6x`, `1x`, `2x`, etc.).
- [x] Tap-to-focus and metering point calculation (`FocusMeteringAction`).
- [x] Exposure compensation slider.
- [x] Flash mode controls (Auto, On, Off, Torch).
- [x] Photo countdown timer (Off, 3s, 10s) with animated overlay.
- [x] Shutter flash animation.
- [x] MediaStore saving with scoped storage compliance (`Pictures/AdaptiveCamera`, `Movies/AdaptiveCamera`).
- [x] Camera lifecycle management and recovery.
- [x] Write Phase 1 unit tests (`CameraConfigurationTest`, `CameraSessionStateTest`, `MediaStoreManagerTest`).
- [x] Phase 1 acceptance verification on physical device and report.

---

## Phase 2: Hardware Intelligence
- [x] Comprehensive Camera2 characteristics parser (16 capability dimensions).
- [x] Logical-to-physical camera mapping (Camera 0 composing physical sensors 2 & 3).
- [x] Detailed stream configuration analysis (all formats and sizes: JPEG, RAW_SENSOR, YUV_420_888, PRIVATE).
- [x] Dynamic Range Profiles discovery (HLG10, HDR10, HDR10+).
- [x] Sensor optical stabilization (OIS) and electronic stabilization (EIS) inspection.
- [x] Sensor pixel array, physical dimensions mm, and color filter pattern analysis.
- [x] Manual sensor capabilities and ISO/shutter hardware range discovery.
- [x] Capability persistence & caching via DataStore / Room (`CameraCapabilityRepository`).
- [x] Write Phase 2 unit tests (`HardwareIntelligencePhase2Test` - 9 tests).
- [x] Full device capability report export (JSON/Text).
- [x] Phase 2 acceptance verification on physical device and report.

---

## Phase 3: Smart Auto Photography
- [x] Frame luminance and scene contrast analyzer (`FrameMetricsAnalyzer`).
- [x] Rule-based scene categorization (Portrait, Landscape, Night, Macro, Backlit, Movement, Low Light).
- [x] Smart configuration generator (ISO bias, shutter recommendation, HDR prompt).
- [x] Lens recommendation policy (ultra-wide vs wide vs macro).
- [x] Explainable scene suggestion chip UI with one-tap apply EV adjustment.
- [x] Write Phase 3 unit tests (`SmartAutoPhase3Test` - 12 tests).
- [x] Phase 3 acceptance verification on physical device and report.

---

## Phase 4: Pro Photography & RAW
- [x] Camera2 manual sensor session builder (`Camera2ManualEngine`) on dedicated background `HandlerThread`.
- [x] Manual ISO controller with hardware range clamping (`100` to `6400` on reference device).
- [x] Manual shutter speed controller with nanosecond-to-fraction calculation (`ShutterSpeedHelper`).
- [x] Manual focus distance controller (diopters) with front-camera fixed focus guard.
- [x] Custom Auto White Balance (AWB) presets (`DAYLIGHT`, `CLOUDY`, `SHADE`, `FLUORESCENT`, `INCANDESCENT`, `TWILIGHT`, `AUTO`).
- [x] Real-time 4-channel live RGB and luminance histogram (`HistogramAnalyzer`, `HistogramView`).
- [x] Real-time 2D spatial gradient focus peaking overlay (`FocusPeakingAnalyzer`, `FocusPeakingOverlay`).
- [x] Real-time applied camera sensor telemetry badge (`ProAppliedTelemetryBadge`).
- [x] Pro parameter control bar (`ProControlBar`) with parameter drawers and auto restore.
- [x] RAW capture (`RAW_SENSOR`) and Adobe DNG creation via `DngCreator` (`DngCaptureManager`).
- [x] TIFF magic bytes validator (`0x49492A00` / `0x4D4D002A`).
- [x] Write Phase 4 unit tests (`ProPhotographyPhase4Test` - 13 tests).
- [x] Phase 4 acceptance verification on physical device and report.

---

## Phase 4 Addendum: Camera Settings, Hardware Diagnostics & Exposure Reliability
- [x] Relocate Hardware Intelligence into dedicated Settings screen (`SettingsScreen.kt`).
- [x] Implement "Check Hardware Capability" section organizing all 16 Phase 2 capability categories.
- [x] Add asynchronous capability re-scan with cached vs freshly scanned indicator badge.
- [x] Implement copy diagnostics as JSON/TXT and system share sheet export.
- [x] Fix exposure slider reliability: read actual CameraX `ExposureState` hardware range dynamically.
- [x] Implement exposure readiness guards (`isExposureStateReady`, `isExposureControlAvailable`).
- [x] Prevent HAL request flooding and desynchronization with `future.addListener` callback synchronization.
- [x] Implement visible loading, disabled ("Exposure Fixed"), and reset controls in `ExposureSlider`.
- [x] Reinitialize exposure state safely across camera switching and session recreation.
- [x] Implement immersive camera screen hiding Android status bar using `WindowInsetsControllerCompat`.
- [x] Restore normal status bar when navigating away from camera screen and on app pause.
- [x] Write Phase 4 Addendum unit tests (`ExposureReliabilityAndSettingsTest` - 8 tests).
- [x] Phase 4 Addendum acceptance verification on physical device and report.

---

## Phase 5: Viewfinder Ergonomics, Composition Tools & Assistive Overlays
- [x] Redesign camera viewfinder UI with Apple Camera-inspired, dark-first photography aesthetics.
- [x] Implement Composition Grids: Rule of Thirds, Golden Ratio, Golden Spiral, and Center Crosshairs (`FEAT-VIEW-02`).
- [x] Implement Virtual Horizon level indicator using `Sensor.TYPE_ROTATION_VECTOR` with fallback and cyan level feedback (`FEAT-VIEW-03`).
- [x] Implement unified Aspect Ratio Selector: 4:3 (3:4 portrait), 16:9, 1:1, Full, and 2.39:1 framing masks and Bitmap center-crop on save (`FEAT-VIEW-04`).
- [x] Build interactive rotary Zoom Dial with log scale, step snapping pills, and single `ZoomController` (`FEAT-VIEW-05`).
- [x] Implement tactile Haptic Feedback engine for shutter trip, dial notches, level zeroing, and mode switches (`FEAT-VIEW-08`).
- [x] Implement Floating Movable Shutter button draggable across the viewfinder with tap vs drag detection (`FEAT-VIEW-09`).
- [x] Add Android Quick Launch intent handling (`ACTION_STILL_IMAGE_CAMERA`, `ACTION_IMAGE_CAPTURE`, double-tap power button) (`FEAT-VIEW-10`).
- [x] Implement Front Screen Flash with full-screen warm white display illumination boost (`FEAT-VIEW-11`).
- [x] Implement DataStore preferences persistence via `CameraPreferencesRepository` and Settings UI integration.
- [x] Write Phase 5 unit tests (`AspectRatioConfigTest`, `ZoomControllerTest`, `CompositionGridTypeTest`, `VirtualHorizonCalculationTest`, `CameraPreferencesTest`, `CameraConfigurationTest` - 23 new tests, 84 total).
- [x] Phase 5 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 6: Pro Exposure & Focus Monitoring Engines (Zebra, False Color & Focus Assist)
- [ ] Implement independent Auto Exposure Lock (AE Lock) and Autofocus Lock (AF Lock) (`FEAT-EXP-03`, `FEAT-FOC-02`).
- [ ] Build unified `PhotometryAnalyzer` consolidating preview frame analysis for all photometric monitors.
- [ ] Implement live Zebra Striping overlay with configurable IRE threshold (70%–100%) (`FEAT-EXP-05`).
- [ ] Implement full-spectrum 16-step False Color exposure heatmap overlay (Arri/IRE scale) (`FEAT-EXP-06`).
- [ ] Build Focus Assist / Magnifier Loupe offering 2x and 4x digital preview center crop during manual focusing (`FEAT-FOC-05`).
- [ ] Implement fine-grained Kelvin White Balance slider (2000K–10000K) with green/magenta tint offset (`FEAT-COL-02`).
- [ ] Implement Auto White Balance Lock (AWB Lock) (`FEAT-COL-03`).
- [ ] Write Phase 6 unit tests (`PhotometryLumaZebraTest`, `FalseColorZoneMappingTest`, `KelvinToRgbGainsTest`, `AeAfLockStateTransitionTest`).
- [ ] Write Phase 6 instrumented tests (`ZebraOverlayRenderingTest`, `FocusMagnifierTransformTest`).
- [ ] Phase 6 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 7: Advanced Capture Pipelines (ZSL, AEB, RAW+HEIC & EXIF Engine)
- [ ] Implement Zero Shutter Lag (ZSL) circular preview frame ring buffer (`FEAT-EXP-08`).
- [ ] Implement Auto Exposure Bracketing (AEB): rapid sequence of 3, 5, or 7 frames stepped across EV biases (`FEAT-EXP-07`).
- [ ] Implement simultaneous dual-saving for RAW+JPEG and RAW+HEIC (`FEAT-RAW-03`).
- [ ] Build Image Watermarking engine (Date/Time, GPS, Camera Model, Custom Copyright string) (`FEAT-RAW-04`).
- [ ] Build comprehensive EXIF Metadata manager with "Strip EXIF" privacy mode (`FEAT-RAW-05`).
- [ ] Build Storage Destination Selector (Internal app storage, public `DCIM`, custom directory, SD card) (`FEAT-RAW-06`).
- [ ] Implement isolated RAW Vault directory for non-indexed digital negative archiving (`FEAT-RAW-07`).
- [ ] Write Phase 7 unit tests (`ZslRingBufferTest`, `AebExposureStepTest`, `ExifPrivacyStripTest`, `WatermarkCompositorTest`).
- [ ] Write Phase 7 instrumented tests (`HeicEncodingTest`, `SafCustomDirectoryWriteTest`).
- [ ] Phase 7 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 8: LUT Library & Color Science Engine (.cube 3D LUTs & Log Profiles)
- [ ] Design and bundle 5 original 3D LUT presets (Natural, Cinematic Warm, Cinematic Cool, Filmic Contrast, Soft Vintage).
- [ ] Implement high-performance Adobe `.cube` 3D LUT parser (sizes 17x17x17, 33x33x33, 64x64x64) with validation (`FEAT-COL-04`).
- [ ] Integrate Storage Access Framework (SAF) for external `.cube` file import.
- [ ] Implement Room database for user LUT profiles, favorites, and import dates.
- [ ] Implement mathematical Log tone curve profiles (Flat, S-Log/Log-C emulations) (`FEAT-COL-07`).
- [ ] Build LUT Management & Selection UI screen.
- [ ] Write Phase 8 unit tests (`CubeLutParserTest`, `CubeLutParserErrorHandlingTest`, `LogToneCurveTransferTest`, `LutDatabaseCrudTest`).
- [ ] Write Phase 8 instrumented tests (`LutAssetLoadingTest`, `SafLutImportTest`).
- [ ] Phase 8 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 9: GPU Color Pipeline & Creative Shaders (Photo LUT, Film Grain & Halation)
- [ ] Build EGL context and surface lifecycle manager (`EglCore`, `WindowSurface`).
- [ ] Implement OpenGL ES 3.0 shader program for 3D LUT sampling with trilinear interpolation (`FEAT-COL-05`).
- [ ] Implement real-time LUT intensity slider (0%–100%) with uniform mixing.
- [ ] Implement Graded Photo Capture with dual-save option (Save graded only, or Graded + Original) (`FEAT-COL-06`).
- [ ] Build procedural GPU Film Grain shader simulating 35mm and 16mm organic noise (`FEAT-COL-08`).
- [ ] Build photochemical Halation glow shader isolating specular highlights with red-orange dispersion (`FEAT-COL-09`).
- [ ] Write Phase 9 unit tests (`GlShaderCompilationTest`, `Lut3dTextureAllocationTest`, `FilmGrainParameterTest`, `HalationThresholdTest`).
- [ ] Write Phase 9 instrumented tests (`OffscreenGpuPhotoRenderTest`, `EglSurfaceLifecycleTest`).
- [ ] Phase 9 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 10: Professional Video Engine (Bitrate, Shutter Angle, Stabilization, VU Meters & Audio Routing)
- [ ] Build verified video resolution and frame-rate matrix (4K 24/30/60, 1080p 24/30/60/120, 720p 240) (`FEAT-VID-01`).
- [ ] Implement configurable Video Bitrate (Standard, High 100 Mbps, Intra-frame All-I) (`FEAT-VID-02`).
- [ ] Implement cinematic 180° Shutter Angle automatic exposure calculator (`FEAT-VID-03`).
- [ ] Implement Multi-Mode Video Stabilization coordinator (Standard OIS, Combined OIS+EIS, Super Steady) (`FEAT-VID-04`).
- [ ] Implement Professional Audio Routing (Internal Mic array, USB-C external audio interface, Bluetooth mic) (`FEAT-VID-05`).
- [ ] Build dual-channel real-time Audio VU Meters (-40dBFS to 0dBFS) with clipping indicator (`FEAT-VID-06`).
- [ ] Write Phase 10 unit tests (`VideoProfileValidatorTest`, `ShutterAngleCalculatorTest`, `AudioVuMeterDecibelTest`, `StabilizationModeNegotiationTest`).
- [ ] Write Phase 10 instrumented tests (`AudioDeviceEnumerationTest`, `VideoRecordingSanityTest`).
- [ ] Phase 10 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 11: Real-Time Video GPU Pipeline & Advanced Video Modes (Time-lapse, Slow-mo, Anamorphic & Dual-Cam)
- [ ] Build dedicated GPU-to-Encoder video pipeline (`MediaCodec` input surface + `MediaMuxer` + synchronized audio) (`FEAT-VID-07`).
- [ ] Enable live LUT switching and intensity adjustment during active video recording.
- [ ] Implement Anamorphic De-Squeeze (1.33x, 1.55x, 1.8x live preview and video export scaling) (`FEAT-VID-08`).
- [ ] Implement Time-Lapse Intervalometer with configurable capture intervals (0.5s to 60s) (`FEAT-VID-09`).
- [ ] Implement High-Speed Slow Motion mode (120fps/240fps capture rendered to 24fps playback) (`FEAT-VID-10`).
- [ ] Implement Dual-Camera / Director's View with hardware concurrency verification and graceful notice fallback (`FEAT-VID-11`).
- [ ] Write Phase 11 unit tests (`AnamorphicMatrixTest`, `TimelapseIntervalCalculatorTest`, `SlowMotionPlaybackSpeedTest`, `DualCameraConcurrencyCheckTest`).
- [ ] Write Phase 11 instrumented tests (`GpuVideoEncodingTest`, `AudioVideoSyncTest`).
- [ ] Phase 11 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 12: Specialized Shooting Modes (Night, Astro, Portrait, Burst, Panorama, Macro, Document/QR)
- [ ] Build handheld multi-frame Night mode with motion de-ghosting and temporal noise reduction (`FEAT-CMP-03`).
- [ ] Build Astrophotography mode with celestial star-motion tracking and sky de-noising (`FEAT-CMP-04`).
- [ ] Build Portrait mode with synthetic depth-of-field extraction and variable bokeh simulation (f/1.4–f/16) (`FEAT-CMP-05`).
- [ ] Build high-speed Burst mode (15–30 fps) with AI-powered best-shot selector (`FEAT-CMP-06`).
- [ ] Build Sweep Panorama mode with real-time gyroscope guidance and cylindrical stitching (`FEAT-CMP-07`).
- [ ] Build dedicated Macro mode locking minimum focus distance or routing to physical macro lens (`FEAT-FOC-07`).
- [ ] Implement real-time Document Scanner with automatic edge detection and quadrilateral rectification (`FEAT-INT-04`).
- [ ] Implement real-time QR / Barcode Scanner with sub-frame recognition and action sheet (`FEAT-INT-05`).
- [ ] Write Phase 12 unit tests (`NightFrameAlignmentTest`, `AstrophotographyStackingTest`, `BurstBestShotScorerTest`, `BarcodeParsingTest`, `DocumentQuadRectificationTest`).
- [ ] Write Phase 12 instrumented tests (`PortraitSegmentationTest`, `PanoramaSensorTrackerTest`).
- [ ] Phase 12 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 13: Multi-Frame Computational Photography (HDR Fusion, Focus Stacking, Super-Res & Stroboscopic)
- [ ] Implement multi-frame HDR Fusion pipeline merging AEB bracketed exposures with radiance de-ghosting (`FEAT-CMP-08`).
- [ ] Implement Macro/Landscape Focus Stacking combining depth slices into an all-in-focus composite (`FEAT-CMP-09`).
- [ ] Implement sub-pixel Super-Resolution leveraging natural hand micro-tremor (`FEAT-CMP-10`).
- [ ] Implement Action Stroboscopic merge tracking and blending moving subjects across a static background (`FEAT-CMP-11`).
- [ ] Implement Variable Physical Aperture controller with graceful fixed-aperture fallback (`FEAT-FOC-08`).
- [ ] Write Phase 13 unit tests (`HdrDeghostingTest`, `FocusStackingLaplacianTest`, `SuperResolutionShiftTest`, `VariableApertureFallbackTest`).
- [ ] Write Phase 13 instrumented tests (`NativeComputationalPipelineTest`).
- [ ] Phase 13 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 14: Peripherals, Media Processing & Gallery Integration (Bluetooth, Gimbals, FFmpeg Export & Media Management)
- [ ] Implement direct Multi-Lens sensor selection bypassing logical compositor (`FEAT-FOC-06`).
- [ ] Implement Bluetooth HID remote shutter trigger support (`FEAT-INT-06`).
- [ ] Implement DJI / Zhiyun gimbal Bluetooth protocol integration for wheel and joystick control.
- [ ] Integrate embedded on-device FFmpeg engine for LUT baking, transcoding, and anamorphic stretching (`FEAT-INT-07`).
- [ ] Expand in-app MediaStore Gallery with DNG inspector, EXIF browser, and video player (`FEAT-INT-08`).
- [ ] Write Phase 14 unit tests (`BluetoothHidKeycodeTest`, `FFmpegCommandBuilderTest`, `GalleryDngMetadataExtractionTest`).
- [ ] Write Phase 14 instrumented tests (`GimbalConnectionTest`, `FFmpegTranscodeExecutionTest`).
- [ ] Phase 14 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 15: UI Polish, Performance Benchmarking & Hardening
- [ ] Audit entire application for dark-first aesthetic consistency, high-contrast typography, and 120Hz smoothness.
- [ ] Audit and optimize camera open latency to achieve < 500ms cold startup.
- [ ] Conduct GPU memory leak audit, texture recycling verification, and surface lifecycle hardening.
- [ ] Implement thermal mitigation policies and thermal state listener (`ThermalMonitor`).
- [ ] Profile battery consumption during prolonged 4K recording and live preview.
- [ ] Write Phase 15 unit tests (`ThermalStateMitigationTest`, `MemoryRecyclingTest`).
- [ ] Write Phase 15 instrumented tests (`StartupBenchmarkTest`, `FrameDropBenchmarkTest`).
- [ ] Phase 15 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 16: Final Release & Publishing
- [ ] Run full automated test regression suite across all 16 phases (Target: 150+ unit tests).
- [ ] Configure and verify ProGuard/R8 optimization rules (`proguard-rules.pro`).
- [ ] Assemble signed Release APK (`app-release.apk`) and compute SHA256 checksums.
- [ ] Perform open-source third-party license and privacy compliance audit.
- [ ] Commit all code and documentation to GitHub repository (`Xamera`).
- [ ] Create GitHub Release with release APK, checksum manifest, and release notes.
- [ ] Produce Final Project Completion Report.
