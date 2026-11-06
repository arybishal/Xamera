package com.bishal.adaptivecamera.ui.camera.lut

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.graphics.pipeline.GpuColorConfig
import com.bishal.adaptivecamera.lut.presets.BuiltInLutPresets
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import kotlin.math.roundToInt

/**
 * Floating glass color look & shader effects panel.
 * Provides real-time controls for active 3D LUT presets, intensity, film grain, and halation.
 */
@Composable
fun LookControlPanel(
    isVisible: Boolean,
    config: GpuColorConfig,
    onUpdateConfig: (GpuColorConfig) -> Unit,
    onOpenLutLibrary: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    allLuts: List<com.bishal.adaptivecamera.lut.repository.LutItem> = emptyList()
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
        modifier = modifier
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkSurface.copy(alpha = 0.94f)),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, PrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(PrimaryCyan.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Palette, contentDescription = null, tint = PrimaryCyan, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(text = "Color Look & Film Effects", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            val activeLut = allLuts.firstOrNull { it.id == config.selectedLutId }
                            val lookName = if (config.lutEnabled) {
                                activeLut?.name ?: BuiltInLutPresets.PRESET_INFOS.firstOrNull { it.id == config.selectedLutId }?.title ?: "Custom Look"
                            } else {
                                "Off (Natural)"
                            }
                            Text(text = "Active: $lookName (${(config.lutIntensity * 100).roundToInt()}%)", color = PrimaryCyan, fontSize = 11.sp)
                        }
                    }

                    IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. Preset Look Pills Row
                Text(text = "CREATIVE LOOK", color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // None / Off
                    LookChip(
                        title = "Off",
                        isSelected = !config.lutEnabled,
                        onClick = { onUpdateConfig(config.copy(lutEnabled = false)) }
                    )

                    // 5 Built-in presets
                    BuiltInLutPresets.PRESET_INFOS.forEach { preset ->
                        val isSelected = config.lutEnabled && config.selectedLutId == preset.id
                        LookChip(
                            title = preset.title,
                            isSelected = isSelected,
                            colorDot = Color(preset.previewColorArgb),
                            onClick = {
                                onUpdateConfig(config.copy(lutEnabled = true, selectedLutId = preset.id))
                            }
                        )
                    }

                    // User Imported LUTs
                    val importedLuts = allLuts.filter { !it.isBuiltIn }
                    importedLuts.forEach { item ->
                        val isSelected = config.lutEnabled && config.selectedLutId == item.id
                        LookChip(
                            title = item.name,
                            isSelected = isSelected,
                            colorDot = Color(item.previewColorArgb ?: 0xFF00BCD4.toInt()),
                            onClick = {
                                onUpdateConfig(config.copy(lutEnabled = true, selectedLutId = item.id))
                            }
                        )
                    }

                    // Open Full Library
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceVariant)
                            .clickable { onOpenLutLibrary() }
                            .padding(horizontal = 12.dp, vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "+ More...", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // 2. LUT Intensity Slider
                if (config.lutEnabled) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Look Strength", color = TextPrimary, fontSize = 11.sp)
                        Text(text = "${(config.lutIntensity * 100).roundToInt()}%", color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = config.lutIntensity,
                        onValueChange = { onUpdateConfig(config.copy(lutIntensity = it)) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. Film Grain Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Organic Film Grain", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Simulates 35mm silver halide emulsion", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(
                        checked = config.grainEnabled,
                        onCheckedChange = { onUpdateConfig(config.copy(grainEnabled = it, grainIntensity = if (it && config.grainIntensity == 0f) 0.3f else config.grainIntensity)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = AccentAmber, checkedTrackColor = AccentAmber.copy(alpha = 0.5f))
                    )
                }

                if (config.grainEnabled) {
                    Slider(
                        value = config.grainIntensity,
                        onValueChange = { onUpdateConfig(config.copy(grainIntensity = it)) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = AccentAmber, activeTrackColor = AccentAmber)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. Photochemical Halation Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = "Photochemical Halation", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        Text(text = "Red-amber highlight bloom & edge glow", color = TextSecondary, fontSize = 10.sp)
                    }
                    Switch(
                        checked = config.halationEnabled,
                        onCheckedChange = { onUpdateConfig(config.copy(halationEnabled = it, halationIntensity = if (it && config.halationIntensity == 0f) 0.35f else config.halationIntensity)) },
                        colors = SwitchDefaults.colors(checkedThumbColor = PrimaryCyan, checkedTrackColor = PrimaryCyan.copy(alpha = 0.5f))
                    )
                }

                if (config.halationEnabled) {
                    Slider(
                        value = config.halationIntensity,
                        onValueChange = { onUpdateConfig(config.copy(halationIntensity = it)) },
                        valueRange = 0.0f..1.0f,
                        colors = SliderDefaults.colors(thumbColor = PrimaryCyan, activeTrackColor = PrimaryCyan)
                    )
                }
            }
        }
    }
}

@Composable
private fun LookChip(
    title: String,
    isSelected: Boolean,
    colorDot: Color? = null,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) PrimaryCyan.copy(alpha = 0.2f) else DarkSurfaceVariant)
            .border(
                1.dp,
                if (isSelected) PrimaryCyan else Color.Transparent,
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (colorDot != null) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(colorDot)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = title,
                color = if (isSelected) PrimaryCyan else TextSecondary,
                fontSize = 11.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}
