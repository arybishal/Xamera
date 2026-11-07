package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.MinimalistGold
import kotlin.math.abs

/**
 * Clean, text-based horizontal zoom selector matching the reference screenshot.
 * Displays dynamically available optical and digital steps (e.g. 0.6, 1, 2x)
 * with a dark pill capsule and gold typography for the active step.
 */
@Composable
fun MinimalistZoomBar(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    availableSteps: List<Float>,
    onZoomChanged: (Float) -> Unit,
    onHapticTick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Generate valid steps constrained within hardware HAL boundaries
    val resolvedSteps = remember(minZoom, maxZoom, availableSteps) {
        if (availableSteps.isNotEmpty()) {
            availableSteps.filter { it in minZoom..maxZoom }
        } else {
            listOf(minZoom, 1.0f, 2.0f, 5.0f).filter { it in minZoom..maxZoom }
        }
    }

    Row(
        modifier = modifier
            .height(36.dp)
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        resolvedSteps.forEach { step ->
            val isSelected = abs(currentZoom - step) < 0.15f

            val textColor by animateColorAsState(
                targetValue = if (isSelected) MinimalistGold else Color.White.copy(alpha = 0.85f),
                animationSpec = tween(durationMillis = 200),
                label = "zoomTextColor"
            )

            val pillBgColor by animateColorAsState(
                targetValue = if (isSelected) DarkCharcoal.copy(alpha = 0.85f) else Color.Transparent,
                animationSpec = tween(durationMillis = 200),
                label = "zoomPillBg"
            )

            // Format step label: e.g. "0.6", "1", "2x"
            val label = when {
                step < 1.0f -> String.format(java.util.Locale.US, "%.1f", step)
                isSelected -> "${step.toInt()}x"
                else -> "${step.toInt()}"
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(pillBgColor)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        if (!isSelected) {
                            onHapticTick()
                            onZoomChanged(step)
                        }
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontSize = 13.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
