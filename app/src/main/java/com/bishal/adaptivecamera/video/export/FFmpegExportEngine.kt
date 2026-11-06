package com.bishal.adaptivecamera.video.export

import android.content.Context
import android.util.Log
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Executes or orchestrates FFmpeg-based transcoding jobs (FEAT-INT-07).
 * When native CLI binary is present or callable, it spawns the process;
 * otherwise provides validated command-line logs and delegates to native
 * hardware-accelerated MediaCodec pipeline.
 */
class FFmpegExportEngine(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager,
    private val nativeExportEngine: VideoExportEngine
) {

    private val _engineState = MutableStateFlow<VideoExportState>(VideoExportState.Idle)
    val engineState: StateFlow<VideoExportState> = _engineState.asStateFlow()

    /**
     * Executes transcode using FFmpeg command arguments.
     */
    suspend fun executeTranscode(options: FFmpegVideoOptions): VideoExportState = withContext(Dispatchers.IO) {
        _engineState.value = VideoExportState.Preparing("Assembling FFmpeg transcode pipeline...")

        val commandArgs = FFmpegCommandBuilder.buildCommand(options)
        val cmdString = FFmpegCommandBuilder.buildCommandLineString(options)
        Log.i(TAG, "Generated FFmpeg Command: $cmdString")

        val inputFile = File(options.inputPath)
        if (!inputFile.exists()) {
            val error = VideoExportState.Error("Input video file does not exist: ${options.inputPath}")
            _engineState.value = error
            return@withContext error
        }

        val outputFile = File(options.outputPath)
        outputFile.parentFile?.mkdirs()

        // Check for bundled/system ffmpeg binary
        val ffmpegBinary = findFFmpegBinary()
        if (ffmpegBinary != null) {
            try {
                _engineState.value = VideoExportState.Processing(
                    progress = 0.1f,
                    currentBytes = 0L,
                    totalEstimatedBytes = outputFile.length(),
                    elapsedMs = 0L,
                    etaMs = 0L
                )
                val fullCommand = listOf(ffmpegBinary.absolutePath) + commandArgs.drop(0)
                val process = ProcessBuilder(fullCommand)
                    .redirectErrorStream(true)
                    .start()

                val reader = process.inputStream.bufferedReader()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    Log.v(TAG, "FFmpeg: $line")
                }

                val exitCode = process.waitFor()
                if (exitCode == 0 && outputFile.exists()) {
                    val mediaItem = mediaStoreManager.insertVideoFile(
                        file = outputFile,
                        mimeType = "video/mp4"
                    )
                    val completed = VideoExportState.Completed(
                        outputUri = mediaItem?.uri,
                        outputPath = outputFile.absolutePath,
                        outputSizeBytes = outputFile.length(),
                        durationMs = 0L,
                        resolution = "${options.targetWidth ?: 1920}x${options.targetHeight ?: 1080}",
                        codec = options.codec
                    )
                    _engineState.value = completed
                    return@withContext completed
                } else {
                    Log.w(TAG, "FFmpeg process failed with exit code $exitCode. Falling back to native MediaCodec.")
                }
            } catch (e: Exception) {
                Log.w(TAG, "FFmpeg binary execution failed, falling back", e)
            }
        }

        // Graceful fallback to native hardware-accelerated MediaCodec pipeline
        Log.i(TAG, "Using native hardware MediaCodec export engine for maximum on-device efficiency.")
        val fallbackConfig = VideoExportConfig(
            sourceUri = android.net.Uri.fromFile(inputFile),
            codec = if (options.codec == "libx265") com.bishal.adaptivecamera.video.config.VideoCodec.H265_HEVC else com.bishal.adaptivecamera.video.config.VideoCodec.H264_AVC,
            audioAction = if (options.muteAudio) VideoExportAudioAction.MUTE else VideoExportAudioAction.KEEP,
            trimStartMs = options.startTrimSec?.let { (it * 1000).toLong() } ?: 0L,
            trimEndMs = options.endTrimSec?.let { (it * 1000).toLong() } ?: 0L
        )

        return@withContext nativeExportEngine.exportVideo(fallbackConfig)
    }

    fun isFFmpegBinaryAvailable(): Boolean {
        return findFFmpegBinary() != null
    }

    fun inspectExecutionPlan(options: FFmpegVideoOptions): FFmpegExecutionPlan {
        val cmd = FFmpegCommandBuilder.buildCommandLineString(options)
        val binary = findFFmpegBinary()
        val method = if (binary != null) FFmpegExecutionMethod.FFMPEG_CLI_BINARY else FFmpegExecutionMethod.MEDIA_CODEC_HARDWARE_FALLBACK
        return FFmpegExecutionPlan(
            commandConstructed = cmd.isNotBlank(),
            commandLine = cmd,
            binaryAvailable = binary != null,
            executionMethod = method,
            binaryPath = binary?.absolutePath
        )
    }

    private fun findFFmpegBinary(): File? {
        val candidates = listOf(
            File(context.applicationInfo.nativeLibraryDir, "libffmpeg.so"),
            File(context.filesDir, "ffmpeg"),
            File("/system/bin/ffmpeg")
        )
        return candidates.firstOrNull { it.exists() && it.canExecute() }
    }

    companion object {
        private const val TAG = "FFmpegExportEngine"
    }
}

/**
 * Execution method classification for video export and transcoding jobs.
 */
enum class FFmpegExecutionMethod(val displayName: String) {
    FFMPEG_CLI_BINARY("FFmpeg CLI Native Binary"),
    MEDIA_CODEC_HARDWARE_FALLBACK("Android Native MediaCodec Hardware Pipeline")
}

/**
 * Diagnostic record detailing execution prerequisites for a transcode job.
 */
data class FFmpegExecutionPlan(
    val commandConstructed: Boolean,
    val commandLine: String,
    val binaryAvailable: Boolean,
    val executionMethod: FFmpegExecutionMethod,
    val binaryPath: String? = null
)
