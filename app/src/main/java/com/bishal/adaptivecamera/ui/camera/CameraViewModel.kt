package com.bishal.adaptivecamera.ui.camera

import android.app.Application
import android.content.Context
import androidx.camera.view.PreviewView
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
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
    val toastMessage: String? = null
)

class CameraViewModel(
    application: Application,
    private val cameraEngine: CameraEngine = CameraXEngine(application.applicationContext),
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var focusTimeoutJob: Job? = null

    init {
        viewModelScope.launch {
            cameraEngine.sessionState.collect { state ->
                _uiState.update { it.copy(sessionState = state) }
            }
        }
        viewModelScope.launch {
            cameraEngine.configuration.collect { config ->
                _uiState.update { it.copy(configuration = config) }
            }
        }
        viewModelScope.launch {
            cameraEngine.recordingDurationSec.collect { sec ->
                _uiState.update { it.copy(recordingDurationSec = sec) }
            }
        }
        refreshLatestThumbnail()
    }

    fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        cameraEngine.bindCamera(lifecycleOwner, previewView)
    }

    fun unbindCamera() {
        cameraEngine.unbindCamera()
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
        cameraEngine.switchFacing()
    }

    fun setCameraMode(mode: CameraMode) {
        cameraEngine.setMode(mode)
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

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
