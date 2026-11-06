package com.bishal.adaptivecamera.photography.computational

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Motion estimation engine for detecting object movement and camera parallax between burst frames.
 * Creates per-pixel motion rejection masks to eliminate ghosting artifacts during image fusion.
 */
object MotionEstimationEngine {

    /**
     * Estimates motion between [reference] and [alignedTarget] frames.
     * Pixels with luminance change greater than [motionThreshold] (normalized 0.0..1.0)
     * are weighted towards 1.0f (motion detected / ghost rejection).
     */
    fun estimateMotion(
        reference: Bitmap,
        alignedTarget: Bitmap,
        motionThreshold: Float = 0.08f,
        downsampleFactor: Int = 2
    ): MotionMask {
        val origW = reference.width
        val origH = reference.height
        val w = max(1, origW / downsampleFactor)
        val h = max(1, origH / downsampleFactor)

        val refScaled = if (downsampleFactor > 1) {
            Bitmap.createScaledBitmap(reference, w, h, true)
        } else {
            reference
        }

        val targetScaled = if (downsampleFactor > 1) {
            Bitmap.createScaledBitmap(alignedTarget, w, h, true)
        } else {
            alignedTarget
        }

        val refPixels = IntArray(w * h)
        val targetPixels = IntArray(w * h)
        refScaled.getPixels(refPixels, 0, w, 0, 0, w, h)
        targetScaled.getPixels(targetPixels, 0, w, 0, 0, w, h)

        if (refScaled !== reference) refScaled.recycle()
        if (targetScaled !== alignedTarget) targetScaled.recycle()

        val rawMask = FloatArray(w * h)
        var motionPixelCount = 0

        val thresholdInt = (motionThreshold * 255f).toInt()

        for (i in 0 until (w * h)) {
            val rc = refPixels[i]
            val tc = targetPixels[i]

            val rY = (77 * ((rc shr 16) and 0xFF) + 150 * ((rc shr 8) and 0xFF) + 29 * (rc and 0xFF)) shr 8
            val tY = (77 * ((tc shr 16) and 0xFF) + 150 * ((tc shr 8) and 0xFF) + 29 * (tc and 0xFF)) shr 8

            val deltaY = abs(rY - tY)
            if (deltaY > thresholdInt) {
                // Soft ramp from threshold to 2x threshold
                val weight = ((deltaY - thresholdInt).toFloat() / thresholdInt.toFloat()).coerceIn(0.0f, 1.0f)
                rawMask[i] = weight
                if (weight > 0.5f) {
                    motionPixelCount++
                }
            } else {
                rawMask[i] = 0.0f
            }
        }

        // Apply 3x3 box blur to soften motion mask boundaries
        val smoothMask = FloatArray(w * h)
        for (y in 0 until h) {
            val yMin = max(0, y - 1)
            val yMax = min(h - 1, y + 1)
            for (x in 0 until w) {
                val xMin = max(0, x - 1)
                val xMax = min(w - 1, x + 1)
                var sum = 0f
                var count = 0
                for (sy in yMin..yMax) {
                    val row = sy * w
                    for (sx in xMin..xMax) {
                        sum += rawMask[row + sx]
                        count++
                    }
                }
                smoothMask[y * w + x] = sum / count
            }
        }

        val motionRatio = motionPixelCount.toFloat() / (w * h).toFloat()
        return MotionMask(
            width = w,
            height = h,
            mask = smoothMask,
            motionRatio = motionRatio
        )
    }

    /**
     * Samples the motion mask at full-resolution coordinate (x, y).
     */
    fun sampleMotionWeight(mask: MotionMask, x: Int, y: Int, fullWidth: Int, fullHeight: Int): Float {
        val mx = ((x.toFloat() / fullWidth.toFloat()) * mask.width).toInt().coerceIn(0, mask.width - 1)
        val my = ((y.toFloat() / fullHeight.toFloat()) * mask.height).toInt().coerceIn(0, mask.height - 1)
        return mask.mask[my * mask.width + mx]
    }
}
