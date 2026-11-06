package com.bishal.adaptivecamera.video.export

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoResolution

/**
 * Information about a detected video encoder on the device.
 */
data class CodecInfoSummary(
    val codec: VideoCodec,
    val mimeType: String,
    val encoderName: String,
    val isHardwareAccelerated: Boolean,
    val maxSupportedWidth: Int,
    val maxSupportedHeight: Int,
    val maxSupportedBitrateBps: Int
)

/**
 * Discovers and queries hardware and software video encoders available on the device.
 */
class VideoCodecCapabilityDetector(
    private val codecList: MediaCodecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
) {

    /**
     * Finds the primary encoder for a given video codec, prioritizing hardware acceleration.
     */
    fun findEncoder(codec: VideoCodec): CodecInfoSummary? {
        val mime = codec.mimeType
        val encoders = codecList.codecInfos.filter { it.isEncoder && it.supportedTypes.any { t -> t.equals(mime, ignoreCase = true) } }

        if (encoders.isEmpty()) return null

        // Sort hardware accelerated encoders first
        val bestInfo = encoders.minByOrNull {
            if (it.isHardwareAccelerated) 0 else 1
        } ?: return null

        val caps = bestInfo.getCapabilitiesForType(mime)
        val videoCaps = caps.videoCapabilities

        return CodecInfoSummary(
            codec = codec,
            mimeType = mime,
            encoderName = bestInfo.name,
            isHardwareAccelerated = bestInfo.isHardwareAccelerated,
            maxSupportedWidth = videoCaps?.supportedWidths?.upper ?: 3840,
            maxSupportedHeight = videoCaps?.supportedHeights?.upper ?: 2160,
            maxSupportedBitrateBps = videoCaps?.bitrateRange?.upper ?: 50_000_000
        )
    }

    /**
     * Returns true if the device has a viable encoder for the specified codec and resolution.
     */
    fun isCodecSupported(codec: VideoCodec, resolution: VideoResolution): Boolean {
        val encoder = findEncoder(codec) ?: return false
        val width = resolution.width
        val height = resolution.height

        return width <= encoder.maxSupportedWidth && height <= encoder.maxSupportedHeight
    }

    /**
     * Returns the list of all supported video codecs on this device.
     */
    fun getSupportedCodecs(): List<VideoCodec> {
        return VideoCodec.entries.filter { findEncoder(it) != null }
    }
}
