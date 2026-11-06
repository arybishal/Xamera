package com.bishal.adaptivecamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.ui.camera.components.BottomCaptureControls
import com.bishal.adaptivecamera.ui.camera.components.CompositionGridOverlay
import com.bishal.adaptivecamera.ui.camera.components.CountdownOverlay
import com.bishal.adaptivecamera.ui.camera.components.ExposureSlider
import com.bishal.adaptivecamera.ui.camera.components.FloatingShutterButton
import com.bishal.adaptivecamera.ui.camera.components.FocusRingOverlay
import com.bishal.adaptivecamera.ui.camera.components.RotaryZoomDial
import com.bishal.adaptivecamera.ui.camera.components.SceneSuggestionChip
import com.bishal.adaptivecamera.ui.camera.components.ScreenFlashOverlay
import com.bishal.adaptivecamera.ui.camera.components.TopBarControls
import com.bishal.adaptivecamera.ui.camera.components.ViewfinderFramingMask
import com.bishal.adaptivecamera.ui.camera.components.VirtualHorizonOverlay
import com.bishal.adaptivecamera.ui.camera.lut.LookControlPanel
import com.bishal.adaptivecamera.ui.camera.video.VideoControlBar
import com.bishal.adaptivecamera.ui.camera.video.VideoQuickSettingsSheet
import com.bishal.adaptivecamera.ui.camera.pro.FalseColorOverlay
import com.bishal.adaptivecamera.ui.camera.pro.FocusMagnifierOverlay
import com.bishal.adaptivecamera.ui.camera.pro.FocusPeakingOverlay
import com.bishal.adaptivecamera.ui.camera.pro.HistogramView
import com.bishal.adaptivecamera.ui.camera.pro.ProAppliedTelemetryBadge
import com.bishal.adaptivecamera.ui.camera.pro.ProControlBar
import com.bishal.adaptivecamera.ui.camera.pro.ZebraStripingOverlay
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@Composable
fun CameraScreen(
    viewModel: CameraViewModel,
    onOpenGallery: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLutLibrary: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()
    val audioVuState by viewModel.audioVuState.collectAsState()

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] == true
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                )
            )
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    var previewWidthPx by remember { mutableFloatStateOf(0f) }
    var previewHeightPx by remember { mutableFloatStateOf(0f) }

    val isPortrait = previewHeightPx >= previewWidthPx
    val framingBounds = remember(previewWidthPx, previewHeightPx, uiState.configuration.aspectRatio, isPortrait) {
        uiState.configuration.aspectRatio.calculateFramingBounds(previewWidthPx, previewHeightPx, isPortrait)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopBarControls(
                config = uiState.configuration,
                onToggleFlash = { viewModel.toggleFlash() },
                onToggleTimer = { viewModel.toggleTimer() },
                onToggleExposure = { viewModel.toggleExposureSlider() },
                onCycleAspectRatio = { viewModel.cycleAspectRatio() },
                onCycleGrid = { viewModel.cycleGridType() },
                isLookActive = uiState.gpuColorConfig.lutEnabled || uiState.gpuColorConfig.grainEnabled || uiState.gpuColorConfig.halationEnabled,
                onToggleColorLooks = { viewModel.toggleLookControlPanel() },
                onOpenSettings = onOpenSettings
            )
        },
        bottomBar = {
            Column {
                if (uiState.configuration.mode == CameraMode.PRO) {
                    ProControlBar(
                        proConfig = uiState.proConfig,
                        activeProfile = uiState.activeProfile,
                        onSetManualIso = { enabled, iso -> viewModel.setManualIso(enabled, iso) },
                        onSetManualShutter = { enabled, shutter -> viewModel.setManualShutterSpeed(enabled, shutter) },
                        onSetManualFocus = { enabled, dist -> viewModel.setManualFocus(enabled, dist) },
                        onSetWhiteBalance = { wb -> viewModel.setWhiteBalance(wb) },
                        onSetManualKelvin = { enabled, k, t -> viewModel.setManualKelvin(enabled, k, t) },
                        onToggleAeLock = { viewModel.toggleAeLock() },
                        onToggleAfLock = { viewModel.toggleAfLock() },
                        onToggleAwbLock = { viewModel.toggleAwbLock() },
                        onToggleRaw = { enabled -> viewModel.toggleRawCapture(enabled) },
                        onToggleHistogram = { viewModel.toggleHistogram() },
                        onTogglePeaking = { enabled -> viewModel.toggleFocusPeaking(enabled) },
                        onSetPeakingSensitivity = { sens -> viewModel.setFocusPeakingSensitivity(sens) },
                        onToggleZebra = { viewModel.toggleZebraStriping(it) },
                        onSetZebraThreshold = { viewModel.setZebraThreshold(it) },
                        onToggleFalseColor = { viewModel.toggleFalseColor(it) },
                        onToggleFocusMagnifier = { viewModel.toggleFocusMagnifier(it) },
                        onSetFocusMagnifierScale = { viewModel.setFocusMagnifierScale(it) },
                        onSetCaptureFormat = { viewModel.setCaptureFormat(it) },
                        onSetAebBracketCount = { viewModel.setAebBracketCount(it) },
                        onSetAebEvStep = { viewModel.setAebEvStep(it) }
                    )
                }
                BottomCaptureControls(
                    mode = uiState.configuration.mode,
                    sessionState = uiState.sessionState,
                    latestMedia = uiState.latestMedia,
                    recordingDurationSec = uiState.recordingDurationSec,
                    onModeChanged = { viewModel.setCameraMode(it) },
                    onShutterPressed = { viewModel.onShutterPressed() },
                    onSwitchCamera = { viewModel.switchCamera() },
                    onOpenGallery = onOpenGallery
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                if (uiState.configuration.mode == CameraMode.PRO) {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                previewWidthPx = size.width.toFloat()
                                previewHeightPx = size.height.toFloat()
                            },
                        factory = { ctx ->
                            TextureView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, width: Int, height: Int) {
                                        viewModel.bindProSurface(Surface(st))
                                    }
                                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, width: Int, height: Int) {}
                                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                                        viewModel.unbindProSurface()
                                        return true
                                    }
                                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
                                }
                            }
                        }
                    )
                } else {
                    AndroidView(
                        modifier = Modifier
                            .fillMaxSize()
                            .onSizeChanged { size ->
                                previewWidthPx = size.width.toFloat()
                                previewHeightPx = size.height.toFloat()
                            }
                            .pointerInput(previewWidthPx, previewHeightPx) {
                                detectTransformGestures { _, _, zoomChange, _ ->
                                    if (zoomChange != 1f) {
                                        viewModel.applyZoomScaleFactor(zoomChange)
                                    }
                                }
                            }
                            .pointerInput(previewWidthPx, previewHeightPx) {
                                detectTapGestures { offset ->
                                    if (previewWidthPx > 0f && previewHeightPx > 0f) {
                                        val xNorm = offset.x / previewWidthPx
                                        val yNorm = offset.y / previewHeightPx
                                        viewModel.onFocusTap(xNorm, yNorm)
                                    }
                                }
                            },
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                scaleType = PreviewView.ScaleType.FILL_CENTER
                                implementationMode = PreviewView.ImplementationMode.PERFORMANCE
                                viewModel.bindCamera(lifecycleOwner, this)
                            }
                        }
                    )
                }

                DisposableEffect(lifecycleOwner) {
                    onDispose {
                        viewModel.unbindCamera()
                    }
                }

                // 1. Aspect Ratio Viewfinder Framing Mask (Darkened Letterbox/Pillarbox)
                ViewfinderFramingMask(
                    aspectRatio = uiState.configuration.aspectRatio,
                    framingBounds = framingBounds
                )

                // 2. Composition Grid Overlay (Rule of Thirds, Golden Ratio, Golden Spiral, Center Crosshairs)
                CompositionGridOverlay(
                    gridType = uiState.configuration.gridType,
                    framingBounds = framingBounds
                )

                // 3. Gyroscope Virtual Horizon Overlay
                if (uiState.configuration.isVirtualHorizonEnabled) {
                    VirtualHorizonOverlay(
                        horizonState = uiState.virtualHorizonState,
                        framingBounds = framingBounds
                    )
                }

                // 4. Focus Peaking Overlay (Pro Mode)
                if (uiState.configuration.mode == CameraMode.PRO && uiState.proConfig.isFocusPeakingEnabled) {
                    FocusPeakingOverlay(
                        peakingMap = uiState.focusPeakingMap,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4b. Live Zebra Striping Overlay (Pro Mode)
                if (uiState.configuration.mode == CameraMode.PRO && uiState.proConfig.isZebraEnabled) {
                    ZebraStripingOverlay(
                        zebraMap = uiState.zebraMap,
                        framingBounds = framingBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4c. Broadcast False Color Overlay (Pro Mode)
                if (uiState.configuration.mode == CameraMode.PRO && uiState.proConfig.isFalseColorEnabled) {
                    FalseColorOverlay(
                        falseColorMap = uiState.falseColorMap,
                        framingBounds = framingBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4d. Focus Magnifier Loupe Overlay (Pro Mode)
                if (uiState.configuration.mode == CameraMode.PRO && uiState.proConfig.isFocusMagnifierEnabled) {
                    FocusMagnifierOverlay(
                        isEnabled = uiState.proConfig.isFocusMagnifierEnabled,
                        scale = uiState.proConfig.focusMagnifierScale,
                        framingBounds = framingBounds,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // 4e. AE/AF Lock Visual Banner (Pro Mode)
                if (uiState.configuration.mode == CameraMode.PRO && (uiState.proConfig.isAeLocked || uiState.proConfig.isAfLocked)) {
                    val lockLabel = when {
                        uiState.proConfig.isAeLocked && uiState.proConfig.isAfLocked -> "AE/AF LOCK"
                        uiState.proConfig.isAeLocked -> "AE LOCK"
                        else -> "AF LOCK"
                    }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 46.dp)
                            .background(AccentAmber, RoundedCornerShape(4.dp))
                            .padding(horizontal = 10.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = lockLabel,
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // 5. Focus Ring Overlay (Tap to focus)
                uiState.focusPoint?.let { point ->
                    FocusRingOverlay(
                        focusPointNorm = point,
                        containerWidthPx = previewWidthPx,
                        containerHeightPx = previewHeightPx
                    )
                }

                // 6. Pro Applied Telemetry Badge (Pro Mode) / Video Control Bar (Video Mode) / Scene Suggestion Chip (Photo Mode)
                if (uiState.configuration.mode == CameraMode.PRO) {
                    ProAppliedTelemetryBadge(
                        appliedState = uiState.proAppliedState,
                        proConfig = uiState.proConfig,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )
                } else if (uiState.configuration.mode == CameraMode.VIDEO) {
                    VideoControlBar(
                        videoConfig = uiState.videoConfig,
                        recordingState = uiState.videoRecordingState,
                        audioVuState = audioVuState,
                        onOpenQuickSettings = { viewModel.toggleVideoQuickSettings() },
                        onTogglePauseResume = { viewModel.toggleVideoPauseResume() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                    )
                } else {
                    // Smart Scene Suggestion Overlay (Photo Mode)
                    SceneSuggestionChip(
                        autoState = uiState.smartAutoState,
                        isDismissed = uiState.isSuggestionDismissed,
                        onApplySuggestion = { viewModel.applySmartAutoSuggestion() },
                        onDismiss = { viewModel.dismissSmartAutoSuggestion() },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 12.dp)
                    )
                }

                // 7. Live Histogram View (Pro Mode)
                if (uiState.configuration.mode == CameraMode.PRO && uiState.proConfig.isHistogramVisible) {
                    HistogramView(
                        data = uiState.histogramData,
                        isExpanded = uiState.proConfig.isHistogramExpanded,
                        onToggleExpanded = { viewModel.toggleHistogramExpanded() },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 44.dp, end = 12.dp)
                    )
                }

                // 8. Countdown Timer Overlay
                CountdownOverlay(
                    secondsRemaining = uiState.countdownRemaining,
                    modifier = Modifier.align(Alignment.Center)
                )

                // 9. Rotary Zoom Dial and Step Pills (Photo & Video mode)
                if (uiState.configuration.mode != CameraMode.PRO) {
                    RotaryZoomDial(
                        currentZoom = uiState.configuration.zoomRatio,
                        minZoom = uiState.configuration.minZoomRatio,
                        maxZoom = uiState.configuration.maxZoomRatio,
                        availableSteps = viewModel.zoomController.getAvailableSteps(),
                        onZoomChanged = { viewModel.setZoomRatio(it) },
                        onHapticTick = { viewModel.hapticEngine.performTick() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }

                // 10. Exposure Slider Overlay (Side)
                if (uiState.isExposureSliderVisible && uiState.configuration.mode != CameraMode.PRO) {
                    ExposureSlider(
                        currentIndex = uiState.configuration.exposureCompensationIndex,
                        minIndex = uiState.configuration.minExposureIndex,
                        maxIndex = uiState.configuration.maxExposureIndex,
                        stepEv = uiState.configuration.exposureStepEv,
                        isSupported = uiState.configuration.isExposureCompensationSupported,
                        isReady = uiState.configuration.isExposureStateReady,
                        onIndexChanged = { viewModel.setExposureIndex(it) },
                        onReset = { viewModel.resetExposure() },
                        onClose = { viewModel.closeExposureSlider() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )
                }

                // 11. Movable Floating Shutter Button
                if (uiState.configuration.isFloatingShutterEnabled) {
                    FloatingShutterButton(
                        containerWidthPx = previewWidthPx,
                        containerHeightPx = previewHeightPx,
                        initialNormX = uiState.configuration.floatingShutterX,
                        initialNormY = uiState.configuration.floatingShutterY,
                        onShutterPressed = { viewModel.onShutterPressed() },
                        onPositionChanged = { normX, normY -> viewModel.updateFloatingShutterPosition(normX, normY) }
                    )
                }

                // 12. Shutter Flash Animation Overlay
                AnimatedVisibility(
                    visible = uiState.captureFlashAnimation,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
                }

                // 13. Front Screen Flash Overlay
                ScreenFlashOverlay(isActive = uiState.isScreenFlashActive)

                // 14. Floating Look & Shader Control Panel
                LookControlPanel(
                    isVisible = uiState.isLookControlPanelVisible,
                    config = uiState.gpuColorConfig,
                    onUpdateConfig = { viewModel.updateGpuColorConfig(it) },
                    onOpenLutLibrary = onOpenLutLibrary,
                    onClose = { viewModel.toggleLookControlPanel(false) },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )

                // 15. Video Quick Settings Sheet
                if (uiState.isVideoQuickSettingsVisible) {
                    VideoQuickSettingsSheet(
                        config = uiState.videoConfig,
                        onConfigChanged = { viewModel.setVideoConfig(it) },
                        onDismiss = { viewModel.dismissVideoQuickSettings() },
                        modifier = Modifier.align(Alignment.BottomCenter)
                    )
                }

            } else {
                // Permission Denied Card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Camera Permission Required",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Adaptive Camera needs camera access to provide live preview, photo capture, and video recording.",
                                color = TextSecondary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                            Button(
                                onClick = {
                                    permissionLauncher.launch(
                                        arrayOf(
                                            Manifest.permission.CAMERA,
                                            Manifest.permission.RECORD_AUDIO
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Grant Permission", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
