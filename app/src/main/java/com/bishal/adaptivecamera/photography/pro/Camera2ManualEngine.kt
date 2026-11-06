package com.bishal.adaptivecamera.photography.pro

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepository
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepositoryImpl
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraFacing
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.camera.engine.CameraEngine
import com.bishal.adaptivecamera.camera.session.CameraSessionState
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.photography.raw.DngCaptureManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

class Camera2ManualEngine(
    private val context: Context,
    private val capabilityRepository: CameraCapabilityRepository = CameraCapabilityRepositoryImpl(context),
    private val mediaStoreManager: MediaStoreManager = MediaStoreManager(context),
    private val dngCaptureManager: DngCaptureManager = DngCaptureManager(context)
) : CameraEngine {

    companion object {
        private const val TAG = "Camera2ManualEngine"
    }

    private val engineScope = CoroutineScope(Dispatchers.Main + Job())
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private var backgroundThread: HandlerThread? = null
    private var backgroundHandler: Handler? = null

    private val _sessionState = MutableStateFlow(CameraSessionState.CLOSED)
    override val sessionState: StateFlow<CameraSessionState> = _sessionState.asStateFlow()

    private val _configuration = MutableStateFlow(CameraConfiguration(mode = CameraMode.PRO))
    override val configuration: StateFlow<CameraConfiguration> = _configuration.asStateFlow()

    private val _recordingDurationSec = MutableStateFlow(0L)
    override val recordingDurationSec: StateFlow<Long> = _recordingDurationSec.asStateFlow()

    private val _proConfig = MutableStateFlow(ProConfiguration())
    val proConfig: StateFlow<ProConfiguration> = _proConfig.asStateFlow()

    private val _appliedState = MutableStateFlow(ProAppliedState())
    val appliedState: StateFlow<ProAppliedState> = _appliedState.asStateFlow()

    private val _histogramData = MutableStateFlow(HistogramData())
    val histogramData: StateFlow<HistogramData> = _histogramData.asStateFlow()

    private val _focusPeakingMap = MutableStateFlow(FocusPeakingMap())
    val focusPeakingMap: StateFlow<FocusPeakingMap> = _focusPeakingMap.asStateFlow()

    private val _zebraMap = MutableStateFlow(ZebraStripingMap())
    val zebraMap: StateFlow<ZebraStripingMap> = _zebraMap.asStateFlow()

    private val _falseColorMap = MutableStateFlow(FalseColorMap())
    val falseColorMap: StateFlow<FalseColorMap> = _falseColorMap.asStateFlow()

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var currentCharacteristics: CameraCharacteristics? = null
    private var activeCameraId: String = "0"

    private var previewSurface: Surface? = null
    private var jpegImageReader: ImageReader? = null
    private var rawImageReader: ImageReader? = null
    private var analysisImageReader: ImageReader? = null

    private var previewRequestBuilder: CaptureRequest.Builder? = null
    private var currentPreviewView: PreviewView? = null

    private fun startBackgroundThread() {
        if (backgroundThread == null) {
            backgroundThread = HandlerThread("Camera2ProBackground").apply {
                start()
                backgroundHandler = Handler(looper)
            }
        }
    }

    private fun stopBackgroundThread() {
        backgroundThread?.quitSafely()
        try {
            backgroundThread?.join()
            backgroundThread = null
            backgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e(TAG, "Error stopping background thread", e)
        }
    }

    override fun bindCamera(lifecycleOwner: LifecycleOwner, previewView: PreviewView) {
        currentPreviewView = previewView
        startBackgroundThread()
        openActiveCamera()
    }

    fun bindSurface(surface: Surface) {
        this.previewSurface = surface
        startBackgroundThread()
        openActiveCamera()
    }

    @SuppressLint("MissingPermission")
    private fun openActiveCamera() {
        _sessionState.value = CameraSessionState.OPENING
        try {
            val facing = _configuration.value.facing
            val targetFacing = if (facing == CameraFacing.FRONT) {
                CameraMetadata.LENS_FACING_FRONT
            } else {
                CameraMetadata.LENS_FACING_BACK
            }

            // Find matching camera ID
            val cameraIdList = cameraManager.cameraIdList
            var selectedId = cameraIdList.firstOrNull() ?: "0"
            for (id in cameraIdList) {
                val chars = cameraManager.getCameraCharacteristics(id)
                val f = chars.get(CameraCharacteristics.LENS_FACING)
                if (f == targetFacing) {
                    selectedId = id
                    break
                }
            }

            activeCameraId = selectedId
            currentCharacteristics = cameraManager.getCameraCharacteristics(activeCameraId)

            // Reset manual settings for newly opened camera to avoid stale ranges
            resetManualSettingsForCamera(currentCharacteristics!!)

            cameraManager.openCamera(activeCameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    setupCaptureSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    cameraDevice = null
                    _sessionState.value = CameraSessionState.CLOSED
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    cameraDevice = null
                    _sessionState.value = CameraSessionState.ERROR
                    Log.e(TAG, "CameraDevice error code: $error")
                }
            }, backgroundHandler)
        } catch (e: Exception) {
            Log.e(TAG, "Error opening Camera2 device", e)
            _sessionState.value = CameraSessionState.ERROR
        }
    }

    private fun resetManualSettingsForCamera(characteristics: CameraCharacteristics) {
        val isoRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
        val expTimeRange = characteristics.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)
        val minFocus = characteristics.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f

        _proConfig.update { cur ->
            cur.copy(
                requestedIso = isoRange?.lower ?: 100,
                requestedShutterSpeedNs = expTimeRange?.let {
                    val defaultShutter = 16_666_666L // 1/60s
                    min(it.upper, max(it.lower, defaultShutter))
                } ?: 16_666_666L,
                requestedFocusDistanceDiopters = minFocus / 2.0f
            )
        }
    }

    private fun setupCaptureSession() {
        val device = cameraDevice ?: return
        val chars = currentCharacteristics ?: return

        try {
            val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) ?: return

            // 1. JPEG ImageReader (Max still resolution)
            val jpegSizes = streamMap.getOutputSizes(ImageFormat.JPEG) ?: emptyArray()
            val maxJpegSize = jpegSizes.maxByOrNull { it.width * it.height } ?: android.util.Size(1920, 1080)
            jpegImageReader = ImageReader.newInstance(maxJpegSize.width, maxJpegSize.height, ImageFormat.JPEG, 2)

            // 2. RAW ImageReader (Max RAW resolution, if eligible)
            val rawEligible = dngCaptureManager.checkRawEligibility(chars)
            val surfaces = mutableListOf<Surface>()
            surfaces.add(jpegImageReader!!.surface)

            if (rawEligible.isEligible) {
                val rawSizes = streamMap.getOutputSizes(ImageFormat.RAW_SENSOR)
                val maxRawSize = rawSizes?.maxByOrNull { it.width * it.height }
                if (maxRawSize != null) {
                    rawImageReader = ImageReader.newInstance(maxRawSize.width, maxRawSize.height, ImageFormat.RAW_SENSOR, 2)
                    surfaces.add(rawImageReader!!.surface)
                }
            }

            // 3. Analysis ImageReader (YUV_420_888 for histograms and focus peaking)
            val yuvSizes = streamMap.getOutputSizes(ImageFormat.YUV_420_888) ?: emptyArray()
            val analysisSize = yuvSizes.filter { it.width <= 1280 }.maxByOrNull { it.width * it.height } ?: android.util.Size(640, 480)
            analysisImageReader = ImageReader.newInstance(analysisSize.width, analysisSize.height, ImageFormat.YUV_420_888, 2).apply {
                setOnImageAvailableListener({ reader ->
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        processAnalysisImage(image)
                    } catch (_: Exception) {
                    } finally {
                        image.close()
                    }
                }, backgroundHandler)
            }
            surfaces.add(analysisImageReader!!.surface)

            // 4. Preview Surface
            val previewView = currentPreviewView
            if (previewView != null && previewSurface == null) {
                // If using PreviewView, use its surface provider / texture
                // On SurfaceView/TextureView, attach surface
            }

            previewSurface?.let { surfaces.add(it) }

            device.createCaptureSession(surfaces, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    _sessionState.value = CameraSessionState.READY
                    applyRepeatingProRequest()
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    _sessionState.value = CameraSessionState.ERROR
                    Log.e(TAG, "CameraCaptureSession configuration failed")
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring CameraCaptureSession", e)
            _sessionState.value = CameraSessionState.ERROR
        }
    }

    private fun processAnalysisImage(image: android.media.Image) {
        val yPlane = image.planes[0]
        val uPlane = if (image.planes.size > 1) image.planes[1] else null
        val vPlane = if (image.planes.size > 2) image.planes[2] else null

        val config = _proConfig.value

        // Execute unified photometry analysis (Histogram, Zebra, False Color)
        val photometryResult = PhotometryAnalyzer.analyzeYuv(
            yBuffer = yPlane.buffer,
            uBuffer = uPlane?.buffer,
            vBuffer = vPlane?.buffer,
            width = image.width,
            height = image.height,
            yRowStride = yPlane.rowStride,
            yPixelStride = yPlane.pixelStride,
            uvRowStride = uPlane?.rowStride ?: 0,
            uvPixelStride = uPlane?.pixelStride ?: 1,
            isHistogramEnabled = config.isHistogramVisible,
            isZebraEnabled = config.isZebraEnabled,
            zebraThresholdIre = config.zebraThresholdIre,
            isFalseColorEnabled = config.isFalseColorEnabled
        )

        if (config.isHistogramVisible) {
            _histogramData.value = photometryResult.histogram
        }
        if (config.isZebraEnabled) {
            _zebraMap.value = photometryResult.zebraMap
        }
        if (config.isFalseColorEnabled) {
            _falseColorMap.value = photometryResult.falseColorMap
        }

        // Calculate Focus Peaking edges
        if (config.isFocusPeakingEnabled) {
            val peaking = FocusPeakingAnalyzer.analyzeY(
                yBuffer = yPlane.buffer,
                width = image.width,
                height = image.height,
                rowStride = yPlane.rowStride,
                pixelStride = yPlane.pixelStride,
                sensitivity = config.focusPeakingSensitivity
            )
            _focusPeakingMap.value = peaking
        }
    }

    private fun applyRepeatingProRequest() {
        val session = captureSession ?: return
        val device = cameraDevice ?: return
        val chars = currentCharacteristics ?: return

        try {
            val requestBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            previewSurface?.let { requestBuilder.addTarget(it) }
            analysisImageReader?.surface?.let { requestBuilder.addTarget(it) }

            val config = _proConfig.value

            // 1. ISO & Exposure Time (Manual Sensor)
            val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
            val expTimeRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

            if (config.isManualIso || config.isManualShutter) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_OFF)

                if (config.isManualIso && isoRange != null) {
                    val clampedIso = min(isoRange.upper, max(isoRange.lower, config.requestedIso))
                    requestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, clampedIso)
                }

                if (config.isManualShutter && expTimeRange != null) {
                    val clampedShutter = min(expTimeRange.upper, max(expTimeRange.lower, config.requestedShutterSpeedNs))
                    requestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, clampedShutter)
                }
            } else {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON)
                requestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, config.exposureCompensationIndex)
                requestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, config.isAeLocked)
            }

            // 2. Focus Distance (Manual Focus & AF Lock)
            val minFocus = chars.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) ?: 0f
            if (config.isManualFocus && minFocus > 0f) {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_OFF)
                val clampedFocus = min(minFocus, max(0.0f, config.requestedFocusDistanceDiopters))
                requestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, clampedFocus)
            } else if (config.isAfLocked) {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO)
            } else {
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
            }

            // 3. White Balance (Presets, Custom Kelvin, & AWB Lock)
            if (config.isManualKelvin || config.whiteBalance == ProWhiteBalance.MANUAL_KELVIN) {
                requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CameraMetadata.CONTROL_AWB_MODE_OFF)
                requestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX)
                val gains = KelvinConverter.kelvinToRgbGains(config.requestedKelvin, config.requestedTint)
                requestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, gains.toCamera2Gains())
            } else {
                requestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, config.whiteBalance.camera2Mode)
                requestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, config.isAwbLocked)
            }

            previewRequestBuilder = requestBuilder

            session.setRepeatingRequest(requestBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    val appliedIso = result.get(CaptureResult.SENSOR_SENSITIVITY)
                    val appliedShutter = result.get(CaptureResult.SENSOR_EXPOSURE_TIME)
                    val appliedFocus = result.get(CaptureResult.LENS_FOCUS_DISTANCE)
                    val appliedAwb = result.get(CaptureResult.CONTROL_AWB_MODE)
                    val appliedEv = result.get(CaptureResult.CONTROL_AE_EXPOSURE_COMPENSATION)
                    val aperture = result.get(CaptureResult.LENS_APERTURE)
                    val focalLen = result.get(CaptureResult.LENS_FOCAL_LENGTH)
                    val appliedGains = result.get(CaptureResult.COLOR_CORRECTION_GAINS)
                    val appliedKelvin = if (config.isManualKelvin || config.whiteBalance == ProWhiteBalance.MANUAL_KELVIN) {
                        config.requestedKelvin
                    } else {
                        appliedGains?.let { KelvinConverter.rgbGainsToKelvin(it.red, it.blue) } ?: config.whiteBalance.kelvinApprox
                    }

                    _appliedState.update {
                        it.copy(
                            appliedIso = appliedIso,
                            appliedShutterSpeedNs = appliedShutter,
                            appliedFocusDistanceDiopters = appliedFocus,
                            appliedAwbMode = appliedAwb,
                            appliedExposureCompensation = appliedEv,
                            aperture = aperture,
                            focalLength = focalLen,
                            isManualSensorActive = config.isManualIso || config.isManualShutter,
                            isManualFocusActive = config.isManualFocus,
                            isAeLocked = config.isAeLocked,
                            isAfLocked = config.isAfLocked,
                            isAwbLocked = config.isAwbLocked,
                            appliedKelvin = appliedKelvin
                        )
                    }
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            Log.e(TAG, "Error applying Pro repeating request", e)
        }
    }

    override fun capturePhoto(onSuccess: (MediaItem) -> Unit, onError: (Throwable) -> Unit) {
        val session = captureSession ?: run {
            onError(IllegalStateException("Pro session not ready"))
            return
        }
        val device = cameraDevice ?: run {
            onError(IllegalStateException("Pro camera device not open"))
            return
        }
        val chars = currentCharacteristics ?: run {
            onError(IllegalStateException("Characteristics unavailable"))
            return
        }

        _sessionState.value = CameraSessionState.CAPTURING

        try {
            val captureBuilder = device.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            captureBuilder.addTarget(jpegImageReader!!.surface)

            val isRawRequested = _proConfig.value.isRawCaptureEnabled && rawImageReader != null
            if (isRawRequested) {
                captureBuilder.addTarget(rawImageReader!!.surface)
            }

            // Copy manual parameters
            previewRequestBuilder?.let { p ->
                p.get(CaptureRequest.SENSOR_SENSITIVITY)?.let { captureBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, it) }
                p.get(CaptureRequest.SENSOR_EXPOSURE_TIME)?.let { captureBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, it) }
                p.get(CaptureRequest.CONTROL_AE_MODE)?.let { captureBuilder.set(CaptureRequest.CONTROL_AE_MODE, it) }
                p.get(CaptureRequest.LENS_FOCUS_DISTANCE)?.let { captureBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, it) }
                p.get(CaptureRequest.CONTROL_AF_MODE)?.let { captureBuilder.set(CaptureRequest.CONTROL_AF_MODE, it) }
                p.get(CaptureRequest.CONTROL_AWB_MODE)?.let { captureBuilder.set(CaptureRequest.CONTROL_AWB_MODE, it) }
            }

            // Set up JPEG listener
            jpegImageReader?.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                engineScope.launch {
                    val saved = saveJpegImage(image)
                    image.close()
                    _sessionState.value = CameraSessionState.READY
                    saved.fold(onSuccess = onSuccess, onFailure = onError)
                }
            }, backgroundHandler)

            // Set up RAW listener if requested
            if (isRawRequested) {
                rawImageReader?.setOnImageAvailableListener({ reader ->
                    val rawImg = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    // DngCreator handled in session capture completed
                }, backgroundHandler)
            }

            session.capture(captureBuilder.build(), object : CameraCaptureSession.CaptureCallback() {
                override fun onCaptureCompleted(
                    session: CameraCaptureSession,
                    request: CaptureRequest,
                    result: TotalCaptureResult
                ) {
                    if (isRawRequested) {
                        val rawImg = rawImageReader?.acquireLatestImage()
                        if (rawImg != null) {
                            engineScope.launch {
                                dngCaptureManager.saveDngImage(
                                    rawImage = rawImg,
                                    characteristics = chars,
                                    captureResult = result
                                )
                            }
                        }
                    }
                    _sessionState.value = CameraSessionState.READY
                }
            }, backgroundHandler)

        } catch (e: Exception) {
            _sessionState.value = CameraSessionState.READY
            onError(e)
        }
    }

    private suspend fun saveJpegImage(image: android.media.Image): Result<MediaItem> = withContext(Dispatchers.IO) {
        try {
            val buffer = image.planes[0].buffer
            val bytes = ByteArray(buffer.remaining())
            buffer.get(bytes)

            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val filename = "IMG_${timeStamp}.jpg"
            val croppedBytes = mediaStoreManager.cropJpegBytes(bytes, _configuration.value.aspectRatio)
            val item = mediaStoreManager.saveImageBytes(croppedBytes, filename)
            Result.success(item)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Manual Pro Control Modifiers
    fun setManualIso(enabled: Boolean, iso: Int = 100) {
        _proConfig.update { it.copy(isManualIso = enabled, requestedIso = iso) }
        applyRepeatingProRequest()
    }

    fun setManualShutterSpeed(enabled: Boolean, shutterNs: Long = 16_666_666L) {
        _proConfig.update { it.copy(isManualShutter = enabled, requestedShutterSpeedNs = shutterNs) }
        applyRepeatingProRequest()
    }

    fun setManualFocus(enabled: Boolean, distanceDiopters: Float = 0.0f) {
        _proConfig.update { it.copy(isManualFocus = enabled, requestedFocusDistanceDiopters = distanceDiopters) }
        applyRepeatingProRequest()
    }

    fun setWhiteBalance(mode: ProWhiteBalance) {
        _proConfig.update { it.copy(whiteBalance = mode) }
        applyRepeatingProRequest()
    }

    fun setRawCaptureEnabled(enabled: Boolean) {
        _proConfig.update { it.copy(isRawCaptureEnabled = enabled) }
    }

    fun setHistogramVisible(visible: Boolean) {
        _proConfig.update { it.copy(isHistogramVisible = visible) }
    }

    fun setHistogramExpanded(expanded: Boolean) {
        _proConfig.update { it.copy(isHistogramExpanded = expanded) }
    }

    fun setFocusPeakingEnabled(enabled: Boolean) {
        _proConfig.update { it.copy(isFocusPeakingEnabled = enabled) }
    }

    fun setFocusPeakingSensitivity(sensitivity: Float) {
        _proConfig.update { it.copy(focusPeakingSensitivity = sensitivity) }
    }

    // Phase 6 Pro Exposure & Monitoring Setters
    fun setAeLock(locked: Boolean) {
        _proConfig.update { it.copy(isAeLocked = locked) }
        applyRepeatingProRequest()
    }

    fun setAfLock(locked: Boolean) {
        _proConfig.update { it.copy(isAfLocked = locked) }
        applyRepeatingProRequest()
    }

    fun setAwbLock(locked: Boolean) {
        _proConfig.update { it.copy(isAwbLocked = locked) }
        applyRepeatingProRequest()
    }

    fun setManualKelvin(enabled: Boolean, kelvin: Int = KelvinConverter.DEFAULT_KELVIN, tint: Int = KelvinConverter.DEFAULT_TINT) {
        _proConfig.update {
            it.copy(
                isManualKelvin = enabled,
                requestedKelvin = kelvin.coerceIn(KelvinConverter.MIN_KELVIN, KelvinConverter.MAX_KELVIN),
                requestedTint = tint.coerceIn(KelvinConverter.MIN_TINT, KelvinConverter.MAX_TINT),
                whiteBalance = if (enabled) ProWhiteBalance.MANUAL_KELVIN else it.whiteBalance
            )
        }
        applyRepeatingProRequest()
    }

    fun setZebraStriping(enabled: Boolean, thresholdIre: Int = 90) {
        _proConfig.update {
            it.copy(
                isZebraEnabled = enabled,
                zebraThresholdIre = thresholdIre.coerceIn(50, 100)
            )
        }
    }

    fun setFalseColor(enabled: Boolean) {
        _proConfig.update { it.copy(isFalseColorEnabled = enabled) }
    }

    fun setFocusMagnifier(enabled: Boolean, scale: Float = 2.0f) {
        _proConfig.update {
            it.copy(
                isFocusMagnifierEnabled = enabled,
                focusMagnifierScale = scale.coerceIn(1.5f, 5.0f)
            )
        }
    }

    fun resetAllManualToAuto() {
        _proConfig.update {
            it.copy(
                isManualIso = false,
                isManualShutter = false,
                isManualFocus = false,
                whiteBalance = ProWhiteBalance.AUTO,
                isManualKelvin = false,
                requestedKelvin = KelvinConverter.DEFAULT_KELVIN,
                requestedTint = KelvinConverter.DEFAULT_TINT,
                isAeLocked = false,
                isAfLocked = false,
                isAwbLocked = false,
                exposureCompensationIndex = 0,
                isFocusMagnifierEnabled = false
            )
        }
        applyRepeatingProRequest()
    }

    override fun unbindCamera() {
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
            jpegImageReader?.close()
            rawImageReader?.close()
            analysisImageReader?.close()
            _sessionState.value = CameraSessionState.CLOSED
        } catch (e: Exception) {
            Log.e(TAG, "Error unbinding Camera2ProEngine", e)
        } finally {
            stopBackgroundThread()
        }
    }

    override fun switchFacing() {
        unbindCamera()
        _configuration.update { it.copy(facing = it.facing.toggle()) }
        openActiveCamera()
    }

    override fun setFacing(facing: CameraFacing) {
        if (_configuration.value.facing != facing) {
            unbindCamera()
            _configuration.update { it.copy(facing = facing) }
            openActiveCamera()
        }
    }

    override fun setMode(mode: CameraMode) {
        _configuration.update { it.copy(mode = mode) }
    }

    override fun setFlashMode(flashMode: FlashMode) {
        _configuration.update { it.copy(flashMode = flashMode) }
    }

    override fun setTimer(timer: PhotoTimer) {
        _configuration.update { it.copy(timer = timer) }
    }

    override fun setAspectRatio(aspectRatio: com.bishal.adaptivecamera.camera.config.AspectRatioConfig) {
        _configuration.update { it.copy(aspectRatio = aspectRatio) }
    }

    override fun setZoomRatio(ratio: Float) {
        _configuration.update { it.copy(zoomRatio = ratio) }
        applyRepeatingProRequest()
    }

    override fun setLinearZoom(percentage: Float) {}

    override fun setExposureCompensation(index: Int) {
        _proConfig.update { it.copy(exposureCompensationIndex = index) }
        applyRepeatingProRequest()
    }

    override fun resetExposureCompensation() {
        _proConfig.update { it.copy(exposureCompensationIndex = 0) }
        applyRepeatingProRequest()
    }

    override fun focusAndMeter(xNorm: Float, yNorm: Float) {}

    override fun setImageAnalyzer(analyzer: androidx.camera.core.ImageAnalysis.Analyzer?) {}

    override fun startVideoRecording(
        withAudio: Boolean,
        onStarted: () -> Unit,
        onFinalized: (MediaItem?) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        onError(UnsupportedOperationException("Video recording is not supported in Pro Photography engine"))
    }

    override fun stopVideoRecording() {}
}
