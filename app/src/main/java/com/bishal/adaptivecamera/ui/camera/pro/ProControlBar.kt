package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityProfile
import com.bishal.adaptivecamera.photography.pro.ProConfiguration
import com.bishal.adaptivecamera.photography.pro.ProWhiteBalance
import com.bishal.adaptivecamera.photography.pro.ShutterSpeedHelper
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import kotlin.math.roundToInt

enum class ProControlTab {
    NONE,
    ISO,
    SHUTTER,
    FOCUS,
    WB,
    RAW,
    PEAKING
}

@Composable
fun ProControlBar(
    proConfig: ProConfiguration,
    activeProfile: CameraCapabilityProfile?,
    onSetManualIso: (Boolean, Int) -> Unit,
    onSetManualShutter: (Boolean, Long) -> Unit,
    onSetManualFocus: (Boolean, Float) -> Unit,
    onSetWhiteBalance: (ProWhiteBalance) -> Unit,
    onToggleRaw: (Boolean) -> Unit,
    onToggleHistogram: () -> Unit,
    onTogglePeaking: (Boolean) -> Unit,
    onSetPeakingSensitivity: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(ProControlTab.NONE) }

    val isoMin = activeProfile?.isoRange?.lower ?: 100
    val isoMax = activeProfile?.isoRange?.upper ?: 6400
    val minExpNs = activeProfile?.exposureTimeRangeNs?.lower ?: 100_000L
    val maxExpNs = activeProfile?.exposureTimeRangeNs?.upper ?: 30_000_000_000L
    val minFocusDiopters = activeProfile?.minimumFocusDistance ?: 10.0f
    val supportsRaw = activeProfile?.supportsRaw ?: false

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .background(DarkBackground.copy(alpha = 0.90f))
            .border(1.dp, PrimaryCyan.copy(alpha = 0.25f), RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
            .padding(vertical = 8.dp)
    ) {
        // 1. Parameter Adjuster Drawer (Visible when a tab is active)
        AnimatedVisibility(
            visible = activeTab != ProControlTab.NONE,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                when (activeTab) {
                    ProControlTab.ISO -> {
                        IsoControlRow(
                            isManual = proConfig.isManualIso,
                            currentIso = proConfig.requestedIso,
                            minIso = isoMin,
                            maxIso = isoMax,
                            onIsoChanged = { onSetManualIso(true, it) },
                            onAutoSelected = { onSetManualIso(false, isoMin) }
                        )
                    }

                    ProControlTab.SHUTTER -> {
                        ShutterControlRow(
                            isManual = proConfig.isManualShutter,
                            currentShutterNs = proConfig.requestedShutterSpeedNs,
                            minNs = minExpNs,
                            maxNs = maxExpNs,
                            onShutterChanged = { onSetManualShutter(true, it) },
                            onAutoSelected = { onSetManualShutter(false, 16_666_666L) }
                        )
                    }

                    ProControlTab.FOCUS -> {
                        FocusControlRow(
                            isManual = proConfig.isManualFocus,
                            currentDiopters = proConfig.requestedFocusDistanceDiopters,
                            maxDiopters = minFocusDiopters,
                            onFocusChanged = { onSetManualFocus(true, it) },
                            onAutoSelected = { onSetManualFocus(false, 0f) }
                        )
                    }

                    ProControlTab.WB -> {
                        WbControlRow(
                            currentMode = proConfig.whiteBalance,
                            onModeSelected = onSetWhiteBalance
                        )
                    }

                    ProControlTab.RAW -> {
                        RawControlRow(
                            isEnabled = proConfig.isRawCaptureEnabled,
                            supportsRaw = supportsRaw,
                            onToggleRaw = onToggleRaw
                        )
                    }

                    ProControlTab.PEAKING -> {
                        PeakingControlRow(
                            isEnabled = proConfig.isFocusPeakingEnabled,
                            sensitivity = proConfig.focusPeakingSensitivity,
                            onTogglePeaking = onTogglePeaking,
                            onSetSensitivity = onSetPeakingSensitivity
                        )
                    }

                    ProControlTab.NONE -> Unit
                }
            }
        }

        // 2. Pro Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProTabChip("ISO", activeTab == ProControlTab.ISO, proConfig.isManualIso) {
                activeTab = if (activeTab == ProControlTab.ISO) ProControlTab.NONE else ProControlTab.ISO
            }
            ProTabChip("SHUTTER", activeTab == ProControlTab.SHUTTER, proConfig.isManualShutter) {
                activeTab = if (activeTab == ProControlTab.SHUTTER) ProControlTab.NONE else ProControlTab.SHUTTER
            }
            ProTabChip("FOCUS", activeTab == ProControlTab.FOCUS, proConfig.isManualFocus) {
                activeTab = if (activeTab == ProControlTab.FOCUS) ProControlTab.NONE else ProControlTab.FOCUS
            }
            ProTabChip("WB", activeTab == ProControlTab.WB, proConfig.whiteBalance != ProWhiteBalance.AUTO) {
                activeTab = if (activeTab == ProControlTab.WB) ProControlTab.NONE else ProControlTab.WB
            }
            ProTabChip("RAW", activeTab == ProControlTab.RAW, proConfig.isRawCaptureEnabled) {
                activeTab = if (activeTab == ProControlTab.RAW) ProControlTab.NONE else ProControlTab.RAW
            }
            ProTabChip("HIST", false, proConfig.isHistogramVisible) {
                onToggleHistogram()
            }
            ProTabChip("PEAK", activeTab == ProControlTab.PEAKING, proConfig.isFocusPeakingEnabled) {
                activeTab = if (activeTab == ProControlTab.PEAKING) ProControlTab.NONE else ProControlTab.PEAKING
            }
        }
    }
}

