package com.bishal.adaptivecamera.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.camera.capability.CameraHardwareLevel
import com.bishal.adaptivecamera.camera.capability.CapabilityStatus
import com.bishal.adaptivecamera.camera.capability.DeviceCapabilityReport
import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo
import com.bishal.adaptivecamera.core.diagnostics.PhysicalIdentityConfidence
import com.bishal.adaptivecamera.ui.diagnostics.DiagnosticsViewModel
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import com.bishal.adaptivecamera.ui.camera.CameraViewModel
import com.bishal.adaptivecamera.ui.theme.AppThemeMode
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.TouchApp
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import com.bishal.adaptivecamera.photography.capture.CaptureFormat
import com.bishal.adaptivecamera.photography.computational.ImageStackingFrameCount
import com.bishal.adaptivecamera.photography.computational.ImageStackingMode
import com.bishal.adaptivecamera.photography.portrait.PortraitAperture
import com.bishal.adaptivecamera.photography.portrait.PortraitLightingStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    diagnosticsViewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    cameraViewModel: CameraViewModel? = null,
    onNavigateToRawVault: () -> Unit = {},
    onNavigateToLutLibrary: () -> Unit = {},
    onNavigateToDiagnostics: () -> Unit = {}
) {
    val context = LocalContext.current
    val diagState by diagnosticsViewModel.uiState.collectAsState()
    val camUiState = cameraViewModel?.uiState?.collectAsState()?.value

    var isHardwareExpanded by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        if (diagState.report == null) {
            diagnosticsViewModel.loadHardwareReport(forceRefresh = false)
        }
    }

    LaunchedEffect(diagState.toastMessage) {
        diagState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            diagnosticsViewModel.clearToastMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Camera Settings",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: General Camera Preferences
            item {
                SettingsSectionHeader("General Preferences", Icons.Default.Tune)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingRow(
                            icon = Icons.Default.Folder,
                            title = "Storage Location",
                            subtitle = "DCIM/Xamera (Scoped Storage)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingRow(
                            icon = Icons.Default.PhotoCamera,
                            title = "Capture Pipeline",
                            subtitle = "CameraX 1.4.0 (Standard) · Camera2 (Pro & RAW)"
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        SettingRow(
                            icon = Icons.Default.Info,
                            title = "Application Version",
                            subtitle = "Xamera v1.0.0 (Build 17) · Production Release"
                        )
                    }
                }
            }

            // Section 1b: Appearance & Theme (Phase 11)
            item {
                SettingsSectionHeader("Appearance & Theme", Icons.Default.Palette)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "System Theme",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatic: Dynamically matches Android system Dark/Light theme mode",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FOLLOW SYSTEM MODE",
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "AUTOMATIC",
                                    color = SuccessGreen,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Viewfinder & Ergonomics (Phase 5)
            item {
                SettingsSectionHeader("Viewfinder & Composition", Icons.Default.Crop)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Aspect Ratio Selector
                        Text(
                            text = "Aspect Ratio Framing",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Select photographic composition framing (4:3 is 3:4 in portrait)",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        val currentRatio = camUiState?.configuration?.aspectRatio ?: AspectRatioConfig.RATIO_4_3
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            AspectRatioConfig.values().forEach { ratio ->
                                val isSelected = currentRatio == ratio
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { cameraViewModel?.setAspectRatio(ratio) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ratio.displayName,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Composition Grid Selector
                        Text(
                            text = "Composition Grid Overlay",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Assists in framing; does not modify captured photos",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                        )
                        val currentGrid = camUiState?.configuration?.gridType ?: CompositionGridType.OFF
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            CompositionGridType.values().forEach { grid ->
                                val isSelected = currentGrid == grid
                                val shortName = when (grid) {
                                    CompositionGridType.OFF -> "Off"
                                    CompositionGridType.RULE_OF_THIRDS -> "3x3"
                                    CompositionGridType.GOLDEN_RATIO -> "Golden"
                                    CompositionGridType.GOLDEN_SPIRAL -> "Spiral"
                                    CompositionGridType.CENTER_CROSSHAIRS -> "Cross"
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable { cameraViewModel?.setGridType(grid) }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = shortName,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Virtual Horizon Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Virtual Horizon Level", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Real-time gyroscope pitch and roll leveling reticle", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.configuration?.isVirtualHorizonEnabled ?: true,
                                onCheckedChange = { cameraViewModel?.toggleVirtualHorizon() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Haptic Feedback Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Haptic Feedback", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Tactile response on shutter trip, zoom dial ticks, and horizon lock", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.configuration?.isHapticsEnabled ?: true,
                                onCheckedChange = { cameraViewModel?.toggleHaptics() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Movable Floating Shutter Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Floating Shutter Button", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Repositionable capture button for one-handed reach", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.configuration?.isFloatingShutterEnabled ?: false,
                                onCheckedChange = { cameraViewModel?.toggleFloatingShutter() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }

                        if (camUiState?.configuration?.isFloatingShutterEnabled == true) {
                            OutlinedButton(
                                onClick = { cameraViewModel?.resetFloatingShutterPosition() },
                                modifier = Modifier
                                    .padding(top = 6.dp)
                                    .align(Alignment.End),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset Shutter Position", fontSize = 11.sp, color = PrimaryCyan)
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Left-Handed UI Mode Toggle (FEAT-VIEW-07)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Left-Handed UI Mode", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Mirrors capture controls and flip button for ergonomic left-thumb operation", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.isLeftHandedModeEnabled ?: false,
                                onCheckedChange = { cameraViewModel?.updateLeftHandedModeEnabled(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }
                    }
                }
            }

            // Section 3: Advanced Capture & RAW (Phase 7)
            item {
                SettingsSectionHeader("Advanced Capture & RAW", Icons.Default.PhotoCamera)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // 1. Zero Shutter Lag (ZSL)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Zero Shutter Lag (ZSL)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Bounded rolling buffer for instantaneous shutter response", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.configuration?.isZslEnabled ?: true,
                                onCheckedChange = { cameraViewModel?.toggleZsl() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Capture Format
                        Text("Capture Format", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Select image capture output stream and RAW negative format", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeFormat = camUiState?.proConfig?.captureFormat ?: CaptureFormat.JPEG
                            CaptureFormat.values().forEach { fmt ->
                                val isSelected = activeFormat == fmt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .clickable { cameraViewModel?.setCaptureFormat(fmt) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = fmt.shortLabel,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Auto Exposure Bracketing (AEB)
                        Text("Auto Exposure Bracketing (AEB)", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text("Burst exposure sequence with calibrated EV offsets", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val currentBrackets = camUiState?.proConfig?.aebBracketCount ?: 0
                            val bracketOptions = listOf(0 to "OFF", 3 to "3 Frames", 5 to "5 Frames", 7 to "7 Frames")
                            bracketOptions.forEach { (count, label) ->
                                val isSelected = currentBrackets == count
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AccentAmber else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, if (isSelected) AccentAmber else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .clickable { cameraViewModel?.setAebBracketCount(count) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        color = if (isSelected) Color.Black else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if ((camUiState?.proConfig?.aebBracketCount ?: 0) > 0) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Bracket Step Size (ΔEV)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentStep = camUiState?.proConfig?.aebEvStep ?: 1.0f
                                val stepOptions = listOf(0.5f, 1.0f, 1.5f, 2.0f)
                                stepOptions.forEach { step ->
                                    val isSelected = currentStep == step
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) AccentAmber.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) AccentAmber else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .clickable { cameraViewModel?.setAebEvStep(step) }
                                            .padding(vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "±${step} EV",
                                            color = if (isSelected) Color.Black else TextPrimary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 4. Photo Watermark System
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Photo Watermark", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Burn subtle metadata into exported JPEG/HEIC", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.proConfig?.isWatermarkEnabled ?: false,
                                onCheckedChange = { cameraViewModel?.toggleWatermark() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. EXIF Privacy Controls
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("EXIF Privacy Mode", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                Text("Strip GPS location and camera hardware serial from exported photos", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                            }
                            Switch(
                                checked = camUiState?.proConfig?.isExifPrivacyEnabled ?: false,
                                onCheckedChange = { cameraViewModel?.toggleExifPrivacy() },
                                colors = SwitchDefaults.colors(checkedThumbColor = MaterialTheme.colorScheme.primary, checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 6. RAW Vault Navigation Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToRawVault() }
                                .border(1.dp, AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(AccentAmber.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Folder,
                                            contentDescription = null,
                                            tint = AccentAmber,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Open RAW Vault",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Manage, inspect, share, and export local DNGs",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open",
                                    tint = AccentAmber,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4: Color Science & LUT Library (Phase 8)
            item {
                SettingsSectionHeader("Color Science & Look Profiles", Icons.Default.Palette)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "LUT Library & Color Calibration",
                            color = PrimaryCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Offline color science management, .cube parser, mathematical film presets, and look profile database.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // LUT Library Navigation Card
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToLutLibrary() }
                                .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryCyan.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Palette,
                                            contentDescription = null,
                                            tint = PrimaryCyan,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Open LUT Library",
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Browse presets, import 1D/3D .cube files, manage favorites",
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Open",
                                    tint = PrimaryCyan,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4b: Computational Photography & Image Stacking (Phase 13)
            item {
                SettingsSectionHeader("Computational Photography & Stacking", Icons.Default.AutoAwesome)
            }

            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // 1. Image Stacking Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Image Stacking & Fusion",
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Multi-frame alignment, ghost rejection & temporal fusion",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = camUiState?.imageStackingConfig?.isEnabled ?: false,
                                onCheckedChange = { cameraViewModel?.updateImageStackingEnabled(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                )
                            )
                        }

                        if (camUiState?.imageStackingConfig?.isEnabled == true) {
                            Spacer(modifier = Modifier.height(14.dp))

                            // 2. Stacking Mode
                            Text(
                                "Stacking Algorithm Mode",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Select fusion objective: noise reduction, dynamic range, or edge detail",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentMode = camUiState?.imageStackingConfig?.mode ?: ImageStackingMode.SMART_AUTO
                                ImageStackingMode.values().forEach { mode ->
                                    val isSelected = currentMode == mode
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .clickable { cameraViewModel?.updateImageStackingMode(mode) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = mode.label,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 10.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 3. Frame Count
                            Text(
                                "Burst Frame Count",
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                val currentCount = camUiState?.imageStackingConfig?.frameCount ?: ImageStackingFrameCount.AUTO
                                ImageStackingFrameCount.values().forEach { count ->
                                    val isSelected = currentCount == count
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.surfaceVariant)
                                            .border(1.dp, if (isSelected) PrimaryCyan else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                            .clickable { cameraViewModel?.updateImageStackingFrameCount(count) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = count.label,
                                            color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // 4. Retain Original Frames
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Retain Original Frames",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Save all intermediate unaligned burst captures into the gallery",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                                Switch(
                                    checked = camUiState?.imageStackingConfig?.retainOriginalFrames ?: false,
                                    onCheckedChange = { cameraViewModel?.updateRetainOriginalFrames(it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = MaterialTheme.colorScheme.primary,
                                        checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                    )
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // 5. Default Portrait Aperture
                        Text(
                            "Default Portrait Aperture",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeAperture = camUiState?.portraitConfig?.aperture ?: PortraitAperture.F2_8
                            PortraitAperture.values().forEach { ap ->
                                val isSelected = activeAperture == ap
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AccentAmber else MaterialTheme.colorScheme.surfaceVariant)
                                        .border(1.dp, if (isSelected) AccentAmber else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                        .clickable { cameraViewModel?.updatePortraitAperture(ap) }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = ap.label,
                                        color = if (isSelected) Color.Black else MaterialTheme.colorScheme.onSurface,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Section 5: Prominent "Check Hardware Capability" Section
            item {
                SettingsSectionHeader("Hardware Intelligence", Icons.Default.Memory)
            }

            item {
                // Central Hardware Capability Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(PrimaryCyan.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Memory,
                                        contentDescription = null,
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Check Hardware Capability",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Deep HAL & sensor telemetry",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            // Toggle expand/collapse
                            IconButton(onClick = { isHardwareExpanded = !isHardwareExpanded }) {
                                Icon(
                                    imageVector = if (isHardwareExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = "Expand",
                                    tint = PrimaryCyan
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dedicated entry to full Camera Diagnostics & Log Center
                        Button(
                            onClick = onNavigateToDiagnostics,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open Camera Diagnostics & Logs", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(16.dp))
                        }

                        // Status Banner: Cached vs Fresh Scan
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                val isCached = diagState.isFromCache
                                val badgeColor = if (isCached) AccentAmber else SuccessGreen
                                val badgeText = if (isCached) "CACHED HAL DATA" else "LIVE SCANNED DATA"

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(badgeColor.copy(alpha = 0.2f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = badgeText,
                                        color = badgeColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            // Re-scan button
                            OutlinedButton(
                                onClick = { diagnosticsViewModel.refreshHardwareScan(context) },
                                enabled = !diagState.isLoading,
                                modifier = Modifier.height(28.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                            ) {
                                if (diagState.isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(12.dp),
                                        color = PrimaryCyan,
                                        strokeWidth = 1.5.dp
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scanning...", fontSize = 10.sp, color = PrimaryCyan)
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Re-scan",
                                        tint = PrimaryCyan,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Re-scan", fontSize = 10.sp, color = PrimaryCyan)
                                }
                            }
                        }

                        // Real-time Thermal Status Telemetry (FEAT-THRM-01)
                        val thermalStatus = camUiState?.thermalMitigationPolicy?.thermalStatus
                        if (thermalStatus != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Thermal Health", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    text = thermalStatus.label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (thermalStatus.isThrottling) ErrorRed else SuccessGreen
                                )
                            }
                        }

                        // Diagnostics Action Buttons: Copy JSON, Copy TXT, Share
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { diagnosticsViewModel.copyToClipboard(context, asJson = true) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { diagnosticsViewModel.copyToClipboard(context, asJson = false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TXT", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { diagnosticsViewModel.shareReport(context, asJson = false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Expanded 17 Hardware Capability Sections
            if (isHardwareExpanded) {
                val report = diagState.report
                if (report != null) {
                    item {
                        DeviceIdentitySection(report.deviceInfo)
                    }

                    item {
                        InventoryAndHardwareLevelSection(report)
                    }

                    itemsIndexed(report.cameraProfiles) { index, profile ->
                        CameraDetailedCapabilitySection(index = index, profile = profile)
                    }

                    if (report.diagnosticWarnings.isNotEmpty()) {
                        item {
                            CapabilityWarningsSection(report.diagnosticWarnings)
                        }
                    }
                } else if (diagState.isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryCyan)
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }
    }
}

// 1. Device and Firmware Identity Section
@Composable
private fun DeviceIdentitySection(info: DeviceInfo) {
    CapabilitySectionCard(title = "1. Device & Firmware Identity") {
        CapabilityInfoRow("Reported Model", info.model)
        CapabilityInfoRow("Reported Device / Board", "${info.device} / ${info.hardware}")
        CapabilityInfoRow("Reported Product", info.product)
        CapabilityInfoRow("Hardware Platform", info.probableHardwareFamily)
        CapabilityInfoRow("Android Version", "Android ${info.androidVersion} (API ${info.apiLevel})")
        CapabilityInfoRow("Build Fingerprint", info.buildFingerprint, isMonospace = true)
        CapabilityInfoRow("Firmware Identity", info.firmwareIdentity)
        CapabilityInfoRow("Physical Hardware", info.physicalHardwareIdentity)
        CapabilityInfoRow("Physical Confidence", info.physicalIdentityConfidence.name)
    }
}

// 2. Camera Inventory & Hardware Levels
@Composable
private fun InventoryAndHardwareLevelSection(report: DeviceCapabilityReport) {
    val totalSensors = report.cameraProfiles.size
    val backCount = report.cameraProfiles.count { it.isFacingBack }
    val frontCount = report.cameraProfiles.count { it.isFacingFront }
    val logicalCount = report.cameraProfiles.count { it.isLogicalCamera }
    val rawCount = report.cameraProfiles.count { it.supportsRaw }
    val manualCount = report.cameraProfiles.count { it.supportsManualSensor }

    CapabilitySectionCard(title = "2. Camera Inventory & HAL Levels") {
        CapabilityInfoRow("Total Detected Sensors", "$totalSensors")
        CapabilityInfoRow("Back Facing Cameras", "$backCount")
        CapabilityInfoRow("Front Facing Cameras", "$frontCount")
        CapabilityInfoRow("Logical Multi-Cameras", "$logicalCount")
        CapabilityInfoRow("RAW Capable Sensors", "$rawCount")
        CapabilityInfoRow("Manual Control Capable", "$manualCount")
    }
}

// 3-16. Detailed Camera Capabilities for each discovered camera
@Composable
private fun CameraDetailedCapabilitySection(index: Int, profile: CameraCapabilityProfile) {
    CapabilitySectionCard(
        title = "Camera #${index + 1} [ID: ${profile.cameraId} · ${profile.facingLabel}]"
    ) {
        // Hardware Level & Facing
        CapabilityInfoRow("Camera Facing", "${profile.facingLabel} (HAL enum: ${profile.rawFacing})")
        CapabilityInfoRow("Hardware Level", profile.hardwareLevel.name)

        // 4. Logical & Physical Relationships
        CapabilityInfoRow("Logical Camera", if (profile.isLogicalCamera) "YES (Multi-Camera Master)" else "NO (Single Sensor)")
        if (profile.physicalCameraIds.isNotEmpty()) {
            CapabilityInfoRow("Physical Sub-Sensors", profile.physicalCameraIds.joinToString(", "))
        }

        // 5. Sensor Specifications
        CapabilityInfoRow("Sensor Active Array", profile.sensorActiveArraySize)
        CapabilityInfoRow("Sensor Orientation", "${profile.sensorOrientation}°")

        // 6. Lens Specifications
        if (profile.focalLengths.isNotEmpty()) {
            CapabilityInfoRow("Focal Lengths", profile.focalLengths.joinToString(", ") { "${it}mm" })
        }

        // 7. Supported Resolutions & Formats
        profile.maxPhotoResolution?.let {
            CapabilityInfoRow("Max Photo Resolution", it.toString())
        }

        // 8. Supported Video Resolutions & Frame Rates
        profile.maxVideoResolution?.let {
            CapabilityInfoRow("Max Video Resolution", it.toString())
        }
        if (profile.supportedFpsRanges.isNotEmpty()) {
            CapabilityInfoRow("Supported FPS Ranges", profile.supportedFpsRanges.joinToString(", ") { "${it.lower}-${it.upper}" })
        }

        // 11. HDR & Dynamic Range
        CapabilityInfoRow("10-Bit Dynamic Range", if (profile.dynamicRangeProfilesSupported.isNotEmpty()) profile.dynamicRangeProfilesSupported.joinToString(", ") else "Standard 8-bit SDR")

        // 12. RAW & DNG Capabilities
        CapabilityInfoRow("RAW_SENSOR Support", profile.rawStatus.name)

        // 13. Manual ISO & Shutter
        profile.isoRange?.let {
            CapabilityInfoRow("Manual ISO Range", "${it.lower} - ${it.upper}")
        }
        if (profile.exposureTimeRangeNs != null) {
            CapabilityInfoRow("Shutter Speed Range", profile.exposureTimeRangeDisplay)
        }

        // 14. Manual Focus Capabilities
        if (profile.minimumFocusDistance != null) {
            val cm = if (profile.minimumFocusDistance > 0f) "${(100f / profile.minimumFocusDistance).toInt()}cm" else "Infinity"
            CapabilityInfoRow("Min Focus Distance", "${profile.minimumFocusDistance} D ($cm)")
        } else {
            CapabilityInfoRow("Focus Capability", "Fixed Focus Lens")
        }

        // 15. Zoom Information
        CapabilityInfoRow("Zoom Ratio Range", "${profile.minZoomRatio}x - ${profile.maxZoomRatio}x (${profile.zoomStatus})")

        // 16. Stabilization Capabilities
        val hasStab = profile.opticalStabilizationModes.isNotEmpty() || profile.videoStabilizationModes.isNotEmpty()
        CapabilityInfoRow("Stabilization Modes", if (hasStab) "OIS / EIS Advertised" else "Not Advertised")

        // Diagnostic notes / limitations
        if (profile.diagnosticNotes.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            profile.diagnosticNotes.forEach { note ->
                Text(
                    text = "• $note",
                    color = TextMuted,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(vertical = 1.dp)
                )
            }
        }
    }
}

// 17. Capability Errors & Unsupported Features
@Composable
private fun CapabilityWarningsSection(warnings: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentAmber.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "17. Capability Notices & Limitations",
                    color = AccentAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            warnings.forEach { w ->
                Text(
                    text = "• $w",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CapabilitySectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                color = PrimaryCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            content()
        }
    }
}

@Composable
private fun CapabilityInfoRow(
    label: String,
    value: String,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(1.5f)
        )
    }
}
