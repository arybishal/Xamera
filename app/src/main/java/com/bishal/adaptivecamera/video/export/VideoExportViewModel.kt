package com.bishal.adaptivecamera.video.export

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.core.storage.MediaStoreManager
import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI State for the Video Export Sheet and processing workflow.
 */
data class VideoExportUiState(
    val isLoadingMetadata: Boolean = true,
    val sourceMetadata: SourceVideoMetadata? = null,
    val config: VideoExportConfig = VideoExportConfig(),
    val supportedCodecs: List<VideoCodec> = listOf(VideoCodec.H264_AVC),
    val estimatedSizeBytes: Long = 0L,
    val exportState: VideoExportState = VideoExportState.Idle
) {
    val estimatedSizeFormatted: String
        get() = VideoExportEstimator.formatFileSize(estimatedSizeBytes)

    val sourceSizeFormatted: String
        get() = sourceMetadata?.let { VideoExportEstimator.formatFileSize(it.fileSizeBytes) } ?: "0 B"

    val durationFormatted: String
        get() = VideoExportEstimator.formatDuration(config.effectiveDurationMs)
}

/**
 * ViewModel orchestrating video export parameters, estimation, and execution.
 */
class VideoExportViewModel @JvmOverloads constructor(
    application: Application,
    private val exportEngine: VideoExportEngine = VideoExportEngine(application, MediaStoreManager(application)),
    private val codecDetector: VideoCodecCapabilityDetector = VideoCodecCapabilityDetector()
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(VideoExportUiState())
    val uiState: StateFlow<VideoExportUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            exportEngine.exportState.collect { state ->
                _uiState.update { it.copy(exportState = state) }
            }
        }
    }

    /**
     * Initializes export session with a source video URI and file size.
     */
    fun loadSourceVideo(uri: Uri, fileSizeBytes: Long = 0L) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMetadata = true) }

            val metadata = VideoMetadataExtractor.extract(getApplication(), uri, fileSizeBytes)
            val supported = codecDetector.getSupportedCodecs().ifEmpty { listOf(VideoCodec.H264_AVC) }

            val initialConfig = VideoExportConfig(
                sourceUri = uri,
                sourcePath = uri.path,
                sourceDurationMs = metadata.durationMs,
                sourceSizeBytes = fileSizeBytes,
                trimStartMs = 0L,
                trimEndMs = metadata.durationMs,
                resolution = metadata.closestResolution,
                frameRate = metadata.closestFrameRate,
                codec = if (supported.contains(metadata.detectedCodec)) metadata.detectedCodec else VideoCodec.H264_AVC,
                bitrateProfile = VideoBitrateProfile.STANDARD,
                rotationDegrees = metadata.rotationDegrees,
                audioAction = if (metadata.hasAudio) VideoExportAudioAction.KEEP else VideoExportAudioAction.MUTE,
                preset = SocialExportPreset.CUSTOM
            )

            val estimatedSize = VideoExportEstimator.estimateExportSizeBytes(initialConfig)

            _uiState.update {
                it.copy(
                    isLoadingMetadata = false,
                    sourceMetadata = metadata,
                    config = initialConfig,
                    supportedCodecs = supported,
                    estimatedSizeBytes = estimatedSize,
                    exportState = VideoExportState.Idle
                )
            }
        }
    }

    /**
     * Selects a social media export preset.
     */
    fun selectPreset(preset: SocialExportPreset) {
        val current = _uiState.value.config
        val meta = _uiState.value.sourceMetadata

        val newResolution = if (preset == SocialExportPreset.ORIGINAL && meta != null) {
            meta.closestResolution
        } else {
            preset.targetResolution
        }

        val newFrameRate = if (preset == SocialExportPreset.ORIGINAL && meta != null) {
            meta.closestFrameRate
        } else {
            preset.targetFrameRate
        }

        val updated = current.copy(
            preset = preset,
            resolution = newResolution,
            frameRate = newFrameRate,
            bitrateProfile = preset.bitrateProfile,
            codec = if (_uiState.value.supportedCodecs.contains(preset.preferredCodec)) preset.preferredCodec else VideoCodec.H264_AVC,
            customBitrateMbps = null
        )

        updateConfigAndEstimate(updated)
    }

    /**
     * Updates trimming boundaries in milliseconds.
     */
    fun updateTrim(startMs: Long, endMs: Long) {
        val current = _uiState.value.config
        val validStart = startMs.coerceAtLeast(0L)
        val validEnd = if (endMs > 0L) endMs.coerceAtLeast(validStart + 100L) else current.sourceDurationMs

        val updated = current.copy(
            trimStartMs = validStart,
            trimEndMs = validEnd
        )
        updateConfigAndEstimate(updated)
    }

    /**
     * Updates target output resolution.
     */
    fun setResolution(resolution: VideoResolution) {
        val updated = _uiState.value.config.copy(
            resolution = resolution,
            preset = SocialExportPreset.CUSTOM
        )
        updateConfigAndEstimate(updated)
    }

    /**
     * Updates target frame rate.
     */
    fun setFrameRate(frameRate: VideoFrameRate) {
        val updated = _uiState.value.config.copy(
            frameRate = frameRate,
            preset = SocialExportPreset.CUSTOM
        )
        updateConfigAndEstimate(updated)
    }

    /**
     * Updates video codec.
     */
    fun setCodec(codec: VideoCodec) {
        val updated = _uiState.value.config.copy(
            codec = codec,
            preset = SocialExportPreset.CUSTOM
        )
        updateConfigAndEstimate(updated)
    }

    /**
     * Updates bitrate profile.
     */
    fun setBitrateProfile(profile: VideoBitrateProfile) {
        val updated = _uiState.value.config.copy(
            bitrateProfile = profile,
            customBitrateMbps = null,
            preset = SocialExportPreset.CUSTOM
        )
        updateConfigAndEstimate(updated)
    }

    /**
     * Updates custom bitrate in Mbps.
     */
    fun setCustomBitrate(mbps: Float) {
        val updated = _uiState.value.config.copy(
            customBitrateMbps = mbps,
            preset = SocialExportPreset.CUSTOM
        )
        updateConfigAndEstimate(updated)
    }

    /**
     * Updates audio action (Keep, Mute, Extract).
     */
    fun setAudioAction(action: VideoExportAudioAction) {
        val updated = _uiState.value.config.copy(audioAction = action)
        updateConfigAndEstimate(updated)
    }

    /**
     * Begins asynchronous video export.
     */
    fun startExport() {
        val config = _uiState.value.config
        if (config.sourceUri == null) return

        viewModelScope.launch {
            exportEngine.exportVideo(config)
        }
    }

    /**
     * Cancels active export.
     */
    fun cancelExport() {
        exportEngine.cancelExport()
    }

    private fun updateConfigAndEstimate(newConfig: VideoExportConfig) {
        val estimatedSize = VideoExportEstimator.estimateExportSizeBytes(newConfig)
        _uiState.update {
            it.copy(
                config = newConfig,
                estimatedSizeBytes = estimatedSize
            )
        }
    }
}
