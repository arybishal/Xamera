package com.bishal.adaptivecamera.core.diagnostics

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.io.StringWriter
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.atomic.AtomicInteger

data class CameraHealthMetrics(
    val totalLogs: Int,
    val debugCount: Int,
    val infoCount: Int,
    val warningCount: Int,
    val errorCount: Int,
    val criticalCount: Int,
    val lastErrorMessage: String?,
    val lastErrorTimestamp: Long?,
    val healthStatus: HealthStatus
) {
    enum class HealthStatus {
        HEALTHY,
        DEGRADED,
        CRITICAL
    }
}

class CameraDiagnosticsLogger private constructor(
    private val appContext: Context
) {
    companion object {
        private const val TAG = "CameraDiagnostics"
        private const val MAX_LOGS = 2000
        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L // 5 MB
        private const val RETENTION_PERIOD_MS = 7 * 24 * 60 * 60 * 1000L // 7 days

        @Volatile
        private var INSTANCE: CameraDiagnosticsLogger? = null

        fun getInstance(context: Context): CameraDiagnosticsLogger {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CameraDiagnosticsLogger(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val memoryBuffer = ConcurrentLinkedDeque<CameraLogEntry>()
    private val entryCount = AtomicInteger(0)

    private val _logsFlow = MutableStateFlow<List<CameraLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<CameraLogEntry>> = _logsFlow.asStateFlow()

    private val logDir: File by lazy {
        File(appContext.cacheDir, "camera_diagnostics").apply {
            if (!exists()) mkdirs()
        }
    }

    private val currentLogFile: File by lazy {
        File(logDir, "camera_events.log")
    }

    init {
        // Prune old logs on initialization
        scope.launch {
            cleanUpOldLogs()
        }
    }

    fun log(
        severity: CameraLogSeverity,
        module: String,
        eventType: String,
        message: String,
        errorCode: String? = null,
        throwable: Throwable? = null,
        technicalDetails: String? = null,
        cameraId: String? = null,
        lens: String? = null,
        captureMode: String? = null,
        resolution: String? = null,
        fps: Int? = null
    ) {
        val details = when {
            throwable != null && technicalDetails != null -> {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                "$technicalDetails\nException: $sw"
            }
            throwable != null -> {
                val sw = StringWriter()
                throwable.printStackTrace(PrintWriter(sw))
                "Exception: $sw"
            }
            else -> technicalDetails
        }

        val entry = CameraLogEntry(
            severity = severity,
            module = module,
            eventType = eventType,
            message = message,
            errorCode = errorCode,
            technicalDetails = details,
            cameraId = cameraId,
            lens = lens,
            captureMode = captureMode,
            resolution = resolution,
            fps = fps
        )

        // Native Android Log output
        when (severity) {
            CameraLogSeverity.DEBUG -> Log.d(TAG, entry.toFormattedLogLine())
            CameraLogSeverity.INFO -> Log.i(TAG, entry.toFormattedLogLine())
            CameraLogSeverity.WARNING -> Log.w(TAG, entry.toFormattedLogLine())
            CameraLogSeverity.ERROR -> Log.e(TAG, entry.toFormattedLogLine())
            CameraLogSeverity.CRITICAL -> Log.wtf(TAG, entry.toFormattedLogLine())
        }

        // Circular memory buffer
        memoryBuffer.addFirst(entry)
        while (memoryBuffer.size > MAX_LOGS) {
            memoryBuffer.removeLast()
        }
        _logsFlow.value = memoryBuffer.toList()

        // Append to file asynchronously
        scope.launch {
            appendToFile(entry)
        }
    }

    fun d(module: String, eventType: String, message: String, cameraId: String? = null, mode: String? = null) {
        log(CameraLogSeverity.DEBUG, module, eventType, message, cameraId = cameraId, captureMode = mode)
    }

    fun i(module: String, eventType: String, message: String, cameraId: String? = null, mode: String? = null) {
        log(CameraLogSeverity.INFO, module, eventType, message, cameraId = cameraId, captureMode = mode)
    }

    fun w(module: String, eventType: String, message: String, errorCode: String? = null, details: String? = null) {
        log(CameraLogSeverity.WARNING, module, eventType, message, errorCode = errorCode, technicalDetails = details)
    }

    fun e(module: String, eventType: String, message: String, throwable: Throwable? = null, errorCode: String? = null) {
        log(CameraLogSeverity.ERROR, module, eventType, message, errorCode = errorCode, throwable = throwable)
    }

    fun critical(module: String, eventType: String, message: String, throwable: Throwable? = null, errorCode: String? = null) {
        log(CameraLogSeverity.CRITICAL, module, eventType, message, errorCode = errorCode, throwable = throwable)
    }

    @Synchronized
    private fun appendToFile(entry: CameraLogEntry) {
        try {
            if (currentLogFile.length() > MAX_FILE_SIZE_BYTES) {
                // Rotate log
                val backupFile = File(logDir, "camera_events_old.log")
                if (backupFile.exists()) backupFile.delete()
                currentLogFile.renameTo(backupFile)
            }
            FileWriter(currentLogFile, true).use { writer ->
                writer.append(entry.toFormattedLogLine())
                writer.append("\n")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write log to file: ${e.message}")
        }
    }

    private fun cleanUpOldLogs() {
        try {
            val cutoff = System.currentTimeMillis() - RETENTION_PERIOD_MS
            val files = logDir.listFiles() ?: return
            for (file in files) {
                if (file.lastModified() < cutoff) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed log cleanup: ${e.message}")
        }
    }

    fun clearLogs() {
        memoryBuffer.clear()
        _logsFlow.value = emptyList()
        scope.launch {
            try {
                if (currentLogFile.exists()) currentLogFile.delete()
                val backupFile = File(logDir, "camera_events_old.log")
                if (backupFile.exists()) backupFile.delete()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear log files: ${e.message}")
            }
        }
    }

    fun getHealthMetrics(): CameraHealthMetrics {
        val logs = memoryBuffer.toList()
        var debugs = 0
        var infos = 0
        var warnings = 0
        var errors = 0
        var criticals = 0
        var lastErrorMsg: String? = null
        var lastErrorTime: Long? = null

        for (entry in logs) {
            when (entry.severity) {
                CameraLogSeverity.DEBUG -> debugs++
                CameraLogSeverity.INFO -> infos++
                CameraLogSeverity.WARNING -> warnings++
                CameraLogSeverity.ERROR -> {
                    errors++
                    if (lastErrorMsg == null) {
                        lastErrorMsg = "[${entry.module}] ${entry.message}"
                        lastErrorTime = entry.timestamp
                    }
                }
                CameraLogSeverity.CRITICAL -> {
                    criticals++
                    if (lastErrorMsg == null) {
                        lastErrorMsg = "[CRITICAL] [${entry.module}] ${entry.message}"
                        lastErrorTime = entry.timestamp
                    }
                }
            }
        }

        val status = when {
            criticals > 0 || errors >= 5 -> CameraHealthMetrics.HealthStatus.CRITICAL
            errors > 0 || warnings >= 10 -> CameraHealthMetrics.HealthStatus.DEGRADED
            else -> CameraHealthMetrics.HealthStatus.HEALTHY
        }

        return CameraHealthMetrics(
            totalLogs = logs.size,
            debugCount = debugs,
            infoCount = infos,
            warningCount = warnings,
            errorCount = errors,
            criticalCount = criticals,
            lastErrorMessage = lastErrorMsg,
            lastErrorTimestamp = lastErrorTime,
            healthStatus = status
        )
    }

    fun exportAsText(context: Context): File {
        val exportFile = File(context.cacheDir, "xamera_diagnostics_export.txt")
        FileWriter(exportFile, false).use { writer ->
            writer.write("===================================================\n")
            writer.write("           XAMERA CAMERA DIAGNOSTICS LOG           \n")
            writer.write("===================================================\n")
            writer.write("Exported: ${java.util.Date()}\n")
            val metrics = getHealthMetrics()
            writer.write("Health Status: ${metrics.healthStatus.name}\n")
            writer.write("Total Logs: ${metrics.totalLogs} | Errors: ${metrics.errorCount} | Critical: ${metrics.criticalCount} | Warnings: ${metrics.warningCount}\n")
            writer.write("---------------------------------------------------\n\n")

            memoryBuffer.toList().reversed().forEach { entry ->
                writer.write(entry.toFormattedLogLine())
                writer.write("\n")
            }
        }
        return exportFile
    }

    fun exportAsJson(context: Context): File {
        val exportFile = File(context.cacheDir, "xamera_diagnostics_export.json")
        val json = JSONObject()
        json.put("app", "Xamera")
        json.put("exportedAt", System.currentTimeMillis())

        val metrics = getHealthMetrics()
        val metricsJson = JSONObject().apply {
            put("status", metrics.healthStatus.name)
            put("totalLogs", metrics.totalLogs)
            put("errors", metrics.errorCount)
            put("warnings", metrics.warningCount)
            put("criticals", metrics.criticalCount)
            put("lastError", metrics.lastErrorMessage ?: "None")
        }
        json.put("metrics", metricsJson)

        val logsArray = JSONArray()
        memoryBuffer.toList().reversed().forEach { entry ->
            val obj = JSONObject().apply {
                put("id", entry.id)
                put("timestamp", entry.timestamp)
                put("formattedTime", entry.formattedTimestamp)
                put("severity", entry.severity.name)
                put("module", entry.module)
                put("eventType", entry.eventType)
                put("message", entry.message)
                put("errorCode", entry.errorCode)
                put("technicalDetails", entry.technicalDetails)
                put("cameraId", entry.cameraId)
                put("lens", entry.lens)
                put("captureMode", entry.captureMode)
                put("resolution", entry.resolution)
                put("fps", entry.fps)
            }
            logsArray.put(obj)
        }
        json.put("logs", logsArray)

        FileWriter(exportFile, false).use { writer ->
            writer.write(json.toString(2))
        }
        return exportFile
    }
}
