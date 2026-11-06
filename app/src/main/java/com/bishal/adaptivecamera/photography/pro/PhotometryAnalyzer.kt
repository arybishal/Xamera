package com.bishal.adaptivecamera.photography.pro

import androidx.camera.core.ImageProxy
import java.nio.ByteBuffer
import kotlin.math.max
import kotlin.math.min

/**
 * Standard broadcast False Color exposure zones based on 0-100% IRE scale.
 */
enum class FalseColorZone(
    val displayName: String,
    val ireMin: Int,
    val ireMax: Int,
    val colorArgb: Int,
    val description: String
) {
    CRUSHED_BLACK("Crushed Black", 0, 9, 0xFF7B1FA2.toInt(), "Severe underexposure / lost shadow detail"),
    DEEP_SHADOW("Deep Shadow", 10, 24, 0xFF1976D2.toInt(), "Low shadow detail"),
    MID_SHADOW("Mid Shadow", 25, 37, 0xFF00BCD4.toInt(), "Graduated shadow transition"),
    MIDDLE_GRAY_18("18% Middle Gray", 38, 44, 0xFF4CAF50.toInt(), "Standard 18% neutral gray reference card"),
    MIDTONE("Midtones", 45, 54, 0xFF9E9E9E.toInt(), "Neutral medium exposure"),
    SKIN_TONES("Skin Tones", 55, 69, 0xFFF48FB1.toInt(), "Optimal human skin tone exposure range"),
    HIGH_MIDTONES("High Midtones", 70, 79, 0xFFFFF59D.toInt(), "Bright surfaces and high-key tones"),
    NEAR_CLIP("Near Clipping", 80, 97, 0xFFFFEB3B.toInt(), "Warning highlights approaching sensor saturation"),
    CLIPPED_HIGHLIGHT("Clipped Highlight", 98, 100, 0xFFD32F2F.toInt(), "Sensor saturation / blown-out white");

    companion object {
        fun fromIre(ire: Int): FalseColorZone {
            val clamped = ire.coerceIn(0, 100)
            for (zone in values()) {
                if (clamped in zone.ireMin..zone.ireMax) return zone
            }
            return if (clamped >= 98) CLIPPED_HIGHLIGHT else CRUSHED_BLACK
        }
    }
}

data class ZebraPoint(
    val xNorm: Float,
    val yNorm: Float,
    val ire: Int
)

data class ZebraStripingMap(
    val points: List<ZebraPoint> = emptyList(),
    val isEnabled: Boolean = false,
    val thresholdIre: Int = 90,
    val timestampMs: Long = System.currentTimeMillis()
)

data class FalseColorSample(
    val xNorm: Float,
    val yNorm: Float,
    val colorArgb: Int,
    val ire: Int
)

data class FalseColorMap(
    val samples: List<FalseColorSample> = emptyList(),
    val isEnabled: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)

data class PhotometryAnalysisResult(
    val histogram: HistogramData = HistogramData(),
    val zebraMap: ZebraStripingMap = ZebraStripingMap(),
    val falseColorMap: FalseColorMap = FalseColorMap()
)

/**
 * Unified Photometry Analyzer conforming to Deduplication Rule 2:
 * A single downsampled preview analysis pass generates Histogram distributions,
 * Live Zebra Striping bitmasks, and False Color exposure heatmaps without
 * duplicate frame subscriptions.
 */
object PhotometryAnalyzer {

    private const val BINS_COUNT = 64
    private const val DEFAULT_STRIDE = 6 // Downsample step for fast real-time 60fps throughput

