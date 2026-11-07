package com.bishal.adaptivecamera.ui.diagnostics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.core.diagnostics.CameraHealthMetrics
import com.bishal.adaptivecamera.core.diagnostics.CameraLogEntry
import com.bishal.adaptivecamera.core.diagnostics.CameraLogSeverity
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import androidx.compose.ui.graphics.SolidColor
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.MinimalistGold
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraLogsSection(
    uiState: DiagnosticsUiState,
    viewModel: DiagnosticsViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize()) {
        // 1. System Health Overview Card
        uiState.healthMetrics?.let { metrics ->
            HealthOverviewCard(
                metrics = metrics,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }

        // 2. Action Toolbar (Share, Copy, Export, Clear)
        ActionButtonsRow(
            onShare = { viewModel.exportAndShareLogs(context, asJson = false) },
            onShareJson = { viewModel.exportAndShareLogs(context, asJson = true) },
            onCopy = { viewModel.copyLogsToClipboard(context) },
            onClear = { showClearDialog = true },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // 3. Search and Filters
        SearchAndFiltersRow(
            searchQuery = uiState.searchQuery,
            onSearchQueryChange = { viewModel.setSearchQuery(it) },
            selectedSeverity = uiState.selectedSeverityFilter,
            onSeveritySelect = { viewModel.setSeverityFilter(it) },
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
        )

        // 4. Log Items List
        if (uiState.filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = SuccessGreen,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (uiState.allLogs.isEmpty()) "No camera events recorded yet." else "No events match active filter.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "EVENT LOGS (${uiState.filteredLogs.size} of ${uiState.allLogs.size})",
                        color = MinimalistGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(uiState.filteredLogs, key = { it.id }) { entry ->
                    LogEntryCard(entry = entry)
                }
                item {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(text = "Clear All Logs?", color = TextPrimary) },
            text = {
                Text(
                    text = "This will permanently delete all in-memory and persisted camera diagnostic logs.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearLogs()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text(text = "Clear", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(text = "Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun HealthOverviewCard(
    metrics: CameraHealthMetrics,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CAMERA SUBSYSTEM HEALTH",
                    color = MinimalistGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.8.sp
                )
                val statusColor = when (metrics.healthStatus) {
                    CameraHealthMetrics.HealthStatus.HEALTHY -> SuccessGreen
                    CameraHealthMetrics.HealthStatus.DEGRADED -> AccentAmber
                    CameraHealthMetrics.HealthStatus.CRITICAL -> ErrorRed
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(statusColor.copy(alpha = 0.15f))
                        .border(1.dp, statusColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = metrics.healthStatus.name,
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MetricItem("Total", metrics.totalLogs.toString(), TextPrimary)
                MetricItem("Errors", metrics.errorCount.toString(), if (metrics.errorCount > 0) ErrorRed else TextSecondary)
                MetricItem("Warnings", metrics.warningCount.toString(), if (metrics.warningCount > 0) AccentAmber else TextSecondary)
                MetricItem("Critical", metrics.criticalCount.toString(), if (metrics.criticalCount > 0) ErrorRed else TextSecondary)
            }

            metrics.lastErrorMessage?.let { lastErr ->
                Spacer(modifier = Modifier.height(10.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(ErrorRed.copy(alpha = 0.1f))
                        .border(0.5.dp, ErrorRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                        .padding(8.dp)
                ) {
                    Text(
                        text = "Last Failure: $lastErr",
                        color = ErrorRed,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String, valueColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, color = valueColor, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Text(text = label, color = TextMuted, fontSize = 10.sp)
    }
}

@Composable
private fun ActionButtonsRow(
    onShare: () -> Unit,
    onShareJson: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onShare,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Share TXT", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onShareJson,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Export JSON", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onCopy,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
        ) {
            Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Copy", fontSize = 12.sp)
        }
        OutlinedButton(
            onClick = onClear,
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
        ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(text = "Clear", fontSize = 12.sp, color = ErrorRed)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchAndFiltersRow(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedSeverity: CameraLogSeverity?,
    onSeveritySelect: (CameraLogSeverity?) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Search logs by event, module, error...", color = TextMuted, fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MinimalistGold,
                unfocusedBorderColor = DividerColor,
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            FilterChip(
                selected = selectedSeverity == null,
                onClick = { onSeveritySelect(null) },
                label = { Text("ALL", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MinimalistGold.copy(alpha = 0.2f),
                    selectedLabelColor = MinimalistGold
                )
            )
            CameraLogSeverity.values().forEach { sev ->
                val isSelected = selectedSeverity == sev
                val chipColor = when (sev) {
                    CameraLogSeverity.DEBUG -> TextMuted
                    CameraLogSeverity.INFO -> PrimaryCyan
                    CameraLogSeverity.WARNING -> AccentAmber
                    CameraLogSeverity.ERROR -> ErrorRed
                    CameraLogSeverity.CRITICAL -> ErrorRed
                }
                FilterChip(
                    selected = isSelected,
                    onClick = { onSeveritySelect(sev) },
                    label = { Text(sev.name, fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = chipColor.copy(alpha = 0.2f),
                        selectedLabelColor = chipColor
                    )
                )
            }
        }
    }
}

@Composable
private fun LogEntryCard(entry: CameraLogEntry) {
    var expanded by remember { mutableStateOf(false) }

    val sevColor = when (entry.severity) {
        CameraLogSeverity.DEBUG -> TextMuted
        CameraLogSeverity.INFO -> PrimaryCyan
        CameraLogSeverity.WARNING -> AccentAmber
        CameraLogSeverity.ERROR -> ErrorRed
        CameraLogSeverity.CRITICAL -> ErrorRed
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, if (entry.severity == CameraLogSeverity.ERROR || entry.severity == CameraLogSeverity.CRITICAL) sevColor.copy(alpha = 0.4f) else DividerColor, RoundedCornerShape(8.dp))
            .clickable { expanded = !expanded }
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(sevColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = entry.severity.name,
                            color = sevColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${entry.module}/${entry.eventType}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = entry.formattedTimestamp.substring(11), // HH:mm:ss.SSS
                        color = TextMuted,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = entry.message,
                color = TextSecondary,
                fontSize = 12.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 2
            )

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    if (!entry.errorCode.isNullOrBlank()) {
                        Text(
                            text = "Error Code: ${entry.errorCode}",
                            color = ErrorRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (!entry.cameraId.isNullOrBlank() || !entry.lens.isNullOrBlank()) {
                        Text(
                            text = "Camera: ID=${entry.cameraId ?: "-"} | Lens=${entry.lens ?: "-"}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    if (!entry.captureMode.isNullOrBlank() || !entry.resolution.isNullOrBlank() || entry.fps != null) {
                        Text(
                            text = "State: Mode=${entry.captureMode ?: "-"} | Res=${entry.resolution ?: "-"} | FPS=${entry.fps ?: "-"}",
                            color = TextMuted,
                            fontSize = 10.sp
                        )
                    }
                    if (!entry.technicalDetails.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkCharcoal)
                                .padding(8.dp)
                        ) {
                            Text(
                                text = entry.technicalDetails,
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }
    }
}
