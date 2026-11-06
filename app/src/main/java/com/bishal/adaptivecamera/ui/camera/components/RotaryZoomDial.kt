package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.roundToInt

/**
 * Apple Camera-inspired rotary zoom dial and step pill control.
 * Provides discrete lens shortcuts in compact mode, expanding into a precision rotary dial on drag.
 */
@Composable
fun RotaryZoomDial(
    currentZoom: Float,
    minZoom: Float,
    maxZoom: Float,
    availableSteps: List<Float>,
    onZoomChanged: (Float) -> Unit,
    onHapticTick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var lastInteractionTime by remember { mutableStateOf(0L) }
    var lastHapticStepIndex by remember { mutableIntStateOf(-1) }

    // Auto-collapse timer
    LaunchedEffect(lastInteractionTime) {
        if (isExpanded && lastInteractionTime > 0) {
            delay(2200)
            isExpanded = false
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Expanded Precision Rotary Dial
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                // Large numeric readout
                val zoomFormatted = if (currentZoom < 10f) {
                    String.format(java.util.Locale.US, "%.1fx", currentZoom)
                } else {
                    String.format(java.util.Locale.US, "%dx", currentZoom.toInt())
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = zoomFormatted,
                        color = PrimaryCyan,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Interactive Rotary Graduation Dial Canvas
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(38.dp)
                        .clip(RoundedCornerShape(19.dp))
                        .background(Color.Black.copy(alpha = 0.50f))
                        .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(19.dp))
                        .pointerInput(minZoom, maxZoom) {
                            detectDragGestures(
                                onDragStart = {
                                    isExpanded = true
                                    lastInteractionTime = System.currentTimeMillis()
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    lastInteractionTime = System.currentTimeMillis()

                                    // Log-space drag mapping for natural optical zoom scaling
                                    val sensitivity = 0.0035f
                                    val currentLog = ln(currentZoom.coerceIn(minZoom, maxZoom))
                                    val deltaLog = dragAmount.x * sensitivity
                                    val newZoom = Math.exp((currentLog + deltaLog).toDouble()).toFloat().coerceIn(minZoom, maxZoom)

                                    if (abs(newZoom - currentZoom) > 0.02f) {
                                        onZoomChanged(newZoom)

                                        // Check if crossed a discrete step
                                        val nearestStep = availableSteps.minByOrNull { abs(it - newZoom) }
                                        if (nearestStep != null && abs(nearestStep - newZoom) < 0.08f) {
                                            val stepIdx = availableSteps.indexOf(nearestStep)
                                            if (stepIdx != lastHapticStepIndex) {
                                                lastHapticStepIndex = stepIdx
                                                onHapticTick()
                                            }
                                        } else {
                                            lastHapticStepIndex = -1
                                        }
                                    }
                                }
                            )
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxWidth().height(38.dp)) {
                        val width = size.width
                        val height = size.height
                        val centerX = width / 2f
                        val centerY = height / 2f

                        // Draw ticks relative to center zoom
                        val visibleRange = 2.5f
                        val tickSpacingPx = 14.dp.toPx()
                        val currentLog = ln(currentZoom.coerceIn(minZoom, maxZoom))

                        for (step in availableSteps) {
                            val stepLog = ln(step)
                            val offsetPx = (stepLog - currentLog) * (width / visibleRange)
                            val tickX = centerX + offsetPx

                            if (tickX in 0f..width) {
                                drawLine(
                                    color = if (abs(currentZoom - step) < 0.12f) PrimaryCyan else Color.White.copy(alpha = 0.7f),
                                    start = Offset(tickX, centerY - 10.dp.toPx()),
                                    end = Offset(tickX, centerY + 10.dp.toPx()),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        // Center reticle needle
                        drawLine(
                            color = PrimaryCyan,
                            start = Offset(centerX, centerY - 12.dp.toPx()),
                            end = Offset(centerX, centerY + 12.dp.toPx()),
                            strokeWidth = 2.5.dp.toPx()
                        )
                    }
                }
            }
        }

        // 2. Compact Step Pill Selector Bar
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f))
                .border(1.dp, Color.White.copy(alpha = 0.15f), CircleShape)
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .pointerInput(minZoom, maxZoom) {
                    detectDragGestures(
                        onDragStart = {
                            isExpanded = true
                            lastInteractionTime = System.currentTimeMillis()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            isExpanded = true
                            lastInteractionTime = System.currentTimeMillis()

                            val sensitivity = 0.004f
                            val currentLog = ln(currentZoom.coerceIn(minZoom, maxZoom))
                            val deltaLog = dragAmount.x * sensitivity
                            val newZoom = Math.exp((currentLog + deltaLog).toDouble()).toFloat().coerceIn(minZoom, maxZoom)
                            onZoomChanged(newZoom)
                        }
                    )
                },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            availableSteps.forEach { step ->
                val isSelected = abs(currentZoom - step) < 0.15f

                val label = if (step == 0.6f) "0.6" else if (step == 1.0f) "1" else if (step == 2.0f) "2" else if (step == 3.0f) "3" else if (step == 5.0f) "5" else "${step.toInt()}"

                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) PrimaryCyan else Color.Transparent)
                        .clickable {
                            onHapticTick()
                            onZoomChanged(step)
                            lastInteractionTime = System.currentTimeMillis()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelected && abs(currentZoom - step) > 0.05f) {
                            String.format(java.util.Locale.US, "%.1f", currentZoom)
                        } else {
                            label
                        },
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
