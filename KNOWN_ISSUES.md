# Known Issues & Limitations | Adaptive Camera

## Phase 1 Baseline

1. **Physical Identity in Cross-Flashed / Converted ROM Environments:**
   - *Status:* When a device runs flashed firmware (e.g. Indian OnePlus 13R `CPH2691IN` on board `OP5D3BL1`), `Build.MODEL`, `Build.PRODUCT`, and `Build.FINGERPRINT` report the operating system's target SKU rather than physical PCB component provenance.
   - *Mitigation:* The application never claims confirmed physical identity solely based on software properties. It reports `firmwareIdentity` separately from `probableHardwareFamily`, sets `physicalIdentityConfidence` to `UNCONFIRMED`, and presents explainable identity evidence factors.

2. **Auxiliary Camera Access in Standard CameraX Engine:**
   - *Status:* On devices with logical multi-camera HALs (such as Camera 0 on the Ace 5 / 13R), CameraX binds to the logical camera ID, allowing the vendor HAL to dynamically switch between physical sub-lenses (IDs 2 and 3) during optical and digital zoom. Direct, uncomposited streaming from individual physical sub-cameras is not exposed by standard CameraX selectors.
   - *Mitigation:* Physical IDs are accurately discovered, audited, and displayed in Diagnostics. Advanced direct physical sensor binding is architected for Phase 2 and Phase 4 via Camera2 extensions.

3. **Android Runtime Permission Delegation in Shell:**
   - *Status:* On modern OEM skins (ColorOS / OxygenOS 15/17), shell commands `pm grant` and `appops set` cannot grant runtime permissions without root or UI user confirmation.
   - *Mitigation:* The application UI includes an in-app permission banner that properly delegates permission requests to the Android system activity contract (`ActivityResultContracts.RequestMultiplePermissions`).
