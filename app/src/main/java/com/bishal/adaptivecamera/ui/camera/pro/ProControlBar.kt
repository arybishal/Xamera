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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import com.bishal.adaptivecamera.photography.pro.KelvinConverter
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
    LOCKS,
    MONITOR,
    RAW
}

@Composable
fun ProControlBar(
    proConfig: ProConfiguration,
    activeProfile: CameraCapabilityProfile?,
    onSetManualIso: (Boolean, Int) -> Unit,
    onSetManualShutter: (Boolean, Long) -> Unit,
    onSetManualFocus: (Boolean, Float) -> Unit,
    onSetWhiteBalance: (ProWhiteBalance) -> Unit,
    onSetManualKelvin: (Boolean, Int, Int) -> Unit = { _, _, _ -> },
    onToggleAeLock: () -> Unit = {},
    onToggleAfLock: () -> Unit = {},
    onToggleAwbLock: () -> Unit = {},
    onToggleRaw: (Boolean) -> Unit,
    onToggleHistogram: () -> Unit,
    onTogglePeaking: (Boolean) -> Unit,
    onSetPeakingSensitivity: (Float) -> Unit,
    onToggleZebra: (Boolean) -> Unit = {},
    onSetZebraThreshold: (Int) -> Unit = {},
    onToggleFalseColor: (Boolean) -> Unit = {},
    onToggleFocusMagnifier: (Boolean) -> Unit = {},
    onSetFocusMagnifierScale: (Float) -> Unit = {},
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
            .background(DarkBackground.copy(alpha = 0.92f))
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
                            isAfLocked = proConfig.isAfLocked,
                            isMagnifierEnabled = proConfig.isFocusMagnifierEnabled,
                            magnifierScale = proConfig.focusMagnifierScale,
                            onFocusChanged = { onSetManualFocus(true, it) },
                            onAutoSelected = { onSetManualFocus(false, 0f) },
                            onToggleAfLock = onToggleAfLock,
                            onToggleMagnifier = { onToggleFocusMagnifier(!proConfig.isFocusMagnifierEnabled) },
                            onSetMagnifierScale = onSetFocusMagnifierScale
                        )
                    }

                    ProControlTab.WB -> {
                        WbControlRow(
                            currentMode = proConfig.whiteBalance,
                            isManualKelvin = proConfig.isManualKelvin,
                            currentKelvin = proConfig.requestedKelvin,
                            currentTint = proConfig.requestedTint,
                            isAwbLocked = proConfig.isAwbLocked,
                            onModeSelected = onSetWhiteBalance,
                            onKelvinChanged = { k, t -> onSetManualKelvin(true, k, t) },
                            onToggleAwbLock = onToggleAwbLock
                        )
                    }

                    ProControlTab.LOCKS -> {
                        LocksControlRow(
                            isAeLocked = proConfig.isAeLocked,
                            isAfLocked = proConfig.isAfLocked,
                            isAwbLocked = proConfig.isAwbLocked,
                            onToggleAeLock = onToggleAeLock,
                            onToggleAfLock = onToggleAfLock,
                            onToggleAwbLock = onToggleAwbLock
                        )
                    }

                    ProControlTab.MONITOR -> {
                        MonitorControlRow(
                            isHistogramVisible = proConfig.isHistogramVisible,
                            isPeakingEnabled = proConfig.isFocusPeakingEnabled,
                            peakingSensitivity = proConfig.focusPeakingSensitivity,
                            isZebraEnabled = proConfig.isZebraEnabled,
                            zebraThreshold = proConfig.zebraThresholdIre,
                            isFalseColorEnabled = proConfig.isFalseColorEnabled,
                            onToggleHistogram = onToggleHistogram,
                            onTogglePeaking = onTogglePeaking,
                            onSetPeakingSensitivity = onSetPeakingSensitivity,
                            onToggleZebra = onToggleZebra,
                            onSetZebraThreshold = onSetZebraThreshold,
                            onToggleFalseColor = onToggleFalseColor
                        )
                    }

                    ProControlTab.RAW -> {
                        RawControlRow(
                            isEnabled = proConfig.isRawCaptureEnabled,
                            supportsRaw = supportsRaw,
                            onToggleRaw = onToggleRaw
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
            ProTabChip("FOCUS", activeTab == ProControlTab.FOCUS, proConfig.isManualFocus || proConfig.isAfLocked) {
                activeTab = if (activeTab == ProControlTab.FOCUS) ProControlTab.NONE else ProControlTab.FOCUS
            }
            ProTabChip("WB", activeTab == ProControlTab.WB, proConfig.isManualKelvin || proConfig.isAwbLocked || proConfig.whiteBalance != ProWhiteBalance.AUTO) {
                activeTab = if (activeTab == ProControlTab.WB) ProControlTab.NONE else ProControlTab.WB
            }
            ProTabChip("LOCKS", activeTab == ProControlTab.LOCKS, proConfig.isAeLocked || proConfig.isAfLocked || proConfig.isAwbLocked) {
                activeTab = if (activeTab == ProControlTab.LOCKS) ProControlTab.NONE else ProControlTab.LOCKS
            }
            ProTabChip("MONITOR", activeTab == ProControlTab.MONITOR, proConfig.isZebraEnabled || proConfig.isFalseColorEnabled || proConfig.isFocusPeakingEnabled) {
                activeTab = if (activeTab == ProControlTab.MONITOR) ProControlTab.NONE else ProControlTab.MONITOR
            }
            ProTabChip("RAW", activeTab == ProControlTab.RAW, proConfig.isRawCaptureEnabled) {
                activeTab = if (activeTab == ProControlTab.RAW) ProControlTab.NONE else ProControlTab.RAW
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoOptionChip(isSelected = !isManual, onClick = onAutoSelected)
            Text(
                text = if (isManual) "ISO $currentIso" else "ISO AUTO",
                color = if (isManual) PrimaryCyan else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        if (isManual) {
            Slider(
                value = currentIso.toFloat(),
                onValueChange = { onIsoChanged(it.roundToInt()) },
                valueRange = minIso.toFloat()..maxIso.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryCyan,
                    activeTrackColor = PrimaryCyan
                )
            )
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
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AutoOptionChip(isSelected = !isManual, onClick = onAutoSelected)
            Text(
                text = if (isManual) ShutterSpeedHelper.formatNanoseconds(currentShutterNs) else "SHUTTER AUTO",
                color = if (isManual) PrimaryCyan else TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
        if (isManual) {
            val minLog = kotlin.math.ln(minNs.toDouble())
            val maxLog = kotlin.math.ln(maxNs.toDouble())
            val currentLog = kotlin.math.ln(currentShutterNs.toDouble().coerceIn(minNs.toDouble(), maxNs.toDouble()))

            Slider(
                value = currentLog.toFloat(),
                onValueChange = { logVal ->
                    val calculatedNs = kotlin.math.exp(logVal.toDouble()).toLong()
                    onShutterChanged(calculatedNs)
                },
                valueRange = minLog.toFloat()..maxLog.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = PrimaryCyan,
                    activeTrackColor = PrimaryCyan
                )
            )
        }
    }
}

@Composable
private fun FocusControlRow(
    isManual: Boolean,
    currentDiopters: Float,
    maxDiopters: Float,
    isAfLocked: Boolean,
    isMagnifierEnabled: Boolean,
    magnifierScale: Float,
    onFocusChanged: (Float) -> Unit,
    onAutoSelected: () -> Unit,
    onToggleAfLock: () -> Unit,
    onToggleMagnifier: () -> Unit,
    onSetMagnifierScale: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AutoOptionChip(isSelected = !isManual && !isAfLocked, onClick = onAutoSelected)
                // AF Lock Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isAfLocked) AccentAmber else DarkSurface)
                        .clickable { onToggleAfLock() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isAfLocked) "AF-LOCKED" else "LOCK AF",
                        color = if (isAfLocked) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Focus Loupe Button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isMagnifierEnabled) PrimaryCyan else DarkSurface)
                        .clickable { onToggleMagnifier() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isMagnifierEnabled) "${String.format("%.0f", magnifierScale)}x LOUPE" else "LOUPE",
                        color = if (isMagnifierEnabled) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = if (isManual) {
                    if (currentDiopters <= 0.01f) "∞ (Infinity)"
                    else "${(100f / currentDiopters).toInt()}cm"
                } else if (isAfLocked) "LOCKED" else "AUTO FOCUS",
                color = if (isManual || isAfLocked) AccentAmber else TextSecondary,
                fontSize = 12.sp,
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
    isManualKelvin: Boolean,
    currentKelvin: Int,
    currentTint: Int,
    isAwbLocked: Boolean,
    onModeSelected: (ProWhiteBalance) -> Unit,
    onKelvinChanged: (Int, Int) -> Unit,
    onToggleAwbLock: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        // Preset / Kelvin Mode Selector Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // AWB Lock Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isAwbLocked) PrimaryCyan else DarkSurface)
                    .clickable { onToggleAwbLock() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isAwbLocked) "AWB-LOCKED" else "LOCK AWB",
                    color = if (isAwbLocked) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            ProWhiteBalance.values().forEach { wb ->
                val selected = if (wb == ProWhiteBalance.MANUAL_KELVIN) isManualKelvin else (!isManualKelvin && currentMode == wb)
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

        // Custom Kelvin & Tint Sliders
        if (isManualKelvin || currentMode == ProWhiteBalance.MANUAL_KELVIN) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Color Temperature: ${currentKelvin}K", color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text("Tint: ${if (currentTint > 0) "+$currentTint M" else if (currentTint < 0) "$currentTint G" else "0"}", color = TextSecondary, fontSize = 11.sp)
                }
                Slider(
                    value = currentKelvin.toFloat(),
                    onValueChange = { onKelvinChanged(it.roundToInt(), currentTint) },
                    valueRange = KelvinConverter.MIN_KELVIN.toFloat()..KelvinConverter.MAX_KELVIN.toFloat(),
                    colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan)
                )
            }
        }
    }
}

