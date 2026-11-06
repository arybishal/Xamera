package com.bishal.adaptivecamera.video.recording

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import androidx.core.content.ContextCompat
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoConfig
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Hardware-accelerated GPU video recorder encoding direct Surface input frames
 * into MP4 containers using MediaCodec and MediaMuxer.
 *
 * Coordinates:
 * - Surface input video encoding (H.264 AVC or H.265 HEVC)
 * - AAC-LC audio capture via AudioRecord and MediaCodec
 * - Multiplexing into standardized MP4 containers
 * - Thread-safe start, pause, resume, and finalized MediaStore publication.
 */
class GpuVideoRecorder(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager
) {
    private var videoEncoder: MediaCodec? = null
    private var audioEncoder: MediaCodec? = null
    private var audioRecord: AudioRecord? = null
    private var mediaMuxer: MediaMuxer? = null

    private var inputSurface: Surface? = null
    private var outputFile: File? = null

    private var videoTrackIndex = -1
    private var audioTrackIndex = -1
    private var isMuxerStarted = false

    private var drainThread: HandlerThread? = null
    private var drainHandler: Handler? = null

    private val isRecording = AtomicBoolean(false)
    private val isPaused = AtomicBoolean(false)

    private var totalBytesWritten = 0L
    private var recordingStartTimeNs = 0L

    @SuppressLint("MissingPermission")
    fun start(
        config: VideoConfig,
        withAudio: Boolean,
        isTimelapse: Boolean = false,
        onStarted: () -> Unit,
        onError: (Throwable) -> Unit
    ): Surface? {
        try {
            val width = config.resolution.width
            val height = config.resolution.height
            val mimeType = if (config.videoCodec == VideoCodec.H265_HEVC) {
                MediaFormat.MIMETYPE_VIDEO_HEVC
            } else {
                MediaFormat.MIMETYPE_VIDEO_AVC
            }

            // 1. Configure Video Encoder with Surface Input
            val videoFormat = MediaFormat.createVideoFormat(mimeType, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, config.targetBitrateBps.toInt().coerceAtLeast(1_000_000))
                setInteger(MediaFormat.KEY_FRAME_RATE, if (isTimelapse) 30 else config.frameRate.fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }

            val vEncoder = MediaCodec.createEncoderByType(mimeType)
            vEncoder.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            val surface = vEncoder.createInputSurface()
            vEncoder.start()
            videoEncoder = vEncoder
            inputSurface = surface

            // 2. Setup Temporary Output File
            val fileName = MediaStoreManager.generateMediaName("VID_GPU", "mp4")
            val cacheFile = File(context.cacheDir, fileName)
            outputFile = cacheFile
            val muxer = MediaMuxer(cacheFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            mediaMuxer = muxer

            // 3. Optional Audio Encoder (Skipped in Time-Lapse)
            val hasAudioPerm = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED

            if (withAudio && hasAudioPerm && !isTimelapse) {
                try {
                    val sampleRate = 48000
                    val channelConfig = AudioFormat.CHANNEL_IN_STEREO
                    val audioFormat = AudioFormat.ENCODING_PCM_16BIT
                    val minBufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)
                    val bufferSize = (minBufferSize * 2).coerceAtLeast(4096)

                    val aRecord = AudioRecord(
                        MediaRecorder.AudioSource.CAMCORDER,
                        sampleRate,
                        channelConfig,
                        audioFormat,
                        bufferSize
                    )

                    val aFormat = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, 2).apply {
                        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
                        setInteger(MediaFormat.KEY_BIT_RATE, 192_000)
                        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, bufferSize)
                    }

                    val aEncoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
                    aEncoder.configure(aFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    aEncoder.start()
                    aRecord.startRecording()

                    audioRecord = aRecord
                    audioEncoder = aEncoder
                } catch (e: Exception) {
                    Log.w(TAG, "Audio recording initialization failed, continuing video-only: ${e.message}")
                }
            }

            // 4. Start Background Drain Loop
            val thread = HandlerThread("GpuVideoRecorderDrain").apply { start() }
            drainThread = thread
            val handler = Handler(thread.looper)
            drainHandler = handler

            isRecording.set(true)
            isPaused.set(false)
            recordingStartTimeNs = System.nanoTime()

            handler.post {
                runDrainLoop()
            }

            Log.i(TAG, "GpuVideoRecorder started: ${width}x$height, mime=$mimeType, file=${cacheFile.name}")
            onStarted()
            return surface
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start GpuVideoRecorder: ${e.message}", e)
            release()
            onError(e)
            return null
        }
    }

    private fun runDrainLoop() {
        val vEncoder = videoEncoder ?: return
        val muxer = mediaMuxer ?: return
        val bufferInfo = MediaCodec.BufferInfo()

        while (isRecording.get()) {
            if (isPaused.get()) {
                try {
                    Thread.sleep(10)
                } catch (_: InterruptedException) {
                    break
                }
                continue
            }

            // Drain Video
            var encoderStatus = vEncoder.dequeueOutputBuffer(bufferInfo, 5000)
            if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (isMuxerStarted) {
                    Log.e(TAG, "Video format changed twice")
                } else {
                    val newFormat = vEncoder.outputFormat
                    videoTrackIndex = muxer.addTrack(newFormat)
                    Log.i(TAG, "Muxer added video track #$videoTrackIndex: $newFormat")

                    // Start muxer if audio isn't expected or audio track is already added
                    if (audioEncoder == null || audioTrackIndex >= 0) {
                        muxer.start()
                        isMuxerStarted = true
                    }
                }
            } else if (encoderStatus >= 0) {
                val encodedData = vEncoder.getOutputBuffer(encoderStatus)
                if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                    if (bufferInfo.size > 0 && isMuxerStarted && videoTrackIndex >= 0) {
                        encodedData.position(bufferInfo.offset)
                        encodedData.limit(bufferInfo.offset + bufferInfo.size)
                        muxer.writeSampleData(videoTrackIndex, encodedData, bufferInfo)
                        totalBytesWritten += bufferInfo.size
                    }
                }
                vEncoder.releaseOutputBuffer(encoderStatus, false)
                if ((bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    break
                }
            }

            // Drain Audio
            drainAudio(muxer, bufferInfo)
        }
    }

    private fun drainAudio(muxer: MediaMuxer, bufferInfo: MediaCodec.BufferInfo) {
        val aEncoder = audioEncoder ?: return
        val aRecord = audioRecord ?: return

        // Feed AudioRecord into AudioEncoder
        val inputIndex = aEncoder.dequeueInputBuffer(0)
        if (inputIndex >= 0) {
            val inputBuffer = aEncoder.getInputBuffer(inputIndex)
            if (inputBuffer != null) {
                inputBuffer.clear()
                val readBytes = aRecord.read(inputBuffer, inputBuffer.capacity())
                if (readBytes > 0) {
                    val pts = (System.nanoTime() - recordingStartTimeNs) / 1000
                    aEncoder.queueInputBuffer(inputIndex, 0, readBytes, pts, 0)
                } else {
                    aEncoder.queueInputBuffer(inputIndex, 0, 0, 0, 0)
                }
            }
        }

        // Drain encoded audio
        var audioStatus = aEncoder.dequeueOutputBuffer(bufferInfo, 0)
        if (audioStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            if (!isMuxerStarted) {
                audioTrackIndex = muxer.addTrack(aEncoder.outputFormat)
                Log.i(TAG, "Muxer added audio track #$audioTrackIndex")
                if (videoTrackIndex >= 0) {
                    muxer.start()
                    isMuxerStarted = true
                }
            }
        } else if (audioStatus >= 0) {
            val encodedData = aEncoder.getOutputBuffer(audioStatus)
            if (encodedData != null && (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0) {
                if (bufferInfo.size > 0 && isMuxerStarted && audioTrackIndex >= 0) {
                    encodedData.position(bufferInfo.offset)
                    encodedData.limit(bufferInfo.offset + bufferInfo.size)
                    muxer.writeSampleData(audioTrackIndex, encodedData, bufferInfo)
                    totalBytesWritten += bufferInfo.size
                }
            }
            aEncoder.releaseOutputBuffer(audioStatus, false)
        }
    }

    fun pause() {
        isPaused.set(true)
    }

    fun resume() {
        isPaused.set(false)
    }

    fun stop(onFinalized: (MediaItem?) -> Unit) {
        if (!isRecording.getAndSet(false)) {
            onFinalized(null)
            return
        }

        val thread = drainThread
        drainHandler?.post {
            try {
                videoEncoder?.signalEndOfInputStream()
            } catch (e: Exception) {
                Log.w(TAG, "Signal EOS error: ${e.message}")
            }

            // Allow remaining frames to drain briefly
            try {
                Thread.sleep(100)
            } catch (_: InterruptedException) {}

            release()

            val file = outputFile
            if (file != null && file.exists() && file.length() > 0) {
                Log.i(TAG, "Publishing recorded GPU video: ${file.length()} bytes")
                val item = mediaStoreManager.insertVideoFile(file)
                file.delete()
                onFinalized(item)
            } else {
                Log.w(TAG, "Output file was empty or missing")
                onFinalized(null)
            }

            thread?.quitSafely()
        }
    }

    private fun release() {
        try {
            audioRecord?.stop()
            audioRecord?.release()
        } catch (_: Exception) {}
        audioRecord = null

        try {
            audioEncoder?.stop()
            audioEncoder?.release()
        } catch (_: Exception) {}
        audioEncoder = null

        try {
            videoEncoder?.stop()
            videoEncoder?.release()
        } catch (_: Exception) {}
        videoEncoder = null

        try {
            inputSurface?.release()
        } catch (_: Exception) {}
        inputSurface = null

        try {
            if (isMuxerStarted) {
                mediaMuxer?.stop()
            }
            mediaMuxer?.release()
        } catch (_: Exception) {}
        mediaMuxer = null
        isMuxerStarted = false
        videoTrackIndex = -1
        audioTrackIndex = -1
    }

    companion object {
        private const val TAG = "GpuVideoRecorder"
    }
}
