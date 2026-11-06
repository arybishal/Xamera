package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Movable secondary floating shutter button.
 * Can be dragged within safe viewfinder boundaries to accommodate one-handed capture from any grip.
 */
@Composable
fun FloatingShutterButton(
    containerWidthPx: Float,
    containerHeightPx: Float,
    initialNormX: Float,
    initialNormY: Float,
    onShutterPressed: () -> Unit,
    onPositionChanged: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    if (containerWidthPx <= 0f || containerHeightPx <= 0f) return

    val buttonSizePx = 64f * 3f // Approximate ~64dp in px (we clamp based on container)
    val minX = 40f
    val maxX = (containerWidthPx - buttonSizePx).coerceAtLeast(minX)
    val minY = containerHeightPx * 0.15f
    val maxY = (containerHeightPx * 0.78f).coerceAtLeast(minY)

    var currentX by remember(initialNormX, containerWidthPx) {
        mutableFloatStateOf((initialNormX * containerWidthPx).coerceIn(minX, maxX))
    }
    var currentY by remember(initialNormY, containerHeightPx) {
        mutableFloatStateOf((initialNormY * containerHeightPx).coerceIn(minY, maxY))
    }

    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .offset { IntOffset(currentX.roundToInt(), currentY.roundToInt()) }
            .size(64.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.40f))
            .border(2.dp, Color.White.copy(alpha = 0.70f), CircleShape)
            .pointerInput(containerWidthPx, containerHeightPx) {
                detectDragGestures(
                    onDragStart = {
                        totalDragDistance = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val dx = dragAmount.x
                        val dy = dragAmount.y
                        totalDragDistance += sqrt(dx * dx + dy * dy)

                        currentX = (currentX + dx).coerceIn(minX, maxX)
                        currentY = (currentY + dy).coerceIn(minY, maxY)
                    },
                    onDragEnd = {
                        if (totalDragDistance < 12f) {
                            // Tap event triggered
                            onShutterPressed()
                        } else {
                            // Drag ended: persist position
                            val normX = (currentX / containerWidthPx).coerceIn(0.05f, 0.95f)
                            val normY = (currentY / containerHeightPx).coerceIn(0.10f, 0.90f)
                            onPositionChanged(normX, normY)
                        }
                    },
                    onDragCancel = {
                        totalDragDistance = 0f
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Inner white shutter core
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}
