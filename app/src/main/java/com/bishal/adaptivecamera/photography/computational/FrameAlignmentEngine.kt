package com.bishal.adaptivecamera.photography.computational

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * High-performance frame alignment engine for multi-frame computational photography.
 * Estimates sub-pixel / integer translational displacement between frames caused by
 * handheld device motion using fast dual-axis luminance projection cross-correlation.
 */
object FrameAlignmentEngine {

    private const val ANALYSIS_WIDTH = 160
    private const val ANALYSIS_HEIGHT = 120

    /**
     * Estimates displacement vector of [target] relative to [reference].
     * Returns [DisplacementVector] containing pixel shifts (dx, dy) to align [target] onto [reference].
     */
    fun estimateDisplacement(
        reference: Bitmap,
        target: Bitmap,
        maxShiftPx: Int = 32
    ): DisplacementVector {
        if (reference.width != target.width || reference.height != target.height) {
            return DisplacementVector(0, 0, 0f)
        }

        // Downscale frames to small analysis buffers for ultra-fast correlation
        val refScaled = if (reference.width > ANALYSIS_WIDTH || reference.height > ANALYSIS_HEIGHT) {
            Bitmap.createScaledBitmap(reference, ANALYSIS_WIDTH, ANALYSIS_HEIGHT, true)
        } else {
            reference
        }

        val targetScaled = if (target.width > ANALYSIS_WIDTH || target.height > ANALYSIS_HEIGHT) {
            Bitmap.createScaledBitmap(target, ANALYSIS_WIDTH, ANALYSIS_HEIGHT, true)
        } else {
            target
        }

        val scaleX = reference.width.toFloat() / refScaled.width.toFloat()
        val scaleY = reference.height.toFloat() / refScaled.height.toFloat()
        val scaledMaxShiftX = max(2, (maxShiftPx / scaleX).toInt())
        val scaledMaxShiftY = max(2, (maxShiftPx / scaleY).toInt())

        val w = refScaled.width
        val h = refScaled.height

        val refPixels = IntArray(w * h)
        val targetPixels = IntArray(w * h)
        refScaled.getPixels(refPixels, 0, w, 0, 0, w, h)
        targetScaled.getPixels(targetPixels, 0, w, 0, 0, w, h)

        // Clean up scaled bitmaps if allocated
        if (refScaled !== reference) refScaled.recycle()
        if (targetScaled !== target) targetScaled.recycle()

        // 1. Compute 1D horizontal and vertical luminance projections
        val refProjX = FloatArray(w)
        val refProjY = FloatArray(h)
        val targetProjX = FloatArray(w)
        val targetProjY = FloatArray(h)

        for (y in 0 until h) {
            val rowOffset = y * w
            for (x in 0 until w) {
                val refColor = refPixels[rowOffset + x]
                val refY = 0.299f * ((refColor shr 16) and 0xFF) +
                        0.587f * ((refColor shr 8) and 0xFF) +
                        0.114f * (refColor and 0xFF)
                refProjX[x] += refY
                refProjY[y] += refY

                val targetColor = targetPixels[rowOffset + x]
                val tgtY = 0.299f * ((targetColor shr 16) and 0xFF) +
                        0.587f * ((targetColor shr 8) and 0xFF) +
                        0.114f * (targetColor and 0xFF)
                targetProjX[x] += tgtY
                targetProjY[y] += tgtY
            }
        }

        // 2. Cross-correlate horizontal projections to find optimal dx
        val bestShiftX = findBestShift(refProjX, targetProjX, scaledMaxShiftX)
        // 3. Cross-correlate vertical projections to find optimal dy
        val bestShiftY = findBestShift(refProjY, targetProjY, scaledMaxShiftY)

        val fullDx = (bestShiftX * scaleX).toInt().coerceIn(-maxShiftPx, maxShiftPx)
        val fullDy = (bestShiftY * scaleY).toInt().coerceIn(-maxShiftPx, maxShiftPx)

        return DisplacementVector(fullDx, fullDy, 1.0f)
    }

    /**
     * Shifts [bitmap] by [dx] and [dy] pixels, clamping within valid boundaries.
     */
    fun alignBitmap(bitmap: Bitmap, dx: Int, dy: Int): Bitmap {
        if (dx == 0 && dy == 0) return bitmap

        val aligned = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config ?: Bitmap.Config.ARGB_8888)
        val canvas = Canvas(aligned)
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)

        // Draw source bitmap translated by dx, dy
        canvas.drawBitmap(bitmap, dx.toFloat(), dy.toFloat(), paint)
        return aligned
    }

    private fun findBestShift(ref: FloatArray, target: FloatArray, maxShift: Int): Int {
        val n = ref.size
        var bestShift = 0
        var bestCorrelation = Double.NEGATIVE_INFINITY

        val searchLimit = min(maxShift, n / 4)

        for (shift in -searchLimit..searchLimit) {
            var dot = 0.0
            var normRef = 0.0
            var normTgt = 0.0

            val startRef = max(0, -shift)
            val endRef = min(n, n - shift)

            for (i in startRef until endRef) {
                val r = ref[i].toDouble()
                val t = target[i + shift].toDouble()
                dot += r * t
                normRef += r * r
                normTgt += t * t
            }

            val denom = kotlin.math.sqrt(normRef * normTgt)
            val corr = if (denom > 1e-6) dot / denom else 0.0

            if (corr > bestCorrelation) {
                bestCorrelation = corr
                bestShift = shift
            }
        }

        return bestShift
    }
}
