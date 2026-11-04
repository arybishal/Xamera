# Tasks & Work Breakdown | Adaptive Camera

## Phase 0: Foundation and Hardware Research
- [x] Inspect development environment (JDK, SDK, Gradle, ADB, Git, GitHub).
- [x] Configure Gradle Kotlin DSL project with Version Catalog (`gradle/libs.versions.toml`).
- [x] Configure Jetpack Compose BOM, Material 3, CameraX, Room, DataStore, and Coroutines.
- [x] Set up package identity: `com.bishal.adaptivecamera`.
- [x] Establish modular package structure per Part 4 architecture specifications.
- [x] Define DeviceInfo model and OnePlus Ace 5 China target identifier logic.
- [x] Implement CapabilityStatus enum and CameraHardwareLevel mapping.
- [x] Implement CameraCapabilityProfile and DeviceCapabilityReport data models.
- [x] Build prototype CameraCapabilityScanner using Android Camera2 CameraManager.
- [x] Create Diagnostics Compose UI with real-time capability inspection and JSON/Text export.
- [x] Write unit tests for DeviceInfo, CameraResolution, CameraCapabilityProfile, and CapabilityStatus.
- [x] Assemble and verify debug APK (`app/build/outputs/apk/debug/app-debug.apk`).
- [x] Document environment inspection and produce Phase 0 Completion Report.

---

## Phase 1: Camera Foundation
- [x] Runtime permission manager for Camera, Audio, and Storage.
- [x] CameraX Preview use case with Viewfinder integration.
- [x] Photo capture pipeline via ImageCapture.
- [x] Video recording pipeline via VideoCapture and Recorder.
- [x] Camera lens switcher (Front / Rear).
- [x] Pinch-to-zoom and zoom ratio controls.
- [x] Tap-to-focus and metering point calculation.
- [x] Exposure compensation slider.
- [x] Flash mode controls (Auto, On, Off, Torch).
- [x] MediaStore saving with scoped storage compliance.
- [x] Camera lifecycle management and recovery.
- [x] Hardware diagnostic correction (device classification audit & camera facing fix).
- [x] Phase 1 acceptance tests and report.

---

## Phase 2: Hardware Intelligence
- [x] Comprehensive Camera2 characteristics parser.
- [x] Logical-to-physical camera mapping.
- [x] Detailed stream configuration analysis (all formats and sizes).
- [x] Dynamic Range Profiles discovery (HLG10, HDR10, etc.).
- [x] Sensor optical stabilization (OIS) and electronic stabilization (EIS) inspection.
- [x] Capability persistence & caching via DataStore / Room.
- [x] Full device capability report export (JSON/Text).
- [x] Phase 2 acceptance tests and report.

---

## Phase 3: Smart Auto Photography
- [x] Frame luminance and scene contrast analyzer.
- [x] Rule-based scene categorization (Portrait, Landscape, Night, Macro, Backlit, Movement).
- [x] Smart configuration generator (ISO bias, shutter recommendation, HDR prompt).
- [x] Lens recommendation policy.
- [x] Explainable scene suggestion UI.
- [x] Phase 3 acceptance tests and report.

---

## Phase 4: Pro Photography
- [x] Camera2 manual sensor session builder.
- [x] Manual ISO controller with hardware range clamping.
- [x] Manual shutter speed controller with nanosecond-to-fraction display.
- [x] Manual focus distance controller (diopters).
- [x] Custom Auto White Balance (AWB) lock and manual Kelvin presets.
- [x] Real-time RGB and luminance histogram.
- [x] RAW capture (RAW_SENSOR) and DNG creation via `DngCreator`.
- [x] Phase 4 acceptance tests and report.

---

