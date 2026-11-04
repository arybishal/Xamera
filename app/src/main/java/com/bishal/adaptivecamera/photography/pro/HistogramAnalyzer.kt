package com.bishal.adaptivecamera.photography.pro

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

data class HistogramData(
    val lumaBins: FloatArray = FloatArray(64),
    val rBins: FloatArray = FloatArray(64),
    val gBins: FloatArray = FloatArray(64),
    val bBins: FloatArray = FloatArray(64),
    val shadowClippingRatio: Float = 0f,
    val highlightClippingRatio: Float = 0f,
    val isPreviewBased: Boolean = true
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HistogramData
        if (!lumaBins.contentEquals(other.lumaBins)) return false
        if (!rBins.contentEquals(other.rBins)) return false
        if (!gBins.contentEquals(other.gBins)) return false
        if (!bBins.contentEquals(other.bBins)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = lumaBins.contentHashCode()
        result = 31 * result + rBins.contentHashCode()
        result = 31 * result + gBins.contentHashCode()
        result = 31 * result + bBins.contentHashCode()
        return result
    }
}

object HistogramAnalyzer {

    private const val BINS_COUNT = 64
    private const val STRIDE = 4 // Process 1 in 4 pixels

    fun analyzeYuv(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer?,
        vBuffer: ByteBuffer?,
        width: Int,
        height: Int,
        yRowStride: Int,
        yPixelStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int
    ): HistogramData {
        val rawLuma = IntArray(BINS_COUNT)
        val rawR = IntArray(BINS_COUNT)
        val rawG = IntArray(BINS_COUNT)
        val rawB = IntArray(BINS_COUNT)

        var totalSampled = 0
        var shadowClips = 0
        var highlightClips = 0

        val sampleWidth = width / STRIDE
        val sampleHeight = height / STRIDE

        val hasChroma = uBuffer != null && vBuffer != null

        for (row in 0 until sampleHeight) {
            val yPos = row * STRIDE
            val yRowOffset = yPos * yRowStride
            val uvRowOffset = (yPos / 2) * uvRowStride

            for (col in 0 until sampleWidth) {
                val xPos = col * STRIDE
                val yOffset = yRowOffset + (xPos * yPixelStride)

                val yVal = yBuffer.get(yOffset).toInt() and 0xFF
                totalSampled++

                val lumaBin = min(BINS_COUNT - 1, yVal * BINS_COUNT / 256)
                rawLuma[lumaBin]++

                if (yVal <= 2) shadowClips++
                if (yVal >= 253) highlightClips++

                var rVal = yVal
                var gVal = yVal
                var bVal = yVal

                if (hasChroma) {
                    val uvOffset = uvRowOffset + ((xPos / 2) * uvPixelStride)
                    if (uvOffset < uBuffer!!.limit() && uvOffset < vBuffer!!.limit()) {
                        val uVal = (uBuffer.get(uvOffset).toInt() and 0xFF) - 128
                        val vVal = (vBuffer.get(uvOffset).toInt() and 0xFF) - 128

                        rVal = min(255, max(0, (yVal + 1.402 * vVal).toInt()))
                        gVal = min(255, max(0, (yVal - 0.344136 * uVal - 0.714136 * vVal).toInt()))
                        bVal = min(255, max(0, (yVal + 1.772 * uVal).toInt()))
                    }
                }

                rawR[min(BINS_COUNT - 1, rVal * BINS_COUNT / 256)]++
                rawG[min(BINS_COUNT - 1, gVal * BINS_COUNT / 256)]++
                rawB[min(BINS_COUNT - 1, bVal * BINS_COUNT / 256)]++
            }
        }

        if (totalSampled == 0) return HistogramData()

        val maxLuma = max(1, rawLuma.maxOrNull() ?: 1).toFloat()
        val maxR = max(1, rawR.maxOrNull() ?: 1).toFloat()
        val maxG = max(1, rawG.maxOrNull() ?: 1).toFloat()
        val maxB = max(1, rawB.maxOrNull() ?: 1).toFloat()

        val normLuma = FloatArray(BINS_COUNT) { rawLuma[it] / maxLuma }
        val normR = FloatArray(BINS_COUNT) { rawR[it] / maxR }
        val normG = FloatArray(BINS_COUNT) { rawG[it] / maxG }
        val normB = FloatArray(BINS_COUNT) { rawB[it] / maxB }

        return HistogramData(
            lumaBins = normLuma,
            rBins = normR,
            gBins = normG,
            bBins = normB,
            shadowClippingRatio = shadowClips.toFloat() / totalSampled,
            highlightClippingRatio = highlightClips.toFloat() / totalSampled,
            isPreviewBased = true
        )
    }

    fun analyzeImageProxy(image: ImageProxy): HistogramData {
        val yPlane = image.planes[0]
        val uPlane = if (image.planes.size > 1) image.planes[1] else null
        val vPlane = if (image.planes.size > 2) image.planes[2] else null

        return analyzeYuv(
            yBuffer = yPlane.buffer,
            uBuffer = uPlane?.buffer,
            vBuffer = vPlane?.buffer,
            width = image.width,
            height = image.height,
            yRowStride = yPlane.rowStride,
            yPixelStride = yPlane.pixelStride,
            uvRowStride = uPlane?.rowStride ?: 0,
            uvPixelStride = uPlane?.pixelStride ?: 1
        )
    }
}
