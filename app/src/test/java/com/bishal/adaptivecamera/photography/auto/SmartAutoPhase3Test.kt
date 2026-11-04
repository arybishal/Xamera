package com.bishal.adaptivecamera.photography.auto

import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.camera.capability.CameraHardwareLevel
import com.bishal.adaptivecamera.camera.capability.LensInfo
import com.bishal.adaptivecamera.camera.capability.RangeVal
import com.bishal.adaptivecamera.intelligence.analysis.FrameMetrics
import com.bishal.adaptivecamera.intelligence.analysis.FrameMetricsAnalyzer
import com.bishal.adaptivecamera.intelligence.policies.LensRecommendationPolicy
import com.bishal.adaptivecamera.intelligence.policies.SmartConfigurationGenerator
import com.bishal.adaptivecamera.intelligence.scene.SceneCategory
import com.bishal.adaptivecamera.intelligence.scene.SceneClassifier
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartAutoPhase3Test {

    @Test
    fun testFrameMetricsAnalyzerOnLowLightFrame() {
        val analyzer = FrameMetricsAnalyzer()
        val width = 64
        val height = 64
        // Create dark frame with pixel values around 15/255 (~0.06 luma)
        val darkBytes = ByteArray(width * height) { 15.toByte() }

        val metrics = analyzer.analyzeRawY(darkBytes, width, height)

        assertTrue(metrics.meanLuminance < 0.15f)
        assertTrue(metrics.shadowRatio > 0.90f)
        assertTrue(metrics.isLowLight)
        assertFalse(metrics.isBrightOrHighKey)
    }

    @Test
    fun testFrameMetricsAnalyzerOnBacklitHighContrastFrame() {
        val analyzer = FrameMetricsAnalyzer()
        val width = 64
        val height = 64
        val contrastBytes = ByteArray(width * height) { idx ->
            // Top half pitch black (5), bottom half pure bright (250)
            if (idx < (width * height) / 2) 5.toByte() else 250.toByte()
        }

        val metrics = analyzer.analyzeRawY(contrastBytes, width, height)

        assertTrue(metrics.shadowRatio > 0.40f)
        assertTrue(metrics.highlightRatio > 0.40f)
        assertTrue(metrics.rmsContrast > 0.40f)
        assertTrue(metrics.isHighContrastOrBacklit)
        assertTrue(metrics.estimatedDynamicRangeStops > 9.0f)
    }

    @Test
    fun testFrameMetricsAnalyzerTemporalMotionDelta() {
        val analyzer = FrameMetricsAnalyzer()
        val width = 32
        val height = 32

        val frame1 = ByteArray(width * height) { 50.toByte() }
        val frame2 = ByteArray(width * height) { 180.toByte() } // Large change

        val metrics1 = analyzer.analyzeRawY(frame1, width, height)
        assertEquals(0.0f, metrics1.motionDelta, 0.001f) // Initial frame has 0 motion

        val metrics2 = analyzer.analyzeRawY(frame2, width, height)
        assertTrue(metrics2.motionDelta > 0.40f)
        assertTrue(metrics2.isHighMotion)
    }

    @Test
    fun testSceneClassifierNightDetection() {
        val darkMetrics = FrameMetrics(
            meanLuminance = 0.08f,
            rmsContrast = 0.12f,
            shadowRatio = 0.85f,
            highlightRatio = 0.02f,
            midtoneRatio = 0.13f,
            estimatedDynamicRangeStops = 4.5f,
            motionDelta = 0.02f
        )

        val result = SceneClassifier.classify(
            metrics = darkMetrics,
            currentZoom = 1.0f
        )

        assertEquals(SceneCategory.NIGHT, result.primaryCategory)
        assertTrue(result.confidence > 0.70f)
        assertTrue(result.explanation.contains("Low ambient luminance"))
    }

    @Test
    fun testSceneClassifierBacklitDetection() {
        val backlitMetrics = FrameMetrics(
            meanLuminance = 0.45f,
            rmsContrast = 0.35f,
            shadowRatio = 0.35f,
            highlightRatio = 0.30f,
            midtoneRatio = 0.35f,
            estimatedDynamicRangeStops = 10.5f,
            motionDelta = 0.03f
        )

        val result = SceneClassifier.classify(
            metrics = backlitMetrics,
            currentZoom = 1.0f
        )

        assertEquals(SceneCategory.BACKLIT, result.primaryCategory)
        assertTrue(result.explanation.contains("High contrast backlight detected"))
    }

    @Test
    fun testSceneClassifierMacroDetection() {
        val normalMetrics = FrameMetrics(
            meanLuminance = 0.50f,
            rmsContrast = 0.18f,
            shadowRatio = 0.05f,
            highlightRatio = 0.05f,
            midtoneRatio = 0.90f,
            estimatedDynamicRangeStops = 6.0f,
            motionDelta = 0.01f
        )

        // Focus distance = 10 diopters (~10cm close focus)
        val result = SceneClassifier.classify(
            metrics = normalMetrics,
            currentZoom = 1.0f,
            focusDistanceDiopters = 10.0f
        )

        assertEquals(SceneCategory.MACRO, result.primaryCategory)
        assertTrue(result.explanation.contains("approx 10cm"))
    }

    @Test
    fun testSceneClassifierActionMovementDetection() {
        val actionMetrics = FrameMetrics(
            meanLuminance = 0.50f,
            rmsContrast = 0.20f,
            shadowRatio = 0.10f,
            highlightRatio = 0.10f,
            midtoneRatio = 0.80f,
            estimatedDynamicRangeStops = 7.0f,
            motionDelta = 0.28f // High motion
        )

        val result = SceneClassifier.classify(
            metrics = actionMetrics,
            currentZoom = 1.0f
        )

        assertEquals(SceneCategory.ACTION_MOVEMENT, result.primaryCategory)
        assertTrue(result.explanation.contains("Rapid motion detected"))
    }

    @Test
    fun testSceneClassifierFrontFacingPortraitDetection() {
        val normalMetrics = FrameMetrics(
            meanLuminance = 0.50f,
            rmsContrast = 0.20f,
            shadowRatio = 0.10f,
            highlightRatio = 0.10f,
            midtoneRatio = 0.80f,
            estimatedDynamicRangeStops = 6.5f,
            motionDelta = 0.02f
        )

        val result = SceneClassifier.classify(
            metrics = normalMetrics,
            isFacingFront = true
        )

        assertEquals(SceneCategory.PORTRAIT, result.primaryCategory)
        assertTrue(result.explanation.contains("Front camera selfie"))
    }

    @Test
    fun testSceneClassifierLandscapeDetection() {
        val landscapeMetrics = FrameMetrics(
            meanLuminance = 0.55f,
            rmsContrast = 0.22f,
            shadowRatio = 0.12f,
            highlightRatio = 0.15f,
            midtoneRatio = 0.73f,
            estimatedDynamicRangeStops = 8.5f,
            motionDelta = 0.02f
        )

        val result = SceneClassifier.classify(
            metrics = landscapeMetrics,
            currentZoom = 0.8f // Wide framing
        )

        assertEquals(SceneCategory.LANDSCAPE, result.primaryCategory)
        assertTrue(result.explanation.contains("Expansive composition"))
    }

    @Test
    fun testSmartConfigurationGeneratorSuggestions() {
        val dummyProfile = CameraCapabilityProfile(
            cameraId = "0",
            rawFacing = 1,
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = true,
            isoRange = RangeVal(100, 6400)
        )

        // 1. NIGHT SUGGESTION
        val nightResult = SceneClassifier.classify(
            FrameMetrics(0.08f, 0.12f, 0.85f, 0.01f, 0.14f, 4.0f, 0.01f)
        )
        val nightSuggestion = SmartConfigurationGenerator.generateSuggestion(
            sceneResult = nightResult,
            metrics = FrameMetrics(0.08f, 0.12f, 0.85f, 0.01f, 0.14f, 4.0f, 0.01f),
            profile = dummyProfile
        )
        assertTrue(nightSuggestion.isNightModeRecommended)
        assertEquals(-1, nightSuggestion.recommendedEvIndex)
        assertEquals(1600, nightSuggestion.recommendedIso)

        // 2. BACKLIT HDR SUGGESTION
        val backlitResult = SceneClassifier.classify(
            FrameMetrics(0.45f, 0.35f, 0.35f, 0.30f, 0.35f, 10.5f, 0.02f)
        )
        val backlitSuggestion = SmartConfigurationGenerator.generateSuggestion(
            sceneResult = backlitResult,
            metrics = FrameMetrics(0.45f, 0.35f, 0.35f, 0.30f, 0.35f, 10.5f, 0.02f),
            profile = dummyProfile
        )
        assertTrue(backlitSuggestion.isHdrRecommended)
        assertEquals(1, backlitSuggestion.recommendedEvIndex)
        assertEquals(100, backlitSuggestion.recommendedIso)

        // 3. ACTION MOVEMENT SUGGESTION
        val actionResult = SceneClassifier.classify(
            FrameMetrics(0.50f, 0.20f, 0.10f, 0.10f, 0.80f, 7.0f, 0.25f)
        )
        val actionSuggestion = SmartConfigurationGenerator.generateSuggestion(
            sceneResult = actionResult,
            metrics = FrameMetrics(0.50f, 0.20f, 0.10f, 0.10f, 0.80f, 7.0f, 0.25f),
            profile = dummyProfile
        )
        assertEquals(1_000_000L, actionSuggestion.recommendedShutterSpeedNs) // 1/1000s
        assertEquals("Action Shutter Priority", actionSuggestion.actionLabel)
    }

    @Test
    fun testLensRecommendationPolicy() {
        val cam0Main = CameraCapabilityProfile(
            cameraId = "0",
            rawFacing = 1,
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = true,
            lensInfo = LensInfo(
                facing = 1,
                facingLabel = "BACK",
                focalLengths = listOf(5.59f),
                availableApertures = listOf(1.88f)
            )
        )

        val cam3UltraWide = CameraCapabilityProfile(
            cameraId = "3",
            rawFacing = 1,
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = false,
            lensInfo = LensInfo(
                facing = 1,
                facingLabel = "BACK",
                focalLengths = listOf(1.68f),
                availableApertures = listOf(2.2f)
            )
        )

        val profiles = listOf(cam0Main, cam3UltraWide)

        // Landscape with zoom 0.6x should recommend Ultra-wide lens
        val recLandscape = LensRecommendationPolicy.recommendLens(
            activeCameraId = "0",
            profiles = profiles,
            sceneCategory = SceneCategory.LANDSCAPE,
            currentZoomRatio = 0.6f
        )
        assertTrue(recLandscape.requiresLensSwitch)
        assertEquals("3", recLandscape.recommendedCameraId)
        assertTrue(recLandscape.reason.contains("1.68mm"))

        // Night mode with 1.0x on ultra-wide should recommend Main sensor (f/1.88)
        val recNight = LensRecommendationPolicy.recommendLens(
            activeCameraId = "3",
            profiles = profiles,
            sceneCategory = SceneCategory.NIGHT,
            currentZoomRatio = 1.0f
        )
        assertTrue(recNight.requiresLensSwitch)
        assertEquals("0", recNight.recommendedCameraId)
        assertTrue(recNight.recommendedLensDescription.contains("f/1.88"))
        assertTrue(recNight.reason.contains("high-aperture"))
    }

    @Test
    fun testSmartAutoOrchestratorPipeline() {
        val cam0 = CameraCapabilityProfile(
            cameraId = "0",
            rawFacing = 1,
            hardwareLevel = CameraHardwareLevel.LEVEL_3,
            isLogicalCamera = true,
            isoRange = RangeVal(100, 6400)
        )

        val orchestrator = SmartAutoOrchestrator(
            activeProfile = cam0,
            allProfiles = listOf(cam0)
        )

        val darkMetrics = FrameMetrics(
            meanLuminance = 0.05f,
            rmsContrast = 0.10f,
            shadowRatio = 0.90f,
            highlightRatio = 0.00f,
            midtoneRatio = 0.10f,
            estimatedDynamicRangeStops = 4.0f,
            motionDelta = 0.01f
        )

        orchestrator.processFrameMetrics(darkMetrics)

        val state = orchestrator.autoState.value
        assertNotNull(state.sceneResult)
        assertEquals(SceneCategory.NIGHT, state.sceneResult?.primaryCategory)
        assertNotNull(state.configSuggestion)
        assertTrue(state.configSuggestion?.isNightModeRecommended == true)
    }
}
