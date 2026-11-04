package com.bishal.adaptivecamera.camera.engine

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraFacing
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.camera.session.CameraSessionState
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class CameraXEngine(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(context)
) : CameraEngine {

    companion object {
        private const val TAG = "CameraXEngine"
    }

    private val engineScope = CoroutineScope(Dispatchers.Main + Job())

    private val _sessionState = MutableStateFlow(CameraSessionState.CLOSED)
    override val sessionState: StateFlow<CameraSessionState> = _sessionState.asStateFlow()

    private val _configuration = MutableStateFlow(CameraConfiguration())
    override val configuration: StateFlow<CameraConfiguration> = _configuration.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0L)
    override val recordingDurationSec: StateFlow<Long> = _recordingDurationSec.asStateFlow()

    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var preview: Preview? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: Recording? = null
    private var recordingTimerJob: Job? = null

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null

    init {
        val future = ProcessCameraProvider.getInstance(context)
        future.addListener({
            try {
                cameraProvider = future.get()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get ProcessCameraProvider", e)
                _sessionState.value = CameraSessionState.ERROR
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView

        val provider = cameraProvider
        if (provider == null) {
            _sessionState.value = CameraSessionState.OPENING
            val future = ProcessCameraProvider.getInstance(context)
            future.addListener({
                try {
                    cameraProvider = future.get()
                    rebindInternal()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to initialize CameraProvider during bind", e)
                    _sessionState.value = CameraSessionState.ERROR
                }
            }, ContextCompat.getMainExecutor(context))
        } else {
            rebindInternal()
        }
    }

    private fun rebindInternal() {
        val provider = cameraProvider ?: return
        val lifecycleOwner = currentLifecycleOwner ?: return
        val previewView = currentPreviewView ?: return

        _sessionState.value = CameraSessionState.OPENING

        try {
            provider.unbindAll()

            // 1. Selector
            val selector = _configuration.value.facing.selector

            // Verify if camera is available
            if (!provider.hasCamera(selector)) {
                Log.w(TAG, "Requested camera selector unavailable, falling back")
                _sessionState.value = CameraSessionState.ERROR
                return
            }

            // 2. Preview
            val newPreview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }

            // 3. ImageCapture
            val newImageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(_configuration.value.flashMode.imageCaptureFlashMode)
                .build()

            // 4. VideoCapture
            val qualitySelector = QualitySelector.fromOrderedList(
                listOf(Quality.FHD, Quality.HD, Quality.SD),
                androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
            val recorder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .build()
            val newVideoCapture = VideoCapture.withOutput(recorder)

            // 5. Bind
            val newCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                newPreview,
                newImageCapture,
                newVideoCapture
            )

            camera = newCamera
            preview = newPreview
            imageCapture = newImageCapture
            videoCapture = newVideoCapture

            // Initialize Exposure and Zoom ranges safely
            val zoomState = newCamera.cameraInfo.zoomState.value
            val exposureState = newCamera.cameraInfo.exposureState

            val stepDenom = exposureState.exposureCompensationStep.denominator
            val stepNum = exposureState.exposureCompensationStep.numerator
            val stepEv = if (stepDenom != 0) {
                stepNum.toFloat() / stepDenom.toFloat()
            } else {
                0.5f
            }

            val minExp = exposureState.exposureCompensationRange.lower
            val maxExp = exposureState.exposureCompensationRange.upper

            _configuration.update { current ->
                current.copy(
                    zoomRatio = zoomState?.zoomRatio ?: 1.0f,
                    minZoomRatio = zoomState?.minZoomRatio ?: 1.0f,
                    maxZoomRatio = zoomState?.maxZoomRatio ?: 10.0f,
                    exposureCompensationIndex = exposureState.exposureCompensationIndex,
                    minExposureIndex = minExp,
                    maxExposureIndex = maxExp,
                    exposureStepEv = stepEv
                )
            }

            // Apply torch if configured
            if (_configuration.value.flashMode == FlashMode.TORCH) {
                newCamera.cameraControl.enableTorch(true)
            }

            _sessionState.value = CameraSessionState.READY
            Log.i(TAG, "Camera bound successfully to ${selector}")

        } catch (e: Exception) {
            Log.e(TAG, "Error binding camera lifecycle", e)
            _sessionState.value = CameraSessionState.ERROR
        }
    }

    override fun unbindCamera() {
        stopVideoRecording()
        try {
            cameraProvider?.unbindAll()
        } catch (e: Exception) {
            Log.w(TAG, "Error unbinding cameras", e)
        }
        camera = null
        preview = null
        imageCapture = null
        videoCapture = null
        _sessionState.value = CameraSessionState.CLOSED
    }

    override fun switchFacing() {
        if (!_sessionState.value.canSwitchCamera) return
        _sessionState.value = CameraSessionState.SWITCHING
        val newFacing = _configuration.value.facing.toggle()
        _configuration.update { it.copy(facing = newFacing) }
        rebindInternal()
    }

    override fun setFacing(facing: CameraFacing) {
        if (_configuration.value.facing == facing) return
        if (!_sessionState.value.canSwitchCamera) return
        _sessionState.value = CameraSessionState.SWITCHING
        _configuration.update { it.copy(facing = facing) }
        rebindInternal()
    }

    override fun setMode(mode: CameraMode) {
        if (_configuration.value.mode == mode) return
        if (_sessionState.value == CameraSessionState.RECORDING) {
            stopVideoRecording()
        }
        _configuration.update { it.copy(mode = mode) }
    }

    override fun setFlashMode(flashMode: FlashMode) {
        _configuration.update { it.copy(flashMode = flashMode) }
        val cam = camera ?: return
        val imgCap = imageCapture ?: return

        try {
            if (flashMode == FlashMode.TORCH) {
                cam.cameraControl.enableTorch(true)
                imgCap.flashMode = ImageCapture.FLASH_MODE_OFF
            } else {
                cam.cameraControl.enableTorch(false)
                imgCap.flashMode = flashMode.imageCaptureFlashMode
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply flash mode $flashMode", e)
        }
    }

    override fun setTimer(timer: PhotoTimer) {
        _configuration.update { it.copy(timer = timer) }
    }

    override fun setZoomRatio(ratio: Float) {
        val cam = camera ?: return
        val clamped = ratio.coerceIn(
            _configuration.value.minZoomRatio,
            _configuration.value.maxZoomRatio
        )
        cam.cameraControl.setZoomRatio(clamped)
        _configuration.update { it.copy(zoomRatio = clamped) }
    }

    override fun setLinearZoom(percentage: Float) {
        val cam = camera ?: return
        val clamped = percentage.coerceIn(0.0f, 1.0f)
        cam.cameraControl.setLinearZoom(clamped)
    }

    override fun setExposureCompensation(index: Int) {
        val cam = camera ?: return
        val clamped = index.coerceIn(
            _configuration.value.minExposureIndex,
            _configuration.value.maxExposureIndex
        )
        cam.cameraControl.setExposureCompensationIndex(clamped)
        _configuration.update { it.copy(exposureCompensationIndex = clamped) }
    }

    override fun resetExposureCompensation() {
        setExposureCompensation(0)
    }

    override fun focusAndMeter(xNorm: Float, yNorm: Float) {
        val cam = camera ?: return
        val previewView = currentPreviewView ?: return

        val width = previewView.width.toFloat()
        val height = previewView.height.toFloat()
        if (width <= 0 || height <= 0) return

        val pointFactory = previewView.meteringPointFactory
        val point = pointFactory.createPoint(xNorm * width, yNorm * height)
        val action = FocusMeteringAction.Builder(
            point,
            FocusMeteringAction.FLAG_AF or FocusMeteringAction.FLAG_AE
        ).setAutoCancelDuration(3, TimeUnit.SECONDS).build()

        cam.cameraControl.startFocusAndMetering(action)
    }

    override fun capturePhoto(
        onSuccess: (MediaItem) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val imgCap = imageCapture
        if (imgCap == null) {
            onError(IllegalStateException("Camera capture pipeline not initialized"))
            return
        }

        if (!_sessionState.value.canCapturePhoto) {
            onError(IllegalStateException("Camera is not in ready state (${_sessionState.value})"))
            return
        }

        _sessionState.value = CameraSessionState.CAPTURING

        val outputOptions = mediaStoreManager.createPhotoOutputFileOptions()
        val executor = ContextCompat.getMainExecutor(context)

        imgCap.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    _sessionState.value = CameraSessionState.READY
                    val savedUri = outputFileResults.savedUri
                    if (savedUri != null) {
                        mediaStoreManager.markPhotoPendingComplete(savedUri)
                        val latest = mediaStoreManager.queryLatestMedia()
                        if (latest != null) {
                            onSuccess(latest)
                        } else {
                            onError(IllegalStateException("Saved photo could not be retrieved from MediaStore"))
                        }
                    } else {
                        onError(IllegalStateException("Capture succeeded but savedUri is null"))
                    }
                }

                override fun onError(exception: ImageCaptureException) {
                    _sessionState.value = CameraSessionState.READY
                    Log.e(TAG, "Image capture failed", exception)
                    onError(exception)
                }
            }
        )
    }

    @SuppressLint("MissingPermission")
    override fun startVideoRecording(
        withAudio: Boolean,
        onStarted: () -> Unit,
        onFinalized: (MediaItem?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val vidCap = videoCapture
        if (vidCap == null) {
            onError(IllegalStateException("Video recording pipeline not initialized"))
            return
        }

        if (!_sessionState.value.canStartRecording) {
            onError(IllegalStateException("Cannot start recording in state ${_sessionState.value}"))
            return
        }

        val outputOptions = mediaStoreManager.createVideoOutputOptions()
        val executor = ContextCompat.getMainExecutor(context)

        val pending = vidCap.output.prepareRecording(context, outputOptions)

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (withAudio && hasAudioPermission) {
            pending.withAudioEnabled()
        }

        _sessionState.value = CameraSessionState.RECORDING

        activeRecording = pending.start(executor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    startRecordingTimer()
                    onStarted()
                }
                is VideoRecordEvent.Finalize -> {
                    stopRecordingTimer()
                    _sessionState.value = CameraSessionState.READY
                    if (event.hasError()) {
                        Log.e(TAG, "Video recording error: ${event.error}", event.cause)
                        onError(event.cause ?: RuntimeException("Video recording failed with code ${event.error}"))
                    } else {
                        val latest = mediaStoreManager.queryLatestMedia()
                        onFinalized(latest)
                    }
                }
            }
        }
    }

    override fun stopVideoRecording() {
        val recording = activeRecording
        if (recording != null) {
            try {
                recording.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recording", e)
            }
            activeRecording = null
        }
        stopRecordingTimer()
    }

    private fun startRecordingTimer() {
        recordingTimerJob?.cancel()
        _recordingDurationSec.value = 0L
        recordingTimerJob = engineScope.launch {
            while (isActive) {
                delay(1000)
                _recordingDurationSec.update { it + 1 }
            }
        }
    }

    private fun stopRecordingTimer() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _recordingDurationSec.value = 0L
    }
}
