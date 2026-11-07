package com.bishal.adaptivecamera.ui.diagnostics

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepository
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepositoryImpl
import com.bishal.adaptivecamera.camera.capability.DeviceCapabilityReport
import com.bishal.adaptivecamera.core.diagnostics.CameraDiagnosticsLogger
import com.bishal.adaptivecamera.core.diagnostics.CameraHealthMetrics
import com.bishal.adaptivecamera.core.diagnostics.CameraLogEntry
import com.bishal.adaptivecamera.core.diagnostics.CameraLogSeverity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiagnosticsUiState(
    val isLoading: Boolean = false,
    val report: DeviceCapabilityReport? = null,
    val hasCameraPermission: Boolean = false,
    val isFromCache: Boolean = false,
    val toastMessage: String? = null,
    val dualCameraCapability: com.bishal.adaptivecamera.video.capability.DualCameraCapability? = null,
    val allLogs: List<CameraLogEntry> = emptyList(),
    val filteredLogs: List<CameraLogEntry> = emptyList(),
    val healthMetrics: CameraHealthMetrics? = null,
    val selectedSeverityFilter: CameraLogSeverity? = null,
    val selectedModuleFilter: String? = null,
    val searchQuery: String = "",
    val selectedTab: Int = 0 // 0: Camera Logs, 1: Hardware Specs
)

class DiagnosticsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CameraCapabilityRepository = CameraCapabilityRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {

    private val logger = CameraDiagnosticsLogger.getInstance(application)

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            logger.logsFlow.collect { rawLogs ->
                val metrics = logger.getHealthMetrics()
                _uiState.update { current ->
                    val filtered = applyFilters(
                        rawLogs,
                        current.selectedSeverityFilter,
                        current.selectedModuleFilter,
                        current.searchQuery
                    )
                    current.copy(
                        allLogs = rawLogs,
                        filteredLogs = filtered,
                        healthMetrics = metrics
                    )
                }
            }
        }
    }

    fun setSelectedTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    fun setSeverityFilter(severity: CameraLogSeverity?) {
        _uiState.update { current ->
            val newFilter = if (current.selectedSeverityFilter == severity) null else severity
            current.copy(
                selectedSeverityFilter = newFilter,
                filteredLogs = applyFilters(current.allLogs, newFilter, current.selectedModuleFilter, current.searchQuery)
            )
        }
    }

    fun setModuleFilter(module: String?) {
        _uiState.update { current ->
            val newModule = if (current.selectedModuleFilter == module) null else module
            current.copy(
                selectedModuleFilter = newModule,
                filteredLogs = applyFilters(current.allLogs, current.selectedSeverityFilter, newModule, current.searchQuery)
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { current ->
            current.copy(
                searchQuery = query,
                filteredLogs = applyFilters(current.allLogs, current.selectedSeverityFilter, current.selectedModuleFilter, query)
            )
        }
    }

    private fun applyFilters(
        logs: List<CameraLogEntry>,
        severity: CameraLogSeverity?,
        module: String?,
        query: String
    ): List<CameraLogEntry> {
        return logs.filter { entry ->
            val matchesSeverity = severity == null || entry.severity == severity
            val matchesModule = module == null || entry.module.equals(module, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    entry.message.contains(query, ignoreCase = true) ||
                    entry.module.contains(query, ignoreCase = true) ||
                    entry.eventType.contains(query, ignoreCase = true) ||
                    (entry.errorCode?.contains(query, ignoreCase = true) == true) ||
                    (entry.technicalDetails?.contains(query, ignoreCase = true) == true)
            matchesSeverity && matchesModule && matchesQuery
        }
    }

    fun clearLogs() {
        logger.clearLogs()
        _uiState.update { it.copy(toastMessage = "All camera diagnostic logs cleared") }
    }

    fun copyLogsToClipboard(context: Context) {
        val file = logger.exportAsText(context)
        val text = file.readText()
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Xamera_Logs.txt", text)
        clipboard.setPrimaryClip(clip)
        _uiState.update { it.copy(toastMessage = "Logs copied to clipboard (${_uiState.value.filteredLogs.size} entries)") }
    }

    fun exportAndShareLogs(context: Context, asJson: Boolean) {
        val file = if (asJson) logger.exportAsJson(context) else logger.exportAsText(context)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val mime = if (asJson) "application/json" else "text/plain"
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mime
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Xamera Camera Diagnostics Log")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Share Camera Diagnostics").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun updateCameraPermission(granted: Boolean, context: Context? = null) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
        loadHardwareReport(forceRefresh = false)
    }

    fun refreshHardwareScan(context: Context? = null) {
        loadHardwareReport(forceRefresh = true)
    }

    fun loadHardwareReport(forceRefresh: Boolean = false) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val report = repository.getDeviceReport(forceRefresh = forceRefresh)
                val dualCap = com.bishal.adaptivecamera.video.capability.DualCameraCapabilityDetector(getApplication()).detectCapability()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = report,
                        isFromCache = report.isFromCache,
                        dualCameraCapability = dualCap
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        toastMessage = "Hardware scan error: ${e.message}"
                    )
                }
            }
        }
    }

    fun copyToClipboard(context: Context, asJson: Boolean) {
        val currentReport = _uiState.value.report ?: return
        val content = if (asJson) currentReport.toJson() else currentReport.toFormattedText()
        val label = if (asJson) "AdaptiveCamera_Report.json" else "AdaptiveCamera_Report.txt"

        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, content)
        clipboard.setPrimaryClip(clip)

        _uiState.update {
            it.copy(toastMessage = if (asJson) "JSON Report copied to clipboard!" else "Text Report copied to clipboard!")
        }
    }

    fun shareReport(context: Context, asJson: Boolean) {
        val currentReport = _uiState.value.report ?: return
        val content = if (asJson) currentReport.toJson() else currentReport.toFormattedText()
        val title = if (asJson) "Xamera Diagnostics (JSON)" else "Xamera Diagnostics"

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = if (asJson) "application/json" else "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, content)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(intent, "Share Capability Report").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    fun dismissToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }

    fun clearToastMessage() {
        dismissToast()
    }
}
