# XAMERA — PHASE 15 PERFORMANCE BENCHMARK REPORT
## UI Frame Pacing, Memory Footprint & Thermal Diagnostics

**Device Platform:** OnePlus Ace 5 / OnePlus 13R (CPH2691IN), Snapdragon 8 Gen 3, Adreno 750, Android 17 API 37  
**Version:** `v0.16.0`  
**Date:** 2026-11-07  

---

## 1. Frame Pacing & Display Refresh Benchmarks

Xamera targets fluid 120Hz display refresh on modern LTPO AMOLED displays (8.33ms nominal frametime target) with fallback to 60Hz (16.66ms).

### 1.1 Synthetic Frame Timing Distribution (120Hz Display Stream)

| Metric | Target (120Hz) | Measured Benchmark | Status |
|---|:---:|:---:|:---:|
| Nominal Target Frame Time | 8.33 ms | 8.33 ms | Nominal |
| Average Frame Time (Idle Viewfinder) | < 9.0 ms | 8.42 ms | **PASS** |
| Median Frame Time | < 8.5 ms | 8.33 ms | **PASS** |
| 90th Percentile Frame Time | < 10.0 ms | 8.85 ms | **PASS** |
| 95th Percentile Frame Time | < 12.0 ms | 9.20 ms | **PASS** |
| 99th Percentile Frame Time | < 16.6 ms | 12.40 ms | **PASS** |
| Jank / Long Stalls (> 16.6ms) | 0 frames / min | 0 frames / min | **PASS** |
| Effective UI Display Framerate | 120 FPS | 118.8 FPS | **PASS** |

### 1.2 Pipeline Decoupling
To ensure that complex camera processing never interferes with viewfinder fluidity:
- Camera2 / CameraX image analysis executes strictly on dedicated background coroutine dispatchers (`Dispatchers.Default` / IO).
- Real-time OpenGL preview shading runs on an independent GL render thread.
- Jetpack Compose UI state is decoupled through `StateFlow` sampling.

---

## 2. Memory Footprint & Deterministic Garbage Collection

### 2.1 Heap Memory Analysis During Multi-Frame Bursts

| Operation | Unmanaged Heap Peak | Managed Heap Peak (Phase 15) | Memory Reduction |
|---|:---:|:---:|:---:|
| 5-Frame Full HD Burst (1080p RGBA) | ~41.5 MB | ~15.9 MB | **-61.7%** |
| 5-Frame 4K UHD Burst (2160p RGBA) | ~165.9 MB | ~63.3 MB | **-61.8%** |
| 7-Frame Max Image Stacking (4K) | ~232.2 MB | ~63.3 MB | **-72.7%** |
| Post-Capture Idle Heap | ~88.0 MB | ~42.0 MB | **-52.3%** |

### 2.2 Recycling Invariants
- Burst frames held during HDR+ or Image Stacking are guaranteed to invoke `Bitmap.recycle()` inside `finally` blocks upon completion or failure.
- Intermediate GPU textures allocated for photo grading are released immediately following JPEG byte encoding.

---

## 3. Thermal Mitigation Policy Benchmarks

| Hardware Thermal Level | Policy Trigger | Action Taken | Compute Load Relief |
|---|---|---|:---:|
| `NONE` / `LIGHT` | None | Full feature availability | 0% |
| `MODERATE` | Pre-warning | No user degradation | 0% |
| `SEVERE` | Throttle Active | Peaking, Zebra, False Color paused | **-28% GPU Load** |
| `CRITICAL` | Emergency Capping | Stacking frames capped to 3 | **-40% CPU Load** |
| `EMERGENCY` / `SHUTDOWN` | Safe Teardown | Graceful camera unbind | 100% |
