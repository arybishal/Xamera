package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.camera.config.CompositionGridType

/**
 * High-performance, hardware-accelerated composition grid overlay.
 * Renders precisely within the active framing bounds without modifying captured frames.
 */
@Composable
fun CompositionGridOverlay(
    gridType: CompositionGridType,
    framingBounds: Rect,
    modifier: Modifier = Modifier,
    lineColor: Color = Color.White.copy(alpha = 0.38f)
) {
    if (gridType == CompositionGridType.OFF || framingBounds.width <= 0f || framingBounds.height <= 0f) {
        return
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val left = framingBounds.left
        val top = framingBounds.top
        val width = framingBounds.width
        val height = framingBounds.height
        val strokeWidth = 1.dp.toPx()

        when (gridType) {
            CompositionGridType.OFF -> { /* No-op */ }
            CompositionGridType.RULE_OF_THIRDS -> {
                drawRuleOfThirds(left, top, width, height, strokeWidth, lineColor)
            }
            CompositionGridType.GOLDEN_RATIO -> {
                drawGoldenRatioGrid(left, top, width, height, strokeWidth, lineColor)
            }
            CompositionGridType.GOLDEN_SPIRAL -> {
                drawGoldenSpiral(left, top, width, height, strokeWidth, lineColor)
            }
            CompositionGridType.CENTER_CROSSHAIRS -> {
                drawCenterCrosshairs(left, top, width, height, strokeWidth, lineColor)
            }
        }
    }
}

private fun DrawScope.drawRuleOfThirds(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    strokeWidth: Float,
    color: Color
) {
    val x1 = left + width / 3f
    val x2 = left + (width * 2f) / 3f
    val y1 = top + height / 3f
    val y2 = top + (height * 2f) / 3f

    // Vertical lines
    drawLine(color, Offset(x1, top), Offset(x1, top + height), strokeWidth)
    drawLine(color, Offset(x2, top), Offset(x2, top + height), strokeWidth)

    // Horizontal lines
    drawLine(color, Offset(left, y1), Offset(left + width, y1), strokeWidth)
    drawLine(color, Offset(left, y2), Offset(left + width, y2), strokeWidth)
}

private fun DrawScope.drawGoldenRatioGrid(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    strokeWidth: Float,
    color: Color
) {
    // Golden ratio divisions: ~0.382 and ~0.618
    val x1 = left + width * 0.381966f
    val x2 = left + width * 0.618034f
    val y1 = top + height * 0.381966f
    val y2 = top + height * 0.618034f

    // Vertical lines
    drawLine(color, Offset(x1, top), Offset(x1, top + height), strokeWidth)
    drawLine(color, Offset(x2, top), Offset(x2, top + height), strokeWidth)

    // Horizontal lines
    drawLine(color, Offset(left, y1), Offset(left + width, y1), strokeWidth)
    drawLine(color, Offset(left, y2), Offset(left + width, y2), strokeWidth)
}

private fun DrawScope.drawGoldenSpiral(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    strokeWidth: Float,
    color: Color
) {
    // Geometrically accurate golden spiral constructed through successive golden squares
    var curLeft = left
    var curTop = top
    var curWidth = width
    var curHeight = height

    val path = Path()
    var isPathStarted = false

    // Iteratively subdivide up to 6 golden rectangles and draw connected circular arcs
    for (i in 0 until 6) {
        if (curWidth < 8f || curHeight < 8f) break

        val isLandscape = curWidth >= curHeight
        val squareSize = if (isLandscape) curHeight else curWidth

        // Arc bounding box and sweep angle per iteration quadrant
        val arcRect: Rect
        val startAngle: Float

        when (i % 4) {
            0 -> {
                // First: Arc in top-left or right quadrant
                arcRect = Rect(Offset(curLeft + curWidth - 2 * squareSize, curTop), Size(2 * squareSize, 2 * squareSize))
                startAngle = 270f
                if (!isPathStarted) {
                    path.moveTo(curLeft + curWidth, curTop + squareSize)
                    isPathStarted = true
                }
                curTop += squareSize
                curHeight -= squareSize
            }
            1 -> {
                arcRect = Rect(Offset(curLeft, curTop + curHeight - 2 * squareSize), Size(2 * squareSize, 2 * squareSize))
                startAngle = 0f
                curLeft += squareSize
                curWidth -= squareSize
            }
            2 -> {
                arcRect = Rect(Offset(curLeft, curTop), Size(2 * squareSize, 2 * squareSize))
                startAngle = 90f
                curHeight -= squareSize
            }
            else -> {
                arcRect = Rect(Offset(curLeft + curWidth - 2 * squareSize, curTop), Size(2 * squareSize, 2 * squareSize))
                startAngle = 180f
                curWidth -= squareSize
            }
        }

        path.arcTo(arcRect, startAngle, 90f, false)
    }

    drawPath(path, color, style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
}

private fun DrawScope.drawCenterCrosshairs(
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    strokeWidth: Float,
    color: Color
) {
    val centerX = left + width / 2f
    val centerY = top + height / 2f
    val armLength = 22.dp.toPx()
    val gap = 5.dp.toPx()

    // 4 orthogonal ticks with a clear center gap
    drawLine(color, Offset(centerX - armLength, centerY), Offset(centerX - gap, centerY), strokeWidth)
    drawLine(color, Offset(centerX + gap, centerY), Offset(centerX + armLength, centerY), strokeWidth)
    drawLine(color, Offset(centerX, centerY - armLength), Offset(centerX, centerY - gap), strokeWidth)
    drawLine(color, Offset(centerX, centerY + gap), Offset(centerX, centerY + armLength), strokeWidth)

    // Center circular reticle dot
    drawCircle(color, radius = 1.5.dp.toPx(), center = Offset(centerX, centerY))
}
