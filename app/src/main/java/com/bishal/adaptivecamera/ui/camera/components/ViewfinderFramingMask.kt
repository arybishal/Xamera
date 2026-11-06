package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.camera.config.AspectRatioConfig

/**
 * Letterbox and pillarbox framing masks for aspect ratio preview.
 * Darkens non-active sensor areas to provide an exact photographic composition preview.
 */
@Composable
fun ViewfinderFramingMask(
    aspectRatio: AspectRatioConfig,
    framingBounds: Rect,
    modifier: Modifier = Modifier,
    maskColor: Color = Color.Black.copy(alpha = 0.88f)
) {
    if (aspectRatio == AspectRatioConfig.FULL || framingBounds.width <= 0f || framingBounds.height <= 0f) {
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val totalWidth = size.width
        val totalHeight = size.height

        val left = framingBounds.left
        val top = framingBounds.top
        val right = framingBounds.right
        val bottom = framingBounds.bottom

        // Top letterbox
        if (top > 0f) {
            drawRect(
                color = maskColor,
                topLeft = Offset(0f, 0f),
                size = Size(totalWidth, top)
            )
        }

        // Bottom letterbox
        if (bottom < totalHeight) {
            drawRect(
                color = maskColor,
                topLeft = Offset(0f, bottom),
                size = Size(totalWidth, totalHeight - bottom)
            )
        }

        // Left pillarbox
        if (left > 0f) {
            drawRect(
                color = maskColor,
                topLeft = Offset(0f, top),
                size = Size(left, bottom - top)
            )
        }

        // Right pillarbox
        if (right < totalWidth) {
            drawRect(
                color = maskColor,
                topLeft = Offset(right, top),
                size = Size(totalWidth - right, bottom - top)
            )
        }

        // Subtle 1dp border outlining the active composition frame
        drawRect(
            color = Color.White.copy(alpha = 0.20f),
            topLeft = Offset(left, top),
            size = Size(framingBounds.width, framingBounds.height),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}
