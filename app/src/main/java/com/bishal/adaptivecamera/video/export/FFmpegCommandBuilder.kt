package com.bishal.adaptivecamera.video.export

import java.io.File

data class FFmpegVideoOptions(
    val inputPath: String,
    val outputPath: String,
    val codec: String = "libx265",
    val crf: Int = 22,
    val videoBitrate: String? = null,
    val audioCodec: String = "aac",
    val audioBitrate: String = "256k",
    val muteAudio: Boolean = false,
    val lutFilePath: String? = null,
    val anamorphicFactor: Float? = null,
    val targetWidth: Int? = null,
    val targetHeight: Int? = null,
    val targetFps: Int? = null,
    val startTrimSec: Float? = null,
    val endTrimSec: Float? = null,
    val fastStart: Boolean = true
)

/**
 * Builds standard FFmpeg command argument lists for professional video transcoding,
 * LUT baking, anamorphic de-squeezing, and multi-format export (FEAT-INT-07).
 */
object FFmpegCommandBuilder {

    fun buildCommand(options: FFmpegVideoOptions): List<String> {
        val args = mutableListOf<String>()
        args.add("-y") // Overwrite output without asking

        // Fast seek trim input start if specified
        if (options.startTrimSec != null && options.startTrimSec > 0f) {
            args.add("-ss")
            args.add(String.format(java.util.Locale.US, "%.3f", options.startTrimSec))
        }

        // Input file
        args.add("-i")
        args.add(options.inputPath)

        // End trim
        if (options.endTrimSec != null && options.endTrimSec > (options.startTrimSec ?: 0f)) {
            val duration = options.endTrimSec - (options.startTrimSec ?: 0f)
            args.add("-t")
            args.add(String.format(java.util.Locale.US, "%.3f", duration))
        }

        // Build Video Filter Graph
        val filterList = mutableListOf<String>()

        // 1. Anamorphic de-squeeze
        if (options.anamorphicFactor != null && options.anamorphicFactor > 1.0f) {
            filterList.add("scale=iw*${String.format(java.util.Locale.US, "%.2f", options.anamorphicFactor)}:ih")
        }

        // 2. 3D LUT bake
        if (!options.lutFilePath.isNullOrEmpty() && File(options.lutFilePath).exists()) {
            filterList.add("lut3d=file='${options.lutFilePath}'")
        }

        // 3. Resolution scaling
        if (options.targetWidth != null && options.targetHeight != null) {
            filterList.add("scale=${options.targetWidth}:${options.targetHeight}:force_original_aspect_ratio=decrease")
        }

        // 4. Frame rate
        if (options.targetFps != null && options.targetFps > 0) {
            filterList.add("fps=fps=${options.targetFps}")
        }

        if (filterList.isNotEmpty()) {
            args.add("-vf")
            args.add(filterList.joinToString(","))
        }

        // Video codec & encoding flags
        args.add("-c:v")
        args.add(options.codec)

        if (options.codec == "libx265") {
            args.add("-tag:v")
            args.add("hvc1") // Crucial for Apple / QuickTime playback
            args.add("-preset")
            args.add("medium")
            args.add("-crf")
            args.add(options.crf.toString())
        } else if (options.codec == "libx264") {
            args.add("-preset")
            args.add("fast")
            args.add("-crf")
            args.add(options.crf.toString())
        }

        if (!options.videoBitrate.isNullOrEmpty()) {
            args.add("-b:v")
            args.add(options.videoBitrate)
        }

        // Pixel format
        args.add("-pix_fmt")
        args.add("yuv420p")

        // Audio options
        if (options.muteAudio) {
            args.add("-an")
        } else {
            args.add("-c:a")
            args.add(options.audioCodec)
            args.add("-b:a")
            args.add(options.audioBitrate)
        }

        // Container options
        if (options.fastStart) {
            args.add("-movflags")
            args.add("+faststart")
        }

        // Output path
        args.add(options.outputPath)

        return args
    }

    fun buildCommandLineString(options: FFmpegVideoOptions): String {
        return buildCommand(options).joinToString(" ")
    }
}
