package com.bishal.adaptivecamera.photography.pro

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.min

data class PeakingPoint(
    val xNorm: Float,
    val yNorm: Float,
    val intensityNorm: Float
)

data class FocusPeakingMap(
    val points: List<PeakingPoint> = emptyList(),
    val isEnabled: Boolean = false,
    val sensitivity: Float = 0.5f,
    val timestampMs: Long = System.currentTimeMillis()
)

object FocusPeakingAnalyzer {

    private const val STEP = 6 // Downsample grid step for high responsiveness

    fun analyzeY(
        yBuffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        sensitivity: Float
    ): FocusPeakingMap {
        // Sensitivity maps 0.1..1.0 to gradient thresholds 120 down to 25
        val threshold = (120 - (sensitivity.coerceIn(0.1f, 1.0f) * 95)).toInt()
        val points = mutableListOf<PeakingPoint>()

        val sampleWidth = width / STEP
        val sampleHeight = height / STEP

        for (row in 1 until sampleHeight - 1) {
            val y = row * STEP
            val rowOffset = y * rowStride
            val rowOffsetPrev = (y - STEP) * rowStride
            val rowOffsetNext = (y + STEP) * rowStride

            for (col in 1 until sampleWidth - 1) {
                val x = col * STEP
                val offset = rowOffset + (x * pixelStride)

                val center = yBuffer.get(offset).toInt() and 0xFF
                val left = yBuffer.get(rowOffset + ((x - STEP) * pixelStride)).toInt() and 0xFF
                val right = yBuffer.get(rowOffset + ((x + STEP) * pixelStride)).toInt() and 0xFF
                val top = yBuffer.get(rowOffsetPrev + (x * pixelStride)).toInt() and 0xFF
                val bottom = yBuffer.get(rowOffsetNext + (x * pixelStride)).toInt() and 0xFF

                // Fast 2D gradient magnitude approximation
                val gradX = abs(right - left)
                val gradY = abs(bottom - top)
                val gradTotal = gradX + gradY

                if (gradTotal > threshold) {
                    val intensity = min(1.0f, (gradTotal - threshold).toFloat() / 100f)
                    points.add(
                        PeakingPoint(
                            xNorm = x.toFloat() / width.toFloat(),
                            yNorm = y.toFloat() / height.toFloat(),
                            intensityNorm = intensity
                        )
                    )
                }
            }
        }

        return FocusPeakingMap(
            points = points,
            isEnabled = true,
            sensitivity = sensitivity
        )
    }

    fun analyzeImageProxy(
        image: ImageProxy,
        sensitivity: Float
    ): FocusPeakingMap {
        val yPlane = image.planes[0]
        return analyzeY(
            yBuffer = yPlane.buffer,
            width = image.width,
            height = image.height,
            rowStride = yPlane.rowStride,
            pixelStride = yPlane.pixelStride,
            sensitivity = sensitivity
        )
    }
}
