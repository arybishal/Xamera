package com.bishal.adaptivecamera.video.config

import android.media.MediaFormat

/**
 * Supported video encoding codecs.
 */
enum class VideoCodec(
    val mimeType: String,
    val label: String,
    val shortLabel: String
) {
    H264_AVC(MediaFormat.MIMETYPE_VIDEO_AVC, "H.264 / AVC (High Compatibility)", "H.264"),
    H265_HEVC(MediaFormat.MIMETYPE_VIDEO_HEVC, "H.265 / HEVC (High Efficiency)", "H.265"),
    AV1("video/av01", "AV1 (Next-Gen Open Codec)", "AV1");

    companion object {
        fun fromMime(mime: String): VideoCodec = when (mime) {
            MediaFormat.MIMETYPE_VIDEO_HEVC -> H265_HEVC
            "video/av01" -> AV1
            else -> H264_AVC
        }
    }
}
