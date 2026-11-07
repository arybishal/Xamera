package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.MinimalistGold

/**
 * Mid-viewfinder control row matching the reference screenshot.
 * Houses the left accessory icon (Beauty/AI Filter), the central zoom selector,
 * and the right accessory icon (3D LUT & Color Science studio).
 */
@Composable
fun MinimalistViewfinderControls(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    availableSteps: List<Float>,
    isLookActive: Boolean,
    onZoomChanged: (Float) -> Unit,
    onToggleLooks: () -> Unit,
    onToggleBeautyOrAi: () -> Unit,
    onHapticTick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Accessory Icon: Beauty / AI Portrait Filter
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkCharcoal.copy(alpha = 0.65f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleBeautyOrAi
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = "AI Effects & Retouch",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }

        // Center: Dynamic Zoom Step Selector (0.6, 1, 2x)
        MinimalistZoomBar(
            currentZoom = currentZoom,
            minZoom = minZoom,
            maxZoom = maxZoom,
            availableSteps = availableSteps,
            onZoomChanged = onZoomChanged,
            onHapticTick = onHapticTick
        )

        // Right Accessory Icon: 3D LUT & Creative Color Science (Venn diagram / Palette)
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(DarkCharcoal.copy(alpha = 0.65f))
                .border(
                    1.dp,
                    if (isLookActive) MinimalistGold else Color.White.copy(alpha = 0.08f),
                    CircleShape
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onToggleLooks
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "3D LUTs & Film Grain",
                tint = if (isLookActive) MinimalistGold else Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
