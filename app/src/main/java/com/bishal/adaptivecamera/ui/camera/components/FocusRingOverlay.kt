package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan

@Composable
fun FocusRingOverlay(
    focusPointNorm: Pair<Float, Float>,
    containerWidthPx: Float,
    containerHeightPx: Float
) {
    val scale = remember { Animatable(1.4f) }
    val alpha = remember { Animatable(1.0f) }

    LaunchedEffect(focusPointNorm) {
        scale.snapTo(1.4f)
        alpha.snapTo(1.0f)
        scale.animateTo(
            targetValue = 1.0f,
            animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)
        )
    }

    val xPos = (focusPointNorm.first * containerWidthPx - 36).toInt()
    val yPos = (focusPointNorm.second * containerHeightPx - 36).toInt()

    Box(
        modifier = Modifier
            .offset { IntOffset(xPos, yPos) }
            .size(72.dp)
            .scale(scale.value)
            .alpha(alpha.value)
            .border(1.5.dp, PrimaryCyan, RoundedCornerShape(8.dp))
    )
}
