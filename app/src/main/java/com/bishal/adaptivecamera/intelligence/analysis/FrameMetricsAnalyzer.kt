package com.bishal.adaptivecamera.intelligence.analysis

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * High-performance frame analyzer for scene luminance, contrast, histogram,
 * and temporal motion estimation.
 */
class FrameMetricsAnalyzer(
    private val onMetricsCalculated: (FrameMetrics) -> Unit = {}
) : ImageAnalysis.Analyzer {

    companion object {
        private const val HISTOGRAM_BINS = 16
        private const val DOWNSAMPLE_STEP = 4 // Sample 1 out of every 4 pixels horizontally & vertically
    }

    private var previousSubsampledFrame: ByteArray? = null

    override fun analyze(image: ImageProxy) {
        val plane = image.planes.firstOrNull()
        if (plane == null) {
            image.close()
            return
        }

        try {
            val width = image.width
            val height = image.height
            val buffer = plane.buffer
            val rowStride = plane.rowStride
            val pixelStride = plane.pixelStride

            val metrics = computeMetrics(buffer, width, height, rowStride, pixelStride)
            onMetricsCalculated(metrics)
        } catch (_: Exception) {
            // Non-fatal frame processing protection
        } finally {
            image.close()
        }
    }

    /**
     * Computes luminance, RMS contrast, histogram, and motion metrics from Y buffer.
     */
    fun computeMetrics(
        buffer: ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int
    ): FrameMetrics {
        val sampleWidth = width / DOWNSAMPLE_STEP
        val sampleHeight = height / DOWNSAMPLE_STEP
        val totalSampledPixels = sampleWidth * sampleHeight

        if (totalSampledPixels <= 0) {
            return FrameMetrics(0.5f, 0.2f, 0.1f, 0.1f, 0.8f, 7.0f, 0.0f)
        }

        val currentSubsample = ByteArray(totalSampledPixels)
        val histogram = IntArray(HISTOGRAM_BINS)

        var lumaSum = 0L
        var lumaSqSum = 0.0
        var shadowCount = 0
        var highlightCount = 0

        var sampleIdx = 0

        for (row in 0 until sampleHeight) {
            val y = row * DOWNSAMPLE_STEP
            val rowOffset = y * rowStride
            for (col in 0 until sampleWidth) {
                val x = col * DOWNSAMPLE_STEP
                val offset = rowOffset + (x * pixelStride)

                val rawByte = buffer.get(offset).toInt() and 0xFF
                currentSubsample[sampleIdx++] = rawByte.toByte()

                lumaSum += rawByte
                val normVal = rawByte / 255.0
                lumaSqSum += normVal * normVal

                if (rawByte < 38) { // < ~0.15
                    shadowCount++
                } else if (rawByte > 217) { // > ~0.85
                    highlightCount++
                }

                val bin = min((rawByte * HISTOGRAM_BINS / 256), HISTOGRAM_BINS - 1)
                histogram[bin]++
            }
        }

        val meanLuma = (lumaSum.toDouble() / (totalSampledPixels * 255.0)).toFloat()
        val meanLumaSq = lumaSqSum / totalSampledPixels
        val variance = max(0.0, meanLumaSq - (meanLuma.toDouble() * meanLuma.toDouble()))
        val rmsContrast = sqrt(variance).toFloat()

        val shadowRatio = shadowCount.toFloat() / totalSampledPixels
        val highlightRatio = highlightCount.toFloat() / totalSampledPixels
        val midtoneRatio = max(0f, 1f - (shadowRatio + highlightRatio))

        // Estimate dynamic range stops from populated histogram span
        var firstActiveBin = 0
        while (firstActiveBin < HISTOGRAM_BINS && histogram[firstActiveBin] == 0) {
            firstActiveBin++
        }
        var lastActiveBin = HISTOGRAM_BINS - 1
        while (lastActiveBin >= 0 && histogram[lastActiveBin] == 0) {
            lastActiveBin--
        }
        val activeBinsSpan = max(1, lastActiveBin - firstActiveBin + 1)
        // Map 1..16 bins to approximately 3 to 12 exposure stops
        val estimatedStops = 3.0f + (activeBinsSpan.toFloat() / HISTOGRAM_BINS) * 9.0f

        // Compute temporal motion delta vs previous frame
        var motionDelta = 0.0f
        val prev = previousSubsampledFrame
        if (prev != null && prev.size == currentSubsample.size) {
            var diffSum = 0L
            for (i in currentSubsample.indices) {
                val curVal = currentSubsample[i].toInt() and 0xFF
                val prevVal = prev[i].toInt() and 0xFF
                diffSum += abs(curVal - prevVal)
            }
            motionDelta = (diffSum.toDouble() / (totalSampledPixels * 255.0)).toFloat()
        }
        previousSubsampledFrame = currentSubsample

        return FrameMetrics(
            meanLuminance = meanLuma,
            rmsContrast = rmsContrast,
            shadowRatio = shadowRatio,
            highlightRatio = highlightRatio,
            midtoneRatio = midtoneRatio,
            estimatedDynamicRangeStops = estimatedStops,
            motionDelta = motionDelta,
            histogram = histogram
        )
    }

    /**
     * Overload for testing and offline analysis on raw planar Y byte arrays.
     */
    fun analyzeRawY(
        yBytes: ByteArray,
        width: Int,
        height: Int
    ): FrameMetrics {
        val buffer = ByteBuffer.wrap(yBytes)
        return computeMetrics(
            buffer = buffer,
            width = width,
            height = height,
            rowStride = width,
            pixelStride = 1
        )
    }
}
