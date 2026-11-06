package com.bishal.adaptivecamera.video.export

import android.net.Uri

/**
 * State representing the progress and lifecycle of an asynchronous video export operation.
 */
sealed interface VideoExportState {

    /**
     * Initial idle state before an export has started.
     */
    data object Idle : VideoExportState

    /**
     * Preparing resources, opening input streams, and initializing codecs.
     */
    data class Preparing(val message: String = "Preparing export pipeline...") : VideoExportState

    /**
     * Active transcoding/remuxing progress.
     */
    data class Processing(
        val progress: Float, // 0.0f to 1.0f
        val currentBytes: Long,
        val totalEstimatedBytes: Long,
        val elapsedMs: Long,
        val etaMs: Long
    ) : VideoExportState {
        val percent: Int
            get() = (progress * 100f).toInt().coerceIn(0, 100)
    }

    /**
     * Successfully completed export.
     */
    data class Completed(
        val outputUri: Uri?,
        val outputPath: String,
        val outputSizeBytes: Long,
        val durationMs: Long,
        val resolution: String,
        val codec: String
    ) : VideoExportState

    /**
     * The export operation was explicitly cancelled by the user.
     */
    data object Cancelled : VideoExportState

    /**
     * An error occurred during export with failure details.
     */
    data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : VideoExportState
}
