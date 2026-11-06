package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.photography.pro.FalseColorMap
import com.bishal.adaptivecamera.photography.pro.FalseColorZone
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.TextPrimary

/**
 * Full-Spectrum False Color exposure heatmap overlay and broadcast IRE legend.
 */
@Composable
fun FalseColorOverlay(
    falseColorMap: FalseColorMap,
    framingBounds: Rect,
    modifier: Modifier = Modifier
) {
    if (!falseColorMap.isEnabled) return

    Box(modifier = modifier.fillMaxSize()) {
        // 1. Heatmap pixel grid
        Canvas(modifier = Modifier.fillMaxSize()) {
            val frameLeft = framingBounds.left
            val frameTop = framingBounds.top
            val frameWidth = framingBounds.width
            val frameHeight = framingBounds.height

            if (frameWidth <= 0f || frameHeight <= 0f) return@Canvas

            val sampleSize = Size(frameWidth / 55f, frameHeight / 55f)

            for (sample in falseColorMap.samples) {
                val px = frameLeft + (sample.xNorm * frameWidth)
                val py = frameTop + (sample.yNorm * frameHeight)

                if (px < frameLeft || px > frameLeft + frameWidth ||
                    py < frameTop || py > frameTop + frameHeight
                ) continue

                drawRect(
                    color = Color(sample.colorArgb).copy(alpha = 0.65f),
                    topLeft = Offset(px - sampleSize.width / 2f, py - sampleSize.height / 2f),
                    size = sampleSize
                )
            }
        }

        // 2. Vertical IRE Color Legend Bar (Top Right)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.Black.copy(alpha = 0.70f))
                .border(0.5.dp, Color.White.copy(alpha = 0.20f), RoundedCornerShape(8.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "IRE",
                color = TextPrimary,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )

            val legendZones = listOf(
                FalseColorZone.CLIPPED_HIGHLIGHT,
                FalseColorZone.NEAR_CLIP,
                FalseColorZone.SKIN_TONES,
                FalseColorZone.MIDDLE_GRAY_18,
                FalseColorZone.MID_SHADOW,
                FalseColorZone.DEEP_SHADOW,
                FalseColorZone.CRUSHED_BLACK
            )

            for (zone in legendZones) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color(zone.colorArgb))
                    )
                    Text(
                        text = "${zone.ireMax}",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}
