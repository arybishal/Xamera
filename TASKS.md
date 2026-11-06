# Tasks & Work Breakdown | Xamera

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Workspace:** `D:\Webapp\xamera`  
**Repository:** https://github.com/arybishal/Xamera  
**Developer:** Bishal Aryal  
**Version:** 0.11.0  
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
- [x] Implement independent Auto Exposure Lock (AE Lock) and Autofocus Lock (AF Lock) (`FEAT-EXP-03`, `FEAT-FOC-02`).
- [x] Build unified `PhotometryAnalyzer` consolidating preview frame analysis for all photometric monitors.
- [x] Implement live Zebra Striping overlay with configurable IRE threshold (70%–100%) (`FEAT-EXP-05`).
- [x] Implement full-spectrum 9-zone False Color exposure heatmap overlay (Broadcast IRE scale with vertical legend) (`FEAT-EXP-06`).
- [x] Build Focus Assist / Magnifier Loupe offering 2x and 4x digital preview center crop during manual focusing (`FEAT-FOC-05`).
- [x] Implement fine-grained Kelvin White Balance slider (2000K–10000K) with green/magenta tint offset via `KelvinConverter` and `RggbChannelVector` (`FEAT-COL-02`).
- [x] Implement Auto White Balance Lock (AWB Lock) (`FEAT-COL-03`).
- [x] Write Phase 6 unit tests (`PhotometryLumaZebraTest`, `FalseColorZoneMappingTest`, `KelvinToRgbGainsTest`, `AeAfLockStateTransitionTest` - 18 new tests, 102 total).
- [x] Phase 6 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 7: Advanced Capture Pipelines (ZSL, AEB, RAW+HEIC & EXIF Engine)
- [x] Implement Zero Shutter Lag (ZSL) circular preview frame ring buffer with bounded capacity and stale-frame eviction (`FEAT-EXP-08`).
- [x] Implement Auto Exposure Bracketing (AEB): rapid sequence of 3, 5, or 7 frames stepped across EV biases with invariant restoration (`FEAT-EXP-07`).
- [x] Implement simultaneous synchronized dual-saving for RAW+JPEG and RAW+HEIC sharing identical sensor timestamp (`FEAT-RAW-03`).
- [x] Build Image Watermarking engine (Text, timestamp, SAF PNG logo with alpha and EXIF transfer) (`FEAT-RAW-04`).
- [x] Build comprehensive EXIF Metadata manager with "Strip EXIF" privacy mode (GPS and device identifier stripping) (`FEAT-RAW-05`).
- [x] Build Storage Destination Selector and SAF export integration (`FEAT-RAW-06`).
- [x] Implement isolated RAW Vault screen for managing, inspecting, sharing, and exporting digital negatives (`FEAT-RAW-07`).
- [x] Write Phase 7 unit tests (`ZslRingBufferTest`, `AebExposureCoordinatorTest`, `CaptureFormatTest`, `PhotoWatermarkCompositorTest`, `ExifPrivacyManagerTest`, `RawVaultManagerTest` - 14 new tests, 116 total).
- [x] Phase 7 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 8: LUT Library & Color Science Engine (.cube 3D LUTs & Log Profiles)
- [x] Design and bundle 5 original 3D LUT presets (Natural, Cinematic Warm, Cinematic Cool, Filmic Contrast, Soft Vintage).
- [x] Implement high-performance Adobe `.cube` 3D LUT parser (sizes 17x17x17, 33x33x33, 64x64x64) with validation (`FEAT-COL-04`).
- [x] Integrate Storage Access Framework (SAF) for external `.cube` file import with 20MB bounds and SHA-256 deduplication.
- [x] Implement Room database (`LutDatabase`, `LutDao`, `LutEntity`) for user LUT profiles, favorites, and import dates.
- [x] Implement mathematical Log tone curve profiles (Flat, S-Log3/Log-C emulations) (`FEAT-COL-07`) and CPU trilinear interpolation.
- [x] Build LUT Management & Selection UI screen with search, categories, favorites, sorting, and detail inspect modal.
- [x] Write Phase 8 unit tests (`CubeLutParserTest`, `ColorScienceInterpolationTest`, `BuiltInLutPresetsTest`, `LutRepositoryTest`) — 153 total tests passing.
- [x] Phase 8 architecture verification and completion report.

---

