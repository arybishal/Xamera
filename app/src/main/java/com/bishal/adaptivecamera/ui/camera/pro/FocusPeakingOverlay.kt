package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.photography.pro.FocusPeakingMap

@Composable
fun FocusPeakingOverlay(
    peakingMap: FocusPeakingMap,
    modifier: Modifier = Modifier
) {
    if (!peakingMap.isEnabled || peakingMap.points.isEmpty()) return

    val peakingColor = Color(0xFF00E676) // Sharp neon green for maximum peaking visibility

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        for (pt in peakingMap.points) {
            val cx = pt.xNorm * canvasWidth
            val cy = pt.yNorm * canvasHeight

            drawCircle(
                color = peakingColor.copy(alpha = (0.4f + pt.intensityNorm * 0.5f).coerceIn(0.1f, 0.9f)),
                radius = 2.dp.toPx(),
                center = Offset(cx, cy)
            )
        }
    }
}
