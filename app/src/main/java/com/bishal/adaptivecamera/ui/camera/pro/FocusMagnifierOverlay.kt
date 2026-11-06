package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary

/**
 * Optical Focus Assist Magnifier Loupe aiding micro-focus precision during manual focusing.
 */
@Composable
fun FocusMagnifierOverlay(
    isEnabled: Boolean,
    scale: Float,
    framingBounds: Rect,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val loupeSizeDp = 180.dp

        // Loupe Target Frame with Crosshairs
        Canvas(modifier = Modifier.size(loupeSizeDp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 4f

            // Outer Target Reticle Circle
            drawCircle(
                color = PrimaryCyan,
                radius = radius,
                center = center,
                style = Stroke(
                    width = 2.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                )
            )

            // Precision Optical Crosshairs
            val armLength = 20.dp.toPx()
            val gap = 6.dp.toPx()

            // Left tick
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x - armLength, center.y),
                end = Offset(center.x - gap, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            // Right tick
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x + gap, center.y),
                end = Offset(center.x + armLength, center.y),
                strokeWidth = 1.5.dp.toPx()
            )
            // Top tick
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x, center.y - armLength),
                end = Offset(center.x, center.y - gap),
                strokeWidth = 1.5.dp.toPx()
            )
            // Bottom tick
            drawLine(
                color = Color.White.copy(alpha = 0.9f),
                start = Offset(center.x, center.y + gap),
                end = Offset(center.x, center.y + armLength),
                strokeWidth = 1.5.dp.toPx()
            )

            // Center Target Dot
            drawCircle(
                color = PrimaryCyan,
                radius = 2.dp.toPx(),
                center = center
            )
        }

        // Loupe Magnification Indicator Badge
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 135.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.75f))
                .border(0.5.dp, PrimaryCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${String.format("%.1f", scale)}x FOCUS LOUPE",
                color = TextPrimary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
