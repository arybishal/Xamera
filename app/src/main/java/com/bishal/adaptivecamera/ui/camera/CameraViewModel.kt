package com.bishal.adaptivecamera.ui.camera

import android.app.Application
import android.content.Context
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepository
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepositoryImpl
import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraFacing
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.camera.controls.ZoomController
import com.bishal.adaptivecamera.camera.controls.ZoomControllerImpl
import com.bishal.adaptivecamera.camera.engine.CameraEngine
import com.bishal.adaptivecamera.camera.engine.CameraXEngine
import com.bishal.adaptivecamera.camera.session.CameraSessionState
import com.bishal.adaptivecamera.core.haptics.HapticEngine
import com.bishal.adaptivecamera.core.haptics.HapticEngineImpl
import com.bishal.adaptivecamera.core.sensors.VirtualHorizonSensor
import com.bishal.adaptivecamera.core.sensors.VirtualHorizonSensorImpl
import com.bishal.adaptivecamera.core.sensors.VirtualHorizonState
import com.bishal.adaptivecamera.core.settings.CameraPreferencesRepository
import com.bishal.adaptivecamera.core.settings.CameraPreferencesRepositoryImpl
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.photography.auto.SmartAutoOrchestrator
import com.bishal.adaptivecamera.photography.auto.SmartAutoState
import com.bishal.adaptivecamera.photography.pro.Camera2ManualEngine
import com.bishal.adaptivecamera.photography.pro.FalseColorMap
import com.bishal.adaptivecamera.photography.pro.FocusPeakingMap
import com.bishal.adaptivecamera.photography.pro.HistogramData
import com.bishal.adaptivecamera.photography.pro.KelvinConverter
import com.bishal.adaptivecamera.photography.pro.ProAppliedState
import com.bishal.adaptivecamera.photography.pro.ProConfiguration
import com.bishal.adaptivecamera.photography.pro.ProWhiteBalance
import com.bishal.adaptivecamera.photography.pro.ZebraStripingMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CameraUiState(
    val sessionState: CameraSessionState = CameraSessionState.CLOSED,
    val configuration: CameraConfiguration = CameraConfiguration(),
    val latestMedia: MediaItem? = null,
    val countdownRemaining: Int? = null,
    val focusPoint: Pair<Float, Float>? = null,
    val isExposureSliderVisible: Boolean = false,
    val recordingDurationSec: Long = 0L,
    val captureFlashAnimation: Boolean = false,
    val smartAutoState: SmartAutoState = SmartAutoState(),
    val isSuggestionDismissed: Boolean = false,
    val toastMessage: String? = null,
    val proConfig: ProConfiguration = ProConfiguration(),
    val proAppliedState: ProAppliedState = ProAppliedState(),
    val histogramData: HistogramData = HistogramData(),
    val focusPeakingMap: FocusPeakingMap = FocusPeakingMap(),
    val zebraMap: ZebraStripingMap = ZebraStripingMap(),
    val falseColorMap: FalseColorMap = FalseColorMap(),
    val activeProfile: CameraCapabilityProfile? = null,
    val virtualHorizonState: VirtualHorizonState = VirtualHorizonState(),
    val isScreenFlashActive: Boolean = false
)

