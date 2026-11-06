package com.bishal.adaptivecamera.video.recording

import android.net.Uri

/**
 * Lifecycle states of the video recording engine.
 */
enum class VideoRecordingStatus {
    IDLE,
    PREPARING,
    RECORDING,
    PAUSED,
    FINALIZING,
    COMPLETED,
    ERROR;

    val isRecordingOrPaused: Boolean get() = this == RECORDING || this == PAUSED
    val isActivelyWriting: Boolean get() = this == RECORDING
}

/**
 * Snapshot of active recording state and live recording statistics.
 */
data class VideoRecordingState(
    val status: VideoRecordingStatus = VideoRecordingStatus.IDLE,
    val durationMs: Long = 0L,
    val bytesRecorded: Long = 0L,
    val currentBitrateBps: Long = 0L,
    val outputUri: Uri? = null,
    val errorMessage: String? = null
) {
    val formattedDuration: String
        get() {
            val totalSeconds = durationMs / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) {
                String.format("%02d:%02d:%02d", hours, minutes, seconds)
            } else {
                String.format("%02d:%02d", minutes, seconds)
            }
        }

    val formattedSize: String
        get() {
            val mb = bytesRecorded.toDouble() / (1024.0 * 1024.0)
            return if (mb >= 1024.0) {
                String.format("%.2f GB", mb / 1024.0)
            } else {
                String.format("%.1f MB", mb)
            }
        }
}
