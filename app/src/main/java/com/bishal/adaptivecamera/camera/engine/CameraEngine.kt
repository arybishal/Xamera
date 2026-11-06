package com.bishal.adaptivecamera.camera.engine

import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraFacing
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.camera.session.CameraSessionState
import com.bishal.adaptivecamera.core.storage.MediaItem
import kotlinx.coroutines.flow.StateFlow

interface CameraEngine {
    val sessionState: StateFlow<CameraSessionState>
    val configuration: StateFlow<CameraConfiguration>
    val recordingDurationSec: StateFlow<Long>

    fun bindCamera(
        lifecycleOwner: LifecycleOwner,
        previewView: PreviewView,
        surfaceProvider: androidx.camera.core.Preview.SurfaceProvider? = null
    )
    fun unbindCamera()

    fun switchFacing()
    fun setFacing(facing: CameraFacing)
    fun setMode(mode: CameraMode)
    fun setFlashMode(flashMode: FlashMode)
    fun setTimer(timer: PhotoTimer)
    fun setAspectRatio(aspectRatio: com.bishal.adaptivecamera.camera.config.AspectRatioConfig) {}
    fun setZslEnabled(enabled: Boolean) {}
    fun setAebBracketCount(count: Int) {}
    fun setAebEvStep(step: Float) {}
    fun setCaptureFormat(format: com.bishal.adaptivecamera.photography.capture.CaptureFormat) {}

    fun setZoomRatio(ratio: Float)
    fun setLinearZoom(percentage: Float)

    fun setExposureCompensation(index: Int)
    fun resetExposureCompensation()

    fun focusAndMeter(xNorm: Float, yNorm: Float)

    fun setImageAnalyzer(analyzer: androidx.camera.core.ImageAnalysis.Analyzer?)

    fun capturePhoto(
        onSuccess: (MediaItem) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun startVideoRecording(
        withAudio: Boolean,
        onStarted: () -> Unit,
        onFinalized: (MediaItem?) -> Unit,
        onError: (Throwable) -> Unit
    )

    fun stopVideoRecording()
    fun pauseVideoRecording() {}
    fun resumeVideoRecording() {}
    fun updateVideoConfig(config: com.bishal.adaptivecamera.video.config.VideoConfig) {}
}
