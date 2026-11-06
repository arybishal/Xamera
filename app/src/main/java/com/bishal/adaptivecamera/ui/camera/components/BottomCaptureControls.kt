package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.session.CameraSessionState
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.gallery.ui.MediaThumbnail
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary

@Composable
fun BottomCaptureControls(
    mode: CameraMode,
    sessionState: CameraSessionState,
    latestMedia: MediaItem?,
    recordingDurationSec: Long,
    onModeChanged: (CameraMode) -> Unit,
    onShutterPressed: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    isLeftHanded: Boolean = false
) {
    val isRecording = sessionState == CameraSessionState.RECORDING

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .background(DarkSurface.copy(alpha = 0.85f))
            .padding(bottom = 12.dp, top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording duration indicator
        if (isRecording) {
            val minutes = recordingDurationSec / 60
            val seconds = recordingDurationSec % 60
            val timeString = String.format("%02d:%02d", minutes, seconds)

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(ErrorRed.copy(alpha = 0.2f))
                    .border(1.dp, ErrorRed, RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(ErrorRed)
                )
                Text(
                    text = timeString,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
        } else {
            // Mode Selector: PHOTO | PRO | VIDEO | TIME-LAPSE | SLOW-MO | DUAL
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(20.dp))
                CameraMode.values().forEach { m ->
                    ModeItem(
                        label = m.displayName,
                        isSelected = mode == m,
                        onClick = { onModeChanged(m) }
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
        }

        // Primary Buttons Row: Gallery Thumbnail | Shutter Button | Camera Flip
        // (Mirrored in Left-Handed UI mode for ergonomic left thumb reach)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val galleryButton: @Composable () -> Unit = {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenGallery),
                    contentAlignment = Alignment.Center
                ) {
                    if (latestMedia?.uri != null) {
                        MediaThumbnail(
                            uri = latestMedia.uri,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PhotoLibrary,
                            contentDescription = "Gallery",
                            tint = TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            val switchCameraButton: @Composable () -> Unit = {
                IconButton(
                    onClick = onSwitchCamera,
                    enabled = sessionState.canSwitchCamera,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Switch Camera",
                        tint = TextPrimary,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            if (isLeftHanded) {
                switchCameraButton()
                ShutterButton(
                    mode = mode,
                    isRecording = isRecording,
                    onClick = onShutterPressed
                )
                galleryButton()
            } else {
                galleryButton()
                ShutterButton(
                    mode = mode,
                    isRecording = isRecording,
                    onClick = onShutterPressed
                )
                switchCameraButton()
            }
        }
    }
}

@Composable
private fun ModeItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = if (isSelected) PrimaryCyan else TextMuted,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            letterSpacing = 1.sp
        )
        if (isSelected) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(width = 16.dp, height = 2.dp)
                    .background(PrimaryCyan, RoundedCornerShape(1.dp))
            )
        }
    }
}

@Composable
private fun ShutterButton(
    mode: CameraMode,
    isRecording: Boolean,
    onClick: () -> Unit
) {
    val coreSize by animateDpAsState(
        targetValue = if (isRecording) 32.dp else if (mode.isVideoMode) 54.dp else 58.dp,
        label = "coreSize"
    )
    val cornerRadius by animateDpAsState(
        targetValue = if (isRecording) 8.dp else 36.dp,
        label = "cornerRadius"
    )

    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(CircleShape)
            .border(4.dp, Color.White, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        val coreColor = if (mode.isVideoMode || isRecording) ErrorRed else Color.White

        Box(
            modifier = Modifier
                .size(coreSize)
                .clip(RoundedCornerShape(cornerRadius))
                .background(coreColor)
        )
    }
}
