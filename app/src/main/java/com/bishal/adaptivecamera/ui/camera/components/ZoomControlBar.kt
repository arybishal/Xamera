package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import kotlin.math.abs

@Composable
fun ZoomControlBar(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    onZoomSelected: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    // Generate logical zoom steps within hardware boundaries
    val candidateSteps = if (minZoom <= 0.55f) {
        listOf(0.5f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
    } else if (minZoom <= 0.65f) {
        listOf(0.6f, 1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
    } else {
        listOf(1.0f, 2.0f, 3.0f, 5.0f, 10.0f)
    }
    val validSteps = candidateSteps.filter { it in minZoom..maxZoom }
    val displaySteps = if (validSteps.isEmpty()) listOf(1.0f) else validSteps

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        displaySteps.forEach { step ->
            val isSelected = abs(currentZoom - step) < 0.15f
            val label = if (step < 1.0f) String.format(java.util.Locale.US, "%.1f", step) else step.toInt().toString()
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isSelected) PrimaryCyan else Color.Transparent)
                    .clickable { onZoomSelected(step) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${label}x",
                    color = if (isSelected) Color.Black else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
