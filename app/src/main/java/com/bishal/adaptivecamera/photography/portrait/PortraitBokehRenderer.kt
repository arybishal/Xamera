package com.bishal.adaptivecamera.photography.portrait

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Optical bokeh and portrait lighting rendering engine.
 * Applies depth-aware synthetic aperture bokeh blur, specular highlight expansion,
 * and portrait studio lighting effects.
 */
object PortraitBokehRenderer {

    /**
     * Renders [sourceBitmap] with portrait bokeh and lighting effects according to [config].
     */
    fun renderPortrait(
        sourceBitmap: Bitmap,
        config: PortraitConfig,
        segmentation: SubjectSegmentationResult? = null
    ): Bitmap {
        val seg = segmentation ?: SubjectSegmentationEngine.segmentSubject(sourceBitmap)

        // Safe fallback: If no subject/face detected and zero saliency confidence, return sharp original
        if (!seg.hasFaceDetected && seg.confidence <= 0.05f) {
            return sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        }

        val width = sourceBitmap.width
        val height = sourceBitmap.height
        val blurRadius = config.aperture.blurRadius

        // 1. Generate blurred background with specular highlight expansion
        val blurredBackground = createSpecularBokehBlur(
            source = sourceBitmap,
            radius = blurRadius,
            boostHighlights = config.boostHighlights
        )

        // 2. Blend sharp foreground and blurred background using subject mask
        val blended = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val srcPixels = IntArray(width * height)
        val blurPixels = IntArray(width * height)
        val outPixels = IntArray(width * height)

        sourceBitmap.getPixels(srcPixels, 0, width, 0, 0, width, height)
        blurredBackground.getPixels(blurPixels, 0, width, 0, 0, width, height)
        blurredBackground.recycle()

        val mask = seg.mask

        for (i in 0 until (width * height)) {
            val m = mask[i]
            if (m >= 0.99f) {
                // Pin-sharp subject pixel
                outPixels[i] = srcPixels[i]
            } else if (m <= 0.01f) {
                // Pure bokeh background pixel
                outPixels[i] = blurPixels[i]
            } else {
                // Feathered boundary pixel
                val sp = srcPixels[i]
                val bp = blurPixels[i]

                val sR = (sp shr 16) and 0xFF
                val sG = (sp shr 8) and 0xFF
                val sB = sp and 0xFF

                val bR = (bp shr 16) and 0xFF
                val bG = (bp shr 8) and 0xFF
                val bB = bp and 0xFF

                val outR = (sR * m + bR * (1.0f - m)).roundToInt().coerceIn(0, 255)
                val outG = (sG * m + bG * (1.0f - m)).roundToInt().coerceIn(0, 255)
                val outB = (sB * m + bB * (1.0f - m)).roundToInt().coerceIn(0, 255)

                outPixels[i] = (0xFF shl 24) or (outR shl 16) or (outG shl 8) or outB
            }
        }

        // 3. Apply Portrait Lighting Style
        applyLightingStyle(outPixels, mask, width, height, config.lightingStyle)

        blended.setPixels(outPixels, 0, width, 0, 0, width, height)
        return blended
    }

    private fun createSpecularBokehBlur(source: Bitmap, radius: Int, boostHighlights: Boolean): Bitmap {
        val w = source.width
        val h = source.height
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)

        val inPixels = IntArray(w * h)
        val tempPixels = IntArray(w * h)
        val outPixels = IntArray(w * h)

        source.getPixels(inPixels, 0, w, 0, 0, w, h)

        // Highlight boost threshold for specular bokeh circles
        if (boostHighlights) {
            for (i in 0 until (w * h)) {
                val p = inPixels[i]
                val r = (p shr 16) and 0xFF
                val g = (p shr 8) and 0xFF
                val b = p and 0xFF
                val lum = (77 * r + 150 * g + 29 * b) shr 8
                if (lum > 215) {
                    val boostFactor = 1.25f
                    val nr = (r * boostFactor).toInt().coerceIn(0, 255)
                    val ng = (g * boostFactor).toInt().coerceIn(0, 255)
                    val nb = (b * boostFactor).toInt().coerceIn(0, 255)
                    inPixels[i] = (0xFF shl 24) or (nr shl 16) or (ng shl 8) or nb
                }
            }
        }