## Phase 4 Addendum: Camera Settings, Hardware Diagnostics & Exposure Reliability
- [x] Relocate Hardware Intelligence into dedicated Settings screen (`SettingsScreen.kt`).
- [x] Implement "Check Hardware Capability" section organizing all 16 Phase 2 capability categories.
- [x] Add asynchronous capability re-scan with cached vs freshly scanned indicator.
- [x] Implement copy diagnostics as JSON/TXT and system share sheet export.
- [x] Fix exposure slider reliability: read actual CameraX `ExposureState` hardware range dynamically.
- [x] Implement exposure readiness guards (`isExposureStateReady`, `isExposureControlAvailable`).
- [x] Prevent HAL request flooding and desynchronization with `future.addListener` callback synchronization.
- [x] Implement visible loading, disabled ("Exposure Fixed"), and reset controls in `ExposureSlider`.
- [x] Reinitialize exposure state safely across camera switching and session recreation.
- [x] Implement immersive camera screen hiding Android status bar using `WindowInsetsControllerCompat`.
- [x] Restore normal status bar when navigating away from camera screen and on app pause.
- [x] Add Phase 4 Addendum unit tests (`ExposureReliabilityAndSettingsTest`).

---

## Phase 5: LUT Library
- [ ] Design 5 original built-in 3D LUT presets (Natural, Cinematic Warm, Cinematic Cool, Filmic Contrast, Soft Vintage).
- [ ] Implement `.cube` file parser with domain and dimension validation.
- [ ] Support 3D LUT sizes 17x17x17, 33x33x33, and 64x64x64.
- [ ] Storage Access Framework import for external `.cube` files.
- [ ] Room database for LUT profiles and favorite flags.
- [ ] LUT thumbnail generator.
- [ ] Phase 5 acceptance tests and report.

---

## Phase 6: GPU Photo LUT
- [ ] EGL context and surface lifecycle manager.
- [ ] OpenGL ES 3.0 shader program for 3D LUT sampling with trilinear interpolation.
- [ ] Live preview LUT rendering pipeline.
- [ ] LUT intensity slider (0% to 100%) with uniform mixing.
- [ ] Side-by-side original vs graded preview comparison.
- [ ] Capture graded JPEG matching viewfinder color grade.
- [ ] Dual-save option (Save graded only, or Save graded + original).
- [ ] Phase 6 acceptance tests and report.

---

## Phase 7: Real-Time Video LUT
- [ ] Dedicated EGL video recording pipeline.
- [ ] MediaCodec video encoder with surface input.
- [ ] Direct GPU-to-Encoder frame rendering.
- [ ] Audio recording and AudioRecord / MediaCodec synchronization.
- [ ] Live LUT switching and intensity adjustment during active recording.
- [ ] Frame timestamp tracking and dropped frame handling.
- [ ] Finalization via MediaMuxer.
- [ ] Phase 7 acceptance tests and report.

---

## Phase 8: Pro Video
- [ ] Capability-verified resolution and FPS combinations (1080p 30/60, 4K 30/60).
- [ ] Manual exposure and focus during recording.
- [ ] Optical and electronic stabilization toggles.
- [ ] Audio input source selection (Default, External Mic, Unprocessed).
- [ ] Recording timer, remaining storage estimator, and thermal throttler.
- [ ] Phase 8 acceptance tests and report.

---

## Phase 9: Computational Photography
- [ ] Multi-frame night capture sequencer.
- [ ] Frame alignment and temporal noise reduction.
- [ ] Exposure bracket capture for HDR merging.
- [ ] Highlight recovery and shadow detail enhancement.
- [ ] Phase 9 acceptance tests and report.

---

## Phase 10: Gallery & Media Management
- [ ] MediaStore query engine for app photos and videos.
- [ ] Responsive grid layout with media badges (RAW, LUT, Video).
- [ ] Full-screen image viewer with gesture zooming.
- [ ] Video playback controller.
- [ ] EXIF and LUT metadata inspector.
- [ ] Delete, share, and export operations.
- [ ] Phase 10 acceptance tests and report.

---

## Phase 11: UI & Performance Optimization
- [ ] Refined dark-first professional camera interface.
- [ ] Haptic feedback and smooth control transitions.
- [ ] GPU memory leak audit and texture recycling.
- [ ] Camera open latency benchmarking.
- [ ] Thermal mitigation policies.
- [ ] Phase 11 acceptance tests and report.

---

## Phase 12: Final Release & Publishing
- [ ] Full test suite execution and device regression.
- [ ] Release APK assembly and signature verification.
- [ ] License and privacy audits.
- [ ] Git commit and push to GitHub (`Xamera`).
- [ ] Create GitHub release with release APK and SHA256 checksums.
- [ ] Final project completion report.
