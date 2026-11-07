package com.bishal.adaptivecamera.video.config

/**
 * Supported dynamic range recording profiles.
 */
enum class VideoDynamicRange(val label: String, val bitDepth: Int) {
    SDR("SDR (8-Bit)", 8),
    HLG10("HLG10 (10-Bit HDR)", 10),
    HDR10("HDR10 (10-Bit PQ)", 10);

    val isHdr: Boolean get() = this != SDR
}

/**
 * Authoritative single source of truth for Xamera video recording configuration.
 */
data class VideoConfig(
    val resolution: VideoResolution = VideoResolution.FHD_1080P,
    val frameRate: VideoFrameRate = VideoFrameRate.FPS_30,
    val bitrateProfile: VideoBitrateProfile = VideoBitrateProfile.HIGH,
    val targetBitrateBps: Long = VideoBitrateProfile.HIGH.getTargetBitrateBps(
        VideoResolution.FHD_1080P,
        VideoFrameRate.FPS_30
    ),
    val videoCodec: VideoCodec = VideoCodec.H265_HEVC,
    val audioEnabled: Boolean = true,
    val exposureMode: VideoExposureMode = VideoExposureMode.AUTO,
    val shutterAngle: Float? = 180f,
    val manualShutterSpeedSeconds: Double? = null,
    val manualIso: Int? = null,
    val exposureCompensationIndex: Int = 0,
    val isKelvinManual: Boolean = false,
    val manualKelvin: Int = 5500,
    val isFocusManual: Boolean = false,
    val manualFocusDistanceDiopters: Float = 0.0f,
    val stabilizationMode: VideoStabilizationMode = VideoStabilizationMode.OIS,
    val dynamicRange: VideoDynamicRange = VideoDynamicRange.SDR,
    val recordingOrientation: Int = 0
) {
    /**
     * Computes the effective shutter speed in seconds.
     * If manualShutterSpeedSeconds is provided, it is used directly.
     * Otherwise, if shutterAngle is set, computes:
     *   shutterSpeed = 1.0 / (FPS * (shutterAngle / 360.0))
     * Returns null if running in full auto exposure without shutter constraint.
     */
    val effectiveShutterSpeedSeconds: Double?
        get() {
            if (manualShutterSpeedSeconds != null) return manualShutterSpeedSeconds
            if (shutterAngle != null && shutterAngle > 0f) {
                return (shutterAngle.toDouble() / 360.0) / frameRate.fps.toDouble()
            }
            return null
        }

    /**
     * Formatted string of the effective shutter speed (e.g. "1/60s", "1/120s").
     */
    val formattedShutterSpeed: String
        get() {
            val speed = effectiveShutterSpeedSeconds ?: return "Auto"
            return if (speed >= 1.0) {
                String.format("%.1fs", speed)
            } else {
                val denom = kotlin.math.round(1.0 / speed).toInt()
                "1/${denom}s"
            }
        }

    /**
     * Compact badge label (e.g. "4K · 30", "1080p · 60").
     */
    val badgeLabel: String
        get() = "${resolution.shortLabel} · ${frameRate.shortLabel}"

    /**
     * Returns a copy with target bitrate synchronized with the active resolution and frame rate.
     */
    fun syncBitrate(): VideoConfig {
        if (bitrateProfile == VideoBitrateProfile.CUSTOM) return this
        return copy(targetBitrateBps = bitrateProfile.getTargetBitrateBps(resolution, frameRate))
    }

    /**
     * Validates whether the configuration parameters are mathematically within bounds.
     */
    fun validate(): VideoConfigValidationResult {
        val errors = mutableListOf<String>()

        if (targetBitrateBps < 500_000L || targetBitrateBps > 200_000_000L) {
            errors.add("Target bitrate $targetBitrateBps bps is out of safe bounds [500Kbps, 200Mbps]")
        }

        if (shutterAngle != null && (shutterAngle < 1f || shutterAngle > 360f)) {
            errors.add("Shutter angle $shutterAngle° is outside valid photographic domain [1°, 360°]")
        }

        if (manualIso != null && (manualIso < 50 || manualIso > 25600)) {
            errors.add("Manual ISO $manualIso is outside standard sensor bounds [50, 25600]")
        }

        if (manualKelvin < 2000 || manualKelvin > 10000) {
            errors.add("Manual Kelvin $manualKelvin K is outside Planckian locus range [2000K, 10000K]")
        }

        return if (errors.isEmpty()) {
            VideoConfigValidationResult.Valid
        } else {
            VideoConfigValidationResult.Invalid(errors)
        }
    }
}

sealed interface VideoConfigValidationResult {
    data object Valid : VideoConfigValidationResult
    data class Invalid(val errors: List<String>) : VideoConfigValidationResult
}
