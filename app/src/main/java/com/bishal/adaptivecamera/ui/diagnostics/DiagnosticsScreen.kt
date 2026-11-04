package com.bishal.adaptivecamera.ui.diagnostics

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.camera.capability.CameraHardwareLevel
import com.bishal.adaptivecamera.core.diagnostics.DeviceInfo
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
fun DiagnosticsScreen(
    viewModel: DiagnosticsViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.updateCameraPermission(isGranted, context)
    }

    LaunchedEffect(Unit) {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        viewModel.updateCameraPermission(hasPerm, context)
    }

    LaunchedEffect(uiState.toastMessage) {
        uiState.toastMessage?.let { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToastMessage()
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ADAPTIVE CAMERA",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Hardware Intelligence & Capability Scanner",
                            color = PrimaryCyan,
                            fontSize = 11.sp
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.refreshHardwareScan(context) }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Rescan",
                            tint = PrimaryCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        },
        bottomBar = {
            BottomExportBar(
                onCopyText = { viewModel.copyToClipboard(context, asJson = false) },
                onCopyJson = { viewModel.copyToClipboard(context, asJson = true) },
                onShare = { viewModel.shareReport(context, asJson = true) }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = PrimaryCyan)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Scanning Camera Hardware & Capabilities...",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (!uiState.hasCameraPermission) {
                        item {
                            PermissionBanner(
                                onRequestPermission = {
                                    permissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            )
                        }
                    }

                    uiState.report?.let { report ->
                        item {
                            DeviceInfoCard(report.deviceInfo)
                        }

                        if (report.diagnosticWarnings.isNotEmpty()) {
                            item {
                                WarningsCard(report.diagnosticWarnings)
                            }
                        }

                        item {
                            SummaryCard(report)
                        }

                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "DISCOVERED SENSORS (${report.cameraProfiles.size})",
                                    color = PrimaryCyan,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    letterSpacing = 0.5.sp
                                )
                                Text(
                                    text = "Zero Hardcoded Assumptions",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }
                        }

                        itemsIndexed(report.cameraProfiles) { index, camera ->
                            CameraProfileCard(index = index, profile = camera)
                        }

                        item {
                            Spacer(modifier = Modifier.height(40.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionBanner(
    onRequestPermission: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2E2210)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AccentAmber.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AccentAmber
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Camera Permission Required",
                        color = AccentAmber,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "Grant camera permission to unlock full hardware scanning.",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }
            }
            Button(
                onClick = onRequestPermission,
                colors = ButtonDefaults.buttonColors(containerColor = AccentAmber),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Grant", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DeviceInfoCard(deviceInfo: DeviceInfo) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DEVICE IDENTIFICATION",
                    color = PrimaryCyan,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                if (deviceInfo.isPrimaryTargetAce5) {
                    TargetBadge(text = "OnePlus Ace 5 (Primary Target)", color = SuccessGreen)
                } else {
                    TargetBadge(text = "Generic / Global Device", color = AccentAmber)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            InfoRow("Manufacturer", deviceInfo.manufacturer)
            InfoRow("Model Identifier", deviceInfo.model)
            InfoRow("Product / Device", "${deviceInfo.product} / ${deviceInfo.device}")
            InfoRow("Brand & Hardware", "${deviceInfo.brand} (${deviceInfo.hardware})")
            InfoRow("Android Version", "${deviceInfo.androidVersion} (API ${deviceInfo.apiLevel})")
            InfoRow("Build Fingerprint", deviceInfo.buildFingerprint, isMonospace = true)
        }
    }
}