class CameraViewModel @JvmOverloads constructor(
    application: Application,
    private val cameraEngine: CameraEngine = CameraXEngine(application.applicationContext),
    private val camera2ProEngine: Camera2ManualEngine = Camera2ManualEngine(application.applicationContext),
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(application.applicationContext),
    private val capabilityRepository: CameraCapabilityRepository = CameraCapabilityRepositoryImpl(application.applicationContext),
    val smartAutoOrchestrator: SmartAutoOrchestrator = SmartAutoOrchestrator(),
    private val preferencesRepository: CameraPreferencesRepository = CameraPreferencesRepositoryImpl(application.applicationContext),
    val hapticEngine: HapticEngine = HapticEngineImpl(application.applicationContext) { true },
    val virtualHorizonSensor: VirtualHorizonSensor = VirtualHorizonSensorImpl(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var focusTimeoutJob: Job? = null
    private var lastLifecycleOwner: LifecycleOwner? = null
    private var lastPreviewView: PreviewView? = null

    val zoomController: ZoomController = ZoomControllerImpl(
        initialZoom = 1.0f,
        initialMin = 1.0f,
        initialMax = 10.0f
    ) { ratio ->
        if (_uiState.value.configuration.mode == CameraMode.PRO) {
            camera2ProEngine.setZoomRatio(ratio)
        } else {
            cameraEngine.setZoomRatio(ratio)
        }
    }

    init {
        // Connect frame analyzer to camera engine
        cameraEngine.setImageAnalyzer(smartAutoOrchestrator.frameAnalyzer)

        // Session state listeners
        viewModelScope.launch {
            cameraEngine.sessionState.collect { state ->
                if (_uiState.value.configuration.mode != CameraMode.PRO) {
                    _uiState.update { it.copy(sessionState = state) }
                }
            }
        }
        viewModelScope.launch {
            camera2ProEngine.sessionState.collect { state ->
                if (_uiState.value.configuration.mode == CameraMode.PRO) {
                    _uiState.update { it.copy(sessionState = state) }
                }
            }
        }

        // CameraX configuration listener
        viewModelScope.launch {
            cameraEngine.configuration.collect { config ->
                if (_uiState.value.configuration.mode != CameraMode.PRO) {
                    _uiState.update { current ->
                        current.copy(
                            configuration = current.configuration.copy(
                                facing = config.facing,
                                mode = config.mode,
                                flashMode = config.flashMode,
                                timer = config.timer,
                                zoomRatio = config.zoomRatio,
                                minZoomRatio = config.minZoomRatio,
                                maxZoomRatio = config.maxZoomRatio,
                                exposureCompensationIndex = config.exposureCompensationIndex,
                                minExposureIndex = config.minExposureIndex,
                                maxExposureIndex = config.maxExposureIndex,
                                exposureStepEv = config.exposureStepEv,
                                isExposureCompensationSupported = config.isExposureCompensationSupported,
                                isExposureStateReady = config.isExposureStateReady,
                                isTorchActive = config.isTorchActive,
                                isAudioEnabled = config.isAudioEnabled
                            )
                        )
                    }
                    zoomController.updateRange(config.minZoomRatio, config.maxZoomRatio)
                    smartAutoOrchestrator.updateCameraControls(zoomRatio = config.zoomRatio)
                }
            }
        }

        // Pro configurations
        viewModelScope.launch {
            camera2ProEngine.proConfig.collect { proConfig ->
                _uiState.update { it.copy(proConfig = proConfig) }
            }
        }
        viewModelScope.launch {
            camera2ProEngine.appliedState.collect { applied ->
                _uiState.update { it.copy(proAppliedState = applied) }
            }
        }
        viewModelScope.launch {
            camera2ProEngine.histogramData.collect { hist ->
                _uiState.update { it.copy(histogramData = hist) }
            }
        }
        viewModelScope.launch {
            camera2ProEngine.focusPeakingMap.collect { peaking ->
                _uiState.update { it.copy(focusPeakingMap = peaking) }
            }
        }
        viewModelScope.launch {
            camera2ProEngine.zebraMap.collect { zebra ->
                _uiState.update { it.copy(zebraMap = zebra) }
            }
        }
        viewModelScope.launch {
            camera2ProEngine.falseColorMap.collect { falseColor ->
                _uiState.update { it.copy(falseColorMap = falseColor) }
            }
        }
        viewModelScope.launch {
            cameraEngine.recordingDurationSec.collect { sec ->
                _uiState.update { it.copy(recordingDurationSec = sec) }
            }
        }
        viewModelScope.launch {
            smartAutoOrchestrator.autoState.collect { autoState ->
                _uiState.update { it.copy(smartAutoState = autoState) }
            }
        }

        // Virtual Horizon Listener
        viewModelScope.launch {
            var previousIsLevel = false
            virtualHorizonSensor.horizonState.collect { horizon ->
                _uiState.update { it.copy(virtualHorizonState = horizon) }
                if (horizon.isLevel && !previousIsLevel && _uiState.value.configuration.isVirtualHorizonEnabled) {
                    hapticEngine.performLevelLock()
                }
                previousIsLevel = horizon.isLevel
            }
        }

        // Hardware profile discovery
        viewModelScope.launch {
            try {
                val report = capabilityRepository.getDeviceReport()
                val activeProfile = report.cameraProfiles.firstOrNull { it.isFacingBack }
                _uiState.update { it.copy(activeProfile = activeProfile) }
                smartAutoOrchestrator.updateProfiles(activeProfile, report.cameraProfiles)
            } catch (_: Exception) {}
        }

        // Persisted Preferences Collector
        viewModelScope.launch {
            preferencesRepository.preferencesFlow.collect { prefs ->
                _uiState.update { current ->
                    current.copy(
                        configuration = current.configuration.copy(
                            gridType = prefs.gridType,
                            isVirtualHorizonEnabled = prefs.isVirtualHorizonEnabled,
                            aspectRatio = prefs.aspectRatio,
                            isHapticsEnabled = prefs.isHapticsEnabled,
                            isFloatingShutterEnabled = prefs.isFloatingShutterEnabled,
                            floatingShutterX = prefs.floatingShutterX,
                            floatingShutterY = prefs.floatingShutterY
                        )
                    )
                }
                cameraEngine.setAspectRatio(prefs.aspectRatio)
                camera2ProEngine.setAspectRatio(prefs.aspectRatio)
            }
        }

        refreshLatestThumbnail()
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lastLifecycleOwner = lifecycleOwner
        lastPreviewView = previewView
        if (_uiState.value.configuration.mode != CameraMode.PRO) {
            cameraEngine.bindCamera(lifecycleOwner, previewView)
        }
        startVirtualHorizon()
    }

    fun unbindCamera() {
        stopVirtualHorizon()
        if (_uiState.value.configuration.mode == CameraMode.PRO) {
            camera2ProEngine.unbindCamera()
        } else {
            cameraEngine.unbindCamera()
        }
    }

    fun bindProSurface(surface: Surface) {
        camera2ProEngine.bindSurface(surface)
        startVirtualHorizon()
    }

    fun unbindProSurface() {
        camera2ProEngine.unbindCamera()
        stopVirtualHorizon()
    }

    fun refreshLatestThumbnail() {
        viewModelScope.launch {
            val latest = mediaStoreManager.queryLatestMedia()
            _uiState.update { it.copy(latestMedia = latest) }
        }
    }

    fun onShutterPressed() {
        val state = _uiState.value
        hapticEngine.performShutter()

        if (state.configuration.mode == CameraMode.VIDEO) {
            if (state.sessionState == CameraSessionState.RECORDING) {
                cameraEngine.stopVideoRecording()
            } else if (state.sessionState.canStartRecording) {
                cameraEngine.startVideoRecording(
                    withAudio = state.configuration.isAudioEnabled,
                    onStarted = {},
                    onFinalized = { media ->
                        _uiState.update {
                            it.copy(
                                latestMedia = media ?: it.latestMedia,
                                toastMessage = if (media != null) "Video saved!" else null
                            )
                        }
                    },
                    onError = { error ->
                        _uiState.update { it.copy(toastMessage = "Video error: ${error.message}") }
                    }
                )
            }
            return
        }

        val isFront = state.configuration.facing == CameraFacing.FRONT
        val isScreenFlashActive = isFront && state.configuration.flashMode != FlashMode.OFF

        if (state.configuration.mode == CameraMode.PRO) {
            if (isScreenFlashActive) {
                viewModelScope.launch {
                    _uiState.update { it.copy(isScreenFlashActive = true) }
                    delay(180)
                    triggerFlashAnimation()
                    executeProCapture()
                    delay(120)
                    _uiState.update { it.copy(isScreenFlashActive = false) }
                }
            } else {
                triggerFlashAnimation()
                executeProCapture()
            }
            return
        }

        // Photo Mode
        if (countdownJob != null) {
            countdownJob?.cancel()
            countdownJob = null
            _uiState.update { it.copy(countdownRemaining = null) }
            return
        }

        val timerSeconds = state.configuration.timer.seconds
        if (timerSeconds > 0) {
            countdownJob = viewModelScope.launch {
                for (remaining in timerSeconds downTo 1) {
                    _uiState.update { it.copy(countdownRemaining = remaining) }
                    hapticEngine.performTick()
                    delay(1000)
                }
                _uiState.update { it.copy(countdownRemaining = null) }
                countdownJob = null
                triggerPhotoCaptureWithOptionalScreenFlash(isScreenFlashActive)
            }
        } else {
            triggerPhotoCaptureWithOptionalScreenFlash(isScreenFlashActive)
        }
    }

    private fun triggerPhotoCaptureWithOptionalScreenFlash(isScreenFlashActive: Boolean) {
        if (isScreenFlashActive) {
            viewModelScope.launch {
                _uiState.update { it.copy(isScreenFlashActive = true) }
                delay(180)
                executePhotoCapture()
                delay(120)
                _uiState.update { it.copy(isScreenFlashActive = false) }
            }
        } else {
            executePhotoCapture()
        }
    }

    private fun executePhotoCapture() {
        triggerFlashAnimation()
        cameraEngine.capturePhoto(
            onSuccess = { item ->
                _uiState.update {
                    it.copy(
                        latestMedia = item,
                        toastMessage = "Photo saved!",
                        isScreenFlashActive = false
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        toastMessage = "Capture failed: ${error.message}",
                        isScreenFlashActive = false
                    )
                }
            }
        )
    }

    private fun executeProCapture() {
        camera2ProEngine.capturePhoto(
            onSuccess = { item ->
                val isRaw = _uiState.value.proConfig.isRawCaptureEnabled
                val msg = if (isRaw) "RAW DNG + JPEG saved!" else "Pro Photo saved!"
                _uiState.update {
                    it.copy(
                        latestMedia = item,
                        toastMessage = msg,
                        isScreenFlashActive = false
                    )
                }
            },
            onError = { error ->
                _uiState.update {
                    it.copy(
                        toastMessage = "Capture failed: ${error.message}",
                        isScreenFlashActive = false
                    )
                }
            }
        )
    }

    private fun triggerFlashAnimation() {
        viewModelScope.launch {
            _uiState.update { it.copy(captureFlashAnimation = true) }
            delay(80)
            _uiState.update { it.copy(captureFlashAnimation = false) }
        }
    }

    fun onFocusTap(xNorm: Float, yNorm: Float) {
        cameraEngine.focusAndMeter(xNorm, yNorm)
        hapticEngine.performClick()
        _uiState.update { it.copy(focusPoint = Pair(xNorm, yNorm)) }
        focusTimeoutJob?.cancel()
        focusTimeoutJob = viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun switchCamera() {
        hapticEngine.performClick()
        if (_uiState.value.configuration.mode == CameraMode.PRO) {
            camera2ProEngine.switchFacing()
            _uiState.update { it.copy(configuration = it.configuration.copy(facing = it.configuration.facing.toggle())) }
        } else {
            cameraEngine.switchFacing()
        }
    }

    fun setCameraMode(mode: CameraMode) {
        if (_uiState.value.configuration.mode == mode) return
        hapticEngine.performModeSwitch()
        val previousMode = _uiState.value.configuration.mode
        if (mode == CameraMode.PRO) {
            cameraEngine.unbindCamera()
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(mode = CameraMode.PRO),
                    sessionState = camera2ProEngine.sessionState.value
                )
            }
        } else {
            if (previousMode == CameraMode.PRO) {
                camera2ProEngine.unbindCamera()
            }
            cameraEngine.setMode(mode)
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(mode = mode),
                    sessionState = cameraEngine.sessionState.value
                )
            }
            val owner = lastLifecycleOwner
            val preview = lastPreviewView
            if (owner != null && preview != null) {
                cameraEngine.bindCamera(owner, preview)
            }
        }
    }

    fun setManualIso(enabled: Boolean, iso: Int) {
        camera2ProEngine.setManualIso(enabled, iso)
    }

    fun setManualShutterSpeed(enabled: Boolean, shutterNs: Long) {
        camera2ProEngine.setManualShutterSpeed(enabled, shutterNs)
    }

    fun setManualFocus(enabled: Boolean, distanceDiopters: Float) {
        camera2ProEngine.setManualFocus(enabled, distanceDiopters)
    }

    fun setWhiteBalance(wb: ProWhiteBalance) {
        camera2ProEngine.setWhiteBalance(wb)
    }

    fun toggleRawCapture(enabled: Boolean) {
        camera2ProEngine.setRawCaptureEnabled(enabled)
    }

    fun toggleHistogram() {
        val current = _uiState.value.proConfig.isHistogramVisible
        camera2ProEngine.setHistogramVisible(!current)
    }

    fun toggleHistogramExpanded() {
        val current = _uiState.value.proConfig.isHistogramExpanded
        camera2ProEngine.setHistogramExpanded(!current)
    }

    fun toggleFocusPeaking(enabled: Boolean) {
        camera2ProEngine.setFocusPeakingEnabled(enabled)
    }

    fun setFocusPeakingSensitivity(sensitivity: Float) {
        camera2ProEngine.setFocusPeakingSensitivity(sensitivity)
    }

    // Phase 6 Exposure & Focus Monitoring Controllers
    fun toggleAeLock() {
        hapticEngine.performClick()
        val next = !_uiState.value.proConfig.isAeLocked
        camera2ProEngine.setAeLock(next)
    }

    fun toggleAfLock() {
        hapticEngine.performClick()
        val next = !_uiState.value.proConfig.isAfLocked
        camera2ProEngine.setAfLock(next)
    }

    fun toggleAwbLock() {
        hapticEngine.performClick()
        val next = !_uiState.value.proConfig.isAwbLocked
        camera2ProEngine.setAwbLock(next)
    }

    fun setManualKelvin(enabled: Boolean, kelvin: Int, tint: Int = 0) {
        camera2ProEngine.setManualKelvin(enabled, kelvin, tint)
    }

    fun toggleZebraStriping(enabled: Boolean) {
        hapticEngine.performClick()
        camera2ProEngine.setZebraStriping(enabled, _uiState.value.proConfig.zebraThresholdIre)
    }

    fun setZebraThreshold(thresholdIre: Int) {
        camera2ProEngine.setZebraStriping(_uiState.value.proConfig.isZebraEnabled, thresholdIre)
    }

    fun toggleFalseColor(enabled: Boolean) {
        hapticEngine.performClick()
        camera2ProEngine.setFalseColor(enabled)
    }

    fun toggleFocusMagnifier(enabled: Boolean) {
        hapticEngine.performClick()
        camera2ProEngine.setFocusMagnifier(enabled, _uiState.value.proConfig.focusMagnifierScale)
    }

    fun setFocusMagnifierScale(scale: Float) {
        camera2ProEngine.setFocusMagnifier(_uiState.value.proConfig.isFocusMagnifierEnabled, scale)
    }

    fun resetProToAuto() {
        camera2ProEngine.resetAllManualToAuto()
    }

    fun toggleFlash() {
        hapticEngine.performClick()
        val next = _uiState.value.configuration.flashMode.next()
        cameraEngine.setFlashMode(next)
    }

    fun toggleTimer() {
        hapticEngine.performClick()
        val next = _uiState.value.configuration.timer.next()
        cameraEngine.setTimer(next)
    }

    fun setZoomRatio(ratio: Float) {
        zoomController.setZoomRatio(ratio)
        _uiState.update { it.copy(configuration = it.configuration.copy(zoomRatio = ratio)) }
    }

    fun applyZoomScaleFactor(factor: Float) {
        val newRatio = zoomController.applyScaleFactor(factor)
        _uiState.update { it.copy(configuration = it.configuration.copy(zoomRatio = newRatio)) }
    }

    fun setExposureIndex(index: Int) {
        cameraEngine.setExposureCompensation(index)
    }

    fun toggleExposureSlider() {
        val config = _uiState.value.configuration
        if (!config.isExposureStateReady) {
            _uiState.update { it.copy(toastMessage = "Camera is initializing...") }
            return
        }
        if (!config.isExposureCompensationSupported) {
            _uiState.update { it.copy(toastMessage = "Exposure compensation not supported on this camera") }
            return
        }
        hapticEngine.performClick()
        _uiState.update { it.copy(isExposureSliderVisible = !it.isExposureSliderVisible) }
    }

    fun closeExposureSlider() {
        _uiState.update { it.copy(isExposureSliderVisible = false) }
    }

    fun resetExposure() {
        cameraEngine.resetExposureCompensation()
    }

    // Phase 5 Viewfinder Ergonomics & Controls
    fun setGridType(gridType: CompositionGridType) {
        viewModelScope.launch {
            preferencesRepository.updateGridType(gridType)
            _uiState.update { it.copy(configuration = it.configuration.copy(gridType = gridType)) }
            hapticEngine.performClick()
        }
    }

    fun cycleGridType() {
        val next = _uiState.value.configuration.gridType.next()
        setGridType(next)
    }

    fun setAspectRatio(ratio: AspectRatioConfig) {
        viewModelScope.launch {
            preferencesRepository.updateAspectRatio(ratio)
            cameraEngine.setAspectRatio(ratio)
            camera2ProEngine.setAspectRatio(ratio)
            _uiState.update { it.copy(configuration = it.configuration.copy(aspectRatio = ratio)) }
            hapticEngine.performClick()
        }
    }

    fun cycleAspectRatio() {
        val next = _uiState.value.configuration.aspectRatio.next()
        setAspectRatio(next)
    }

    fun toggleVirtualHorizon() {
        val next = !_uiState.value.configuration.isVirtualHorizonEnabled
        viewModelScope.launch {
            preferencesRepository.updateVirtualHorizonEnabled(next)
            _uiState.update { it.copy(configuration = it.configuration.copy(isVirtualHorizonEnabled = next)) }
            hapticEngine.performClick()
        }
    }

    fun toggleHaptics() {
        val next = !_uiState.value.configuration.isHapticsEnabled
        viewModelScope.launch {
            preferencesRepository.updateHapticsEnabled(next)
            _uiState.update { it.copy(configuration = it.configuration.copy(isHapticsEnabled = next)) }
            if (next) hapticEngine.performClick()
        }
    }

    fun toggleFloatingShutter() {
        val next = !_uiState.value.configuration.isFloatingShutterEnabled
        viewModelScope.launch {
            preferencesRepository.updateFloatingShutterEnabled(next)
            _uiState.update { it.copy(configuration = it.configuration.copy(isFloatingShutterEnabled = next)) }
            hapticEngine.performClick()
        }
    }

    fun updateFloatingShutterPosition(normX: Float, normY: Float) {
        viewModelScope.launch {
            preferencesRepository.updateFloatingShutterPosition(normX, normY)
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(
                        floatingShutterX = normX,
                        floatingShutterY = normY
                    )
                )
            }
        }
    }

    fun resetFloatingShutterPosition() {
        viewModelScope.launch {
            preferencesRepository.resetFloatingShutterPosition()
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(
                        floatingShutterX = 0.82f,
                        floatingShutterY = 0.65f
                    )
                )
            }
            hapticEngine.performClick()
        }
    }

    fun startVirtualHorizon() {
        virtualHorizonSensor.startListening()
    }

    fun stopVirtualHorizon() {
        virtualHorizonSensor.stopListening()
    }

    fun applySmartAutoSuggestion() {
        val suggestion = _uiState.value.smartAutoState.configSuggestion ?: return
        cameraEngine.setExposureCompensation(suggestion.recommendedEvIndex)
        _uiState.update {
            it.copy(
                isSuggestionDismissed = true,
                toastMessage = "Applied: ${suggestion.actionLabel}"
            )
        }
    }

    fun dismissSmartAutoSuggestion() {
        _uiState.update { it.copy(isSuggestionDismissed = true) }
    }

    fun resetSuggestionDismissal() {
        _uiState.update { it.copy(isSuggestionDismissed = false) }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
