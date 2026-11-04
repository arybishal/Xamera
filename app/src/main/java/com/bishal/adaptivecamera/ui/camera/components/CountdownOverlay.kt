package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
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

@Composable
fun CountdownOverlay(
    secondsRemaining: Int?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = secondsRemaining != null && secondsRemaining > 0,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.65f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${secondsRemaining ?: ""}",
                color = PrimaryCyan,
                fontSize = 54.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}
