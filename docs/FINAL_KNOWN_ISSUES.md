# XAMERA — FINAL KNOWN ISSUES & LIMITATIONS
## Release v1.0.0 Status & Hardware Constraints

**Application:** Xamera  
**Version:** `1.0.0` (Build 17)  
**Date:** 2026-11-07  

---

## 1. Classification & Status Overview

In adherence to Phase 16 transparency standards, all outstanding hardware behaviors and known constraints are disclosed below:

| Severity | Issue Key | Title | Platform Impact | Workaround / Mitigation |
|:---:|:---:|---|---|---|
| **LOW** | `ISSUE-OPT-01` | Ultra-Wide Minimum Zoom is 0.6x | OnePlus Ace 5 / 13R | Working as intended: dynamically displays 0.6x to reflect physical HAL limits honestly. |
| **LOW** | `ISSUE-OPT-02` | Fixed Focus on Front & Ultra-Wide Sensors | OnePlus Ace 5 / 13R | Working as intended: Focus diopter controls disabled with `"Fixed Focus (Front)"` visual badge. |
| **LOW** | `ISSUE-OPT-03` | Fixed Physical Aperture | OnePlus Ace 5 / 13R | Working as intended: Camera telemetry displays `"Fixed Aperture (f/1.88)"` without mechanical adjustments. |
| **LOW** | `ISSUE-VID-01` | Concurrent Multi-Camera Streaming Unsupported | Devices lacking multi-ISP concurrency | Fallback cleanly routes to active primary camera stream. |
| **LOW** | `ISSUE-INT-01` | Embedded FFmpeg CLI Binary Not Bundled in APK | All devices | Disclosed: Video export executes via native hardware-accelerated `MediaCodec` pipeline. |

---

## 2. No Release-Blocking Critical Defects

- **0 Critical Crashes (ANRs or unhandled exceptions)**
- **0 Data Loss Bugs (all capture buffers and MediaStore URIs verified)**
- **0 Security Vulnerabilities (offline operation verified, secrets isolated)**