    fun analyzeYuv(
        yBuffer: ByteBuffer,
        uBuffer: ByteBuffer?,
        vBuffer: ByteBuffer?,
        width: Int,
        height: Int,
        yRowStride: Int,
        yPixelStride: Int,
        uvRowStride: Int,
        uvPixelStride: Int,
        isHistogramEnabled: Boolean,
        isZebraEnabled: Boolean,
        zebraThresholdIre: Int,
        isFalseColorEnabled: Boolean
    ): PhotometryAnalysisResult {
        if (!isHistogramEnabled && !isZebraEnabled && !isFalseColorEnabled) {
            return PhotometryAnalysisResult()
        }

        val rawLuma = IntArray(BINS_COUNT)
        val rawR = IntArray(BINS_COUNT)
        val rawG = IntArray(BINS_COUNT)
        val rawB = IntArray(BINS_COUNT)

        var totalSampled = 0
        var shadowClips = 0
        var highlightClips = 0

        val zebraPoints = if (isZebraEnabled) ArrayList<ZebraPoint>(256) else emptyList<ZebraPoint>()
        val falseColorSamples = if (isFalseColorEnabled) ArrayList<FalseColorSample>(512) else emptyList<FalseColorSample>()

        val sampleWidth = width / DEFAULT_STRIDE
        val sampleHeight = height / DEFAULT_STRIDE
        val hasChroma = uBuffer != null && vBuffer != null

        val zebraThresholdY = (zebraThresholdIre.coerceIn(50, 100) * 255) / 100

        for (row in 0 until sampleHeight) {
            val yPos = row * DEFAULT_STRIDE
            val yRowOffset = yPos * yRowStride
            val uvRowOffset = (yPos / 2) * uvRowStride

            for (col in 0 until sampleWidth) {
                val xPos = col * DEFAULT_STRIDE
                val yOffset = yRowOffset + (xPos * yPixelStride)

                val yVal = yBuffer.get(yOffset).toInt() and 0xFF
                totalSampled++

                val ire = (yVal * 100) / 255
                val xNorm = xPos.toFloat() / width.toFloat()
                val yNorm = yPos.toFloat() / height.toFloat()

                // 1. Zebra Striping Detection
                if (isZebraEnabled && yVal >= zebraThresholdY) {
                    (zebraPoints as ArrayList).add(
                        ZebraPoint(xNorm = xNorm, yNorm = yNorm, ire = ire)
                    )
                }

                // 2. False Color Heatmap Sample
                if (isFalseColorEnabled) {
                    val zone = FalseColorZone.fromIre(ire)
                    (falseColorSamples as ArrayList).add(
                        FalseColorSample(
                            xNorm = xNorm,
                            yNorm = yNorm,
                            colorArgb = zone.colorArgb,
                            ire = ire
                        )
                    )
                }

                // 3. Histogram Bins & Clipping
                if (isHistogramEnabled) {
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
        }

        // Construct normalized HistogramData
        val histogramData = if (isHistogramEnabled && totalSampled > 0) {
            val maxLuma = max(1, rawLuma.maxOrNull() ?: 1).toFloat()
            val maxR = max(1, rawR.maxOrNull() ?: 1).toFloat()
            val maxG = max(1, rawG.maxOrNull() ?: 1).toFloat()
            val maxB = max(1, rawB.maxOrNull() ?: 1).toFloat()

            HistogramData(
                lumaBins = FloatArray(BINS_COUNT) { rawLuma[it] / maxLuma },
                rBins = FloatArray(BINS_COUNT) { rawR[it] / maxR },
                gBins = FloatArray(BINS_COUNT) { rawG[it] / maxG },
                bBins = FloatArray(BINS_COUNT) { rawB[it] / maxB },
                shadowClippingRatio = shadowClips.toFloat() / totalSampled,
                highlightClippingRatio = highlightClips.toFloat() / totalSampled,
                isPreviewBased = true
            )
        } else {
            HistogramData()
        }

        return PhotometryAnalysisResult(
            histogram = histogramData,
            zebraMap = ZebraStripingMap(
                points = zebraPoints,
                isEnabled = isZebraEnabled,
                thresholdIre = zebraThresholdIre
            ),
            falseColorMap = FalseColorMap(
                samples = falseColorSamples,
                isEnabled = isFalseColorEnabled
            )
        )
    }

    fun analyzeImageProxy(
        image: ImageProxy,
        isHistogramEnabled: Boolean,
        isZebraEnabled: Boolean,
        zebraThresholdIre: Int,
        isFalseColorEnabled: Boolean
    ): PhotometryAnalysisResult {
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
            uvPixelStride = uPlane?.pixelStride ?: 1,
            isHistogramEnabled = isHistogramEnabled,
            isZebraEnabled = isZebraEnabled,
            zebraThresholdIre = zebraThresholdIre,
            isFalseColorEnabled = isFalseColorEnabled
        )
    }
}