@Composable
private fun ProTabChip(
    label: String,
    isSelected: Boolean,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryCyan else if (isActive) AccentAmber else Color.Transparent
    val bg = if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else if (isActive) AccentAmber.copy(alpha = 0.15f) else DarkSurface

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bg)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (isSelected) PrimaryCyan else if (isActive) AccentAmber else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun IsoControlRow(
    isManual: Boolean,
    currentIso: Int,
    minIso: Int,
    maxIso: Int,
    onIsoChanged: (Int) -> Unit,
    onAutoSelected: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AutoOptionChip(isSelected = !isManual, onClick = onAutoSelected)

        val steps = listOf(50, 100, 200, 400, 800, 1600, 3200, 6400).filter { it in minIso..maxIso }
        steps.forEach { step ->
            val selected = isManual && currentIso == step
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) PrimaryCyan else DarkSurface)
                    .clickable { onIsoChanged(step) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "$step",
                    color = if (selected) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun ShutterControlRow(
    isManual: Boolean,
    currentShutterNs: Long,
    minNs: Long,
    maxNs: Long,
    onShutterChanged: (Long) -> Unit,
    onAutoSelected: () -> Unit
) {
    val presets = ShutterSpeedHelper.getSupportedPresets(minNs, maxNs)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AutoOptionChip(isSelected = !isManual, onClick = onAutoSelected)

        presets.forEach { preset ->
            val selected = isManual && (currentShutterNs == preset.nanoseconds)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) PrimaryCyan else DarkSurface)
                    .clickable { onShutterChanged(preset.nanoseconds) }
                    .padding(horizontal = 9.dp, vertical = 6.dp)
            ) {
                Text(
                    text = preset.displayString,
                    color = if (selected) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
private fun FocusControlRow(
    isManual: Boolean,
    currentDiopters: Float,
    maxDiopters: Float,
    onFocusChanged: (Float) -> Unit,
    onAutoSelected: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoOptionChip(isSelected = !isManual, onClick = onAutoSelected)
            Text(
                text = if (isManual) {
                    if (currentDiopters <= 0.05f) "∞ (Infinity)"
                    else "${(100.0f / currentDiopters).toInt()} cm (${String.format("%.1f", currentDiopters)} D)"
                } else "Auto Focus (AF)",
                color = if (isManual) AccentAmber else PrimaryCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        if (isManual) {
            Slider(
                value = currentDiopters,
                onValueChange = onFocusChanged,
                valueRange = 0f..maxDiopters,
                colors = SliderDefaults.colors(
                    thumbColor = AccentAmber,
                    activeTrackColor = AccentAmber
                )
            )
        }
    }
}

@Composable
private fun WbControlRow(
    currentMode: ProWhiteBalance,
    onModeSelected: (ProWhiteBalance) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProWhiteBalance.values().forEach { wb ->
            val selected = currentMode == wb
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (selected) PrimaryCyan else DarkSurface)
                    .clickable { onModeSelected(wb) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = wb.displayName,
                    color = if (selected) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun RawControlRow(
    isEnabled: Boolean,
    supportsRaw: Boolean,
    onToggleRaw: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Adobe DNG RAW Capture", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(
                text = if (supportsRaw) "Saves 10-bit/12-bit RAW_SENSOR + matching JPEG" else "Camera sensor does not support RAW_SENSOR output",
                color = if (supportsRaw) TextSecondary else Color(0xFFFF5252),
                fontSize = 10.sp
            )
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (!supportsRaw) DarkSurface else if (isEnabled) SuccessGreen else DarkSurface)
                .clickable(enabled = supportsRaw) { onToggleRaw(!isEnabled) }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (!supportsRaw) "UNAVAILABLE" else if (isEnabled) "RAW ACTIVE" else "ENABLE RAW",
                color = if (!supportsRaw) TextMuted else if (isEnabled) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun PeakingControlRow(
    isEnabled: Boolean,
    sensitivity: Float,
    onTogglePeaking: (Boolean) -> Unit,
    onSetSensitivity: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isEnabled) SuccessGreen else DarkSurface)
                .clickable { onTogglePeaking(!isEnabled) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(
                text = if (isEnabled) "PEAKING: ON" else "PEAKING: OFF",
                color = if (isEnabled) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (isEnabled) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Low" to 0.3f, "Med" to 0.5f, "High" to 0.8f).forEach { (label, value) ->
                    val selected = (sensitivity - value).let { kotlin.math.abs(it) < 0.15f }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selected) PrimaryCyan else DarkSurface)
                            .clickable { onSetSensitivity(value) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selected) Color.Black else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AutoOptionChip(
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (isSelected) PrimaryCyan else DarkSurface)
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = "AUTO",
            color = if (isSelected) Color.Black else TextPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
