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
- [ ] Runtime permission manager for Camera, Audio, and Storage.
- [ ] CameraX Preview use case with Viewfinder integration.
- [ ] Photo capture pipeline via ImageCapture.
- [ ] Video recording pipeline via VideoCapture and Recorder.
- [ ] Camera lens switcher (Front / Rear).
- [ ] Pinch-to-zoom and zoom ratio controls.
- [ ] Tap-to-focus and metering point calculation.
- [ ] Exposure compensation slider.
- [ ] Flash mode controls (Auto, On, Off, Torch).
- [ ] MediaStore saving with scoped storage compliance.
- [ ] Camera lifecycle management and recovery.
- [ ] Phase 1 acceptance tests and report.

---

## Phase 2: Hardware Intelligence
- [ ] Comprehensive Camera2 characteristics parser.
- [ ] Logical-to-physical camera mapping.
- [ ] Detailed stream configuration analysis (all formats and sizes).
- [ ] Dynamic Range Profiles discovery (HLG10, HDR10, etc.).
- [ ] Sensor optical stabilization (OIS) and electronic stabilization (EIS) inspection.
- [ ] Capability persistence & caching via DataStore / Room.
- [ ] Full device capability report export (JSON/Text).
- [ ] Phase 2 acceptance tests and report.

---

## Phase 3: Smart Auto Photography
- [ ] Frame luminance and scene contrast analyzer.
- [ ] Rule-based scene categorization (Portrait, Landscape, Night, Macro, Backlit, Movement).
- [ ] Smart configuration generator (ISO bias, shutter recommendation, HDR prompt).
- [ ] Lens recommendation policy.
- [ ] Explainable scene suggestion UI.
- [ ] Phase 3 acceptance tests and report.

---

## Phase 4: Pro Photography
- [ ] Camera2 manual sensor session builder.
- [ ] Manual ISO controller with hardware range clamping.
- [ ] Manual shutter speed controller with nanosecond-to-fraction display.
- [ ] Manual focus distance controller (diopters).
- [ ] Custom Auto White Balance (AWB) lock and manual Kelvin presets.
- [ ] Real-time RGB and luminance histogram.
- [ ] RAW capture (RAW_SENSOR) and DNG creation via `DngCreator`.
- [ ] Phase 4 acceptance tests and report.

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
