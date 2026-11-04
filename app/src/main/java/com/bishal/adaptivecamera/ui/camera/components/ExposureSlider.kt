package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun ExposureSlider(
    currentIndex: Int,
    minIndex: Int,
    maxIndex: Int,
    stepEv: Float,
    isSupported: Boolean = true,
    isReady: Boolean = true,
    onIndexChanged: (Int) -> Unit,
    onReset: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkBackground.copy(alpha = 0.90f))
            .border(1.dp, PrimaryCyan.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp)
    ) {
        if (!isReady) {
            // Camera initializing state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = PrimaryCyan,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Initializing...",
                    color = TextSecondary,
                    fontSize = 11.sp
                )
            }
            return@Box
        }

        if (!isSupported || minIndex >= maxIndex) {
            // Unsupported hardware state
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .width(100.dp)
                    .padding(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Exposure Fixed",
                    color = TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Sensor does not support compensation",
                    color = TextMuted,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
            return@Box
        }

        // Active Exposure Slider
        var sliderPos by remember(currentIndex) { mutableFloatStateOf(currentIndex.toFloat()) }
        val currentEv = sliderPos.roundToInt() * stepEv
        val evDisplay = if (currentEv > 0f) String.format("+%.1f", currentEv)
        else if (currentEv < 0f) String.format("%.1f", currentEv)
        else "0.0"

        val maxEv = maxIndex * stepEv
        val minEv = minIndex * stepEv

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Close Button
            Row(
                modifier = Modifier
                    .width(72.dp)
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EV",
                    color = TextMuted,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // Current Applied / Target EV display
            Text(
                text = "$evDisplay EV",
                color = PrimaryCyan,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )

            // Top boundary (+EV)
            Text(
                text = String.format("+%.1f", maxEv),
                color = TextMuted,
                fontSize = 9.sp,
                modifier = Modifier.padding(top = 4.dp)
            )

            // Rotated Vertical Slider
            Box(
                modifier = Modifier
                    .height(150.dp)
                    .width(48.dp),
                contentAlignment = Alignment.Center
            ) {
                val stepCount = (maxIndex - minIndex) - 1
                Slider(
                    value = sliderPos.coerceIn(minIndex.toFloat(), maxIndex.toFloat()),
                    onValueChange = { newPos ->
                        sliderPos = newPos
                        val rounded = newPos.roundToInt()
                        if (rounded != currentIndex) {
                            onIndexChanged(rounded)
                        }
                    },
                    valueRange = minIndex.toFloat()..maxIndex.toFloat(),
                    steps = if (stepCount > 0) stepCount else 0,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryCyan,
                        activeTrackColor = PrimaryCyan,
                        inactiveTrackColor = DividerColor
                    ),
                    modifier = Modifier
                        .rotate(270f)
                        .width(140.dp)
                )
            }

            // Bottom boundary (-EV)
            Text(
                text = String.format("%.1f", minEv),
                color = TextMuted,
                fontSize = 9.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reset Button
            IconButton(
                onClick = {
                    sliderPos = 0f
                    onReset()
                },
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurface)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Exposure to 0.0 EV",
                    tint = if (currentIndex != 0) PrimaryCyan else TextSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
