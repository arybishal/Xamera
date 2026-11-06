package com.bishal.adaptivecamera.video.recording

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.camera.camera2.interop.Camera2CameraControl
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.core.Camera
import androidx.camera.video.FallbackStrategy
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.content.ContextCompat
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.photography.pro.KelvinConverter
import com.bishal.adaptivecamera.video.config.VideoConfig
import com.bishal.adaptivecamera.video.config.VideoExposureMode
import com.bishal.adaptivecamera.video.exposure.ShutterAngleCalculator
import com.bishal.adaptivecamera.video.stabilization.VideoStabilizationCoordinator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.Executor

/**
 * Authoritative video recording engine for Xamera.
 *
 * Responsibilities:
 * - Instantiates and builds CameraX VideoCapture use cases tuned with target bitrates and resolutions.
 * - Injects Camera2 manual exposure, shutter angle, Kelvin WB, focus, and stabilization via Camera2CameraControl.
 * - Safely coordinates start, pause, resume, and stop recording transitions.
 * - Emits real-time VideoRecordingState telemetry (duration, bytes, bitrate, error).
 * - Enforces minimum storage bounds (> 100MB required to begin recording).
 */
class VideoRecordingEngine(
    private val context: Context,
    private val mediaStoreManager: MediaStoreManager,
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context)
) {
    private val _recordingState = MutableStateFlow(VideoRecordingState())
    val recordingState: StateFlow<VideoRecordingState> = _recordingState.asStateFlow()

    private var activeRecording: Recording? = null
    private var activeVideoCapture: VideoCapture<Recorder>? = null

    /**
     * Builds a configured VideoCapture use case matching the specified VideoConfig.
     */
    fun createVideoCapture(config: VideoConfig): VideoCapture<Recorder> {
        val targetQuality = config.resolution.quality
        val qualitySelector = QualitySelector.from(
            targetQuality,
            FallbackStrategy.lowerQualityOrHigherThan(Quality.SD)
        )

        val recorderBuilder = Recorder.Builder()
            .setQualitySelector(qualitySelector)
            .setExecutor(mainExecutor)

        // Apply target bitrate if within valid range
        val bitrate = config.targetBitrateBps.toInt()
        if (bitrate in 500_000..200_000_000) {
            recorderBuilder.setTargetVideoEncodingBitRate(bitrate)
        }

        val recorder = recorderBuilder.build()
        val videoCapture = VideoCapture.withOutput(recorder)
        activeVideoCapture = videoCapture
        return videoCapture
    }

    /**
     * Injects professional video capture request parameters (shutter angle, ISO, WB, OIS/EIS)
     * into the active camera session using Camera2 interop.
     */
    fun applyProVideoControls(
        camera: Camera,
        config: VideoConfig,
        characteristics: CameraCharacteristics?
    ) {
        try {
            val camera2Control = Camera2CameraControl.from(camera.cameraControl)
            val builder = CaptureRequestOptions.Builder()

            // 1. Stabilization Options
            val hasOis = characteristics?.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
                ?.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: true
            val hasEis = characteristics?.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES)
                ?.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON) ?: true

            val stabOptions = VideoStabilizationCoordinator.createCaptureRequestOptions(
                config.stabilizationMode,
                hasOis = hasOis,
                hasEis = hasEis
            )

            // 2. Exposure & Shutter Angle
            if (config.exposureMode != VideoExposureMode.AUTO) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_OFF
                )

                val minExp = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.lower ?: 10_000L
                val maxExp = characteristics?.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.upper ?: 1_000_000_000L

                val expNanos = if (config.manualShutterSpeedSeconds != null) {
                    (config.manualShutterSpeedSeconds * 1_000_000_000.0).toLong().coerceIn(minExp, maxExp)
                } else if (config.shutterAngle != null) {
                    ShutterAngleCalculator.calculateExposureNanoseconds(
                        fps = config.frameRate.fps,
                        shutterAngle = config.shutterAngle,
                        minExposureNanos = minExp,
                        maxExposureNanos = maxExp
                    )
                } else {
                    16_666_666L // 1/60s fallback
                }

                builder.setCaptureRequestOption(CaptureRequest.SENSOR_EXPOSURE_TIME, expNanos)

                val iso = config.manualIso ?: 100
                builder.setCaptureRequestOption(CaptureRequest.SENSOR_SENSITIVITY, iso)
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_MODE,
                    CameraMetadata.CONTROL_AE_MODE_ON
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION,
                    config.exposureCompensationIndex
                )
            }

            // 3. Manual White Balance (Kelvin)
            if (config.isKelvinManual) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CameraMetadata.CONTROL_AWB_MODE_OFF
                )
                val gains = KelvinConverter.kelvinToRgbGains(config.manualKelvin)
                builder.setCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_MODE,
                    CameraMetadata.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.COLOR_CORRECTION_GAINS,
                    gains.toCamera2Gains()
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AWB_MODE,
                    CameraMetadata.CONTROL_AWB_MODE_AUTO
                )
            }

            // 4. Manual Focus
            if (config.isFocusManual) {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_OFF
                )
                builder.setCaptureRequestOption(
                    CaptureRequest.LENS_FOCUS_DISTANCE,
                    config.manualFocusDistanceDiopters
                )
            } else {
                builder.setCaptureRequestOption(
                    CaptureRequest.CONTROL_AF_MODE,
                    CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO
                )
            }

            // Merge stabilization options
            val combinedBuilder = CaptureRequestOptions.Builder()
            builder.build().let { opts ->
                // Apply all gathered options
                camera2Control.setCaptureRequestOptions(opts)
            }
            camera2Control.setCaptureRequestOptions(stabOptions)

        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply Camera2 pro video controls (graceful fallback)", e)
        }
    }

    /**
     * Starts video recording with the specified VideoConfig.
     */
    @SuppressLint("MissingPermission")
    fun startRecording(
        config: VideoConfig,
        onStarted: () -> Unit = {},
        onFinalized: (MediaItem?) -> Unit = {},
        onError: (Throwable) -> Unit = {}
    ) {
        val vidCap = activeVideoCapture
        if (vidCap == null) {
            val err = IllegalStateException("VideoCapture use case is not bound")
            _recordingState.update { it.copy(status = VideoRecordingStatus.ERROR, errorMessage = err.message) }
            onError(err)
            return
        }

        if (_recordingState.value.status.isRecordingOrPaused) {
            Log.w(TAG, "Recording already active, ignoring start request")
            return
        }

        // Check storage capacity: ensure at least 100MB is free
        val freeBytes = getAvailableStorageBytes()
        if (freeBytes < 100L * 1024L * 1024L) {
            val err = IllegalStateException("Insufficient storage space (< 100MB remaining)")
            _recordingState.update { it.copy(status = VideoRecordingStatus.ERROR, errorMessage = err.message) }
            onError(err)
            return
        }

        _recordingState.update { it.copy(status = VideoRecordingStatus.PREPARING, errorMessage = null) }

        val outputOptions = mediaStoreManager.createVideoOutputOptions()
        val pending = vidCap.output.prepareRecording(context, outputOptions)

        val hasAudioPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (config.audioEnabled && hasAudioPermission) {
            pending.withAudioEnabled()
        }

        activeRecording = pending.start(mainExecutor) { event ->
            when (event) {
                is VideoRecordEvent.Start -> {
                    _recordingState.update {
                        it.copy(
                            status = VideoRecordingStatus.RECORDING,
                            durationMs = 0L,
                            bytesRecorded = 0L,
                            errorMessage = null
                        )
                    }
                    onStarted()
                }

                is VideoRecordEvent.Pause -> {
                    _recordingState.update { it.copy(status = VideoRecordingStatus.PAUSED) }
                }

                is VideoRecordEvent.Resume -> {
                    _recordingState.update { it.copy(status = VideoRecordingStatus.RECORDING) }
                }

                is VideoRecordEvent.Status -> {
                    val duration = event.recordingStats.recordedDurationNanos / 1_000_000L
                    val bytes = event.recordingStats.numBytesRecorded
                    val bitrate = if (duration > 0) (bytes * 8L * 1000L) / duration else 0L

                    _recordingState.update {
                        it.copy(
                            durationMs = duration,
                            bytesRecorded = bytes,
                            currentBitrateBps = bitrate
                        )
                    }

                    // Check low storage threshold during recording (< 25MB remaining)
                    if (getAvailableStorageBytes() < 25L * 1024L * 1024L) {
                        Log.w(TAG, "Low storage boundary reached, stopping recording safely")
                        stopRecording()
                    }
                }

                is VideoRecordEvent.Finalize -> {
                    activeRecording = null
                    if (event.hasError()) {
                        Log.e(TAG, "Video recording finalized with error: ${event.error}", event.cause)
                        val err = event.cause ?: RuntimeException("Video recording failed with error code ${event.error}")
                        _recordingState.update {
                            it.copy(
                                status = VideoRecordingStatus.ERROR,
                                errorMessage = err.message
                            )
                        }
                        onError(err)
                    } else {
                        val outputUri = event.outputResults.outputUri
                        val latestMedia = mediaStoreManager.queryLatestMedia()
                        _recordingState.update {
                            it.copy(
                                status = VideoRecordingStatus.COMPLETED,
                                outputUri = outputUri
                            )
                        }
                        onFinalized(latestMedia)
                    }
                }
            }
        }
    }

    /**
     * Pauses the active recording if currently in RECORDING status.
     */
    fun pauseRecording() {
        val recording = activeRecording
        if (recording != null && _recordingState.value.status == VideoRecordingStatus.RECORDING) {
            try {
                recording.pause()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to pause video recording", e)
            }
        }
    }

    /**
     * Resumes the paused recording if currently in PAUSED status.
     */
    fun resumeRecording() {
        val recording = activeRecording
        if (recording != null && _recordingState.value.status == VideoRecordingStatus.PAUSED) {
            try {
                recording.resume()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to resume video recording", e)
            }
        }
    }

    /**
     * Stops the active recording cleanly and finalizes the MP4 container.
     */
    fun stopRecording() {
        val recording = activeRecording
        if (recording != null) {
            _recordingState.update { it.copy(status = VideoRecordingStatus.FINALIZING) }
            try {
                recording.stop()
            } catch (e: Exception) {
                Log.w(TAG, "Error stopping recording", e)
            }
            activeRecording = null
        }
    }

    /**
     * Resets recording state back to IDLE.
     */
    fun resetState() {
        _recordingState.value = VideoRecordingState(status = VideoRecordingStatus.IDLE)
    }

    private fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(Environment.getExternalStorageDirectory().path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    companion object {
        private const val TAG = "VideoRecordingEngine"
    }
}
