package com.bishal.adaptivecamera.ui.camera

import java.io.File
import com.bishal.adaptivecamera.core.diagnostics.CameraDiagnosticsLogger
import android.app.Application
import android.content.Context
import android.graphics.Bitmap
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
import com.bishal.adaptivecamera.ui.theme.AppThemeMode
import com.bishal.adaptivecamera.photography.pro.Camera2ManualEngine
import com.bishal.adaptivecamera.photography.pro.FalseColorMap
import com.bishal.adaptivecamera.photography.pro.FocusPeakingMap
import com.bishal.adaptivecamera.photography.pro.HistogramData
import android.graphics.SurfaceTexture
import com.bishal.adaptivecamera.camera.config.HdrPlusMode
import com.bishal.adaptivecamera.graphics.pipeline.GpuColorConfig
import com.bishal.adaptivecamera.graphics.pipeline.GpuPhotoProcessor
import com.bishal.adaptivecamera.graphics.pipeline.GpuPreviewRenderer
import com.bishal.adaptivecamera.lut.model.LutDefinition
import com.bishal.adaptivecamera.lut.repository.LutRepository
import com.bishal.adaptivecamera.lut.repository.LutRepositoryImpl
import com.bishal.adaptivecamera.photography.computational.ComputationalPhotographyCoordinator
import com.bishal.adaptivecamera.photography.computational.ImageStackingConfig
import com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount
import com.bishal.adaptivecamera.photography.computational.ImageStackingMode
import com.bishal.adaptivecamera.photography.portrait.PortraitAperture
import com.bishal.adaptivecamera.photography.portrait.PortraitBokehRenderer
import com.bishal.adaptivecamera.photography.portrait.PortraitConfig
import com.bishal.adaptivecamera.photography.portrait.PortraitLightingStyle
import com.bishal.adaptivecamera.photography.pro.KelvinConverter
import com.bishal.adaptivecamera.photography.pro.ProAppliedState
import com.bishal.adaptivecamera.photography.pro.ProConfiguration
import com.bishal.adaptivecamera.core.diagnostics.ThermalMitigationPolicy
import com.bishal.adaptivecamera.core.diagnostics.ThermalMonitor
import com.bishal.adaptivecamera.photography.exif.ExifMetadataHelper
import com.bishal.adaptivecamera.photography.pro.ProWhiteBalance
import com.bishal.adaptivecamera.photography.pro.ZebraStripingMap
import com.bishal.adaptivecamera.video.audio.AudioRecordingManager
import com.bishal.adaptivecamera.video.audio.AudioVuState
import com.bishal.adaptivecamera.video.config.VideoConfig
import com.bishal.adaptivecamera.video.recording.VideoRecordingState
import com.bishal.adaptivecamera.video.recording.VideoRecordingStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val isScreenFlashActive: Boolean = false,
    val gpuColorConfig: GpuColorConfig = GpuColorConfig.DEFAULT,
    val isLookControlPanelVisible: Boolean = false,
    val isHdrPlusProcessing: Boolean = false,
    val isStackingProcessing: Boolean = false,
    val videoConfig: VideoConfig = VideoConfig(),
    val videoRecordingState: VideoRecordingState = VideoRecordingState(),
    val isVideoQuickSettingsVisible: Boolean = false,
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val portraitConfig: PortraitConfig = PortraitConfig(),
    val imageStackingConfig: ImageStackingConfig = ImageStackingConfig(),
    val isLeftHandedModeEnabled: Boolean = false,
    val thermalMitigationPolicy: ThermalMitigationPolicy = ThermalMitigationPolicy()
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
    val virtualHorizonSensor: VirtualHorizonSensor = VirtualHorizonSensorImpl(application.applicationContext),
    private val lutRepository: LutRepository = LutRepositoryImpl(application.applicationContext),
    val gpuPreviewRenderer: GpuPreviewRenderer = GpuPreviewRenderer(),
    private val gpuPhotoProcessor: GpuPhotoProcessor = GpuPhotoProcessor(),
    private val computationalCoordinator: ComputationalPhotographyCoordinator = ComputationalPhotographyCoordinator(application.applicationContext, mediaStoreManager)
) : AndroidViewModel(application) {

    val thermalMonitor = ThermalMonitor(application.applicationContext, viewModelScope)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    val allLutsFlow: kotlinx.coroutines.flow.Flow<List<com.bishal.adaptivecamera.lut.repository.LutItem>> = lutRepository.allLutsFlow

    val audioRecordingManager = AudioRecordingManager(application.applicationContext, viewModelScope)
    val audioVuState: StateFlow<AudioVuState> = audioRecordingManager.vuState
    val availableAudioInputs: StateFlow<List<com.bishal.adaptivecamera.video.audio.AudioInputDevice>> = audioRecordingManager.availableAudioInputs
    val selectedAudioInput: StateFlow<com.bishal.adaptivecamera.video.audio.AudioInputDevice?> = audioRecordingManager.selectedAudioInput

    private val directLensSelector = com.bishal.adaptivecamera.camera.lens.DirectLensSelector(application.applicationContext)
    val availableLenses = MutableStateFlow<List<com.bishal.adaptivecamera.camera.lens.DirectPhysicalLens>>(emptyList())
    val selectedLensId = MutableStateFlow<String?>(null)

    fun selectAudioInput(device: com.bishal.adaptivecamera.video.audio.AudioInputDevice?) {
        audioRecordingManager.selectAudioDevice(device)
    }

    fun selectDirectLens(lens: com.bishal.adaptivecamera.camera.lens.DirectPhysicalLens) {
        val targetId = if (lens.isPhysicalSubCamera) lens.cameraId else null
        selectedLensId.value = targetId
        camera2ProEngine.selectDirectCameraId(targetId)
        if (lens.type == com.bishal.adaptivecamera.camera.lens.PhysicalLensType.ULTRA_WIDE) {
            setZoomRatio(_uiState.value.configuration.minZoomRatio)
        } else if (lens.type == com.bishal.adaptivecamera.camera.lens.PhysicalLensType.WIDE_MAIN) {
            setZoomRatio(1.0f)
        }
    }

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
        viewModelScope.launch(Dispatchers.IO) {
            availableLenses.value = directLensSelector.discoverAvailableLenses()
        }

        // Connect frame analyzer to camera engine
        cameraEngine.setImageAnalyzer(smartAutoOrchestrator.frameAnalyzer)

        // Observe active LUT selection from repository
        viewModelScope.launch {
            lutRepository.selectedLutIdFlow.collect { lutId ->
                if (lutId != null) {
                    _uiState.update { current ->
                        current.copy(
                            gpuColorConfig = current.gpuColorConfig.copy(
                                selectedLutId = lutId,
                                lutEnabled = true
                            )
                        )
                    }
                }
            }
        }

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
                _uiState.update {
                    it.copy(
                        recordingDurationSec = sec,
                        videoRecordingState = it.videoRecordingState.copy(durationMs = sec * 1000L)
                    )
                }
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
                val capFormat = try {
                    com.bishal.adaptivecamera.photography.capture.CaptureFormat.valueOf(prefs.captureFormat)
                } catch (_: Exception) {
                    com.bishal.adaptivecamera.photography.capture.CaptureFormat.JPEG
                }
                _uiState.update { current ->
                    current.copy(
                        configuration = current.configuration.copy(
                            gridType = prefs.gridType,
                            isVirtualHorizonEnabled = prefs.isVirtualHorizonEnabled,
                            aspectRatio = prefs.aspectRatio,
                            isHapticsEnabled = prefs.isHapticsEnabled,
                            isFloatingShutterEnabled = prefs.isFloatingShutterEnabled,
                            floatingShutterX = prefs.floatingShutterX,
                            floatingShutterY = prefs.floatingShutterY,
                            isZslEnabled = prefs.isZslEnabled,
                            aebBracketCount = prefs.aebBracketCount,
                            aebEvStep = prefs.aebEvStep,
                            hdrPlusMode = prefs.hdrPlusMode
                        ),
                        gpuColorConfig = current.gpuColorConfig.copy(
                            selectedLutId = prefs.selectedLutId,
                            lutEnabled = prefs.isLutEnabled,
                            lutIntensity = prefs.lutIntensity,
                            grainEnabled = prefs.isGrainEnabled,
                            grainIntensity = prefs.grainIntensity,
                            halationEnabled = prefs.isHalationEnabled,
                            halationIntensity = prefs.halationIntensity
                        ),
                        proConfig = current.proConfig.copy(
                            isZslEnabled = prefs.isZslEnabled,
                            aebBracketCount = prefs.aebBracketCount,
                            aebEvStep = prefs.aebEvStep,
                            captureFormat = capFormat,
                            isRawCaptureEnabled = capFormat.isRaw,
                            isWatermarkEnabled = prefs.isWatermarkEnabled,
                            isExifPrivacyEnabled = prefs.isExifPrivacyEnabled
                        ),
                        themeMode = prefs.themeMode,
                        portraitConfig = current.portraitConfig.copy(
                            aperture = prefs.portraitAperture,
                            lightingStyle = prefs.portraitLightingStyle
                        ),
                        imageStackingConfig = current.imageStackingConfig.copy(
                            isEnabled = prefs.imageStackingEnabled,
                            mode = prefs.imageStackingMode,
                            frameCount = prefs.imageStackingFrameCount,
                            retainOriginalFrames = prefs.retainOriginalFrames
                        ),
                        isLeftHandedModeEnabled = prefs.isLeftHandedModeEnabled
                    )
                }
                cameraEngine.setAspectRatio(prefs.aspectRatio)
                camera2ProEngine.setAspectRatio(prefs.aspectRatio)
                cameraEngine.setZslEnabled(prefs.isZslEnabled)
                camera2ProEngine.setZslEnabled(prefs.isZslEnabled)
                cameraEngine.setAebBracketCount(prefs.aebBracketCount)
                camera2ProEngine.setAebBracketCount(prefs.aebBracketCount)
                cameraEngine.setAebEvStep(prefs.aebEvStep)
                camera2ProEngine.setAebEvStep(prefs.aebEvStep)
                camera2ProEngine.setCaptureFormat(capFormat)
                camera2ProEngine.setWatermarkEnabled(prefs.isWatermarkEnabled)
                camera2ProEngine.setExifPrivacyEnabled(prefs.isExifPrivacyEnabled)
            }
        }

        // Thermal mitigation listener
        viewModelScope.launch {
            thermalMonitor.mitigationPolicy.collect { policy ->
                _uiState.update { it.copy(thermalMitigationPolicy = policy) }
                if (policy.isThrottlingActive) {
                    // Automatically disable intensive analytical overlays if throttling
                    _uiState.update { current ->
                        current.copy(
                            proConfig = current.proConfig.copy(
                                isZebraEnabled = false,
                                isFalseColorEnabled = false,
                                isFocusPeakingEnabled = false
                            )
                        )
                    }
                }
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

        if (state.configuration.mode.isVideoMode) {
            if (state.sessionState == CameraSessionState.RECORDING || state.videoRecordingState.status.isRecordingOrPaused) {
                cameraEngine.stopVideoRecording()
                _uiState.update {
                    it.copy(videoRecordingState = it.videoRecordingState.copy(status = VideoRecordingStatus.FINALIZING))
                }
            } else if (state.sessionState.canStartRecording) {
                _uiState.update {
                    it.copy(videoRecordingState = VideoRecordingState(status = VideoRecordingStatus.PREPARING))
                }
                val recordAudio = state.videoConfig.audioEnabled && state.configuration.mode != CameraMode.TIME_LAPSE
                cameraEngine.startVideoRecording(
                    withAudio = recordAudio,
                    onStarted = {
                        _uiState.update {
                            it.copy(
                                videoRecordingState = VideoRecordingState(status = VideoRecordingStatus.RECORDING)
                            )
                        }
                    },
                    onFinalized = { media ->
                        _uiState.update {
                            it.copy(
                                latestMedia = media ?: it.latestMedia,
                                videoRecordingState = VideoRecordingState(
                                    status = VideoRecordingStatus.COMPLETED,
                                    outputUri = media?.uri
                                ),
                                toastMessage = if (media != null) "Video saved!" else null
                            )
                        }
                    },
                    onError = { error ->
                        _uiState.update {
                            it.copy(
                                videoRecordingState = VideoRecordingState(
                                    status = VideoRecordingStatus.ERROR,
                                    errorMessage = error.message
                                ),
                                toastMessage = "Video error: ${error.message}"
                            )
                        }
                    }
                )
            }
            return
        }

        // Guard: prevent duplicate shutter requests during active computational stacking or HDR+
        if (state.isStackingProcessing || state.isHdrPlusProcessing) {
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
        val currentMode = _uiState.value.configuration.mode
        val stackingConfig = _uiState.value.imageStackingConfig

        if (currentMode == CameraMode.PORTRAIT) {
            triggerFlashAnimation()
            cameraEngine.capturePhoto(
                onSuccess = { item ->
                    processPortraitCapturedPhoto(item)
                },
                onError = { error ->
                    _uiState.update {
                        it.copy(
                            toastMessage = "Portrait capture failed: ${error.message}",
                            isScreenFlashActive = false
                        )
                    }
                }
            )
            return
        }

        if (currentMode == CameraMode.NIGHT) {
            executeNightModeCapture()
            return
        }

        if (currentMode == CameraMode.PHOTO && stackingConfig.isEnabled) {
            executeStackedPhotoCapture()
            return
        }

        val hdrPlusMode = _uiState.value.configuration.hdrPlusMode
        val shouldExecuteHdrPlus = (currentMode == CameraMode.PHOTO) && (
            hdrPlusMode == HdrPlusMode.ON || (hdrPlusMode == HdrPlusMode.AUTO && _uiState.value.smartAutoState.sceneResult != null)
        )
        if (shouldExecuteHdrPlus) {
            executeHdrPlusCapture()
            return
        }

        triggerFlashAnimation()
        cameraEngine.capturePhoto(
            onSuccess = { item ->
                processCapturedPhotoWithGpu(item)
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

    private fun processPortraitCapturedPhoto(item: MediaItem) {
        val portraitConfig = _uiState.value.portraitConfig
        val uri = item.uri
        if (uri != null) {
            viewModelScope.launch(Dispatchers.IO) {
                var originalBitmap: Bitmap? = null
                var renderedBitmap: Bitmap? = null
                var finalBitmap: Bitmap? = null
                try {
                    val context = getApplication<Application>().applicationContext
                    val exifMetadata = context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifMetadataHelper.extractExifMetadata(stream)
                    } ?: emptyMap()

                    val inputStream = context.contentResolver.openInputStream(uri)
                    originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (originalBitmap != null) {
                        renderedBitmap = PortraitBokehRenderer.renderPortrait(originalBitmap, portraitConfig)

                        // Also apply GPU color look if active
                        val gpuConfig = _uiState.value.gpuColorConfig
                        finalBitmap = if (gpuConfig.lutEnabled || gpuConfig.grainEnabled || gpuConfig.halationEnabled) {
                            val activeLut = if (gpuConfig.lutEnabled && gpuConfig.selectedLutId != null) {
                                lutRepository.loadLutDefinition(gpuConfig.selectedLutId)
                            } else null
                            gpuPhotoProcessor.processPhoto(renderedBitmap, gpuConfig, activeLut)
                        } else {
                            renderedBitmap
                        }

                        val tempFile = java.io.File(context.cacheDir, "temp_portrait_${System.currentTimeMillis()}.jpg")
                        try {
                            java.io.FileOutputStream(tempFile).use { fos ->
                                finalBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
                                fos.flush()
                            }
                            ExifMetadataHelper.writeExifMetadata(tempFile, exifMetadata)

                            context.contentResolver.openOutputStream(uri, "wt")?.use { outStream ->
                                tempFile.inputStream().use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                                outStream.flush()
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CameraViewModel", "Error rendering portrait bokeh: ${e.message}", e)
                } finally {
                    if (finalBitmap != null && finalBitmap !== originalBitmap && finalBitmap !== renderedBitmap && !finalBitmap.isRecycled) {
                        finalBitmap.recycle()
                    }
                    if (renderedBitmap != null && renderedBitmap !== originalBitmap && !renderedBitmap.isRecycled) {
                        renderedBitmap.recycle()
                    }
                    if (originalBitmap != null && !originalBitmap.isRecycled) {
                        originalBitmap.recycle()
                    }
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                latestMedia = item,
                                toastMessage = "Portrait saved (${portraitConfig.aperture.label} · ${portraitConfig.lightingStyle.label})",
                                isScreenFlashActive = false
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    latestMedia = item,
                    toastMessage = "Portrait photo saved!",
                    isScreenFlashActive = false
                )
            }
        }
    }

    private fun executeStackedPhotoCapture() {
        if (_uiState.value.isStackingProcessing) return
        triggerFlashAnimation()
        val config = _uiState.value.imageStackingConfig
        val targetCount = config.frameCount.count
        _uiState.update {
            it.copy(
                isStackingProcessing = true,
                toastMessage = "Stacking: Capturing frame 1 of $targetCount..."
            )
        }
        val capturedBitmaps = mutableListOf<Bitmap>()
        val tempFiles = mutableListOf<File>()
        val cacheDir = getApplication<Application>().cacheDir

        fun captureNext(index: Int) {
            val tempFile = File(cacheDir, "stack_frame_${System.currentTimeMillis()}_$index.jpg")
            tempFiles.add(tempFile)

            cameraEngine.capturePhotoToFile(
                outputFile = tempFile,
                onSuccess = { file ->
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) capturedBitmaps.add(bmp)
                    } catch (e: Exception) {
                        CameraDiagnosticsLogger.getInstance(getApplication()).e("ImageStacking", "FRAME_DECODE_FAILED", e.message ?: "", e)
                    }

                    if (capturedBitmaps.size >= targetCount || index >= targetCount) {
                        _uiState.update {
                            it.copy(toastMessage = "Stacking: Aligning & fusing ${capturedBitmaps.size} frames...")
                        }
                        viewModelScope.launch(Dispatchers.Default) {
                            val stackedItem = computationalCoordinator.processAndSaveStackedPhoto(
                                frames = capturedBitmaps,
                                config = config
                            )
                            // Clean up intermediate temp files from cache
                            for (tf in tempFiles) {
                                if (tf.exists()) tf.delete()
                            }
                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        isStackingProcessing = false,
                                        latestMedia = stackedItem ?: it.latestMedia,
                                        toastMessage = "Stacked Photo saved (${config.mode.label} · ${capturedBitmaps.size}f)",
                                        isScreenFlashActive = false
                                    )
                                }
                                CameraDiagnosticsLogger.getInstance(getApplication()).i("ImageStacking", "STACKING_COMPLETED", "Successfully fused ${capturedBitmaps.size} frames into 1 photo")
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(toastMessage = "Stacking: Capturing frame ${index + 1} of $targetCount...")
                        }
                        captureNext(index + 1)
                    }
                },
                onError = { error: Throwable ->
                    CameraDiagnosticsLogger.getInstance(getApplication()).w("ImageStacking", "FRAME_CAPTURE_ERROR", error.message ?: "")
                    if (capturedBitmaps.isNotEmpty()) {
                        _uiState.update {
                            it.copy(toastMessage = "Stacking: Fusing ${capturedBitmaps.size} frames...")
                        }
                        viewModelScope.launch(Dispatchers.Default) {
                            val stackedItem = computationalCoordinator.processAndSaveStackedPhoto(
                                frames = capturedBitmaps,
                                config = config
                            )
                            for (tf in tempFiles) {
                                if (tf.exists()) tf.delete()
                            }
                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        isStackingProcessing = false,
                                        latestMedia = stackedItem ?: it.latestMedia,
                                        toastMessage = "Stacked Photo saved (${capturedBitmaps.size}f)",
                                        isScreenFlashActive = false
                                    )
                                }
                            }
                        }
                    } else {
                        for (tf in tempFiles) {
                            if (tf.exists()) tf.delete()
                        }
                        _uiState.update {
                            it.copy(
                                isStackingProcessing = false,
                                toastMessage = "Stacking capture failed: ${error.message}",
                                isScreenFlashActive = false
                            )
                        }
                    }
                }
            )
        }

        captureNext(1)
    }

    private fun executeNightModeCapture() {
        if (_uiState.value.isStackingProcessing) return
        triggerFlashAnimation()
        val nightConfig = com.bishal.adaptivecamera.photography.computational.ImageStackingConfig(
            isEnabled = true,
            mode = com.bishal.adaptivecamera.photography.computational.ImageStackingMode.LOW_LIGHT,
            frameCount = com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount.FRAMES_5
        )
        val targetCount = nightConfig.frameCount.count
        _uiState.update {
            it.copy(
                isStackingProcessing = true,
                toastMessage = "Night Mode: Capturing exposure 1 of $targetCount..."
            )
        }
        val capturedBitmaps = mutableListOf<Bitmap>()
        val tempFiles = mutableListOf<File>()
        val cacheDir = getApplication<Application>().cacheDir

        fun captureNext(index: Int) {
            val tempFile = File(cacheDir, "night_frame_${System.currentTimeMillis()}_$index.jpg")
            tempFiles.add(tempFile)

            cameraEngine.capturePhotoToFile(
                outputFile = tempFile,
                onSuccess = { file ->
                    try {
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) capturedBitmaps.add(bmp)
                    } catch (e: Exception) {
                        CameraDiagnosticsLogger.getInstance(getApplication()).e("NightMode", "FRAME_DECODE_FAILED", e.message ?: "", e)
                    }

                    if (capturedBitmaps.size >= targetCount || index >= targetCount) {
                        _uiState.update {
                            it.copy(toastMessage = "Night Mode: Synthesizing ${capturedBitmaps.size} frames...")
                        }
                        viewModelScope.launch(Dispatchers.Default) {
                            val stackedItem = computationalCoordinator.processAndSaveStackedPhoto(
                                frames = capturedBitmaps,
                                config = nightConfig
                            )
                            for (tf in tempFiles) {
                                if (tf.exists()) tf.delete()
                            }
                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        isStackingProcessing = false,
                                        latestMedia = stackedItem ?: it.latestMedia,
                                        toastMessage = "Night Photo synthesized (${capturedBitmaps.size}f)",
                                        isScreenFlashActive = false
                                    )
                                }
                                CameraDiagnosticsLogger.getInstance(getApplication()).i("NightMode", "NIGHT_COMPLETED", "Night mode synthesis finished for ${capturedBitmaps.size} frames")
                            }
                        }
                    } else {
                        _uiState.update {
                            it.copy(toastMessage = "Night Mode: Capturing exposure ${index + 1} of $targetCount...")
                        }
                        captureNext(index + 1)
                    }
                },
                onError = { error ->
                    CameraDiagnosticsLogger.getInstance(getApplication()).w("NightMode", "NIGHT_FRAME_ERROR", error.message ?: "")
                    if (capturedBitmaps.isNotEmpty()) {
                        viewModelScope.launch(Dispatchers.Default) {
                            val stackedItem = computationalCoordinator.processAndSaveStackedPhoto(
                                frames = capturedBitmaps,
                                config = nightConfig
                            )
                            for (tf in tempFiles) {
                                if (tf.exists()) tf.delete()
                            }
                            withContext(Dispatchers.Main) {
                                _uiState.update {
                                    it.copy(
                                        isStackingProcessing = false,
                                        latestMedia = stackedItem ?: it.latestMedia,
                                        toastMessage = "Night Photo saved (${capturedBitmaps.size}f)",
                                        isScreenFlashActive = false
                                    )
                                }
                            }
                        }
                    } else {
                        for (tf in tempFiles) {
                            if (tf.exists()) tf.delete()
                        }
                        _uiState.update {
                            it.copy(
                                isStackingProcessing = false,
                                toastMessage = "Night capture failed: ${error.message}",
                                isScreenFlashActive = false
                            )
                        }
                    }
                }
            )
        }

        captureNext(1)
    }

    private fun executeHdrPlusCapture() {
        triggerFlashAnimation()
        _uiState.update { it.copy(isHdrPlusProcessing = true, toastMessage = "Capturing HDR+ sequence (1/5)...") }

        val targetCount = 5
        val capturedBitmaps = mutableListOf<Bitmap>()
        val tempFiles = mutableListOf<File>()
        val cacheDir = getApplication<Application>().cacheDir
        var refExifMetadata: Map<String, String>? = null

        fun captureNext(index: Int) {
            val tempFile = File(cacheDir, "hdrplus_frame_${System.currentTimeMillis()}_$index.jpg")
            tempFiles.add(tempFile)

            cameraEngine.capturePhotoToFile(
                outputFile = tempFile,
                onSuccess = { file ->
                    try {
                        if (refExifMetadata == null) {
                            file.inputStream().use { stream ->
                                refExifMetadata = ExifMetadataHelper.extractExifMetadata(stream)
                            }
                        }
                        val bmp = android.graphics.BitmapFactory.decodeFile(file.absolutePath)
                        if (bmp != null) capturedBitmaps.add(bmp)
                    } catch (e: Exception) {
                        CameraDiagnosticsLogger.getInstance(getApplication()).e("HDRPlus", "FRAME_DECODE_FAILED", e.message ?: "", e)
                    }

                    if (capturedBitmaps.size >= targetCount || index >= targetCount) {
                        _uiState.update { it.copy(toastMessage = "Aligning & fusing HDR+ frames...") }
                        viewModelScope.launch(Dispatchers.Default) {
                            try {
                                val refFrame = capturedBitmaps[0]
                                val alignedFrames = mutableListOf<Bitmap>()
                                alignedFrames.add(refFrame)

                                for (i in 1 until capturedBitmaps.size) {
                                    val disp = com.bishal.adaptivecamera.photography.computational.FrameAlignmentEngine.estimateDisplacement(
                                        reference = refFrame,
                                        target = capturedBitmaps[i],
                                        maxShiftPx = 32
                                    )
                                    val aligned = com.bishal.adaptivecamera.photography.computational.FrameAlignmentEngine.alignBitmap(
                                        capturedBitmaps[i], disp.dx, disp.dy
                                    )
                                    alignedFrames.add(aligned)
                                }

                                val motionMasks = mutableListOf<com.bishal.adaptivecamera.photography.computational.MotionMask>()
                                for (i in alignedFrames.indices) {
                                    if (i == 0) {
                                        motionMasks.add(com.bishal.adaptivecamera.photography.computational.MotionMask(1, 1, floatArrayOf(0f), 0f))
                                    } else {
                                        motionMasks.add(
                                            com.bishal.adaptivecamera.photography.computational.MotionEstimationEngine.estimateMotion(
                                                reference = refFrame,
                                                alignedTarget = alignedFrames[i],
                                                motionThreshold = 0.08f
                                            )
                                        )
                                    }
                                }

                                val fusedBitmap = com.bishal.adaptivecamera.photography.computational.ImageFusionEngine.fuseFrames(
                                    alignedFrames = alignedFrames,
                                    motionMasks = motionMasks,
                                    mode = com.bishal.adaptivecamera.photography.computational.ImageStackingMode.HDR,
                                    referenceIndex = 0
                                )

                                for (i in 1 until alignedFrames.size) {
                                    if (alignedFrames[i] !== capturedBitmaps[i]) {
                                        alignedFrames[i].recycle()
                                    }
                                }

                                // Apply active GPU color config to fused result if enabled
                                val gpuConfig = _uiState.value.gpuColorConfig
                                val finalBitmap = if (gpuConfig.lutEnabled || gpuConfig.grainEnabled || gpuConfig.halationEnabled) {
                                    val activeLut = if (gpuConfig.lutEnabled && gpuConfig.selectedLutId != null) {
                                        lutRepository.loadLutDefinition(gpuConfig.selectedLutId)
                                    } else null
                                    gpuPhotoProcessor.processPhoto(fusedBitmap, gpuConfig, activeLut)
                                } else {
                                    fusedBitmap
                                }

                                val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US).format(java.util.Date())
                                val savedItem = computationalCoordinator.saveBitmapToMediaStore(
                                    bitmap = finalBitmap,
                                    baseFilename = "IMG_HDRPLUS_$timestamp",
                                    exifAttributes = refExifMetadata
                                )

                                if (finalBitmap !== fusedBitmap) finalBitmap.recycle()
                                fusedBitmap.recycle()

                                // Delete private cache files
                                for (tf in tempFiles) {
                                    if (tf.exists()) tf.delete()
                                }

                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            isHdrPlusProcessing = false,
                                            latestMedia = savedItem ?: it.latestMedia,
                                            toastMessage = "HDR+ Photo saved (${capturedBitmaps.size} frames fused)",
                                            isScreenFlashActive = false
                                        )
                                    }
                                    CameraDiagnosticsLogger.getInstance(getApplication()).i("HDRPlus", "HDR_FUSION_COMPLETED", "Saved fused HDR+ image without gallery duplicates")
                                }
                            } catch (e: Exception) {
                                CameraDiagnosticsLogger.getInstance(getApplication()).e("HDRPlus", "FUSION_FAILED", e.message ?: "", e)
                                for (tf in tempFiles) {
                                    if (tf.exists()) tf.delete()
                                }
                                withContext(Dispatchers.Main) {
                                    _uiState.update {
                                        it.copy(
                                            isHdrPlusProcessing = false,
                                            toastMessage = "HDR+ fusion error: ${e.message}",
                                            isScreenFlashActive = false
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        _uiState.update { it.copy(toastMessage = "Capturing HDR+ sequence (${index + 1}/$targetCount)...") }
                        captureNext(index + 1)
                    }
                },
                onError = { error ->
                    CameraDiagnosticsLogger.getInstance(getApplication()).w("HDRPlus", "CAPTURE_FRAME_ERROR", error.message ?: "")
                    for (tf in tempFiles) {
                        if (tf.exists()) tf.delete()
                    }
                    _uiState.update {
                        it.copy(
                            isHdrPlusProcessing = false,
                            toastMessage = "HDR+ capture error: ${error.message}",
                            isScreenFlashActive = false
                        )
                    }
                }
            )
        }

        captureNext(1)
    }

    fun toggleHdrPlusMode() {
        val nextMode = _uiState.value.configuration.hdrPlusMode.next()
        updateHdrPlusMode(nextMode)
    }

    fun updateHdrPlusMode(mode: HdrPlusMode) {
        _uiState.update { it.copy(configuration = it.configuration.copy(hdrPlusMode = mode)) }
        viewModelScope.launch {
            preferencesRepository.updateHdrPlusMode(mode)
            hapticEngine.performClick()
        }
    }

    private fun executeProCapture() {
        camera2ProEngine.capturePhoto(
            onSuccess = { item ->
                val isRaw = _uiState.value.proConfig.isRawCaptureEnabled
                val msg = if (isRaw) "RAW DNG + JPEG saved!" else "Pro Photo saved!"
                processCapturedPhotoWithGpu(item, msg)
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

    private fun processCapturedPhotoWithGpu(item: MediaItem, customSuccessMsg: String? = null) {
        val config = _uiState.value.gpuColorConfig
        val uri = item.uri
        if (uri != null && (config.lutEnabled || config.grainEnabled || config.halationEnabled)) {
            viewModelScope.launch(Dispatchers.IO) {
                var originalBitmap: Bitmap? = null
                var gradedBitmap: Bitmap? = null
                try {
                    val context = getApplication<Application>().applicationContext
                    val exifMetadata = context.contentResolver.openInputStream(uri)?.use { stream ->
                        ExifMetadataHelper.extractExifMetadata(stream)
                    } ?: emptyMap()

                    val inputStream = context.contentResolver.openInputStream(uri)
                    originalBitmap = android.graphics.BitmapFactory.decodeStream(inputStream)
                    inputStream?.close()

                    if (originalBitmap != null) {
                        val activeLut = if (config.lutEnabled && config.selectedLutId != null) {
                            lutRepository.loadLutDefinition(config.selectedLutId)
                        } else null

                        gradedBitmap = gpuPhotoProcessor.processPhoto(originalBitmap, config, activeLut)

                        val tempFile = java.io.File(context.cacheDir, "temp_gpu_photo_${System.currentTimeMillis()}.jpg")
                        try {
                            java.io.FileOutputStream(tempFile).use { fos ->
                                gradedBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, fos)
                                fos.flush()
                            }
                            ExifMetadataHelper.writeExifMetadata(tempFile, exifMetadata)

                            context.contentResolver.openOutputStream(uri, "wt")?.use { outStream ->
                                tempFile.inputStream().use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                                outStream.flush()
                            }
                        } finally {
                            if (tempFile.exists()) tempFile.delete()
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("CameraViewModel", "Error applying GPU look to photo: ${e.message}", e)
                } finally {
                    if (originalBitmap != null && !originalBitmap.isRecycled) {
                        originalBitmap.recycle()
                    }
                    if (gradedBitmap != null && gradedBitmap !== originalBitmap && !gradedBitmap.isRecycled) {
                        gradedBitmap.recycle()
                    }
                    val msg = customSuccessMsg ?: if (config.lutEnabled) "Graded Photo saved!" else "Photo saved!"
                    withContext(Dispatchers.Main) {
                        _uiState.update {
                            it.copy(
                                latestMedia = item,
                                toastMessage = msg,
                                isScreenFlashActive = false
                            )
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    latestMedia = item,
                    toastMessage = customSuccessMsg ?: "Photo saved!",
                    isScreenFlashActive = false
                )
            }
        }
    }

    fun toggleLookControlPanel(visible: Boolean? = null) {
        _uiState.update { it.copy(isLookControlPanelVisible = visible ?: !it.isLookControlPanelVisible) }
    }

    fun updateGpuColorConfig(newConfig: GpuColorConfig) {
        _uiState.update { it.copy(gpuColorConfig = newConfig) }
        gpuPreviewRenderer.updateConfig(newConfig)
        if (newConfig.lutEnabled && newConfig.selectedLutId != null) {
            viewModelScope.launch {
                lutRepository.selectLut(newConfig.selectedLutId)
                val lutDef = lutRepository.loadLutDefinition(newConfig.selectedLutId)
                if (lutDef != null) {
                    gpuPreviewRenderer.loadLutDefinition(lutDef)
                }
            }
        }
        viewModelScope.launch {
            preferencesRepository.updateColorConfig(
                selectedLutId = newConfig.selectedLutId,
                isLutEnabled = newConfig.lutEnabled,
                lutIntensity = newConfig.lutIntensity,
                isGrainEnabled = newConfig.grainEnabled,
                grainIntensity = newConfig.grainIntensity,
                isHalationEnabled = newConfig.halationEnabled,
                halationIntensity = newConfig.halationIntensity
            )
        }
    }

    fun bindPreviewSurface(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        gpuPreviewRenderer.start(surfaceTexture, width, height)
        val currentGpu = _uiState.value.gpuColorConfig
        gpuPreviewRenderer.updateConfig(currentGpu)
        if (currentGpu.lutEnabled && currentGpu.selectedLutId != null) {
            viewModelScope.launch {
                val lutDef = lutRepository.loadLutDefinition(currentGpu.selectedLutId)
                if (lutDef != null) {
                    gpuPreviewRenderer.loadLutDefinition(lutDef)
                }
            }
        }
    }

    fun unbindPreviewSurface() {
        gpuPreviewRenderer.stop()
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

        if (mode == CameraMode.DUAL) {
            val detector = com.bishal.adaptivecamera.video.capability.DualCameraCapabilityDetector(getApplication())
            val capability = detector.detectCapability()
            if (!capability.isSupported) {
                CameraDiagnosticsLogger.getInstance(getApplication()).w(
                    module = "DualCamera",
                    eventType = "CONCURRENT_ISP_RESTRICTED",
                    message = "Vendor HAL restricts concurrent multi-camera ISP pipelines. Engaging Smart-Flip Dual sequential capture fallback."
                )
                _uiState.update { it.copy(toastMessage = "Smart-Flip Dual Active (Vendor HAL restricts concurrent streaming; using sequential dual-capture)") }
            } else {
                CameraDiagnosticsLogger.getInstance(getApplication()).i(
                    module = "DualCamera",
                    eventType = "CONCURRENT_ISP_ENGAGED",
                    message = "Concurrent ISP streaming active for front/back cameras."
                )
            }
        }

        if (mode == CameraMode.NIGHT) {
            CameraDiagnosticsLogger.getInstance(getApplication()).i(
                module = "NightMode",
                eventType = "MODE_ENGAGED",
                message = "Night mode active: 5-frame computational long exposure synthesis."
            )
        }

        hapticEngine.performModeSwitch()
        val previousMode = _uiState.value.configuration.mode

        if (mode.isVideoMode) {
            if (mode != CameraMode.TIME_LAPSE) {
                audioRecordingManager.startVuMonitoring()
            }
        } else if (previousMode.isVideoMode) {
            audioRecordingManager.stopVuMonitoring()
            _uiState.update { it.copy(isVideoQuickSettingsVisible = false) }
        }

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

    fun toggleSoftLight() {
        hapticEngine.performClick()
        _uiState.update { it.copy(isScreenFlashActive = !it.isScreenFlashActive) }
    }

    fun setZoomRatio(ratio: Float) {
        zoomController.setZoomRatio(ratio)
        _uiState.update { it.copy(configuration = it.configuration.copy(zoomRatio = ratio)) }
    }

    fun applyZoomScaleFactor(factor: Float) {
        val newRatio = zoomController.applyScaleFactor(factor)
        _uiState.update { it.copy(configuration = it.configuration.copy(zoomRatio = newRatio)) }
    }

    fun zoomIn() {
        val current = _uiState.value.configuration.zoomRatio
        val max = _uiState.value.configuration.maxZoomRatio
        val next = (current * 1.2f).coerceAtMost(max)
        setZoomRatio(next)
    }

    fun zoomOut() {
        val current = _uiState.value.configuration.zoomRatio
        val min = _uiState.value.configuration.minZoomRatio
        val next = (current / 1.2f).coerceAtLeast(min)
        setZoomRatio(next)
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

    // Phase 7 Advanced Capture & RAW Methods
    fun setCaptureFormat(format: com.bishal.adaptivecamera.photography.capture.CaptureFormat) {
        viewModelScope.launch {
            preferencesRepository.updateCaptureFormat(format.name)
            camera2ProEngine.setCaptureFormat(format)
            cameraEngine.setCaptureFormat(format)
            _uiState.update { it.copy(proConfig = it.proConfig.copy(captureFormat = format, isRawCaptureEnabled = format.isRaw)) }
            hapticEngine.performClick()
        }
    }

    fun toggleZsl() {
        val next = !_uiState.value.configuration.isZslEnabled
        viewModelScope.launch {
            preferencesRepository.updateZslEnabled(next)
            cameraEngine.setZslEnabled(next)
            camera2ProEngine.setZslEnabled(next)
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(isZslEnabled = next),
                    proConfig = it.proConfig.copy(isZslEnabled = next)
                )
            }
            hapticEngine.performClick()
        }
    }

    fun setAebBracketCount(count: Int) {
        viewModelScope.launch {
            preferencesRepository.updateAebBracketCount(count)
            cameraEngine.setAebBracketCount(count)
            camera2ProEngine.setAebBracketCount(count)
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(aebBracketCount = count),
                    proConfig = it.proConfig.copy(aebBracketCount = count)
                )
            }
            hapticEngine.performClick()
        }
    }

    fun setAebEvStep(step: Float) {
        viewModelScope.launch {
            preferencesRepository.updateAebEvStep(step)
            cameraEngine.setAebEvStep(step)
            camera2ProEngine.setAebEvStep(step)
            _uiState.update {
                it.copy(
                    configuration = it.configuration.copy(aebEvStep = step),
                    proConfig = it.proConfig.copy(aebEvStep = step)
                )
            }
            hapticEngine.performClick()
        }
    }

    fun toggleWatermark() {
        val next = !_uiState.value.proConfig.isWatermarkEnabled
        viewModelScope.launch {
            preferencesRepository.updateWatermarkEnabled(next)
            camera2ProEngine.setWatermarkEnabled(next)
            _uiState.update { it.copy(proConfig = it.proConfig.copy(isWatermarkEnabled = next)) }
            hapticEngine.performClick()
        }
    }

    fun setWatermarkText(text: String) {
        viewModelScope.launch {
            preferencesRepository.updateWatermarkText(text)
        }
    }

    fun toggleExifPrivacy() {
        val next = !_uiState.value.proConfig.isExifPrivacyEnabled
        viewModelScope.launch {
            preferencesRepository.updateExifPrivacyEnabled(next)
            camera2ProEngine.setExifPrivacyEnabled(next)
            _uiState.update { it.copy(proConfig = it.proConfig.copy(isExifPrivacyEnabled = next)) }
            hapticEngine.performClick()
        }
    }

    fun toggleStripGps(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateStripGpsLocation(enabled)
        }
    }

    fun toggleStripDeviceModel(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateStripDeviceModel(enabled)
        }
    }

    fun setVideoConfig(config: VideoConfig) {
        val synced = config.syncBitrate()
        _uiState.update { it.copy(videoConfig = synced) }
        cameraEngine.updateVideoConfig(synced)
    }

    fun toggleVideoQuickSettings() {
        val next = !_uiState.value.isVideoQuickSettingsVisible
        _uiState.update { it.copy(isVideoQuickSettingsVisible = next) }
        hapticEngine.performClick()
    }

    fun dismissVideoQuickSettings() {
        _uiState.update { it.copy(isVideoQuickSettingsVisible = false) }
    }

    fun toggleVideoPauseResume() {
        val state = _uiState.value.videoRecordingState
        if (state.status == VideoRecordingStatus.RECORDING) {
            cameraEngine.pauseVideoRecording()
            _uiState.update { it.copy(videoRecordingState = it.videoRecordingState.copy(status = VideoRecordingStatus.PAUSED)) }
            hapticEngine.performClick()
        } else if (state.status == VideoRecordingStatus.PAUSED) {
            cameraEngine.resumeVideoRecording()
            _uiState.update { it.copy(videoRecordingState = it.videoRecordingState.copy(status = VideoRecordingStatus.RECORDING)) }
            hapticEngine.performClick()
        }
    }

    fun updatePortraitAperture(aperture: PortraitAperture) {
        _uiState.update { it.copy(portraitConfig = it.portraitConfig.copy(aperture = aperture)) }
        viewModelScope.launch {
            preferencesRepository.updatePortraitAperture(aperture)
            hapticEngine.performTick()
        }
    }

    fun updatePortraitLightingStyle(style: PortraitLightingStyle) {
        _uiState.update { it.copy(portraitConfig = it.portraitConfig.copy(lightingStyle = style)) }
        viewModelScope.launch {
            preferencesRepository.updatePortraitLightingStyle(style)
            hapticEngine.performTick()
        }
    }

    fun updateImageStackingEnabled(enabled: Boolean) {
        _uiState.update { it.copy(imageStackingConfig = it.imageStackingConfig.copy(isEnabled = enabled)) }
        viewModelScope.launch {
            preferencesRepository.updateImageStackingEnabled(enabled)
            hapticEngine.performClick()
        }
    }

    fun toggleImageStacking() {
        val current = _uiState.value.imageStackingConfig.isEnabled
        updateImageStackingEnabled(!current)
    }

    fun updateImageStackingMode(mode: ImageStackingMode) {
        _uiState.update { it.copy(imageStackingConfig = it.imageStackingConfig.copy(mode = mode)) }
        viewModelScope.launch {
            preferencesRepository.updateImageStackingMode(mode)
            hapticEngine.performTick()
        }
    }

    fun updateImageStackingFrameCount(count: ImageStackingFrameCount) {
        _uiState.update { it.copy(imageStackingConfig = it.imageStackingConfig.copy(frameCount = count)) }
        viewModelScope.launch {
            preferencesRepository.updateImageStackingFrameCount(count)
            hapticEngine.performTick()
        }
    }

    fun updateRetainOriginalFrames(retain: Boolean) {
        _uiState.update { it.copy(imageStackingConfig = it.imageStackingConfig.copy(retainOriginalFrames = retain)) }
        viewModelScope.launch {
            preferencesRepository.updateRetainOriginalFrames(retain)
            hapticEngine.performClick()
        }
    }

    override fun onCleared() {
        super.onCleared()
        gpuPreviewRenderer.stop()
        audioRecordingManager.stopVuMonitoring()
        cameraEngine.unbindCamera()
        camera2ProEngine.unbindCamera()
        virtualHorizonSensor.stopListening()
        thermalMonitor.release()
    }

    fun updateLeftHandedModeEnabled(enabled: Boolean) {
        _uiState.update { it.copy(isLeftHandedModeEnabled = enabled) }
        viewModelScope.launch {
            preferencesRepository.updateLeftHandedModeEnabled(enabled)
            hapticEngine.performClick()
        }
    }

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun updateThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            preferencesRepository.updateThemeMode(mode)
        }
    }
}
