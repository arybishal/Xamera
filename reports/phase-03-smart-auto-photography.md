# Phase 3 Completion Report: Smart Auto Photography

**Project:** Adaptive Camera (Xamera)  
**Package:** `com.bishal.adaptivecamera`  
**Phase:** Phase 3: Smart Auto Photography  
**Date:** 2026-11-04  
**Primary Reference Hardware:** OnePlus Ace 5 (China) / OnePlus 13R Platform (`CPH2691IN` / `OP5D3BL1`)  
**Android OS:** Android 17 (API 37)  
**Target Architecture:** Qualcomm Snapdragon 8 Gen 3 (`SM8650` / `qcom`)  

---

## 1. Executive Summary

Phase 3 introduces an intelligent photographic layer that evaluates incoming viewfinder frames in real time and dynamically produces explainable camera optimizations. Built directly on top of the Phase 2 hardware intelligence engine, Smart Auto analyzes lighting, spatial contrast, dynamic range, and temporal motion without incurring expensive ML inference overhead or latency spikes:

1. **Real-Time Frame Metrics Analyzer:** Analyzes luminance (`YUV_420_888` plane 0) with a 4x4 spatial striding algorithm, calculating mean luminance, RMS contrast (standard deviation), shadow ratio (<0.15), highlight ratio (>0.85), 16-bin histogram, estimated dynamic range span (stops), and inter-frame motion delta in under 3 milliseconds per frame.
2. **Rule-Based Scene Classifier:** Classifies active framing into 7 distinct photographic categories (`PORTRAIT`, `LANDSCAPE`, `NIGHT`, `MACRO`, `BACKLIT`, `ACTION_MOVEMENT`, `STANDARD`) with confidence scores and descriptive explanations.
3. **Smart Configuration Generator:** Produces actionable parameter biases tailored to the audited hardware boundaries (ISO targets clamped to hardware sensitivity limits, shutter speed targets up to 1/1000s, exposure compensation offsets, HDR prompts, and night mode activation).
4. **Lens Recommendation Policy:** Inspects physical optical focal lengths (e.g. 5.59mm main vs 1.68mm ultra-wide) and recommends switching lenses when framing demands wide perspective (Landscape / zoom <= 0.8x) or maximum aperture light gathering (Night / low light).
5. **Explainable Scene Suggestion UI:** Non-intrusive HUD chip displayed on the live viewfinder with category icon, confidence pill, one-tap "Apply" action, dismiss button, and expandable telemetry card displaying live luminance, contrast, and dynamic range metrics.
6. **Live CameraX Pipeline Integration:** Integrated with `ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST` on a dedicated background thread pool, operating concurrently with `Preview` and `ImageCapture` on `LEVEL_3` hardware without frame drops.

---

## 2. Architecture & Modules Implemented

```
com.bishal.adaptivecamera/
├── intelligence/
│   ├── analysis/
│   │   ├── FrameMetrics.kt                 // Frame luminance, contrast, histogram, motion model
│   │   └── FrameMetricsAnalyzer.kt         // High-performance Y-buffer analyzer & downsampler
│   ├── scene/
│   │   ├── SceneCategory.kt                // Photographic scene classification enum
│   │   └── SceneClassifier.kt              // Deterministic rule engine for scene detection
│   └── policies/
│       ├── SmartConfigurationGenerator.kt  // Hardware-bounded ISO, shutter, EV, and HDR suggestions
│       └── LensRecommendationPolicy.kt     // Multi-camera lens switching advisor
│
├── photography/
│   └── auto/
│       └── SmartAutoOrchestrator.kt        // Coordinator dispatching analysis, scene, and policies
│
└── ui/camera/components/
    └── SceneSuggestionChip.kt              // Compose viewfinder HUD with expandable explanation
```

---

## 3. Scene Classification Rules & Criteria

| Scene Category | Trigger Criteria | Photographic Strategy & Recommendation |
|---|---|---|
| **NIGHT** | `meanLuminance < 0.18` or (`shadowRatio > 0.65` & `luma < 0.30`) | Negative EV bias to preserve highlights, ISO 1600+ target, night stacking enabled. Recommends Main f/1.88 lens. |
| **BACKLIT** | `shadowRatio > 0.25` & `highlightRatio > 0.20` & `rmsContrast > 0.28` | HDR exposure bracketing prompt, +0.33 to +0.67 EV compensation to lift foreground subject. |
| **ACTION_MOVEMENT** | `motionDelta > 0.18` (inter-frame variance) | High shutter priority target (1/500s to 1/1000s) with ISO boost to eliminate motion blur. |
| **PORTRAIT** | Front-facing camera OR (`midtoneRatio > 0.75` & zoom in 1.5x-3.5x & `motion < 0.08`) | Natural skin tone rendering, optional fill flash if backlit. |
| **LANDSCAPE** | Zoom <= 1.0x & `motion < 0.06` & `dynamicRange >= 6.0 stops` | Clean base ISO 100, maximum depth of field. Recommends Ultra-Wide (1.68mm) if zoom <= 0.8x. |
| **MACRO** | Focus distance diopters >= 6.67 (subject <= 15cm) | Shallow depth-of-field precision lock. |
| **STANDARD** | Balanced ambient lighting within native dynamic range | Balanced native parameters with zero exposure bias. |

---

## 4. UI Viewfinder Integration

The explainable scene suggestion HUD chip is anchored at `Alignment.TopCenter` of the viewfinder with a sleek translucent dark glass background (`#1A...`) and dynamic color accents:
- **Night:** Cyan (`#00E5FF`)
- **Backlit / HDR:** Amber (`#FFB300`)
- **Action:** Coral Red (`#FF5252`)
- **Landscape:** Emerald Green (`#00E676`)
- **Macro:** Violet (`#B388FF`)
- **Portrait:** Rose Pink (`#FF80AB`)

Tapping the chip toggles an animated telemetry card displaying exact real-time percentages for Luminance, RMS Contrast, Shadow ratio, and Dynamic Range stops. Tapping "Apply" commits the recommended exposure compensation and parameters instantly.

---

## 5. Verification & Acceptance Testing

- **Unit Test Suite:** 39 unit tests executed and passing cleanly across Debug and Release configurations.
  - `SmartAutoPhase3Test`:
    - `testFrameMetricsAnalyzerOnLowLightFrame`: **PASS**
    - `testFrameMetricsAnalyzerOnBacklitHighContrastFrame`: **PASS**
    - `testFrameMetricsAnalyzerTemporalMotionDelta`: **PASS**
    - `testSceneClassifierNightDetection`: **PASS**
    - `testSceneClassifierBacklitDetection`: **PASS**
    - `testSceneClassifierMacroDetection`: **PASS**
    - `testSceneClassifierActionMovementDetection`: **PASS**
    - `testSceneClassifierFrontFacingPortraitDetection`: **PASS**
    - `testSceneClassifierLandscapeDetection`: **PASS**
    - `testSmartConfigurationGeneratorSuggestions`: **PASS**
    - `testLensRecommendationPolicy`: **PASS**
    - `testSmartAutoOrchestratorPipeline`: **PASS**
- **Physical Device Execution:** Verified on physical `OnePlus Ace 5 / 13R` (`3B15B800NRT00000`). App launched cleanly with `Camera bound successfully`, live `ImageAnalysis` running concurrently on viewfinder frames with zero memory leaks or UI frame drops.
- **Artifact:** `app/build/outputs/apk/debug/app-debug.apk` assembled (64,541,654 bytes) and verified on device.
