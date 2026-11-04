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
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraFacing
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.camera.engine.CameraEngine
import com.bishal.adaptivecamera.camera.engine.CameraXEngine
import com.bishal.adaptivecamera.camera.session.CameraSessionState
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.photography.auto.SmartAutoOrchestrator
import com.bishal.adaptivecamera.photography.auto.SmartAutoState
import com.bishal.adaptivecamera.photography.pro.Camera2ManualEngine
import com.bishal.adaptivecamera.photography.pro.FocusPeakingMap
import com.bishal.adaptivecamera.photography.pro.HistogramData
import com.bishal.adaptivecamera.photography.pro.ProAppliedState
import com.bishal.adaptivecamera.photography.pro.ProConfiguration
import com.bishal.adaptivecamera.photography.pro.ProWhiteBalance
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
    val activeProfile: CameraCapabilityProfile? = null
)

class CameraViewModel @JvmOverloads constructor(
    application: Application,
    private val cameraEngine: CameraEngine = CameraXEngine(application.applicationContext),
    private val camera2ProEngine: Camera2ManualEngine = Camera2ManualEngine(application.applicationContext),
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(application.applicationContext),
    private val capabilityRepository: CameraCapabilityRepository = CameraCapabilityRepositoryImpl(application.applicationContext),
    val smartAutoOrchestrator: SmartAutoOrchestrator = SmartAutoOrchestrator()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var focusTimeoutJob: Job? = null
    private var lastLifecycleOwner: LifecycleOwner? = null
    private var lastPreviewView: PreviewView? = null

    init {
        // Connect frame analyzer to camera engine
        cameraEngine.setImageAnalyzer(smartAutoOrchestrator.frameAnalyzer)

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
        viewModelScope.launch {
            cameraEngine.configuration.collect { config ->
                if (_uiState.value.configuration.mode != CameraMode.PRO) {
                    _uiState.update { it.copy(configuration = config) }
                    smartAutoOrchestrator.updateCameraControls(zoomRatio = config.zoomRatio)
                }
            }
        }
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
            cameraEngine.recordingDurationSec.collect { sec ->
                _uiState.update { it.copy(recordingDurationSec = sec) }
            }
        }
        viewModelScope.launch {
            smartAutoOrchestrator.autoState.collect { autoState ->
                _uiState.update { it.copy(smartAutoState = autoState) }
            }
        }
        viewModelScope.launch {
            try {
                val report = capabilityRepository.getDeviceReport()
                val activeProfile = report.cameraProfiles.firstOrNull { it.isFacingBack }
                _uiState.update { it.copy(activeProfile = activeProfile) }
                smartAutoOrchestrator.updateProfiles(activeProfile, report.cameraProfiles)
            } catch (_: Exception) {}
        }
        refreshLatestThumbnail()
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        lastLifecycleOwner = lifecycleOwner
        lastPreviewView = previewView
        if (_uiState.value.configuration.mode != CameraMode.PRO) {
            cameraEngine.bindCamera(lifecycleOwner, previewView)
        }
    }

    fun unbindCamera() {
        if (_uiState.value.configuration.mode == CameraMode.PRO) {
            camera2ProEngine.unbindCamera()
        } else {
            cameraEngine.unbindCamera()
        }
    }

    fun bindProSurface(surface: Surface) {
        camera2ProEngine.bindSurface(surface)
    }

    fun unbindProSurface() {
        camera2ProEngine.unbindCamera()
    }

    fun refreshLatestThumbnail() {
        viewModelScope.launch {
            val latest = mediaStoreManager.queryLatestMedia()
            _uiState.update { it.copy(latestMedia = latest) }
        }
    }

    fun onShutterPressed() {
        val state = _uiState.value
        if (state.configuration.mode == CameraMode.VIDEO) {
            if (state.sessionState == CameraSessionState.RECORDING) {
                cameraEngine.stopVideoRecording()
            } else if (state.sessionState.canStartRecording) {
                cameraEngine.startVideoRecording(
                    withAudio = state.configuration.isAudioEnabled,
                    onStarted = {
                        // Handled via engine recording state
                    },
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

        if (state.configuration.mode == CameraMode.PRO) {
            triggerFlashAnimation()
            camera2ProEngine.capturePhoto(
                onSuccess = { item ->
                    val isRaw = _uiState.value.proConfig.isRawCaptureEnabled
                    val msg = if (isRaw) "RAW DNG + JPEG saved!" else "Pro Photo saved!"
                    _uiState.update {
                        it.copy(
                            latestMedia = item,
                            toastMessage = msg
                        )
                    }
                },
                onError = { error ->
                    _uiState.update { it.copy(toastMessage = "Capture failed: ${error.message}") }
                }
            )
            return
        }

        // Photo Mode
        if (countdownJob != null) {
            // Cancel active countdown
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
                    delay(1000)
                }
                _uiState.update { it.copy(countdownRemaining = null) }
                countdownJob = null
                executePhotoCapture()
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
                        toastMessage = "Photo saved!"
                    )
                }
            },
            onError = { error ->
                _uiState.update { it.copy(toastMessage = "Capture failed: ${error.message}") }
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
        _uiState.update { it.copy(focusPoint = Pair(xNorm, yNorm)) }
        focusTimeoutJob?.cancel()
        focusTimeoutJob = viewModelScope.launch {
            delay(2500)
            _uiState.update { it.copy(focusPoint = null) }
        }
    }

    fun switchCamera() {
        if (_uiState.value.configuration.mode == CameraMode.PRO) {
            camera2ProEngine.switchFacing()
            _uiState.update { it.copy(configuration = it.configuration.copy(facing = it.configuration.facing.toggle())) }
        } else {
            cameraEngine.switchFacing()
        }
    }

    fun setCameraMode(mode: CameraMode) {
        if (_uiState.value.configuration.mode == mode) return
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

    fun resetProToAuto() {
        camera2ProEngine.resetAllManualToAuto()
    }

    fun toggleFlash() {
        val next = _uiState.value.configuration.flashMode.next()
        cameraEngine.setFlashMode(next)
    }

    fun toggleTimer() {
        val next = _uiState.value.configuration.timer.next()
        cameraEngine.setTimer(next)
    }

    fun setZoomRatio(ratio: Float) {
        cameraEngine.setZoomRatio(ratio)
    }

    fun setExposureIndex(index: Int) {
        cameraEngine.setExposureCompensation(index)
    }

    fun toggleExposureSlider() {
        _uiState.update { it.copy(isExposureSliderVisible = !it.isExposureSliderVisible) }
    }

    fun resetExposure() {
        cameraEngine.resetExposureCompensation()
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
