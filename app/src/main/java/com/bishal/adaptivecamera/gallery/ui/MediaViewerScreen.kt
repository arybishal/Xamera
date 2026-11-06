package com.bishal.adaptivecamera.gallery.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.bishal.adaptivecamera.video.export.VideoExportSheet
import com.bishal.adaptivecamera.video.export.VideoExportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ExifDetails(
    val iso: String? = null,
    val shutterSpeed: String? = null,
    val aperture: String? = null,
    val focalLength: String? = null,
    val makeModel: String? = null,
    val lensModel: String? = null,
    val whiteBalance: String? = null,
    val flash: String? = null,
    val hasGps: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaViewerScreen(
    item: MediaItem,
    onNavigateBack: () -> Unit,
    onDeleteConfirmed: (MediaItem) -> Unit
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showShareDialog by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }
    var showExportSheet by remember { mutableStateOf(false) }
    val exportViewModel: VideoExportViewModel = viewModel()

    // Pinch-to-zoom state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    // Load EXIF Metadata
    val exifDetailsState = produceState<ExifDetails?>(initialValue = null, item.uri) {
        value = withContext(Dispatchers.IO) {
            item.uri?.let { extractExifDetails(context, it) }
        }
    }

    if (showExportSheet && item.uri != null) {
        LaunchedEffect(item.uri) {
            exportViewModel.loadSourceVideo(item.uri, item.sizeBytes)
        }
        VideoExportSheet(
            viewModel = exportViewModel,
            onDismiss = { showExportSheet = false }
        )
    }

    if (showShareDialog) {
        AlertDialog(
            onDismissRequest = { showShareDialog = false },
            title = { Text("Share Photo", color = TextPrimary) },
            text = {
                Text(
                    "Choose whether to share original photo or create a privacy-safe copy with all EXIF metadata and location stripped.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        shareMediaItem(context, item, stripExif = true)
                    }
                ) {
                    Text("Safe Share (Strip EXIF)", color = PrimaryCyan, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showShareDialog = false
                        shareMediaItem(context, item, stripExif = false)
                    }
                ) {
                    Text("Original", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
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
                    IconButton(onClick = {
                        if (item.mediaType == MediaType.PHOTO) {
                            showShareDialog = true
                        } else {
                            shareMediaItem(context, item, stripExif = false)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share",
                            tint = TextSecondary
                        )
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onDoubleTap = {
                                        zoomScale = if (zoomScale > 1.05f) 1f else 2.5f
                                        panOffset = Offset.Zero
                                    }
                                )
                            }
                            .pointerInput(Unit) {
                                detectTransformGestures { _, pan, zoom, _ ->
                                    val newScale = (zoomScale * zoom).coerceIn(1f, 5f)
                                    zoomScale = newScale
                                    panOffset = if (newScale > 1f) {
                                        Offset(
                                            x = (panOffset.x + pan.x).coerceIn(-600f * (newScale - 1f), 600f * (newScale - 1f)),
                                            y = (panOffset.y + pan.y).coerceIn(-900f * (newScale - 1f), 900f * (newScale - 1f))
                                        )
                                    } else {
                                        Offset.Zero
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = item.displayName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = zoomScale
                                    scaleY = zoomScale
                                    translationX = panOffset.x
                                    translationY = panOffset.y
                                }
                        )
                    }
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
                    exif = exifDetailsState.value,
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
    exif: ExifDetails?,
    modifier: Modifier = Modifier
) {
    val dateStr = remember(item.dateTakenMs) {
        val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        formatter.format(Date(item.dateTakenMs))
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface.copy(alpha = 0.94f), RoundedCornerShape(12.dp))
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Column {
            Text(
                text = "MEDIA & EXIF DETAILS",
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

            // EXIF Camera & Exposure Inspector (FEAT-INT-08)
            if (exif != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "CAMERA & EXPOSURE",
                    color = PrimaryCyan.copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                exif.makeModel?.let { InfoRowText("Camera", it) }
                exif.lensModel?.let { InfoRowText("Lens", it) }
                exif.focalLength?.let { InfoRowText("Focal Length", it) }
                exif.aperture?.let { InfoRowText("Aperture", it) }
                exif.shutterSpeed?.let { InfoRowText("Shutter", it) }
                exif.iso?.let { InfoRowText("ISO", it) }
                exif.whiteBalance?.let { InfoRowText("White Balance", it) }
                exif.flash?.let { InfoRowText("Flash", it) }
                InfoRowText("GPS Tagged", if (exif.hasGps) "Yes" else "None")
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

private fun extractExifDetails(context: Context, uri: Uri): ExifDetails {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val exif = ExifInterface(stream)
            val iso = exif.getAttribute("PhotographicSensitivity")
                ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
            val exposureTime = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.toDoubleOrNull()
            val formattedShutter = exposureTime?.let {
                if (it < 1.0 && it > 0.0) {
                    "1/${(1.0 / it).toInt()}s"
                } else {
                    String.format("%.2fs", it)
                }
            }
            val fNumber = exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.toDoubleOrNull()
            val formattedAperture = fNumber?.let { String.format("f/%.1f", it) }
            val focalLength = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
            val make = exif.getAttribute(ExifInterface.TAG_MAKE) ?: ""
            val model = exif.getAttribute(ExifInterface.TAG_MODEL) ?: ""
            val makeModel = "$make $model".trim().ifEmpty { null }
            val lensModel = exif.getAttribute("LensModel")
            val wb = exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)
            val flash = exif.getAttribute(ExifInterface.TAG_FLASH)
            val latLong = FloatArray(2)
            val hasGps = exif.getLatLong(latLong)

            ExifDetails(
                iso = iso?.let { "ISO $it" },
                shutterSpeed = formattedShutter,
                aperture = formattedAperture,
                focalLength = focalLength?.let { "${it}mm" },
                makeModel = makeModel,
                lensModel = lensModel,
                whiteBalance = if (wb == "1") "Manual" else "Auto",
                flash = if (flash != "0" && flash != null) "Fired" else "Did not fire",
                hasGps = hasGps
            )
        } ?: ExifDetails()
    } catch (e: Exception) {
        ExifDetails()
    }
}

private fun shareMediaItem(context: Context, item: MediaItem, stripExif: Boolean) {
    val uri = item.uri ?: return
    if (!stripExif || item.mediaType != MediaType.PHOTO) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
        return
    }

    try {
        val bmp = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
        val cleanFile = File(context.cacheDir, "safe_shared_${System.currentTimeMillis()}.jpg")
        cleanFile.outputStream().use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        val cleanUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            cleanFile
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, cleanUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Safe Photo (EXIF Stripped)"))
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = item.mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Media"))
    }
}
