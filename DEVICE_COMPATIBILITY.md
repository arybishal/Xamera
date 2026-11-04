# Device Compatibility Matrix | Adaptive Camera

## 1. Primary Device: OnePlus Ace 5 (China) / Converted Firmware Reference

* **Market Hardware:** OnePlus Ace 5 (China)
* **Reported Firmware Model:** `CPH2691` (OnePlus 13R India regional build)
* **Reported Device / Board:** `OP5D3BL1`
* **Reported Product:** `CPH2691IN`
* **SoC:** Qualcomm Snapdragon 8 Gen 3 (`SM8650` / `qcom`)
* **Android OS:** Android 17 (API level 37)
* **Build Fingerprint:** `OnePlus/CPH2691IN/OP5D3BL1:17/UKQ1.231108.001/V.R4T3.2d570f8-6d2927-6d2926:user/release-keys`
* **Display ID:** `CPH2691_17.0.0.105(EX01)`

### Critical Hardware & Firmware Differentiation Audit
1. **Firmware Identity vs. Physical Identity:**
   - The device reports model `CPH2691` and product `CPH2691IN`. This represents Indian regional firmware for the OnePlus 13R.
   - However, the developer's physical test device is a Chinese OnePlus Ace 5 with a history of converted/flashed firmware.
   - Software-level properties (`ro.product.*`, `ro.build.*`) reflect the flashed operating system and cannot confirm internal PCB component revisions without physical disassembly.
   - **Adaptive Camera Rule:** The application explicitly labels firmware identity as `OnePlus 13R (India / Regional CPH2691IN)` and classifies physical hardware identity as `Unconfirmed (Ace 5 China / 13R Global-IN Platform)` with confidence `UNCONFIRMED`.

2. **Camera Sensor HAL Enumeration & Facing Discovery:**
   - In the Android Camera2 API specification (`android.hardware.camera2.CameraMetadata`):
     - `LENS_FACING_FRONT = 0`
     - `LENS_FACING_BACK = 1`
     - `LENS_FACING_EXTERNAL = 2`
   - Verified HAL layout from device dumpsys:
     - **Camera ID 0:** `LENS_FACING_BACK` (1) - Logical multi-camera composing physical sensors `2` and `3`. Level: `LEVEL_3`.
     - **Camera ID 1:** `LENS_FACING_FRONT` (0) - Front-facing selfie camera. Level: `FULL`.
     - **Camera ID 2:** `LENS_FACING_BACK` (1) - Physical rear auxiliary sensor (Ultra-wide).
     - **Camera ID 3:** `LENS_FACING_BACK` (1) - Physical rear auxiliary sensor (Macro).
     - **Camera ID 4 / 5:** Internal auxiliary vendor HAL streams.

3. **Phase 4 Pro Photography & RAW Sensor Support:**
   - **Hardware Level:** Rear cameras report `LEVEL_3` and front camera reports `FULL`.
   - **RAW Capabilities:** Camera ID 0 (Logical Back) and Camera ID 2 (Physical Main) expose `REQUEST_AVAILABLE_CAPABILITIES_RAW` and output format `ImageFormat.RAW_SENSOR` at `4096x3072` (12.6 MP binned).
   - **Manual ISO Sensitivity:** Supported range: `100` to `6400`.
   - **Manual Shutter Speed / Exposure Time:** Supported range: `78,120 ns` (~1/12800s) to `74,355,370,092 ns` (~74.35s).
   - **Manual Lens Focus Distance:** Minimum focus distance `10.0` diopters (~10cm macro boundary) on Camera ID 0; Front camera (Camera ID 1) is fixed focus (`0.0` diopters).
   - **Auto White Balance Modes:** Supported presets include `AUTO`, `DAYLIGHT`, `CLOUDY_DAYLIGHT`, `SHADE`, `FLUORESCENT`, `INCANDESCENT`, and `TWILIGHT`.
   - **Real-Time Viewfinder Analysis:** Preview analysis offloaded via YUV_420_888 downsampled stream providing 4x 64-bin live RGB/Luma histograms and 2D spatial gradient focus peaking without preview latency.

---

## 2. Secondary Devices Matrix

| Priority | Device Model | Region | Chipset | Hardware Identity Confidence | Status |
|:---:|---|---|---|:---:|:---:|
| **1** | **OnePlus Ace 5 (China)** | China | Snapdragon 8 Gen 3 | `PROBABLE` if native `PKG110`; `UNCONFIRMED` if flashed `CPH2691` | Reference Device |
| **2** | **OnePlus 13R** | Global / India | Snapdragon 8 Gen 3 | `PROBABLE` if native `CPH2645` / `CPH2691` retail | Supported |
| **3** | **OnePlus 12R** | Global / India | Snapdragon 8 Gen 2 | `PROBABLE` on native retail build | Supported |
| **4** | **Google Pixel 7 / 8 / 9** | Global | Tensor G2/G3/G4 | `CONFIRMED` via canonical Google build strings | Supported |
| **5** | **Generic Android 10+** | Global | Various | `UNCONFIRMED` fallback | Supported |

---

## 3. Verified ADB Telemetry Commands

```bash
# Check connected device identity
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.product.device
adb shell getprop ro.product.name
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint

# Inspect camera service device mapping and facing
adb shell "dumpsys media.camera | grep -E 'Camera HAL device|Facing|Device [0-9] maps'"
```

---

## 4. Phase 4 Addendum: Exposure Range & Hardware Capability Discovery
* **Active Exposure Compensation Range:** Queried directly via `CameraInfo.getExposureState()`. On Camera ID 0: range `[-6, 6]` with step `0.16666667` EV (1/6 EV step) or `[-4, 4]` at 0.5 EV depending on active sensor stream.
* **Exposure Support Guard:** Front/rear camera switches dynamically update `isExposureCompensationSupported` and `isExposureStateReady`, resetting any active EV bias to 0 to prevent stale requests.
* **Hardware Capability in Settings:** Full 16-section capability inspection accessible via Settings top bar icon, allowing copy as JSON/TXT and direct sharing.

