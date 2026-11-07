# Xamera Minimalist UI Feature Mapping Matrix

This document maps all camera engine and application features from Phases 0–16 to their respective UI touchpoints in the new Minimalist Design.

| Feature ID | Feature Name | Previous UI Touchpoint | New Minimalist UI Touchpoint | Status |
|---|---|---|---|---|
| **FEAT-CORE-01** | Photo / Video Capture Engine | `BottomCaptureControls` Shutter | `MinimalistShutterRow` (Dual-ring white button) | Verified |
| **FEAT-CORE-02** | Front / Back Lens Flip | `BottomCaptureControls` Switcher | `MinimalistShutterRow` (Circular switcher) | Verified |
| **FEAT-CORE-03** | Gallery Access & Media Thumbnail | `BottomCaptureControls` Thumbnail | `MinimalistShutterRow` (Rounded thumbnail) | Verified |
| **FEAT-CORE-04** | Camera Mode Selection | `BottomCaptureControls` Tabs | `MinimalistModeSelector` (Horizontal carousel) | Verified |
| **FEAT-OPT-01** | Hardware Multi-Lens Step Zoom | `RotaryZoomDial` | `MinimalistZoomBar` (`0.6  1  2x` pills) | Verified |
| **FEAT-OPT-02** | Pinch-to-Zoom Gesture | Viewfinder touch listener | Viewfinder touch listener (Preserved) | Verified |
| **FEAT-EXP-01** | Flash & Torch Control | `TopBarControls` Flash Pill | `MinimalistTopBar` Left Capsule Flash Icon | Verified |
| **FEAT-EXP-02** | EV Bias Compensation Readout | `TopBarControls` EV Text | `MinimalistTopBar` Left Capsule Monospace Text | Verified |
| **FEAT-EXP-03** | Exposure Slider Overlay | Side exposure slider | `MinimalistTopBar` EV trigger -> Overlay | Verified |
| **FEAT-CAP-01** | Aspect Ratio Selector | `TopBarControls` Aspect Pill | `MinimalistQuickSettingsSheet` Tile 1 | Verified |
| **FEAT-CAP-02** | Self Timer | `TopBarControls` Timer Pill | `MinimalistQuickSettingsSheet` Tile 4 | Verified |
| **FEAT-CAP-03** | Composition Grid Overlays | `TopBarControls` Grid Pill | `MinimalistQuickSettingsSheet` Tile 5 | Verified |
| **FEAT-HDR-01** | HDR+ Radiance Fusion | `TopBarControls` HDR+ Pill | `MinimalistQuickSettingsSheet` Tile 3 (Gold Tile) | Verified |
| **FEAT-LUT-01** | 3D LUT Creative Color Science | `TopBarControls` Look Icon | `MinimalistViewfinderControls` Palette Icon | Verified |
| **FEAT-LUT-02** | Organic Film Grain & Halation | `LookControlPanel` | `MinimalistViewfinderControls` -> Panel | Verified |
| **FEAT-PRO-01** | Manual ISO, Shutter, Focus | `ProControlBar` | `ProControlBar` (Preserved in Pro Mode) | Verified |
| **FEAT-PRO-02** | Live RGB Histogram | `HistogramView` | `HistogramView` (Preserved in Pro Mode) | Verified |
| **FEAT-PRO-03** | Focus Peaking & Zebra Striping | Overlays | Overlays (Preserved in Pro Mode) | Verified |
| **FEAT-PORT-01**| Portrait Aperture & Studio Bokeh | `PortraitControlBar` | `PortraitControlBar` (Preserved in Portrait Mode)| Verified |
| **FEAT-AUTO-01**| Smart Scene Detection & Night Mode | `SceneSuggestionChip` | `MinimalistSceneBadge` (`(🌙) 1s` lower-left) | Verified |
| **FEAT-AUTO-02**| Capture Assist & Auto Optimization | None | `MinimalistTopBar` Center Motion Icon | Verified |
| **FEAT-VIEW-07**| Left-Handed Mode | `BottomCaptureControls` Mirrored | `MinimalistShutterRow` Mirrored Layout | Verified |
| **FEAT-SET-01** | Comprehensive Settings | `TopBarControls` Gear | `MinimalistQuickSettingsSheet` Tile 6 | Verified |
| **FEAT-LGT-01** | Front Screen Soft Light | None | `MinimalistQuickSettingsSheet` Tile 2 | Verified |
