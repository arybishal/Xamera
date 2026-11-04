package com.bishal.adaptivecamera.photography.auto

import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.intelligence.analysis.FrameMetrics
import com.bishal.adaptivecamera.intelligence.analysis.FrameMetricsAnalyzer
import com.bishal.adaptivecamera.intelligence.policies.LensRecommendation
import com.bishal.adaptivecamera.intelligence.policies.LensRecommendationPolicy
import com.bishal.adaptivecamera.intelligence.policies.SmartConfigSuggestion
import com.bishal.adaptivecamera.intelligence.policies.SmartConfigurationGenerator
import com.bishal.adaptivecamera.intelligence.scene.SceneClassifier
import com.bishal.adaptivecamera.intelligence.scene.SceneClassificationResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class SmartAutoState(
    val latestMetrics: FrameMetrics? = null,
    val sceneResult: SceneClassificationResult? = null,
    val configSuggestion: SmartConfigSuggestion? = null,
    val lensRecommendation: LensRecommendation? = null,
    val isAutoTuningActive: Boolean = true
)

class SmartAutoOrchestrator(
    private var activeProfile: CameraCapabilityProfile? = null,
    private var allProfiles: List<CameraCapabilityProfile> = emptyList()
) {

    private val _autoState = MutableStateFlow(SmartAutoState())
    val autoState: StateFlow<SmartAutoState> = _autoState.asStateFlow()

    private var currentZoomRatio: Float = 1.0f
    private var currentFocusDistanceDiopters: Float? = null
    private var isFrontFacing: Boolean = false

    val frameAnalyzer = FrameMetricsAnalyzer { metrics ->
        processFrameMetrics(metrics)
    }

    fun updateProfiles(active: CameraCapabilityProfile?, all: List<CameraCapabilityProfile>) {
        this.activeProfile = active
        this.allProfiles = all
        this.isFrontFacing = active?.isFacingFront ?: false
    }

    fun updateCameraControls(zoomRatio: Float, focusDistanceDiopters: Float? = null) {
        this.currentZoomRatio = zoomRatio
        this.currentFocusDistanceDiopters = focusDistanceDiopters
    }

    fun setAutoTuningEnabled(enabled: Boolean) {
        _autoState.update { it.copy(isAutoTuningActive = enabled) }
    }

    fun processFrameMetrics(metrics: FrameMetrics) {
        val scene = SceneClassifier.classify(
            metrics = metrics,
            currentZoom = currentZoomRatio,
            focusDistanceDiopters = currentFocusDistanceDiopters,
            isFacingFront = isFrontFacing
        )

        val suggestion = SmartConfigurationGenerator.generateSuggestion(
            sceneResult = scene,
            metrics = metrics,
            profile = activeProfile
        )

        val lens = LensRecommendationPolicy.recommendLens(
            activeCameraId = activeProfile?.cameraId ?: "0",
            profiles = allProfiles,
            sceneCategory = scene.primaryCategory,
            currentZoomRatio = currentZoomRatio
        )

        _autoState.update {
            it.copy(
                latestMetrics = metrics,
                sceneResult = scene,
                configSuggestion = suggestion,
                lensRecommendation = lens
            )
        }
    }
}
