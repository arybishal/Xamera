package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import com.bishal.adaptivecamera.photography.pro.ZebraStripingMap

/**
 * Viewfinder overlay rendering real-time diagonal zebra striping over overexposed areas.
 */
@Composable
fun ZebraStripingOverlay(
    zebraMap: ZebraStripingMap,
    framingBounds: Rect,
    modifier: Modifier = Modifier
) {
    if (!zebraMap.isEnabled || zebraMap.points.isEmpty()) return

    val stripeColor = Color(0xFFFFEB3B).copy(alpha = 0.85f) // High-visibility warning yellow
    val stripeDark = Color(0xFF212121).copy(alpha = 0.85f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val frameLeft = framingBounds.left
        val frameTop = framingBounds.top
        val frameWidth = framingBounds.width
        val frameHeight = framingBounds.height

        if (frameWidth <= 0f || frameHeight <= 0f) return@Canvas

        val stripeLength = 12f
        val strokeWidth = 2.5f

        for (point in zebraMap.points) {
            val cx = frameLeft + (point.xNorm * frameWidth)
            val cy = frameTop + (point.yNorm * frameHeight)

            if (cx < frameLeft || cx > frameLeft + frameWidth ||
                cy < frameTop || cy > frameTop + frameHeight
            ) continue

            // Draw alternating 45° diagonal zebra hash marks
            val isEven = ((point.xNorm * 100f).toInt() + (point.yNorm * 100f).toInt()) % 2 == 0
            val color = if (isEven) stripeColor else stripeDark

            drawLine(
                color = color,
                start = Offset(cx - stripeLength / 2f, cy - stripeLength / 2f),
                end = Offset(cx + stripeLength / 2f, cy + stripeLength / 2f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}
