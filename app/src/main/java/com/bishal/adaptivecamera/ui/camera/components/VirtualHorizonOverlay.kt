package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.core.sensors.VirtualHorizonState
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan

/**
 * Apple Camera-inspired minimalist virtual horizon level overlay.
 * Shows a central reference reticle and a dynamic rotating horizon line that snaps and turns cyan when level.
 */
@Composable
fun VirtualHorizonOverlay(
    horizonState: VirtualHorizonState,
    framingBounds: Rect,
    modifier: Modifier = Modifier
) {
    if (!horizonState.isAvailable || framingBounds.width <= 0f || framingBounds.height <= 0f) {
        return
    }

    val isLevel = horizonState.isLevel
    val rollDegrees = if (isLevel) 0f else horizonState.rollDegrees
    val lineColor = if (isLevel) PrimaryCyan else Color.White.copy(alpha = 0.50f)
    val referenceColor = Color.White.copy(alpha = 0.30f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val centerX = framingBounds.left + framingBounds.width / 2f
        val centerY = framingBounds.top + framingBounds.height / 2f

        val strokeWidth = 1.5.dp.toPx()
        val lineHalfLength = 48.dp.toPx()
        val gap = 16.dp.toPx()
        val refLength = 8.dp.toPx()

        // 1. Fixed reference reticle (Subtle horizontal outer ticks indicating absolute level)
        drawLine(
            color = referenceColor,
            start = Offset(centerX - lineHalfLength - refLength, centerY),
            end = Offset(centerX - lineHalfLength, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
        drawLine(
            color = referenceColor,
            start = Offset(centerX + lineHalfLength, centerY),
            end = Offset(centerX + lineHalfLength + refLength, centerY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // 2. Dynamic rotating horizon line
        rotate(degrees = -rollDegrees, pivot = Offset(centerX, centerY)) {
            // Left segment
            drawLine(
                color = lineColor,
                start = Offset(centerX - lineHalfLength, centerY),
                end = Offset(centerX - gap, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Right segment
            drawLine(
                color = lineColor,
                start = Offset(centerX + gap, centerY),
                end = Offset(centerX + lineHalfLength, centerY),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            // Central dot indicator
            drawCircle(
                color = lineColor,
                radius = 2.dp.toPx(),
                center = Offset(centerX, centerY)
            )
        }
    }
}
