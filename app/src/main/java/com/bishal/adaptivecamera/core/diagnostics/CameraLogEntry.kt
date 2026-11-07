package com.bishal.adaptivecamera.core.diagnostics

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class CameraLogSeverity {
    DEBUG,
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

data class CameraLogEntry(
    val id: String = UUID.randomUUID().toString(),
    val timestamp: Long = System.currentTimeMillis(),
    val severity: CameraLogSeverity,
    val module: String,
    val eventType: String,
    val message: String,
    val errorCode: String? = null,
    val technicalDetails: String? = null,
    val cameraId: String? = null,
    val lens: String? = null,
    val captureMode: String? = null,
    val resolution: String? = null,
    val fps: Int? = null
) {
    val formattedTimestamp: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US).format(Date(timestamp))

    fun toFormattedLogLine(): String {
        val sb = StringBuilder()
        sb.append("[$formattedTimestamp] [${severity.name}] [$module/$eventType]")
        if (!errorCode.isNullOrBlank()) sb.append(" [Code: $errorCode]")
        if (!cameraId.isNullOrBlank() || !lens.isNullOrBlank()) {
            sb.append(" [Cam: ${cameraId ?: "-"}/${lens ?: "-"}]")
        }
        if (!captureMode.isNullOrBlank()) sb.append(" [Mode: $captureMode]")
        if (!resolution.isNullOrBlank()) sb.append(" [Res: $resolution]")
        if (fps != null && fps > 0) sb.append(" [FPS: $fps]")
        sb.append(" - $message")
        if (!technicalDetails.isNullOrBlank()) {
            sb.append("\n  Details: $technicalDetails")
        }
        return sb.toString()
    }
}
