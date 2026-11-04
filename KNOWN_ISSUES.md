# Known Issues & Limitations | Adaptive Camera

## Phase 0 Baseline

1. **Physical Device ADB Connection:**
   - *Status:* No physical device was connected via ADB at the time of Phase 0 verification.
   - *Mitigation:* Physical device testing is explicitly marked as `NOT RUN`. Verification suite is ready to execute via ADB as soon as the OnePlus Ace 5 is connected.

2. **OEM Converted Firmware Variations:**
   - *Status:* Devices running cross-flashed or regional converted firmware (e.g., ColorOS to OxygenOS or custom ROMs) may report different logical camera mappings or disable access to auxiliary sensors for non-system apps.
   - *Mitigation:* `CameraCapabilityScanner` queries available camera IDs directly from the OS at runtime and handles empty/restricted camera lists gracefully without crashing.

3. **CameraX vs Camera2 Surface Arbitration:**
   - *Status:* CameraX and Camera2 cannot concurrently hold open sessions to the same camera sensor ID.
   - *Mitigation:* The architectural state machine arbitrates transitions between standard (CameraX) and manual/RAW (Camera2) modes with explicit teardown and initialization stages.
