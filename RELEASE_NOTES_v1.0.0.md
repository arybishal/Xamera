# Xamera v1.0.0 — Production Release & Minimalist UI Redesign

**Xamera** (Adaptive Camera) is an offline-first, professional camera application built for mobile filmmakers, pro photographers, and power users. Designed as a high-performance alternative camera app for modern Android devices and specifically optimized for the **OnePlus Ace 5 / OnePlus 13R** platform (Snapdragon 8 Gen 3, Adreno 750, Android 17 API 37).

This milestone represents the **v1.0.0 Production Release**, featuring the new **Minimalist Premium Camera UI** inspired by user-driven design references, alongside the complete photographic and computational engine developed across Phases 0–16.

---

### 🌟 What's New in v1.0.0

#### 1. Minimalist Premium Camera UI Redesign
- **Pure Black Canvas (`#000000`):** Edge-to-edge dark-first viewfinder interface designed to let the photography remain the visual centerpiece.
- **Top Rounded Pill Capsule:** Left-aligned dark capsule housing Flash controls (`Off`, `Auto`, `On`, `Torch`), capture-assist indicator, and monospace real-time EV bias readout (`EV 0.0`).
- **Quick Controls & AI Assist:** Right-aligned shortcuts for AI auto-suggestions and the 3x3 Quick Settings grid (`:::`).
- **Floating Scene Recommendation Badge:** Amber-gold pill (`(🌙) 1s`) anchored at lower-left, dynamically indicating night-stacking exposure duration or detected scene optimizations.
- **Clean Step Zoom Bar:** Text-based zoom pills (`0.6  1  2x`) bound to real physical sensor HAL limits, flanked by the Beauty/AI button and 3D LUT Studio icon.
- **Ergonomic Bottom Shutter Deck:** The prominent dual-ring white shutter button is placed **above** the mode selector for optimal thumb ergonomics, flanked by rounded gallery thumbnail and circular camera flip button.
- **Horizontal Mode Selector:** Seamless carousel for `PRO`, `VIDEO`, `PHOTO`, `PORTRAIT`, and expandable `MORE` drawer (`TIME-LAPSE`, `SLOW-MO`, `DUAL`).
- **Floating 2-Row Quick Settings Modal:** 6 circular quick-action tiles for Aspect Ratio (`4:3`, `16:9`, `1:1`, `Full`), Soft Light (selfie screen flash), **Auto HDR (solid gold button with bold black text `HDR AUTO`)**, Self Timer, Composition Grid (`Off`, `3x3`, `Golden`, `Spiral`, `Cross`), and Settings.

#### 2. Professional Photography & Camera2 Engine
- Full manual controls: ISO (100–6400), Shutter Speed (1/8000s–30s), Manual Focus with diopter distance slider, Kelvin White Balance (2000K–10000K), AE Lock, AF Lock, and AWB Lock.
- Live Broadcast Monitoring: Focus peaking with configurable sensitivity, live zebra striping (70%–100% threshold), false color exposure heatmaps, and focus magnifier loupe.
- Uncompressed RAW/DNG capture via Camera2 sensor streams with embedded DNG color matrices and full EXIF preservation.
- Computational radiance fusion (HDR+ 5-frame exposure bracketing) and computational multi-frame image stacking (Average, Median, Lighten, Darken).

#### 3. Real-Time GPU Color Science & LUT Studio
- Real-time 3D LUT grading engine with tetrahedral interpolation (`.cube` format support).
- Built-in cinematic Looks: Teal & Orange, Vintage Warm, Black & White Classic, Cyberpunk Night, and Cinematic Faded.
- GPU-accelerated 35mm organic film grain generator and photochemical halation shader.

#### 4. Hardware Optimization & Reliability
- 120Hz high-refresh-rate display frame pacing.
- Hardware thermal monitor (`PowerManager.OnThermalStatusChangedListener`) with adaptive overlay throttling during heavy shooting.
- Physical Multi-Lens Direct Selector: Direct sensor access bypassing HAL compositors (Main 50MP Sony IMX890, Ultra-Wide 8MP).
- Bluetooth remote and gimbal support (DJI Osmo Mobile, Zhiyun Smooth).
- 100% on-device processing: zero network permissions, zero cloud dependencies, zero analytics.

---

### 📦 Release Assets & Cryptographic Verification

| Artifact | Type | File Size | SHA-256 Checksum |
|---|:---:|:---:|---|
| **`app-release.apk`** | Production Signed APK | 49,099,039 bytes (~46.8 MB) | `420a36e670be66292ff31903fe1e5b6fcd03b9b81ce4c75702e490bc1edb22d6` |
| **`app-debug.apk`** | Developer Debug APK | 68,131,235 bytes (~65.0 MB) | `bca352a35ce62c3f72954827d1330c1814b8851bdac064c1fcb852771ee70306` |

#### Signature Telemetry
- **Signer:** `CN=Bishal Aryal, OU=Xamera, O=Adaptive Camera Project, L=Kathmandu, C=NP`
- **Signature Scheme:** APK Signature Scheme v3 (Verified via Android SDK `apksigner`)
- **Key Specification:** RSA 2048-bit with SHA-256 digest

---

### 🧪 Automated Test Verification
- **Total Tests Run:** 295
- **Pass Rate:** 100% (295 Passed, 0 Failed, 0 Skipped across 63 test suites)
