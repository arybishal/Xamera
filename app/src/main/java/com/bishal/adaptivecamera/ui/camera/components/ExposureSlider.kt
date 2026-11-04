package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@Composable
fun ExposureSlider(
    currentIndex: Int,
    minIndex: Int,
    maxIndex: Int,
    stepEv: Float,
    onIndexChanged: (Int) -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (minIndex >= maxIndex) return

    val currentEv = currentIndex * stepEv
    val evDisplay = if (currentEv > 0f) String.format("+%.1f", currentEv)
    else if (currentEv < 0f) String.format("%.1f", currentEv)
    else "0.0"

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface.copy(alpha = 0.88f))
            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$evDisplay EV",
                color = PrimaryCyan,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Rotated slider for vertical orientation
            Box(
                modifier = Modifier
                    .height(140.dp)
                    .width(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Slider(
                    value = currentIndex.toFloat(),
                    onValueChange = { onIndexChanged(it.toInt()) },
                    valueRange = minIndex.toFloat()..maxIndex.toFloat(),
                    steps = (maxIndex - minIndex) - 1,
                    colors = SliderDefaults.colors(
                        thumbColor = PrimaryCyan,
                        activeTrackColor = PrimaryCyan,
                        inactiveTrackColor = DividerColor
                    ),
                    modifier = Modifier
                        .rotate(270f)
                        .width(130.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            IconButton(
                onClick = onReset,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.RestartAlt,
                    contentDescription = "Reset Exposure",
                    tint = TextSecondary
                )
            }
        }
    }
}
