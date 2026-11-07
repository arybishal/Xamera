# Xamera Minimalist UI Component Architecture

## Overview
The Xamera Minimalist UI follows a unified dark-first visual system built with Jetpack Compose. The design prioritizes the viewfinder canvas as an uninterrupted visual centerpiece, framing all controls in high-contrast dark capsules with warm gold accents.

```
┌──────────────────────────────────────────────────────────────┐
│  [ (⚡) (⊙) EV 0.0 ]                           (⊙) (:::)    │  <- MinimalistTopBar
├──────────────────────────────────────────────────────────────┤
│                                                              │
│                                                              │
│                                                              │
│                                                              │
│                                                              │
│                                                              │
│                                                              │
│                                                              │
│  [(🌙) 1s]                                                   │  <- MinimalistSceneBadge
│                                                              │
│  (✦)               0.6    1    [2x]                     (✿)  │  <- MinimalistViewfinderControls
├──────────────────────────────────────────────────────────────┤
│                                                              │
│      [Gallery]              ( ◎ )             [Flip]         │  <- MinimalistShutterRow
│                                                              │
│        PRO       VIDEO    [ PHOTO ]   PORTRAIT    MORE       │  <- MinimalistModeSelector
└──────────────────────────────────────────────────────────────┘
```

## Component Hierarchy

### 1. `MinimalistTopBar`
- **Location:** Attached to `Scaffold.topBar`.
- **Insets:** Employs `Modifier.statusBarsPadding()`.
- **Sub-elements:**
  - `Left Capsule`: Flash state toggle, secondary capture-assist indicator, real-time exposure bias readout.
  - `Right Actions`: AI auto-suggestion toggle, Quick Settings bottom sheet trigger.

### 2. `MinimalistViewfinderControls`
- **Location:** Anchored to bottom of viewfinder preview (`Alignment.BottomCenter`, `padding(bottom = 12.dp)`).
- **Sub-elements:**
  - `Beauty/AI Action`: Triggers smart auto recommendation / portrait retouches.
  - `MinimalistZoomBar`: Step selector (`0.6`, `1`, `2x`, etc.) bound to hardware lens limits.
  - `Look/3D LUT Action`: Triggers floating `LookControlPanel` for color grading and film grain.

### 3. `MinimalistSceneBadge`
- **Location:** Viewfinder lower-left (`Alignment.BottomStart`, `padding(start = 24.dp, bottom = 64.dp)`).
- **Functionality:** Displays active computational exposure recommendations (e.g., night exposure duration `(🌙) 1s`, portrait, landscape, macro, or action).

### 4. `MinimalistBottomDeck`
- **Location:** Attached to `Scaffold.bottomBar`.
- **Insets:** Employs `Modifier.navigationBarsPadding()`.
- **Sub-elements:**
  - `Recording Duration Banner`: Active during video recording sessions.
  - `MinimalistShutterRow`: Left gallery thumbnail, center dual-ring white shutter button, right camera switcher. Supports left-handed mirroring.
  - `MinimalistModeSelector`: Horizontal carousel supporting `PRO`, `VIDEO`, `PHOTO`, `PORTRAIT`, and expandable `MORE` drawer.

### 5. `MinimalistQuickSettingsSheet`
- **Location:** Viewfinder modal overlay with semi-transparent scrim (`Box(Alignment.BottomCenter)`).
- **Grid Layout:** 2 rows of circular quick-action tiles:
  - Row 1: Aspect Ratio, Soft Light, Auto HDR, Timer.
  - Row 2: Composition Grid, Settings.

## State Flow & ViewModel Connections
All components bind directly to `uiState: StateFlow<CameraUiState>` and dispatch intents through `CameraViewModel`:
- Flash: `viewModel.toggleFlash()`
- Exposure Compensation: `viewModel.toggleExposureSlider()` / `viewModel.setExposureIndex(it)`
- Zoom: `viewModel.setZoomRatio(it)`
- Shutter: `viewModel.onShutterPressed()`
- Mode: `viewModel.setCameraMode(it)`
- Camera Switch: `viewModel.switchCamera()`
- Soft Light: `viewModel.toggleSoftLight()`
- HDR+: `viewModel.toggleHdrPlusMode()`
- Aspect Ratio: `viewModel.cycleAspectRatio()`
- Grid: `viewModel.cycleGridType()`
- Timer: `viewModel.toggleTimer()`
- 3D LUT Studio: `viewModel.toggleLookControlPanel()`
