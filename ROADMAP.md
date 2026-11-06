# Master Roadmap | Xamera

**Project:** Xamera  
**Package:** `com.bishal.adaptivecamera`  
**Workspace:** `D:\Webapp\xamera`  
**Repository:** https://github.com/arybishal/Xamera  
**Developer:** Bishal Aryal  
**Version:** 0.13.0  
**Date:** 2026-11-06  

> [!NOTE]
> The complete detailed specification for all phases, tests, and hardware verification criteria is maintained at [docs/MASTER_IMPLEMENTATION_ROADMAP.md](file:///D:/Webapp/xamera/docs/MASTER_IMPLEMENTATION_ROADMAP.md).

---

## 1. Master Implementation Phases Overview

| Phase | Title | Scope | Status |
|:---:|---|---|:---:|
| **0** | **Foundation and Hardware Research** | Project setup, Gradle Kotlin DSL, Jetpack Compose, modular architecture, capability scanner prototype, device identification heuristics, unit tests, debug APK. | **COMPLETED** |
| **1** | **Camera Foundation** | CameraX preview, photo capture, video recording, front/rear switching, zoom, tap focus, exposure compensation, flash, photo timer, MediaStore saving. | **COMPLETED** |
| **2** | **Hardware Intelligence** | Full Camera2 metadata scanner (16 capability dimensions), physical/logical camera mappings, resolution discovery, dynamic range profiles, capability persistence. | **COMPLETED** |
| **3** | **Smart Auto Photography** | Rule-based scene categorization (Portrait, Landscape, Night, Macro, etc.), lighting analysis, motion estimation, recommendation policies, explainable UI suggestion chip. | **COMPLETED** |
| **4** | **Pro Photography & RAW** | Camera2 manual sensor session (`Camera2ManualEngine`), manual ISO/shutter/focus/WB, real-time 4-channel histogram, spatial focus peaking, Adobe DNG capture via `DngCreator`. | **COMPLETED** |
| **4A**| **Settings, Diagnostics & Exposure Reliability** | Relocated diagnostics to Settings ("Check Hardware Capability"), fixed exposure desync/HAL flooding, dynamic range checks, immersive viewfinder hiding status bar. | **COMPLETED** |
| **5** | **Viewfinder Ergonomics & Assistive Overlays** | Composition grids (thirds, golden ratio, golden spiral, crosshairs), virtual horizon level, aspect ratio selection & framing (4:3, 16:9, 1:1, 2.39:1, Full), rotary zoom dial, haptics, floating shutter, screen flash. | **COMPLETED** |
| **6** | **Pro Exposure & Focus Monitoring Engines** | Live zebra striping (IRE 70%–100%), full-spectrum false color heatmap, AE/AF lock, fine Kelvin white balance slider (2000K–10000K), focus assist magnifier loupe. | **COMPLETED** |
| **7** | **Advanced Capture Pipelines** | Zero Shutter Lag (ZSL) circular buffer, Auto Exposure Bracketing (AEB), dual-format saving (RAW+JPEG/HEIC), watermarking engine, EXIF metadata management & privacy strip, RAW vault. | **COMPLETED** |
| **8** | **LUT Library & Color Science Engine** | Built-in original 3D LUT presets (Natural, Warm, Cool, Contrast, Vintage), `.cube` parser (17, 33, 64), Room database, SAF import, Log tone curve profiles (Flat, S-Log/Log-C). | **COMPLETED** |
| **9** | **GPU Color Pipeline & Creative Shaders** | OpenGL ES 3.0 shader graph, live viewfinder grading, graded photo capture with original preservation, procedural film grain shader, photochemical halation glow shader. | **COMPLETED** |
| **10**| **Professional Video Engine** | Capability-verified resolution & FPS matrix (4K 24/30/60, 1080p 120), bitrate selection (up to 100 Mbps All-I), cinematic 180° shutter angle, stabilization coordinator, audio routing & VU meters. | **COMPLETED** |
| **11**| **Real-Time Video GPU Pipeline & Modes** | Direct GPU-to-Encoder video pipeline, live LUT switching in video, time-lapse intervalometer, high-speed slow motion, dual-camera hardware fallback, dark & signature light theme, 120Hz display refresh rate. | **COMPLETED** |
| **12**| **Video Processing & Export** | On-device trimming & PTS rebasing, H.264/H.265 transcoding, social media presets (Reels, Story, YouTube, Shorts, TikTok, WhatsApp), audio extraction/muting, file size estimation, export sheet UI. | **COMPLETED** |
| **13**| **Multi-Frame Computational Photography** | Multi-frame HDR fusion, Macro/Landscape Focus Stacking, sub-pixel Super-Resolution, Action Stroboscopic merge, variable aperture fallback controller. | *QUEUED* |
| **14**| **Peripherals, Media Processing & Gallery** | Bluetooth HID remote shutter, DJI / Zhiyun gimbal protocol integration, embedded on-device FFmpeg export engine, in-app gallery with DNG inspection and sharing. | *QUEUED* |
| **15**| **UI Polish, Benchmarking & Hardening** | Dark-first UI consistency, 120Hz smooth animations, latency tuning (< 500ms startup), GPU memory recycling, battery profiling, thermal mitigation policies. | *QUEUED* |
| **16**| **Final Release & Publishing** | Full test suite regression (unit, instrumented, device), ProGuard/R8 optimization, Release APK signing, license audits, GitHub repository push (`Xamera`), and GitHub Release packaging. | *QUEUED* |
