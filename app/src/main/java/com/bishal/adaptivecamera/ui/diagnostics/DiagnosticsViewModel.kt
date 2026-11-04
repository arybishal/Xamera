package com.bishal.adaptivecamera.ui.diagnostics

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepository
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityRepositoryImpl
import com.bishal.adaptivecamera.camera.capability.DeviceCapabilityReport
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
    val toastMessage: String? = null
)

class DiagnosticsViewModel @JvmOverloads constructor(
    application: Application,
    private val repository: CameraCapabilityRepository = CameraCapabilityRepositoryImpl(application.applicationContext)
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

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
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        report = report,
                        isFromCache = report.isFromCache
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
        val title = if (asJson) "Adaptive Camera Diagnostics (JSON)" else "Adaptive Camera Diagnostics"

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

    fun clearToastMessage() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