## Phase 9: GPU Color Pipeline & Creative Shaders (Photo LUT, Film Grain & Halation)
- [x] Build EGL context and surface lifecycle manager (`EglCore`, `WindowSurface`, `OffscreenSurface`).
- [x] Implement OpenGL ES 3.0 shader program for 3D LUT sampling with half-texel offset and trilinear interpolation (`FEAT-COL-05`).
- [x] Implement real-time LUT intensity slider (0%–100%) with uniform mixing (`GpuColorConfig`).
- [x] Implement Graded Photo Capture with dual-save option (Save graded only, or Graded + Original) (`FEAT-COL-06`).
- [x] Build procedural GPU Film Grain shader simulating 35mm and 16mm organic noise (`FEAT-COL-08`).
- [x] Build photochemical Halation glow shader isolating specular highlights with red-orange dispersion (`FEAT-COL-09`).
- [x] Build Viewfinder Look Control Panel composable drawer with real-time look pills, grain, halation, and LUT library link.
- [x] Write Phase 9 unit tests (`ColorPipelineShadersTest`, `GpuColorConfigTest`, `GlTexture3DMathTest` — 16 new tests, 169 total tests passing).
- [x] Phase 9 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 10: Professional Video Engine (Bitrate, Shutter Angle, Stabilization, VU Meters & Audio Routing)
- [x] Build verified video resolution and frame-rate matrix (4K 24/30/60, 1080p 24/30/60/120, 720p 240) (`FEAT-VID-01`).
- [x] Implement configurable Video Bitrate (Standard, High 100 Mbps, Intra-frame All-I) (`FEAT-VID-02`).
- [x] Implement cinematic 180° Shutter Angle automatic exposure calculator (`FEAT-VID-03`).
- [x] Implement Multi-Mode Video Stabilization coordinator (Standard OIS, Combined OIS+EIS, Super Steady) (`FEAT-VID-04`).
- [x] Implement Professional Audio Routing (Internal Mic array, USB-C external audio interface, Bluetooth mic) (`FEAT-VID-05`).
- [x] Build dual-channel real-time Audio VU Meters (-40dBFS to 0dBFS) with clipping indicator (`FEAT-VID-06`).
- [x] Build VideoControlBar and VideoQuickSettingsSheet for ergonomic video parameters adjustment.
- [x] Write Phase 10 unit tests (`VideoConfigTest`, `ShutterAngleCalculatorTest`, `VideoBitrateProfileTest`, `VideoRecordingStateTest`, `AudioVuStateTest`, `VideoStabilizationCoordinatorTest` — 29 new tests, 198 total passing).
- [x] Phase 10 physical device validation on OnePlus Ace 5 / 13R and completion report.

---

## Phase 11: Real-Time Video GPU Pipeline, Advanced Video Modes & Camera Experience Hardening
- [x] Build dedicated GPU-to-Encoder video pipeline (`MediaCodec` input surface + `MediaMuxer` + synchronized audio) (`FEAT-VID-07`).
- [x] Enable live LUT switching and intensity adjustment during active video recording (`GpuVideoRecorder` + `GpuPreviewRenderer`).
- [x] Implement Time-Lapse Intervalometer with configurable capture intervals (0.5s to 60s) and automatic audio gating (`FEAT-VID-09`).
- [x] Implement High-Speed Slow Motion mode (120fps/240fps capture configuration) (`FEAT-VID-10`).
- [x] Implement Dual-Camera / Director's View with hardware concurrency verification and graceful diagnostic advisory fallback (`FEAT-VID-11`).
- [x] Implement App-Wide Dark Mode and Light Mode with signature Yellow/Black/White palette and DataStore persistence (`FEAT-VIEW-14`).
- [x] Implement System Window Insets hardening: status bar & navigation padding, hole-punch safety, and transient bar restoration on notification shade pull (`FEAT-VIEW-15`).
- [x] Implement 120 Hz High-Refresh-Rate Display mode detection and dynamic window attribute request (`FEAT-VIEW-16`).
- [x] Write Phase 11 unit tests (`AppThemeModeTest`, `DisplayRefreshRateTest`, `DualCameraCapabilityTest`, `TimelapseConfigTest`, `SlowMotionConfigTest`, `CameraModeTest`, updated `CameraPreferencesTest` — 15 new tests, 213 total passing).
- [x] Phase 11 physical device validation on OnePlus Ace 5 / 13R (recorded 1080p 30fps GPU-graded MP4, verified with `ffprobe`) and completion report.

