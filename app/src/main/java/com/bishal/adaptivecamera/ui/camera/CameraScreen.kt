package com.bishal.adaptivecamera.ui.camera

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.bishal.adaptivecamera.ui.camera.components.BottomCaptureControls
import com.bishal.adaptivecamera.ui.camera.components.CountdownOverlay
import com.bishal.adaptivecamera.ui.camera.components.ExposureSlider
import com.bishal.adaptivecamera.ui.camera.components.FocusRingOverlay
import com.bishal.adaptivecamera.ui.camera.components.TopBarControls
import com.bishal.adaptivecamera.ui.camera.components.ZoomControlBar
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
    onOpenDiagnostics: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

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

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopBarControls(
                config = uiState.configuration,
                onToggleFlash = { viewModel.toggleFlash() },
                onToggleTimer = { viewModel.toggleTimer() },
                onToggleExposure = { viewModel.toggleExposureSlider() },
                onOpenDiagnostics = onOpenDiagnostics
            )
        },
        bottomBar = {
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
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Black)
        ) {
            if (hasCameraPermission) {
                // Viewfinder PreviewView
                AndroidView(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { size ->
                            previewWidthPx = size.width.toFloat()
                            previewHeightPx = size.height.toFloat()
                        }
                        .pointerInput(Unit) {
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

                DisposableEffect(lifecycleOwner) {
                    onDispose {
                        viewModel.unbindCamera()
                    }
                }

                // Focus Ring Overlay
                uiState.focusPoint?.let { point ->
                    FocusRingOverlay(
                        focusPointNorm = point,
                        containerWidthPx = previewWidthPx,
                        containerHeightPx = previewHeightPx
                    )
                }

                // Countdown Timer Overlay
                CountdownOverlay(
                    secondsRemaining = uiState.countdownRemaining,
                    modifier = Modifier.align(Alignment.Center)
                )

                // Zoom Level Bar
                ZoomControlBar(
                    currentZoom = uiState.configuration.zoomRatio,
                    minZoom = uiState.configuration.minZoomRatio,
                    maxZoom = uiState.configuration.maxZoomRatio,
                    onZoomSelected = { viewModel.setZoomRatio(it) },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                )

                // Exposure Slider Overlay (Side)
                if (uiState.isExposureSliderVisible) {
                    ExposureSlider(
                        currentIndex = uiState.configuration.exposureCompensationIndex,
                        minIndex = uiState.configuration.minExposureIndex,
                        maxIndex = uiState.configuration.maxExposureIndex,
                        stepEv = uiState.configuration.exposureStepEv,
                        onIndexChanged = { viewModel.setExposureIndex(it) },
                        onReset = { viewModel.resetExposure() },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp)
                    )
                }

                // Shutter Flash Animation Overlay
                AnimatedVisibility(
                    visible = uiState.captureFlashAnimation,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier.fillMaxSize()
                ) {
                    Box(modifier = Modifier.fillMaxSize().background(Color.White))
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