@Composable
private fun WarningsCard(warnings: List<String>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A1515)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(
                    text = "HARDWARE & FIRMWARE NOTICES",
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            warnings.forEach { w ->
                Text(
                    text = "• $w",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun SummaryCard(report: com.bishal.adaptivecamera.camera.capability.DeviceCapabilityReport) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "CAPABILITY OVERVIEW",
                color = PrimaryCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(label = "Accessible Cameras", value = report.cameraProfiles.size.toString())
                StatItem(label = "Rear Sensors", value = report.cameraProfiles.count { it.facing == 0 }.toString())
                StatItem(label = "Front Sensors", value = report.cameraProfiles.count { it.facing == 1 }.toString())
                StatItem(label = "RAW Capable", value = report.cameraProfiles.count { it.supportsRaw }.toString())
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = PrimaryCyan,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun CameraProfileCard(index: Int, profile: CameraCapabilityProfile) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = PrimaryCyan
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Camera ${profile.cameraId} (${profile.facingLabel})",
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                HwLevelBadge(profile.hardwareLevel)
            }
            Spacer(modifier = Modifier.height(10.dp))

            InfoRow("Logical Camera", if (profile.isLogicalCamera) "Yes (Multi-Camera)" else "Single Physical")
            if (profile.physicalCameraIds.isNotEmpty()) {
                InfoRow("Physical IDs", profile.physicalCameraIds.joinToString(", "))
            }
            if (profile.sensorActiveArraySize.isNotEmpty()) {
                InfoRow("Sensor Active Array", profile.sensorActiveArraySize)
            }
            InfoRow("Sensor Orientation", "${profile.sensorOrientation}°")
            if (profile.focalLengths.isNotEmpty()) {
                InfoRow("Focal Lengths", profile.focalLengths.joinToString(", ") { "${it}mm" })
            }
            InfoRow("Zoom Ratio Range", "${profile.minZoomRatio}x - ${profile.maxZoomRatio}x")

            Spacer(modifier = Modifier.height(6.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CapabilityTag("RAW: ${if (profile.supportsRaw) "YES" else "NO"}", profile.supportsRaw)
                CapabilityTag("MF: ${if (profile.supportsManualFocus) "YES" else "NO"}", profile.supportsManualFocus)
                CapabilityTag("Manual ISO/Exp: ${if (profile.supportsManualSensor) "YES" else "NO"}", profile.supportsManualSensor)
                CapabilityTag("HDR: ${if (profile.supportsHdr) "YES" else "NO"}", profile.supportsHdr)
            }
            Spacer(modifier = Modifier.height(8.dp))

            profile.isoRange?.let {
                InfoRow("ISO Range", "${it.lower} - ${it.upper}")
            }
            if (profile.exposureTimeRangeNs != null) {
                InfoRow("Shutter Speed Range", profile.exposureTimeRangeDisplay)
            }
            if (profile.minimumFocusDistance != null) {
                InfoRow("Min Focus Distance", "${profile.minimumFocusDistance} diopters")
            }
            profile.maxPhotoResolution?.let {
                InfoRow("Max Photo Size", it.toString())
            }
            profile.maxVideoResolution?.let {
                InfoRow("Max Video Size", it.toString())
            }
            if (profile.supportedFpsRanges.isNotEmpty()) {
                InfoRow("FPS Ranges", profile.supportedFpsRanges.joinToString(", ") { "${it.lower}-${it.upper}" })
            }
            if (profile.dynamicRangeProfilesSupported.isNotEmpty()) {
                InfoRow("Dynamic Range Profiles", profile.dynamicRangeProfilesSupported.joinToString(", "))
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String, isMonospace: Boolean = false) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp
        )
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

@Composable
private fun CapabilityTag(label: String, supported: Boolean) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (supported) SuccessGreen.copy(alpha = 0.15f) else DarkSurfaceVariant)
            .border(
                1.dp,
                if (supported) SuccessGreen.copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.3f),
                RoundedCornerShape(4.dp)
            )
            .padding(horizontal = 6.dp, vertical = 3.dp)
    ) {
        Text(
            text = label,
            color = if (supported) SuccessGreen else TextMuted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun HwLevelBadge(level: CameraHardwareLevel) {
    val (color, label) = when (level) {
        CameraHardwareLevel.LEVEL_3 -> PrimaryCyan to "LEVEL_3"
        CameraHardwareLevel.FULL -> SuccessGreen to "FULL"
        CameraHardwareLevel.LIMITED -> AccentAmber to "LIMITED"
        CameraHardwareLevel.LEGACY -> ErrorRed to "LEGACY"
        CameraHardwareLevel.EXTERNAL -> PrimaryCyan to "EXTERNAL"
        CameraHardwareLevel.UNKNOWN -> TextMuted to "UNKNOWN"
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun TargetBadge(text: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun BottomExportBar(
    onCopyText: () -> Unit,
    onCopyJson: () -> Unit,
    onShare: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onCopyText,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Copy TXT", fontSize = 11.sp)
        }
        OutlinedButton(
            onClick = onCopyJson,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryCyan)
        ) {
            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
            Text("Copy JSON", fontSize = 11.sp)
        }
        Button(
            onClick = onShare,
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryCyan)
        ) {
            Icon(Icons.Default.Share, contentDescription = null, tint = DarkBackground, modifier = Modifier.padding(end = 4.dp))
            Text("Share", color = DarkBackground, fontWeight = FontWeight.Bold, fontSize = 11.sp)
        }
    }
}