@Composable
private fun LocksControlRow(
    isAeLocked: Boolean,
    isAfLocked: Boolean,
    isAwbLocked: Boolean,
    onToggleAeLock: () -> Unit,
    onToggleAfLock: () -> Unit,
    onToggleAwbLock: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // AE Lock
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAeLocked) AccentAmber else DarkSurface)
                .clickable { onToggleAeLock() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAeLocked) "AE LOCKED" else "AE LOCK",
                color = if (isAeLocked) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // AF Lock
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAfLocked) AccentAmber else DarkSurface)
                .clickable { onToggleAfLock() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAfLocked) "AF LOCKED" else "AF LOCK",
                color = if (isAfLocked) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // AWB Lock
        Box(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isAwbLocked) PrimaryCyan else DarkSurface)
                .clickable { onToggleAwbLock() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isAwbLocked) "AWB LOCKED" else "AWB LOCK",
                color = if (isAwbLocked) Color.Black else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun MonitorControlRow(
    isHistogramVisible: Boolean,
    isPeakingEnabled: Boolean,
    peakingSensitivity: Float,
    isZebraEnabled: Boolean,
    zebraThreshold: Int,
    isFalseColorEnabled: Boolean,
    onToggleHistogram: () -> Unit,
    onTogglePeaking: (Boolean) -> Unit,
    onSetPeakingSensitivity: (Float) -> Unit,
    onToggleZebra: (Boolean) -> Unit,
    onSetZebraThreshold: (Int) -> Unit,
    onToggleFalseColor: (Boolean) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Histogram Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isHistogramVisible) PrimaryCyan else DarkSurface)
                    .clickable { onToggleHistogram() }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isHistogramVisible) "HIST ON" else "HIST OFF",
                    color = if (isHistogramVisible) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Peaking Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isPeakingEnabled) SuccessGreen else DarkSurface)
                    .clickable { onTogglePeaking(!isPeakingEnabled) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isPeakingEnabled) "PEAK ON" else "PEAK OFF",
                    color = if (isPeakingEnabled) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Zebra Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isZebraEnabled) Color(0xFFFFEB3B) else DarkSurface)
                    .clickable { onToggleZebra(!isZebraEnabled) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isZebraEnabled) "ZEBRA ${zebraThreshold}%" else "ZEBRA OFF",
                    color = if (isZebraEnabled) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // False Color Toggle
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isFalseColorEnabled) Color(0xFFAB47BC) else DarkSurface)
                    .clickable { onToggleFalseColor(!isFalseColorEnabled) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isFalseColorEnabled) "FALSE COLOR ON" else "FALSE COLOR OFF",
                    color = if (isFalseColorEnabled) Color.White else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Zebra Threshold Pills when Zebra is Active
        if (isZebraEnabled) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("IRE Clip:", color = TextSecondary, fontSize = 10.sp)
                listOf(70, 80, 90, 95, 100).forEach { th ->
                    val selected = zebraThreshold == th
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (selected) Color(0xFFFFEB3B) else DarkSurface)
                            .clickable { onSetZebraThreshold(th) }
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${th}%",
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
