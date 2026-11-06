package com.bishal.adaptivecamera.video.config

import androidx.camera.video.Quality

/**
 * Supported video recording resolutions with pixel dimensions, label,
 * and mapping to CameraX Quality tier.
 */
enum class VideoResolution(
    val width: Int,
    val height: Int,
    val label: String,
    val quality: Quality
) {
    UHD_4K(3840, 2160, "4K UHD", Quality.UHD),
    FHD_1080P(1920, 1080, "1080p FHD", Quality.FHD),
    HD_720P(1280, 720, "720p HD", Quality.HD),
    SD_480P(720, 480, "480p SD", Quality.SD);

    val totalPixels: Int get() = width * height
    val shortLabel: String get() = when (this) {
        UHD_4K -> "4K"
        FHD_1080P -> "1080p"
        HD_720P -> "720p"
        SD_480P -> "480p"
    }

    companion object {
        fun fromDimensions(w: Int, h: Int): VideoResolution? {
            val maxDim = maxOf(w, h)
            val minDim = minOf(w, h)
            return entries.firstOrNull {
                (it.width == maxDim && it.height == minDim) ||
                (it.width == minDim && it.height == maxDim)
            }
        }
    }
}
