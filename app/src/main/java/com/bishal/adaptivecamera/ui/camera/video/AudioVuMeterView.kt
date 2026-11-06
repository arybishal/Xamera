package com.bishal.adaptivecamera.ui.camera.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.video.audio.AudioVuState

/**
 * Real-time audio VU meter displaying left and right audio levels in dBFS
 * with green (-60 to -12 dBFS), yellow (-12 to -3 dBFS), and red clipping segments.
 */
@Composable
fun AudioVuMeterView(
    vuState: AudioVuState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x99000000))
            .padding(horizontal = 6.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = "MIC",
            color = if (vuState.isMuted) Color.Gray else Color(0xFF888888),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(2.dp),
            modifier = Modifier.width(42.dp)
        ) {
            VuChannelBar(fraction = if (vuState.isMuted) 0f else vuState.leftFraction)
            VuChannelBar(fraction = if (vuState.isMuted) 0f else vuState.rightFraction)
        }

        if (vuState.isClipping) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(8.dp)
                    .background(Color(0xFFFF3B30), RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun VuChannelBar(
    fraction: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color(0xFF222222), RoundedCornerShape(1.dp))
    ) {
        val barColor = when {
            fraction > 0.95f -> Color(0xFFFF3B30) // Red clip zone
            fraction > 0.80f -> Color(0xFFFFCC00) // Amber warning
            else -> Color(0xFF34C759)             // Green nominal zone
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(barColor, RoundedCornerShape(1.dp))
        )
    }
}
