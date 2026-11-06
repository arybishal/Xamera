package com.bishal.adaptivecamera.video.config

/**
 * Predefined video encoding bitrate profiles with calculated bitrates per resolution.
 */
enum class VideoBitrateProfile(val label: String, val description: String) {
    STANDARD("Standard", "Balanced quality and file size for everyday recording"),
    HIGH("High (Pro)", "Enhanced bitrate for professional grading and motion clarity"),
    ALL_I("Intra-Frame (All-I)", "Maximum bitrate with intra-frame encoding for editability"),
    CUSTOM("Custom", "User-specified target bitrate");

    /**
     * Calculates the recommended bitrate in bits per second (bps) for a given resolution and frame rate.
     */
    fun getTargetBitrateBps(resolution: VideoResolution, frameRate: VideoFrameRate): Long {
        val baseBitrate = when (resolution) {
            VideoResolution.UHD_4K -> when (this) {
                STANDARD -> 45_000_000L  // 45 Mbps
                HIGH -> 80_000_000L      // 80 Mbps
                ALL_I -> 120_000_000L    // 120 Mbps
                CUSTOM -> 60_000_000L
            }
            VideoResolution.FHD_1080P -> when (this) {
                STANDARD -> 16_000_000L  // 16 Mbps
                HIGH -> 35_000_000L      // 35 Mbps
                ALL_I -> 60_000_000L     // 60 Mbps
                CUSTOM -> 25_000_000L
            }
            VideoResolution.HD_720P -> when (this) {
                STANDARD -> 8_000_000L   // 8 Mbps
                HIGH -> 16_000_000L      // 16 Mbps
                ALL_I -> 30_000_000L     // 30 Mbps
                CUSTOM -> 12_000_000L
            }
            VideoResolution.SD_480P -> when (this) {
                STANDARD -> 3_000_000L
                HIGH -> 6_000_000L
                ALL_I -> 12_000_000L
                CUSTOM -> 4_000_000L
            }
        }

        // Scale proportionally if frame rate exceeds 30 fps
        val fpsFactor = if (frameRate.fps > 30) {
            frameRate.fps.toDouble() / 30.0
        } else {
            1.0
        }

        return (baseBitrate * fpsFactor).toLong()
    }
}
