package com.bishal.adaptivecamera.ui.diagnostics

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bishal.adaptivecamera.camera.capability.CameraCapabilityScanner
import com.bishal.adaptivecamera.camera.capability.DeviceCapabilityReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class DiagnosticsUiState(
    val isLoading: Boolean = false,
    val report: DeviceCapabilityReport? = null,
    val hasCameraPermission: Boolean = false,
    val toastMessage: String? = null
)

class DiagnosticsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DiagnosticsUiState())
    val uiState: StateFlow<DiagnosticsUiState> = _uiState.asStateFlow()

    fun updateCameraPermission(granted: Boolean, context: Context) {
        _uiState.update { it.copy(hasCameraPermission = granted) }
        refreshHardwareScan(context)
    }

    fun refreshHardwareScan(context: Context) {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val report = withContext(Dispatchers.Default) {
                CameraCapabilityScanner(context).scanDevice()
            }
            _uiState.update {
                it.copy(
                    isLoading = false,
                    report = report
                )
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
