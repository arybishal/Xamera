package com.bishal.adaptivecamera.camera.config

import android.graphics.Rect as AndroidRect
import androidx.compose.ui.geometry.Rect as ComposeRect

/**
 * Authoritative aspect ratio configuration for Xamera.
 * Governs both viewfinder framing overlays and captured image crop geometry.
 */
enum class AspectRatioConfig(
    val displayName: String,
    val description: String
) {
    FULL("Full", "Full sensor active array"),
    RATIO_4_3("4:3", "Standard 4:3 sensor format (presented as 3:4 in portrait)"),
    RATIO_16_9("16:9", "Widescreen cinematic format (9:16 in portrait)"),
    RATIO_1_1("1:1", "Square format"),
    RATIO_2_39_1("2.39:1", "Anamorphic theatrical scope");

    /**
     * Returns the target width-to-height ratio based on orientation.
     * In portrait orientation, 4:3 becomes 3:4 (0.75), 16:9 becomes 9:16 (~0.5625).
     * FULL returns null indicating unconstrained full container bounds.
     */
    fun getTargetRatio(isPortrait: Boolean): Float? {
        return when (this) {
            FULL -> null
            RATIO_4_3 -> if (isPortrait) 3f / 4f else 4f / 3f
            RATIO_16_9 -> if (isPortrait) 9f / 16f else 16f / 9f
            RATIO_1_1 -> 1.0f
            RATIO_2_39_1 -> if (isPortrait) 1f / 2.39f else 2.39f
        }
    }

    /**
     * Calculates the centered framing rectangle within a Compose viewfinder viewport.
     */
    fun calculateFramingBounds(containerWidth: Float, containerHeight: Float, isPortrait: Boolean): ComposeRect {
        if (containerWidth <= 0f || containerHeight <= 0f) {
            return ComposeRect(0f, 0f, 0f, 0f)
        }
        val targetRatio = getTargetRatio(isPortrait)
            ?: return ComposeRect(0f, 0f, containerWidth, containerHeight)

        val containerRatio = containerWidth / containerHeight
        val frameWidth: Float
        val frameHeight: Float

        if (containerRatio > targetRatio) {
            // Container is wider than target ratio: constrained by height
            frameHeight = containerHeight
            frameWidth = (frameHeight * targetRatio).coerceAtMost(containerWidth)
        } else {
            // Container is taller than target ratio: constrained by width
            frameWidth = containerWidth
            frameHeight = (frameWidth / targetRatio).coerceAtMost(containerHeight)
        }

        val left = (containerWidth - frameWidth) / 2f
        val top = (containerHeight - frameHeight) / 2f
        return ComposeRect(left, top, left + frameWidth, top + frameHeight)
    }

    /**
     * Calculates the pixel-exact center crop bounding box for a captured bitmap image.
     */
    fun calculateCropRect(imageWidth: Int, imageHeight: Int): AndroidRect {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return AndroidRect(0, 0, 0, 0)
        }
        val isPortrait = imageHeight > imageWidth
        val targetRatio = getTargetRatio(isPortrait)
            ?: return AndroidRect(0, 0, imageWidth, imageHeight)

        val imageRatio = imageWidth.toFloat() / imageHeight.toFloat()
        val cropWidth: Int
        val cropHeight: Int

        if (imageRatio > targetRatio) {
            cropHeight = imageHeight
            cropWidth = (cropHeight * targetRatio).toInt().coerceIn(1, imageWidth)
        } else {
            cropWidth = imageWidth
            cropHeight = (cropWidth / targetRatio).toInt().coerceIn(1, imageHeight)
        }

        val left = (imageWidth - cropWidth) / 2
        val top = (imageHeight - cropHeight) / 2
        return AndroidRect(left, top, left + cropWidth, top + cropHeight)
    }

    fun next(): AspectRatioConfig = when (this) {
        RATIO_4_3 -> RATIO_16_9
        RATIO_16_9 -> RATIO_1_1
        RATIO_1_1 -> RATIO_2_39_1
        RATIO_2_39_1 -> FULL
        FULL -> RATIO_4_3
    }
}
