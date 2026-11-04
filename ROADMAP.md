# Roadmap | Adaptive Camera

The development follows a strict, sequential, test-driven 13-phase roadmap:

| Phase | Title | Scope | Status |
|:---:|---|---|:---:|
| **0** | **Foundation and Hardware Research** | Project setup, Gradle Kotlin DSL, Jetpack Compose, modular architecture, CameraCapabilityScanner prototype, device identification, unit tests, debug APK. | **COMPLETED** |
| **1** | **Camera Foundation** | CameraX preview, photo capture, video recording, front/rear switching, lens selection, zoom, tap focus, exposure compensation, flash, MediaStore saving. | *QUEUED* |
| **2** | **Hardware Intelligence** | Full Camera2 metadata scanner, physical/logical camera mappings, resolution discovery, FPS ranges, manual sensor controls, dynamic range, capability caching. | *QUEUED* |
| **3** | **Smart Auto Photography** | Rule-based scene evaluation, lighting analysis, motion estimation, recommendation policies, configuration validation. | *QUEUED* |
| **4** | **Pro Photography** | Manual ISO, Shutter Speed, Manual Focus, White Balance, Exposure Compensation, Histogram, Focus Peaking, RAW/DNG capture via Camera2. | *QUEUED* |
| **5** | **LUT Library** | 5 built-in original LUT presets, `.cube` 3D parser, validation, import via Storage Access Framework, Room database, favorites, thumbnails. | *QUEUED* |
| **6** | **GPU Photo LUT** | EGL manager, OpenGL ES 3.0 3D textures, LUT shader, intensity control (0-100%), live preview grading, graded photo capture, original preservation. | *QUEUED* |
| **7** | **Real-Time Video LUT** | Dedicated EGL video pipeline, SurfaceTexture, GPU frame processing, MediaCodec input surface, MediaMuxer, synchronized audio, live LUT switching. | *QUEUED* |
| **8** | **Pro Video** | Resolution and FPS combinations, manual exposure, focus, white balance, video stabilization (EIS/OIS), audio source selection, thermal monitoring. | *QUEUED* |
| **9** | **Computational Photography** | Multi-frame night capture, frame alignment, noise reduction, HDR merging, highlight recovery, portrait background separation. | *QUEUED* |
| **10**| **Gallery & Media Management** | MediaStore photo/video grid, album browser, full-screen viewer, video playback, LUT metadata inspector, DNG handling, export/share. | *QUEUED* |
| **11**| **UI & Performance Optimization** | Edge-to-edge UI polish, dark-first styling, animations, GPU memory optimization, startup latency reduction, thermal protection. | *QUEUED* |
| **12**| **Final Release & Publishing** | Release APK generation, signature validation, final device regression, GitHub repository push (`Xamera`), and GitHub Release creation with SHA256 checksums. | *QUEUED* |
