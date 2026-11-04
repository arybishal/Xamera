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
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Mic
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
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    diagnosticsViewModel: DiagnosticsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val diagState by diagnosticsViewModel.uiState.collectAsState()

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
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Camera Settings",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
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
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        SettingRow(
                            icon = Icons.Default.Folder,
                            title = "Storage Location",
                            subtitle = "DCIM/AdaptiveCamera (Scoped Storage)"
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
                            subtitle = "v0.5.0 · Phase 4 Pro Photography & Settings"
                        )
                    }
                }
            }

            // Section 2: Prominent "Check Hardware Capability" Section
            item {
                SettingsSectionHeader("Hardware Intelligence", Icons.Default.Memory)
            }

            item {
                // Central Hardware Capability Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
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
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "Deep HAL & sensor telemetry",
                                        color = TextSecondary,
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

                        // Status Banner: Cached vs Fresh Scan
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
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
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("JSON", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { diagnosticsViewModel.copyToClipboard(context, asJson = false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TXT", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { diagnosticsViewModel.shareReport(context, asJson = false) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Share", color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
            tint = PrimaryCyan,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            color = PrimaryCyan,
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
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = subtitle,
                color = TextSecondary,
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
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
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
                    color = TextSecondary,
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
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
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
            color = TextSecondary,
            fontSize = 11.sp,
            modifier = Modifier.weight(1.1f)
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.weight(1.5f)
        )
    }
}
