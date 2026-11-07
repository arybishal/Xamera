package com.bishal.adaptivecamera.photography.computational

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Multi-frame image fusion engine.
 * Blends aligned burst frames using ghost-free motion-adaptive weighting,
 * exposure radiance fusion (Mertens method), or high-frequency edge enhancement.
 */
object ImageFusionEngine {

    /**
     * Fuses [alignedFrames] using [motionMasks] according to [mode].
     * [referenceIndex] specifies the master anchor frame (typically frame 0).
     */
    fun fuseFrames(
        alignedFrames: List<Bitmap>,
        motionMasks: List<MotionMask>,
        mode: ImageStackingMode,
        referenceIndex: Int = 0
    ): Bitmap {
        require(alignedFrames.isNotEmpty()) { "Aligned frames list cannot be empty" }
        if (alignedFrames.size == 1) {
            return alignedFrames[0].copy(alignedFrames[0].config ?: Bitmap.Config.ARGB_8888, true)
        }

        val refFrame = alignedFrames[referenceIndex]
        val width = refFrame.width
        val height = refFrame.height

        val effectiveMode = if (mode == ImageStackingMode.SMART_AUTO) {
            selectOptimalMode(refFrame)
        } else {
            mode
        }

        return when (effectiveMode) {
            ImageStackingMode.LOW_LIGHT -> fuseLowLight(alignedFrames, motionMasks, width, height, referenceIndex)
            ImageStackingMode.HDR -> fuseHdr(alignedFrames, motionMasks, width, height, referenceIndex)
            ImageStackingMode.DETAIL_ENHANCEMENT -> fuseDetail(alignedFrames, motionMasks, width, height, referenceIndex)
            ImageStackingMode.SMART_AUTO -> fuseLowLight(alignedFrames, motionMasks, width, height, referenceIndex)
        }
    }

    /**
     * Analyzes reference frame to select the optimal stacking mode.
     */
    fun selectOptimalMode(reference: Bitmap): ImageStackingMode {
        val sampleStep = 8
        var lumSum = 0L
        var minLum = 255
        var maxLum = 0
        var count = 0

        val w = reference.width
        val h = reference.height

        for (y in 0 until h step sampleStep) {
            for (x in 0 until w step sampleStep) {
                val pixel = reference.getPixel(x, y)
                val r = (pixel shr 16) and 0xFF
                val g = (pixel shr 8) and 0xFF
                val b = pixel and 0xFF
                val yVal = (77 * r + 150 * g + 29 * b) shr 8

                lumSum += yVal
                if (yVal < minLum) minLum = yVal
                if (yVal > maxLum) maxLum = yVal
                count++
            }
        }

        if (count == 0) return ImageStackingMode.LOW_LIGHT

        val avgLum = lumSum / count
        val dynamicRange = maxLum - minLum

        return when {
            avgLum < 65 -> ImageStackingMode.LOW_LIGHT
            dynamicRange > 180 -> ImageStackingMode.HDR
            else -> ImageStackingMode.DETAIL_ENHANCEMENT
        }
    }

    private fun fuseLowLight(
        frames: List<Bitmap>,
        masks: List<MotionMask>,
        width: Int,
        height: Int,
        referenceIndex: Int
    ): Bitmap {
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val numFrames = frames.size
        val rowBuffers = Array(numFrames) { IntArray(width) }
        val outRow = IntArray(width)

        for (y in 0 until height) {
            for (f in 0 until numFrames) {
                frames[f].getPixels(rowBuffers[f], 0, width, 0, y, width, 1)
            }

            for (x in 0 until width) {
                var totalWeight = 1.0f
                val refPixel = rowBuffers[referenceIndex][x]
                var accR = ((refPixel shr 16) and 0xFF).toFloat()
                var accG = ((refPixel shr 8) and 0xFF).toFloat()
                var accB = (refPixel and 0xFF).toFloat()

                for (f in 0 until numFrames) {
                    if (f == referenceIndex) continue

                    // Ghost rejection: motion mask values near 1.0 reduce target frame weight to 0.0
                    val motionWeight = if (f < masks.size) {
                        MotionEstimationEngine.sampleMotionWeight(masks[f], x, y, width, height)
                    } else {
                        0.0f
                    }

                    val frameWeight = (1.0f - motionWeight).coerceIn(0.0f, 1.0f)
                    if (frameWeight > 0.01f) {
                        val p = rowBuffers[f][x]
                        accR += ((p shr 16) and 0xFF) * frameWeight
                        accG += ((p shr 8) and 0xFF) * frameWeight
                        accB += (p and 0xFF) * frameWeight
                        totalWeight += frameWeight
                    }
                }

                val finalR = (accR / totalWeight).roundToInt().coerceIn(0, 255)
                val finalG = (accG / totalWeight).roundToInt().coerceIn(0, 255)
                val finalB = (accB / totalWeight).roundToInt().coerceIn(0, 255)

                outRow[x] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }

            outBitmap.setPixels(outRow, 0, width, 0, y, width, 1)
        }

        return outBitmap
    }

