# Device Compatibility Matrix | Adaptive Camera

## 1. Primary Device: OnePlus Ace 5 (China)

* **Priority:** 1 (Primary Development & Reference Device)
* **Market Region:** Mainland China
* **Typical Model Identifiers:** `PKG110`, `PJR110`
* **SoC:** Qualcomm Snapdragon 8 Gen 3
* **Primary Sensor Hardware:** Sony IMX906 (50MP, 1/1.56", OIS)
* **Secondary Sensors:** Ultra-wide (8MP) + Macro (2MP)
* **Front Camera:** 16MP

### Critical Hardware & Firmware Differentiation
The OnePlus Ace 5 is closely related to the globally marketed OnePlus 13R (`CPH2645` / `CPH2647`), but their physical camera configurations and vendor HAL tunings are **not identical**:
1. **Third-Party Camera2 Access:** In converted or flashed firmware environments, certain auxiliary sensors (ultra-wide/telephoto) or high-frame-rate modes may be masked or restricted to system packages.
2. **Dynamic Discovery Required:** Adaptive Camera never assumes factory stock values. Sensor IDs, focal lengths, active array sizes, and manual ranges are retrieved dynamically from the running firmware via `CameraManager`.
3. **Firmware Conversions:** If the Ace 5 has custom or cross-flashed firmware, `CameraCharacteristics` reported by the HAL may differ from retail factory behavior. The app logs firmware build fingerprints in its diagnostic reports to trace discrepancies.

---

## 2. Secondary Devices Matrix

| Priority | Device Model | Region | Chipset | Camera Architecture Notes | Target Status |
|:---:|---|---|---|---|:---:|
| **1** | **OnePlus Ace 5** | China | Snapdragon 8 Gen 3 | IMX906 primary + OIS, multi-camera logical HAL | **Reference Device** |
| **2** | **OnePlus 13R** | Global | Snapdragon 8 Gen 3 | Global variant, differing auxiliary lenses | Supported |
| **3** | **OnePlus 12R** | Global / India | Snapdragon 8 Gen 2 | IMX890 primary + OIS | Supported |
| **4** | **Google Pixel 7 / 8 / 9** | Global | Tensor | Native Camera2 LEVEL_3, full RAW/HDR | Supported |
| **5** | **Generic Android 10+** | Global | Various | CameraX fallback with capability inspection | Supported |

---

## 3. Recommended ADB Diagnostics Commands

To collect comprehensive hardware logs from the physical test device when attached:

```bash
# Check connected device identity
adb devices -l
adb shell getprop ro.product.model
adb shell getprop ro.product.device
adb shell getprop ro.product.manufacturer
adb shell getprop ro.build.version.release
adb shell getprop ro.build.version.sdk
adb shell getprop ro.build.fingerprint

# Dump low-level camera HAL capabilities
adb shell dumpsys media.camera
```