        // Fast two-pass box blur approximating Gaussian/Bokeh circle
        // Pass 1: Horizontal Blur
        val div = 2 * radius + 1
        for (y in 0 until h) {
            val rowOffset = y * w
            var sumR = 0
            var sumG = 0
            var sumB = 0

            // Initialize window
            for (i in -radius..radius) {
                val p = inPixels[rowOffset + i.coerceIn(0, w - 1)]
                sumR += (p shr 16) and 0xFF
                sumG += (p shr 8) and 0xFF
                sumB += p and 0xFF
            }

            for (x in 0 until w) {
                tempPixels[rowOffset + x] = (0xFF shl 24) or
                        ((sumR / div) shl 16) or
                        ((sumG / div) shl 8) or
                        (sumB / div)

                // Slide window
                val left = inPixels[rowOffset + (x - radius).coerceIn(0, w - 1)]
                val right = inPixels[rowOffset + (x + radius + 1).coerceIn(0, w - 1)]

                sumR += ((right shr 16) and 0xFF) - ((left shr 16) and 0xFF)
                sumG += ((right shr 8) and 0xFF) - ((left shr 8) and 0xFF)
                sumB += (right and 0xFF) - (left and 0xFF)
            }
        }

        // Pass 2: Vertical Blur
        for (x in 0 until w) {
            var sumR = 0
            var sumG = 0
            var sumB = 0

            for (i in -radius..radius) {
                val p = tempPixels[i.coerceIn(0, h - 1) * w + x]
                sumR += (p shr 16) and 0xFF
                sumG += (p shr 8) and 0xFF
                sumB += p and 0xFF
            }

            for (y in 0 until h) {
                outPixels[y * w + x] = (0xFF shl 24) or
                        ((sumR / div) shl 16) or
                        ((sumG / div) shl 8) or
                        (sumB / div)

                val top = tempPixels[(y - radius).coerceIn(0, h - 1) * w + x]
                val bottom = tempPixels[(y + radius + 1).coerceIn(0, h - 1) * w + x]

                sumR += ((bottom shr 16) and 0xFF) - ((top shr 16) and 0xFF)
                sumG += ((bottom shr 8) and 0xFF) - ((top shr 8) and 0xFF)
                sumB += (bottom and 0xFF) - (top and 0xFF)
            }
        }

        output.setPixels(outPixels, 0, w, 0, 0, w, h)
        return output
    }

    private fun applyLightingStyle(
        pixels: IntArray,
        mask: FloatArray,
        width: Int,
        height: Int,
        style: PortraitLightingStyle
    ) {
        if (style == PortraitLightingStyle.NATURAL) return

        val cx = width * 0.5f
        val cy = height * 0.5f
        val maxDist = sqrt(cx * cx + cy * cy)

        for (y in 0 until height) {
            val rowOffset = y * width
            val dy = y - cy

            for (x in 0 until width) {
                val idx = rowOffset + x
                val m = mask[idx]
                val p = pixels[idx]

                var r = (p shr 16) and 0xFF
                var g = (p shr 8) and 0xFF
                var b = p and 0xFF

                when (style) {
                    PortraitLightingStyle.STUDIO -> {
                        // Boost subject midtones +12%, gently attenuate background -8%
                        val factor = if (m > 0.5f) {
                            1.0f + 0.12f * m
                        } else {
                            1.0f - 0.08f * (1.0f - m)
                        }
                        r = (r * factor).roundToInt().coerceIn(0, 255)
                        g = (g * factor).roundToInt().coerceIn(0, 255)
                        b = (b * factor).roundToInt().coerceIn(0, 255)
                    }

                    PortraitLightingStyle.SOFT -> {
                        // Flattering warm skin diffusion (+12 R, +4 G on subject)
                        if (m > 0.2f) {
                            r = (r + 12 * m).roundToInt().coerceIn(0, 255)
                            g = (g + 4 * m).roundToInt().coerceIn(0, 255)
                        }
                    }

                    PortraitLightingStyle.DRAMATIC -> {
                        // High-contrast key spotlight on subject + peripheral vignette
                        val dx = x - cx
                        val distNorm = sqrt(dx * dx + dy * dy) / maxDist
                        val vignette = (1.0f - 0.35f * distNorm * distNorm).coerceIn(0.5f, 1.0f)

                        val bgDim = if (m < 0.5f) 0.75f else 1.0f
                        val factor = vignette * (m + (1.0f - m) * bgDim)

                        r = (r * factor).roundToInt().coerceIn(0, 255)
                        g = (g * factor).roundToInt().coerceIn(0, 255)
                        b = (b * factor).roundToInt().coerceIn(0, 255)
                    }

                    PortraitLightingStyle.NATURAL -> {}
                }

                pixels[idx] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }
}
