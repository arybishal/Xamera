package com.bishal.adaptivecamera.video.capability

import android.content.Context
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log

/**
 * Diagnostic model describing device support for concurrent multi-camera streaming.
 */
data class DualCameraCapability(
    val isSupported: Boolean,
    val supportedCameraCombinations: List<List<String>> = emptyList(),
    val diagnosticReason: String,
    val hardwareConcurrencyLevel: String
)

/**
 * Detects device ISP and HAL capabilities for simultaneous front/rear camera streaming
 * via [CameraManager.getConcurrentCameraIds].
 */
class DualCameraCapabilityDetector(
    private val context: Context
) {
    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    fun detectCapability(): DualCameraCapability {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return DualCameraCapability(
                isSupported = false,
                diagnosticReason = "Concurrent camera streaming requires Android 11 (API 30) or higher.",
                hardwareConcurrencyLevel = "UNSUPPORTED_LEGACY_OS"
            )
        }

        try {
            val concurrentSets = cameraManager.concurrentCameraIds
            val combinations = concurrentSets.map { it.toList() }

            if (combinations.isNotEmpty()) {
                Log.i(TAG, "Concurrent cameras supported: $combinations")
                return DualCameraCapability(
                    isSupported = true,
                    supportedCameraCombinations = combinations,
                    diagnosticReason = "Hardware ISP supports ${combinations.size} concurrent streaming camera combination(s).",
                    hardwareConcurrencyLevel = "CONCURRENT_ISP_SUPPORTED"
                )
            } else {
                Log.i(TAG, "Device ISP HAL does not support concurrent streaming")
                return DualCameraCapability(
                    isSupported = false,
                    supportedCameraCombinations = emptyList(),
                    diagnosticReason = "Hardware Incompatible: Device ISP HAL does not expose concurrent front and rear camera sessions. The SoC vendor restricts hardware pipelines to single active camera stream.",
                    hardwareConcurrencyLevel = "SINGLE_STREAM_ONLY"
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying concurrent camera IDs: ${e.message}")
            return DualCameraCapability(
                isSupported = false,
                diagnosticReason = "Error querying hardware concurrency: ${e.message}",
                hardwareConcurrencyLevel = "QUERY_ERROR"
            )
        }
    }

    companion object {
        private const val TAG = "DualCameraDetector"
    }
}
