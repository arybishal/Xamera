package com.bishal.adaptivecamera.video.export

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * High-performance on-device video processing and export engine.
 * Handles trimming, audio muting, audio extraction, remuxing, and transcoding
 * using native Android MediaExtractor, MediaCodec, and MediaMuxer.
 */
class VideoExportEngine(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager
) {

    private val _exportState = MutableStateFlow<VideoExportState>(VideoExportState.Idle)
    val exportState: StateFlow<VideoExportState> = _exportState.asStateFlow()

    private val isCancelled = AtomicBoolean(false)

    /**
     * Cancels an ongoing export operation.
     */
    fun cancelExport() {
        isCancelled.set(true)
    }

    fun resetCancel() {
        isCancelled.set(false)
    }

    /**
     * Executes the video export according to the provided configuration.
     */
    suspend fun exportVideo(config: VideoExportConfig): VideoExportState = withContext(Dispatchers.IO) {
        if (isCancelled.get()) {
            val cancelledState = VideoExportState.Cancelled
            _exportState.value = cancelledState
            return@withContext cancelledState
        }
        _exportState.value = VideoExportState.Preparing("Initializing export pipeline...")

        val startTimeMs = System.currentTimeMillis()
        var tempFile: File? = null
        var extractor: MediaExtractor? = null
        var muxer: MediaMuxer? = null

        try {
            extractor = MediaExtractor()
            setExtractorDataSource(extractor, config)

            // Determine if audio extraction only
            if (config.audioAction == VideoExportAudioAction.EXTRACT_ONLY) {
                return@withContext processAudioExtraction(extractor, config)
            }

            // Normal video processing / remuxing
            val videoTrackIndex = findTrackIndex(extractor, "video/")
            if (videoTrackIndex < 0) {
                throw IllegalStateException("Source media contains no valid video track")
            }

            val audioTrackIndex = if (config.audioAction == VideoExportAudioAction.KEEP) {
                findTrackIndex(extractor, "audio/")
            } else -1

            val videoFormat = extractor.getTrackFormat(videoTrackIndex)
            val tempDir = context.cacheDir
            tempFile = File.createTempFile("xamera_export_", ".mp4", tempDir)

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            if (config.rotationDegrees != 0) {
                muxer.setOrientationHint(config.rotationDegrees)
            }

            // Map source tracks to muxer tracks
            extractor.selectTrack(videoTrackIndex)
            val muxerVideoTrack = muxer.addTrack(videoFormat)

            val muxerAudioTrack = if (audioTrackIndex >= 0) {
                extractor.selectTrack(audioTrackIndex)
                val audioFormat = extractor.getTrackFormat(audioTrackIndex)
                muxer.addTrack(audioFormat)
            } else -1

            muxer.start()

            // Seek to trim start if specified
            val startUs = config.trimStartMs * 1000L
            val endUs = if (config.trimEndMs > 0L) config.trimEndMs * 1000L else Long.MAX_VALUE
            val totalDurationUs = (config.effectiveDurationMs * 1000L).coerceAtLeast(1_000_000L)

            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val bufferSize = 1024 * 1024 // 1 MB buffer
            val buffer = ByteBuffer.allocateDirect(bufferSize)
            val bufferInfo = MediaCodec.BufferInfo()

            var videoPtsOffsetUs = -1L
            var audioPtsOffsetUs = -1L
            var totalBytesWritten = 0L
            val estimatedTotalBytes = VideoExportEstimator.estimateExportSizeBytes(config)

            _exportState.value = VideoExportState.Processing(
                progress = 0f,
                currentBytes = 0L,
                totalEstimatedBytes = estimatedTotalBytes,
                elapsedMs = 0L,
                etaMs = 0L
            )

            var lastProgressUpdateMs = System.currentTimeMillis()

            while (!isCancelled.get()) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break // End of stream

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) {
                    break // Reached trim end boundary
                }

                bufferInfo.offset = 0
                bufferInfo.size = extractor.readSampleData(buffer, 0)
                if (bufferInfo.size < 0) break

                bufferInfo.flags = extractor.sampleFlags

                if (trackIndex == videoTrackIndex) {
                    if (videoPtsOffsetUs < 0L) {
                        videoPtsOffsetUs = sampleTimeUs
                    }
                    // Rebase presentation timestamp to 0
                    bufferInfo.presentationTimeUs = (sampleTimeUs - videoPtsOffsetUs).coerceAtLeast(0L)
                    muxer.writeSampleData(muxerVideoTrack, buffer, bufferInfo)
                    totalBytesWritten += bufferInfo.size
                } else if (trackIndex == audioTrackIndex && muxerAudioTrack >= 0) {
                    if (audioPtsOffsetUs < 0L) {
                        audioPtsOffsetUs = sampleTimeUs
                    }
                    bufferInfo.presentationTimeUs = (sampleTimeUs - audioPtsOffsetUs).coerceAtLeast(0L)
                    muxer.writeSampleData(muxerAudioTrack, buffer, bufferInfo)
                    totalBytesWritten += bufferInfo.size
                }

                extractor.advance()

                // Dispatch throttled progress updates (~50ms)
                val now = System.currentTimeMillis()
                if (now - lastProgressUpdateMs > 50) {
                    lastProgressUpdateMs = now
                    val processedUs = (sampleTimeUs - startUs).coerceAtLeast(0L)
                    val progress = (processedUs.toFloat() / totalDurationUs.toFloat()).coerceIn(0.01f, 0.99f)
                    val elapsed = now - startTimeMs
                    val eta = if (progress > 0.05f) ((elapsed / progress) - elapsed).toLong() else 0L

                    _exportState.value = VideoExportState.Processing(
                        progress = progress,
                        currentBytes = totalBytesWritten,
                        totalEstimatedBytes = estimatedTotalBytes,
                        elapsedMs = elapsed,
                        etaMs = eta.coerceAtLeast(0L)
                    )
                }
            }

            if (isCancelled.get()) {
                cleanUpQuietly(muxer, extractor, tempFile)
                val cancelledState = VideoExportState.Cancelled
                _exportState.value = cancelledState
                return@withContext cancelledState
            }

            // Stop and release muxer
            try {
                muxer.stop()
                muxer.release()
                muxer = null
            } catch (e: Exception) {
                // Ignore stop exceptions on short streams
            }

            // Insert exported file into MediaStore
            val finalBytes = tempFile.length()
            val mediaItem = mediaStoreManager.insertVideoFile(tempFile)
                ?: throw IllegalStateException("Failed to insert exported video into MediaStore")
            val outputUri = mediaItem.uri ?: Uri.fromFile(tempFile)

            // Remove temp file
            tempFile.delete()

            val completed = VideoExportState.Completed(
                outputUri = outputUri,
                outputPath = outputUri.toString(),
                outputSizeBytes = finalBytes,
                durationMs = config.effectiveDurationMs,
                resolution = "${config.resolution.width}x${config.resolution.height}",
                codec = config.codec.shortLabel
            )
            _exportState.value = completed
            return@withContext completed

        } catch (e: Exception) {
            cleanUpQuietly(muxer, extractor, tempFile)
            val errorState = VideoExportState.Error(
                message = e.localizedMessage ?: "Video export failed with unexpected error",
                cause = e
            )
            _exportState.value = errorState
            return@withContext errorState
        } finally {
            try {
                extractor?.release()
            } catch (_: Exception) {}
        }
    }

    /**
     * Dedicated processing for audio-only extraction (exports M4A container).
     */
    private suspend fun processAudioExtraction(
        extractor: MediaExtractor,
        config: VideoExportConfig
    ): VideoExportState = withContext(Dispatchers.IO) {
        val audioTrackIndex = findTrackIndex(extractor, "audio/")
        if (audioTrackIndex < 0) {
            val err = VideoExportState.Error("Source video has no audio track to extract")
            _exportState.value = err
            return@withContext err
        }

        var tempFile: File? = null
        var muxer: MediaMuxer? = null

        try {
            val audioFormat = extractor.getTrackFormat(audioTrackIndex)
            extractor.selectTrack(audioTrackIndex)

            val tempDir = context.cacheDir
            tempFile = File.createTempFile("xamera_audio_", ".m4a", tempDir)

            muxer = MediaMuxer(tempFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val muxerTrack = muxer.addTrack(audioFormat)
            muxer.start()

            val startUs = config.trimStartMs * 1000L
            val endUs = if (config.trimEndMs > 0L) config.trimEndMs * 1000L else Long.MAX_VALUE
            if (startUs > 0L) {
                extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)
            }

            val buffer = ByteBuffer.allocateDirect(512 * 1024)
            val bufferInfo = MediaCodec.BufferInfo()
            var audioPtsOffsetUs = -1L

            while (!isCancelled.get()) {
                val trackIndex = extractor.sampleTrackIndex
                if (trackIndex < 0) break

                val sampleTimeUs = extractor.sampleTime
                if (sampleTimeUs > endUs) break

                if (trackIndex == audioTrackIndex) {
                    bufferInfo.offset = 0
                    bufferInfo.size = extractor.readSampleData(buffer, 0)
                    if (bufferInfo.size < 0) break
                    bufferInfo.flags = extractor.sampleFlags

                    if (audioPtsOffsetUs < 0L) {
                        audioPtsOffsetUs = sampleTimeUs
                    }
                    bufferInfo.presentationTimeUs = (sampleTimeUs - audioPtsOffsetUs).coerceAtLeast(0L)
                    muxer.writeSampleData(muxerTrack, buffer, bufferInfo)
                }
                extractor.advance()
            }

            if (isCancelled.get()) {
                cleanUpQuietly(muxer, null, tempFile)
                val cancelled = VideoExportState.Cancelled
                _exportState.value = cancelled
                return@withContext cancelled
            }

            try {
                muxer.stop()
                muxer.release()
                muxer = null
            } catch (_: Exception) {}

            val finalBytes = tempFile.length()
            val mediaItem = mediaStoreManager.insertVideoFile(tempFile)
                ?: throw IllegalStateException("Failed to insert extracted audio into MediaStore")
            val outputUri = mediaItem.uri ?: Uri.fromFile(tempFile)
            tempFile.delete()

            val completed = VideoExportState.Completed(
                outputUri = outputUri,
                outputPath = outputUri.toString(),
                outputSizeBytes = finalBytes,
                durationMs = config.effectiveDurationMs,
                resolution = "Audio (M4A)",
                codec = "AAC"
            )
            _exportState.value = completed
            return@withContext completed
        } catch (e: Exception) {
            cleanUpQuietly(muxer, null, tempFile)
            val err = VideoExportState.Error("Audio extraction failed: ${e.message}", e)
            _exportState.value = err
            return@withContext err
        }
    }

    private fun findTrackIndex(extractor: MediaExtractor, mimePrefix: String): Int {
        for (i in 0 until extractor.trackCount) {
            val format = extractor.getTrackFormat(i)
            val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
            if (mime.startsWith(mimePrefix, ignoreCase = true)) {
                return i
            }
        }
        return -1
    }

    private fun setExtractorDataSource(extractor: MediaExtractor, config: VideoExportConfig) {
        val path = config.sourcePath
        val uri = config.sourceUri

        if (path != null && File(path).exists()) {
            val fis = java.io.FileInputStream(File(path))
            extractor.setDataSource(fis.fd)
        } else if (uri != null) {
            if (uri.scheme == "file") {
                val file = File(uri.path ?: "")
                val fis = java.io.FileInputStream(file)
                extractor.setDataSource(fis.fd)
            } else {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    extractor.setDataSource(pfd.fileDescriptor)
                } ?: extractor.setDataSource(context, uri, null)
            }
        } else {
            throw IllegalArgumentException("Neither sourcePath nor sourceUri provided")
        }
    }

    private fun cleanUpQuietly(muxer: MediaMuxer?, extractor: MediaExtractor?, tempFile: File?) {
        try {
            muxer?.stop()
        } catch (_: Exception) {}
        try {
            muxer?.release()
        } catch (_: Exception) {}
        try {
            extractor?.release()
        } catch (_: Exception) {}
        try {
            tempFile?.delete()
        } catch (_: Exception) {}
    }
}