    private fun fuseHdr(
        frames: List<Bitmap>,
        masks: List<MotionMask>,
        width: Int,
        height: Int,
        referenceIndex: Int
    ): Bitmap {
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val numFrames = frames.size
        val rowBuffers = Array(numFrames) { IntArray(width) }
        val outRow = IntArray(width)

        for (y in 0 until height) {
            for (f in 0 until numFrames) {
                frames[f].getPixels(rowBuffers[f], 0, width, 0, y, width, 1)
            }

            for (x in 0 until width) {
                var totalWeight = 0.0001f
                var accR = 0f
                var accG = 0f
                var accB = 0f

                for (f in 0 until numFrames) {
                    val p = rowBuffers[f][x]
                    val r = ((p shr 16) and 0xFF)
                    val g = ((p shr 8) and 0xFF)
                    val b = (p and 0xFF)

                    // Well-exposedness weight: Gaussian centered at 128
                    val normY = (77 * r + 150 * g + 29 * b) / 255f
                    val wellExposedness = exp(-0.5f * ((normY - 0.5f) / 0.2f) * ((normY - 0.5f) / 0.2f))

                    // Ghost rejection
                    val motionFactor = if (f != referenceIndex && f < masks.size) {
                        1.0f - MotionEstimationEngine.sampleMotionWeight(masks[f], x, y, width, height)
                    } else {
                        1.0f
                    }

                    val weight = (wellExposedness * motionFactor).coerceAtLeast(0.01f)

                    accR += r * weight
                    accG += g * weight
                    accB += b * weight
                    totalWeight += weight
                }

                val finalR = (accR / totalWeight).roundToInt().coerceIn(0, 255)
                val finalG = (accG / totalWeight).roundToInt().coerceIn(0, 255)
                val finalB = (accB / totalWeight).roundToInt().coerceIn(0, 255)

                outRow[x] = (0xFF shl 24) or (finalR shl 16) or (finalG shl 8) or finalB
            }

            outBitmap.setPixels(outRow, 0, width, 0, y, width, 1)
        }

        return outBitmap
    }

    private fun fuseDetail(
        frames: List<Bitmap>,
        masks: List<MotionMask>,
        width: Int,
        height: Int,
        referenceIndex: Int
    ): Bitmap {
        // 1. First obtain noise-reduced baseline via multi-frame temporal blend
        val denoised = fuseLowLight(frames, masks, width, height, referenceIndex)
        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        var prevRow = IntArray(width)
        var currRow = IntArray(width)
        var nextRow = IntArray(width)
        val outRow = IntArray(width)
        val sharpnessAmount = 0.45f

        // Initial read for y = 0 and y = 1
        denoised.getPixels(currRow, 0, width, 0, 0, width, 1)
        outBitmap.setPixels(currRow, 0, width, 0, 0, width, 1) // Border row 0

        if (height > 1) {
            denoised.getPixels(nextRow, 0, width, 0, 1, width, 1)
        }

        for (y in 1 until height - 1) {
            // Shift rows: prev = curr, curr = next, next = read y+1
            val temp = prevRow
            prevRow = currRow
            currRow = nextRow
            nextRow = temp
            denoised.getPixels(nextRow, 0, width, 0, y + 1, width, 1)

            outRow[0] = currRow[0] // Left border
            for (x in 1 until width - 1) {
                val center = currRow[x]
                val cR = (center shr 16) and 0xFF
                val cG = (center shr 8) and 0xFF
                val cB = center and 0xFF

                // 3x3 Laplacian edge kernel
                val top = prevRow[x]
                val bottom = nextRow[x]
                val left = currRow[x - 1]
                val right = currRow[x + 1]

                val blurR = (((top shr 16) and 0xFF) + ((bottom shr 16) and 0xFF) + ((left shr 16) and 0xFF) + ((right shr 16) and 0xFF)) shr 2
                val blurG = (((top shr 8) and 0xFF) + ((bottom shr 8) and 0xFF) + ((left shr 8) and 0xFF) + ((right shr 8) and 0xFF)) shr 2
                val blurB = ((top and 0xFF) + (bottom and 0xFF) + (left and 0xFF) + (right and 0xFF)) shr 2

                val edgeR = cR - blurR
                val edgeG = cG - blurG
                val edgeB = cB - blurB

                val outR = (cR + (edgeR * sharpnessAmount)).roundToInt().coerceIn(0, 255)
                val outG = (cG + (edgeG * sharpnessAmount)).roundToInt().coerceIn(0, 255)
                val outB = (cB + (edgeB * sharpnessAmount)).roundToInt().coerceIn(0, 255)

                outRow[x] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
            outRow[width - 1] = currRow[width - 1] // Right border
            outBitmap.setPixels(outRow, 0, width, 0, y, width, 1)
        }

        if (height > 1) {
            outBitmap.setPixels(nextRow, 0, width, 0, height - 1, width, 1) // Bottom border
        }

        denoised.recycle()
        return outBitmap
    }
}
