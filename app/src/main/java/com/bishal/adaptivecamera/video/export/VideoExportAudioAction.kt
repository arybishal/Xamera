package com.bishal.adaptivecamera.video.export

/**
 * Action to perform on the audio stream during video export.
 */
enum class VideoExportAudioAction(val displayName: String, val description: String) {
    KEEP("Keep Audio", "Preserve original synchronized audio track"),
    MUTE("Mute Audio", "Remove audio track completely, producing video-only output"),
    EXTRACT_ONLY("Extract Audio Only", "Export audio track to standalone M4A/AAC file")
}
