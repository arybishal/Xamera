package com.bishal.adaptivecamera.gallery.ui

import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.os.Build
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bishal.adaptivecamera.video.export.VideoExportSheet
import com.bishal.adaptivecamera.video.export.VideoExportViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.core.storage.MediaType
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    item: MediaItem,
    onNavigateBack: () -> Unit,
    onDeleteConfirmed: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val exportViewModel: VideoExportViewModel = viewModel()

    if (showExportSheet && item.uri != null) {
        LaunchedEffect(item.uri) {
            exportViewModel.loadSourceVideo(item.uri, item.sizeBytes)
        }
        VideoExportSheet(
            viewModel = exportViewModel,
            onDismiss = { showExportSheet = false }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Media?", color = TextPrimary) },
            text = { Text("Are you sure you want to delete '${item.displayName}'? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed(item)
                    }
                ) {
                    Text("Delete", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = item.displayName,
                        color = TextPrimary,
                        fontSize = 14.sp,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PrimaryCyan
                        )
                    }
                },
                actions = {
                    if (item.mediaType == MediaType.VIDEO) {
                        IconButton(onClick = { showExportSheet = true }) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = "Process & Export Video",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    IconButton(onClick = { showInfoSheet = !showInfoSheet }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = if (showInfoSheet) PrimaryCyan else TextSecondary
                        )
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = ErrorRed
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkSurface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val mediaUri = item.uri
            if (mediaUri != null && item.mediaType == MediaType.PHOTO) {
                val fullImageState = produceState<Bitmap?>(initialValue = null, mediaUri) {
                    value = withContext(Dispatchers.IO) {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                                val source = ImageDecoder.createSource(context.contentResolver, mediaUri)
                                ImageDecoder.decodeBitmap(source)
                            } else {
                                @Suppress("DEPRECATION")
                                MediaStore.Images.Media.getBitmap(context.contentResolver, mediaUri)
                            }
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                val bmp = fullImageState.value
                if (bmp != null) {
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = item.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryCyan)
                    }
                }
            } else if (mediaUri != null && item.mediaType == MediaType.VIDEO) {
                // Video Playback
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            setVideoURI(mediaUri)
                            val controller = MediaController(ctx)
                            controller.setAnchorView(this)
                            setMediaController(controller)
                            setOnPreparedListener { mp ->
                                mp.isLooping = true
                                start()
                            }
                        }
                    }
                )
            }

            if (showInfoSheet) {
                MediaInfoOverlay(
                    item = item,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MediaInfoOverlay(
    item: MediaItem,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(item.dateTakenMs) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(item.dateTakenMs))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface.copy(alpha = 0.92f), RoundedCornerShape(12.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = "MEDIA DETAILS",
                color = PrimaryCyan,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            InfoRowText("Name", item.displayName)
            InfoRowText("Date Taken", dateStr)
            InfoRowText("Type", item.mimeType)
            InfoRowText("Size", item.sizeFormatted)
            if (item.width > 0 && item.height > 0) {
                InfoRowText("Dimensions", "${item.width} x ${item.height}")
            }
            if (item.mediaType == MediaType.VIDEO && item.durationMs > 0) {
                InfoRowText("Duration", item.durationFormatted)
            }
        }
    }
}

@Composable
private fun InfoRowText(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextMuted, fontSize = 11.sp)
        Text(text = value, color = TextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    }
}
