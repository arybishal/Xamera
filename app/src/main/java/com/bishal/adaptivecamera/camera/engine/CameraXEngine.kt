package com.bishal.adaptivecamera.camera.engine

import android.hardware.camera2.CaptureRequest
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import com.bishal.adaptivecamera.core.diagnostics.CameraDiagnosticsLogger
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
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
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
import com.bishal.adaptivecamera.video.config.VideoConfig
import com.bishal.adaptivecamera.video.config.VideoExposureMode
import com.bishal.adaptivecamera.video.exposure.ShutterAngleCalculator
import com.bishal.adaptivecamera.video.stabilization.VideoStabilizationCoordinator
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
import java.util.concurrent.Executors
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
    private var imageAnalysis: ImageAnalysis? = null
    private var imageAnalyzer: ImageAnalysis.Analyzer? = null
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    private var activeRecording: Recording? = null
    private var recordingTimerJob: Job? = null
    private var videoConfig: VideoConfig = VideoConfig()

    private var currentLifecycleOwner: LifecycleOwner? = null
    private var currentPreviewView: PreviewView? = null
    private var currentSurfaceProvider: Preview.SurfaceProvider? = null

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

    override fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        surfaceProvider: Preview.SurfaceProvider?
    ) {
        currentLifecycleOwner = lifecycleOwner
        currentPreviewView = previewView
        currentSurfaceProvider = surfaceProvider

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
                it.surfaceProvider = currentSurfaceProvider ?: previewView.surfaceProvider
            }

            // 3. ImageCapture with Maximum Photo Quality Optimization (Part B)
            val imageCaptureBuilder = ImageCapture.Builder()
                .setCaptureMode(
                    if (_configuration.value.isZslEnabled) ImageCapture.CAPTURE_MODE_ZERO_SHUTTER_LAG
                    else ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY
                )
                .setJpegQuality(98)
                .setFlashMode(_configuration.value.flashMode.imageCaptureFlashMode)

            @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
            val captureExtender = Camera2Interop.Extender(imageCaptureBuilder)
            captureExtender.setCaptureRequestOption(
                CaptureRequest.NOISE_REDUCTION_MODE,
                CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY
            )
            captureExtender.setCaptureRequestOption(
                CaptureRequest.EDGE_MODE,
                CaptureRequest.EDGE_MODE_HIGH_QUALITY
            )
            captureExtender.setCaptureRequestOption(
                CaptureRequest.COLOR_CORRECTION_MODE,
                CaptureRequest.COLOR_CORRECTION_MODE_HIGH_QUALITY
            )
            captureExtender.setCaptureRequestOption(
                CaptureRequest.HOT_PIXEL_MODE,
                CaptureRequest.HOT_PIXEL_MODE_HIGH_QUALITY
            )
            captureExtender.setCaptureRequestOption(
                CaptureRequest.TONEMAP_MODE,
                CaptureRequest.TONEMAP_MODE_HIGH_QUALITY
            )

            val newImageCapture = imageCaptureBuilder.build()

            // 4. ImageAnalysis
            val newImageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalyzer?.let { analyzer ->
                newImageAnalysis.setAnalyzer(analysisExecutor, analyzer)
            }

            // 5. VideoCapture
            val qualitySelector = QualitySelector.from(
                videoConfig.resolution.quality,
                androidx.camera.video.FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
            )
            val recorderBuilder = Recorder.Builder()
                .setQualitySelector(qualitySelector)
                .setExecutor(ContextCompat.getMainExecutor(context))

            val bitrate = videoConfig.targetBitrateBps.toInt()
            if (bitrate in 500_000..200_000_000) {
                recorderBuilder.setTargetVideoEncodingBitRate(bitrate)
            }
            val recorder = recorderBuilder.build()
            val newVideoCapture = VideoCapture.withOutput(recorder)

            // 6. Bind use cases according to active mode
            val useCases = mutableListOf<UseCase>(newPreview)
            if (_configuration.value.mode.isVideoMode) {
                useCases.add(newVideoCapture)
            } else {
                useCases.add(newImageCapture)
                useCases.add(newImageAnalysis)
            }

            val newCamera = provider.bindToLifecycle(
                lifecycleOwner,
                selector,
                *useCases.toTypedArray()
            )

            camera = newCamera
            preview = newPreview
            imageCapture = newImageCapture
            imageAnalysis = newImageAnalysis
            videoCapture = newVideoCapture

            // Initialize Exposure and Zoom ranges safely
            val zoomState = newCamera.cameraInfo.zoomState.value
            val exposureState = newCamera.cameraInfo.exposureState

            val isExposureSupported = exposureState.isExposureCompensationSupported
            val range = exposureState.exposureCompensationRange
            val minExp = range.lower
            val maxExp = range.upper
            val currentExpIndex = exposureState.exposureCompensationIndex

            val stepDenom = exposureState.exposureCompensationStep.denominator
            val stepNum = exposureState.exposureCompensationStep.numerator
            val stepEv = if (stepDenom != 0) {
                stepNum.toFloat() / stepDenom.toFloat()
            } else {
                0.5f
            }

            _configuration.update { current ->
                current.copy(
                    zoomRatio = zoomState?.zoomRatio ?: 1.0f,
                    minZoomRatio = zoomState?.minZoomRatio ?: 1.0f,
                    maxZoomRatio = zoomState?.maxZoomRatio ?: 10.0f,
                    exposureCompensationIndex = currentExpIndex,
                    minExposureIndex = minExp,
                    maxExposureIndex = maxExp,
                    exposureStepEv = stepEv,
                    isExposureCompensationSupported = isExposureSupported,
                    isExposureStateReady = true
                )
            }

            // Dynamic LiveData observation to ensure 0.6x ultra-wide bounds are reflected
            newCamera.cameraInfo.zoomState.observe(lifecycleOwner) { state ->
                if (state != null) {
                    _configuration.update { current ->
                        current.copy(
                            zoomRatio = state.zoomRatio,
                            minZoomRatio = state.minZoomRatio,
                            maxZoomRatio = state.maxZoomRatio
                        )
                    }
                }
            }

            CameraDiagnosticsLogger.getInstance(context).i(
                module = "CameraInit",
                eventType = "CAMERA_BOUND",
                message = "CameraX pipeline bound. Zoom: [${zoomState?.minZoomRatio ?: 1f}x - ${zoomState?.maxZoomRatio ?: 10f}x]",
                cameraId = if (_configuration.value.facing == CameraFacing.BACK) "0" else "1",
                mode = _configuration.value.mode.name
            )

            // Apply torch if configured
            if (_configuration.value.flashMode == FlashMode.TORCH) {
                newCamera.cameraControl.enableTorch(true)
            }

            if (_configuration.value.mode.isVideoMode) {
                applyVideoCamera2Controls(newCamera)
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
        _configuration.update { it.copy(isExposureStateReady = false) }
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
        val prev = _configuration.value.mode
        _configuration.update { it.copy(mode = mode) }
        if (prev.isVideoMode != mode.isVideoMode) {
            rebindInternal()
        }
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

    override fun setAspectRatio(aspectRatio: com.bishal.adaptivecamera.camera.config.AspectRatioConfig) {
        _configuration.update { it.copy(aspectRatio = aspectRatio) }
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
        if (!_configuration.value.isExposureControlAvailable) {
            Log.w(TAG, "Exposure compensation is not supported or ready on active camera")
            return
        }

        val clamped = index.coerceIn(
            _configuration.value.minExposureIndex,
            _configuration.value.maxExposureIndex
        )

        val future = cam.cameraControl.setExposureCompensationIndex(clamped)
        future.addListener({
            try {
                val applied = future.get()
                _configuration.update { it.copy(exposureCompensationIndex = applied) }
            } catch (e: Exception) {
                val cause = e.cause
                if (cause !is androidx.camera.core.CameraControl.OperationCanceledException) {
                    Log.w(TAG, "Failed to apply exposure compensation: ${e.message}")
                    // Re-sync with actual current camera exposure index
                    val currentActual = cam.cameraInfo.exposureState.exposureCompensationIndex
                    _configuration.update { it.copy(exposureCompensationIndex = currentActual) }
                }
            }
        }, ContextCompat.getMainExecutor(context))
    }

    override fun resetExposureCompensation() {
        setExposureCompensation(0)
    }

    override fun setZslEnabled(enabled: Boolean) {
        _configuration.update { it.copy(isZslEnabled = enabled) }
        val owner = currentLifecycleOwner
        val view = currentPreviewView
        if (owner != null && view != null) {
            bindCamera(owner, view)
        }
    }

    override fun setAebBracketCount(count: Int) {
        _configuration.update { it.copy(aebBracketCount = count) }
    }

    override fun setAebEvStep(step: Float) {
        _configuration.update { it.copy(aebEvStep = step) }
    }

    override fun setCaptureFormat(format: com.bishal.adaptivecamera.photography.capture.CaptureFormat) {
        // CameraX engine records format preference; RAW modes are served via Pro Camera2ManualEngine
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

    override fun setImageAnalyzer(analyzer: ImageAnalysis.Analyzer?) {
        imageAnalyzer = analyzer
        imageAnalysis?.let { analysis ->
            if (analyzer != null) {
                analysis.setAnalyzer(analysisExecutor, analyzer)
            } else {
                analysis.clearAnalyzer()
            }
        }
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
                        mediaStoreManager.cropPhotoUri(savedUri, _configuration.value.aspectRatio)
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
                    CameraDiagnosticsLogger.getInstance(context).e("ImageCapture", "CAPTURE_FAILED", exception.message ?: "", exception)
                    onError(exception)
                }
            }
        )
    }

    override fun capturePhotoToFile(
        outputFile: java.io.File,
        onSuccess: (java.io.File) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val imgCap = imageCapture
        if (imgCap == null) {
            val err = IllegalStateException("Camera capture pipeline not initialized")
            CameraDiagnosticsLogger.getInstance(context).e("ImageCapture", "BURST_UNINITIALIZED", err.message ?: "", err)
            onError(err)
            return
        }

        if (!_sessionState.value.canCapturePhoto) {
            val err = IllegalStateException("Camera is not in ready state (${_sessionState.value})")
            CameraDiagnosticsLogger.getInstance(context).w("ImageCapture", "BURST_SESSION_BUSY", err.message ?: "")
            onError(err)
            return
        }

        _sessionState.value = CameraSessionState.CAPTURING
        val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
        val executor = ContextCompat.getMainExecutor(context)

        imgCap.takePicture(
            outputOptions,
            executor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    _sessionState.value = CameraSessionState.READY
                    CameraDiagnosticsLogger.getInstance(context).i("ImageCapture", "BURST_FRAME_SAVED", "Burst frame saved to cache: ${outputFile.name} (${outputFile.length()} bytes)")
                    onSuccess(outputFile)
                }

                override fun onError(exception: ImageCaptureException) {
                    _sessionState.value = CameraSessionState.READY
                    CameraDiagnosticsLogger.getInstance(context).e("ImageCapture", "BURST_FRAME_ERROR", exception.message ?: "", exception)
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

        if (getAvailableStorageBytes() < 100L * 1024L * 1024L) {
            onError(IllegalStateException("Insufficient storage space (< 100MB)"))
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
                is VideoRecordEvent.Pause -> {
                    // Recording paused
                }
                is VideoRecordEvent.Resume -> {
                    // Recording resumed
                }
                is VideoRecordEvent.Status -> {
                    if (getAvailableStorageBytes() < 25L * 1024L * 1024L) {
                        Log.w(TAG, "Low storage boundary reached during recording, stopping safely")
                        stopVideoRecording()
                    }
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

    override fun pauseVideoRecording() {
        val recording = activeRecording
        if (recording != null) {
            try {
                recording.pause()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to pause video recording", e)
            }
        }
    }

    override fun resumeVideoRecording() {
        val recording = activeRecording
        if (recording != null) {
            try {
                recording.resume()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume video recording", e)
            }
        }
    }

    override fun updateVideoConfig(config: VideoConfig) {
        val resolutionChanged = videoConfig.resolution != config.resolution
        val bitrateChanged = videoConfig.targetBitrateBps != config.targetBitrateBps
        videoConfig = config

        val cam = camera
        if (cam != null && _configuration.value.mode.isVideoMode) {
            if (_sessionState.value != CameraSessionState.RECORDING) {
                if (resolutionChanged || bitrateChanged) {
                    rebindInternal()
                } else {
                    applyVideoCamera2Controls(cam)
                }
            }
        }
    }

    private fun applyVideoCamera2Controls(cam: Camera) {
        try {
            val camera2Control = androidx.camera.camera2.interop.Camera2CameraControl.from(cam.cameraControl)
            val builder = androidx.camera.camera2.interop.CaptureRequestOptions.Builder()

            // 1. Stabilization Options
            val stabOptions = VideoStabilizationCoordinator.createCaptureRequestOptions(
                videoConfig.stabilizationMode
            )
            camera2Control.setCaptureRequestOptions(stabOptions)

            // 2. Exposure & Shutter Angle
            if (videoConfig.exposureMode != VideoExposureMode.AUTO) {
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_OFF
                )

                val expNanos = if (videoConfig.manualShutterSpeedSeconds != null) {
                    (videoConfig.manualShutterSpeedSeconds!! * 1_000_000_000.0).toLong()
                } else if (videoConfig.shutterAngle != null) {
                    ShutterAngleCalculator.calculateExposureNanoseconds(
                        fps = videoConfig.frameRate.fps,
                        shutterAngle = videoConfig.shutterAngle!!
                    )
                } else {
                    16_666_666L // 1/60s default
                }

                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.SENSOR_EXPOSURE_TIME,
                    expNanos
                )

                val iso = videoConfig.manualIso ?: 100
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.SENSOR_SENSITIVITY,
                    iso
                )
            } else {
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_AE_MODE_ON
                )
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    videoConfig.exposureCompensationIndex
                )
            }

            // 3. Manual Kelvin White Balance
            if (videoConfig.isKelvinManual) {
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_OFF
                )
                val gains = com.bishal.adaptivecamera.photography.pro.KelvinConverter.kelvinToRgbGains(videoConfig.manualKelvin)
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_MODE,
                    android.hardware.camera2.CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                )
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.COLOR_CORRECTION_GAINS,
                    gains.toCamera2Gains()
                )
            } else {
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AWB_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_AWB_MODE_AUTO
                )
            }

            // 4. Manual Focus
            if (videoConfig.isFocusManual) {
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.LENS_FOCUS_DISTANCE,
                    videoConfig.manualFocusDistanceDiopters
                )
            } else {
                builder.setCaptureRequestOption(
                    android.hardware.camera2.CaptureRequest.CONTROL_AF_MODE,
                    android.hardware.camera2.CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }

            camera2Control.setCaptureRequestOptions(builder.build())

        } catch (e: Exception) {
            Log.w(TAG, "Failed applying Video Camera2 controls", e)
        }
    }

    private fun getAvailableStorageBytes(): Long {
        return try {
            val stat = android.os.StatFs(android.os.Environment.getExternalStorageDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
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