---

## Phase 12: Video Processing & Export (Trimming, Transcoding, Social Presets & Audio Extraction)
- [x] Build on-device video processing pipeline using native `MediaExtractor` and `MediaMuxer` (`FEAT-VID-12`).
- [x] Implement sub-second trimming with monotonic presentation timestamp (PTS) rebasing starting at zero.
- [x] Implement hardware video codec discovery and transcoding (H.264, H.265, AV1) (`FEAT-VID-13`).
- [x] Implement audio routing: Keep audio, Mute audio (video-only MP4), and Extract Audio Only (.m4a container) (`FEAT-VID-14`).
- [x] Implement Social Media Export Presets (Instagram Reels, Instagram Story, YouTube, YouTube Shorts, TikTok, WhatsApp, Custom) (`FEAT-VID-15`).
- [x] Build `VideoExportEstimator` calculating target bitrates, estimated file sizes, and duration metrics.
- [x] Build `VideoExportSheet` Jetpack Compose UI integrated directly into Media Viewer (`FEAT-VID-16`).
- [x] Write Phase 12 automated unit tests (`VideoExportConfigTest`, `SocialExportPresetTest`, `VideoExportEstimatorTest`, `VideoExportStateTest`, `VideoExportAudioActionTest` — 20 new tests, 233 total unit tests passing).
- [x] Write and verify on-device connected tests (`VideoExportDeviceTest` — 4/4 passing on OnePlus Ace 5 / 13R).
- [x] Physical device validation on OnePlus Ace 5 / 13R (trimmed 3.03s 1080p clip `xamera_export_445911020848475683.mp4`, verified with `ffprobe` and frame extraction) and completion report.

---

## Phase 13: Computational Photography & Dedicated Portrait Mode [COMPLETED]
- [x] Implement multi-frame Image Stacking Subsystem (`ImageStackingConfig`, `ImageStackingMode`, `ImageStackingFrameCount`) (`FEAT-CMP-03`, `FEAT-CMP-08`, `FEAT-CMP-10`).
- [x] Implement dual-axis luminance projection Frame Alignment Engine (`FrameAlignmentEngine.kt`).
- [x] Implement pixel difference Motion Estimation Engine with ghost rejection masking (`MotionEstimationEngine.kt`).
- [x] Implement Multi-Frame Image Fusion Engine (`LOW_LIGHT` temporal bilateral averaging, `HDR` Mertens radiance weighting, `DETAIL_ENHANCEMENT` unsharp edge sharpening, and `SMART_AUTO` scene analyzer) (`ImageFusionEngine.kt`).
- [x] Implement Dedicated Portrait Mode (`CameraMode.PORTRAIT`) on bottom viewfinder mode selector (`FEAT-CMP-05`).
- [x] Implement synthetic aperture optical bokeh blur (`PortraitAperture`: f/1.4, f/2.0, f/2.8, f/4.0, f/5.6) with specular highlight expansion (`PortraitBokehRenderer.kt`).
- [x] Implement 4 Portrait Studio Lighting Styles (`NATURAL`, `STUDIO`, `SOFT`, `DRAMATIC`) (`PortraitLightingStyle.kt`).
- [x] Implement on-device hardware-assisted Subject Segmentation with bilateral boundary feathering and safe fallback (`SubjectSegmentationEngine.kt`).
- [x] Implement Viewfinder overlay `PortraitControlBar.kt` and status badges (`PORTRAIT f/2.8`, `STACK: AUTO (5F)`).
- [x] Implement Settings Section 4b for Computational Photography & Image Stacking.
- [x] Write Phase 13 unit tests (`ImageStackingModelsTest`, `PortraitModelsTest`, `CameraPreferencesTest`, `CameraModeTest`).
- [x] Write Phase 13 connected device instrumented tests (`ComputationalPhotographyDeviceTest`).
- [x] Phase 13 physical device validation on OnePlus Ace 5 / 13R (`CPH2691IN`, Snapdragon 8 Gen 3, Android 17 API 37) and completion reports (`PHASE_13_COMPLETION_REPORT.md` and `PHASE_12_13_COMBINED_COMPLETION_REPORT.md`).

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
