package com.bishal.adaptivecamera.ui.vault

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.photography.raw.RawVaultItem
import com.bishal.adaptivecamera.photography.raw.RawVaultManager
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RawVaultScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var vaultItems by remember { mutableStateOf<List<RawVaultItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItemForDetail by remember { mutableStateOf<RawVaultItem?>(null) }
    var itemToDelete by remember { mutableStateOf<RawVaultItem?>(null) }
    var exportingItem by remember { mutableStateOf<RawVaultItem?>(null) }

    // SAF Export Launcher
    val safExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/x-adobe-dng")
    ) { destUri ->
        val item = exportingItem
        if (destUri != null && item != null) {
            coroutineScope.launch {
                val success = RawVaultManager.exportToCustomUri(context, item, destUri)
                if (success) {
                    Toast.makeText(context, "Exported ${item.filename} successfully", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Export failed", Toast.LENGTH_SHORT).show()
                }
                exportingItem = null
            }
        } else {
            exportingItem = null
        }
    }

    fun loadVault() {
        isLoading = true
        coroutineScope.launch {
            vaultItems = RawVaultManager.queryRawVaultItems(context)
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadVault()
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RAW Vault",
                                color = TextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentAmber.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "DNG 10-BIT",
                                    color = AccentAmber,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Text(
                            text = if (vaultItems.isEmpty()) "No Digital Negatives" else "${vaultItems.size} Digital Negative${if (vaultItems.size > 1) "s" else ""}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { loadVault() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = PrimaryCyan
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = PrimaryCyan)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = "Scanning RAW Vault...", color = TextSecondary, fontSize = 13.sp)
                    }
                }
                vaultItems.isEmpty() -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No DNG Negatives Found",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Capture uncompressed RAW (DNG) or synchronized RAW+JPEG photos in Pro Mode to populate your local RAW Vault.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(vaultItems, key = { it.id }) { item ->
                            RawVaultCard(
                                item = item,
                                onClick = { selectedItemForDetail = item },
                                onDelete = { itemToDelete = item },
                                onShare = {
                                    val shareIntent = RawVaultManager.createShareIntent(item)
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Adobe DNG Negative"))
                                }
                            )
                        }
                    }
                }
            }

            // Detail Inspection Dialog
            selectedItemForDetail?.let { item ->
                RawDetailDialog(
                    item = item,
                    onDismiss = { selectedItemForDetail = null },
                    onShare = {
                        val shareIntent = RawVaultManager.createShareIntent(item)
                        context.startActivity(Intent.createChooser(shareIntent, "Share Adobe DNG Negative"))
                    },
                    onExport = {
                        exportingItem = item
                        safExportLauncher.launch(item.filename)
                    },
                    onDelete = {
                        itemToDelete = item
                        selectedItemForDetail = null
                    }
                )
            }

            // Delete Confirmation Dialog
            itemToDelete?.let { item ->
                AlertDialog(
                    onDismissRequest = { itemToDelete = null },
                    containerColor = DarkSurface,
                    title = {
                        Text(text = "Delete RAW Negative?", color = TextPrimary, fontWeight = FontWeight.Bold)
                    },
                    text = {
                        Text(
                            text = "This will permanently remove ${item.filename} (${item.sizeMb}) from device storage. This action cannot be undone.",
                            color = TextSecondary,
                            fontSize = 13.sp
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    val deleted = RawVaultManager.deleteRawItem(context, item)
                                    if (deleted) {
                                        Toast.makeText(context, "Deleted ${item.filename}", Toast.LENGTH_SHORT).show()
                                        loadVault()
                                    } else {
                                        Toast.makeText(context, "Could not delete file", Toast.LENGTH_SHORT).show()
                                    }
                                    itemToDelete = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                        ) {
                            Text("Delete", color = Color.White)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { itemToDelete = null }) {
                            Text("Cancel", color = TextSecondary)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RawVaultCard(
    item: RawVaultItem,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.2f)
                    .background(DarkSurfaceVariant)
            ) {
                // Preview thumbnail (Load from paired JPEG or DNG)
                RawThumbnailView(
                    item = item,
                    modifier = Modifier.fillMaxSize()
                )

                // Top badges
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color.Black.copy(alpha = 0.7f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "RAW",
                            color = AccentAmber,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (item.hasMatchingJpeg) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(PrimaryCyan.copy(alpha = 0.85f))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "+JPG",
                                color = Color.Black,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // File size badge at bottom right
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.75f))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.sizeMb,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = item.filename,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.resolutionDisplay,
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                val exposureText = buildString {
                    item.iso?.let { append("ISO $it ") }
                    item.exposureTimeNs?.let {
                        val sec = it / 1_000_000_000.0
                        val frac = if (sec > 0 && sec < 1.0) "1/${(1.0 / sec).toInt()}s" else String.format("%.1fs", sec)
                        append("· $frac ")
                    }
                    item.aperture?.let { append("· f/$it") }
                }.trim()

                if (exposureText.isNotEmpty()) {
                    Text(
                        text = exposureText,
                        color = PrimaryCyan,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun RawThumbnailView(
    item: RawVaultItem,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val thumbUri = item.pairedJpegUri ?: item.uri

    val bitmapState = produceState<Bitmap?>(initialValue = null, thumbUri) {
        value = withContext(Dispatchers.IO) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && thumbUri != null) {
                    context.contentResolver.loadThumbnail(thumbUri, Size(360, 360), null)
                } else null
            } catch (_: Exception) {
                null
            }
        }
    }

    val bmp = bitmapState.value
    if (bmp != null) {
        Image(
            bitmap = bmp.asImageBitmap(),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(DarkSurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CameraAlt,
                contentDescription = null,
                tint = TextMuted,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
private fun RawDetailDialog(
    item: RawVaultItem,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
    onExport: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "DNG Negative Details",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                DetailItemRow("Filename", item.filename)
                DetailItemRow("File Size", "${item.sizeMb} (${item.fileSizeBytes} bytes)")
                DetailItemRow("Resolution", item.resolutionDisplay)
                DetailItemRow("Sensor Bit Depth", "${item.sensorBitDepth}-bit Raw Bayer")
                DetailItemRow("Color Space", "Camera Native RAW Sensor")
                DetailItemRow("ISO Sensitivity", item.iso?.toString() ?: "Unavailable")
                val expSec = item.exposureTimeNs?.let {
                    val s = it / 1_000_000_000.0
                    if (s > 0 && s < 1.0) "1/${(1.0 / s).toInt()}s" else String.format("%.2fs", s)
                } ?: "Unavailable"
                DetailItemRow("Shutter Speed", expSec)
                DetailItemRow("Aperture", item.aperture?.let { "f/$it" } ?: "Unavailable")
                DetailItemRow("Focal Length", item.focalLengthMm?.let { "${it}mm" } ?: "Unavailable")
                DetailItemRow(
                    "Paired JPEG",
                    if (item.hasMatchingJpeg) "Synchronized JPEG present in DCIM" else "None (RAW only)"
                )
                DetailItemRow("URI", item.uri.toString())
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = ErrorRed)
                }
                Row {
                    IconButton(onClick = onShare) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = "Share", tint = PrimaryCyan)
                    }
                    IconButton(onClick = onExport) {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = "Export via SAF", tint = SuccessGreen)
                    }
                    TextButton(onClick = onDismiss) {
                        Text("Close", color = TextPrimary)
                    }
                }
            }
        }
    )
}

@Composable
private fun DetailItemRow(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = label, color = TextSecondary, fontSize = 11.sp)
        Text(
            text = value,
            color = TextPrimary,
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}
