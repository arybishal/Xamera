package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.core.storage.MediaItem
import com.bishal.adaptivecamera.gallery.ui.MediaThumbnail
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.ShutterRingWhite

/**
 * Clean bottom control row matching the reference screenshot.
 * Positions the rounded gallery thumbnail, dual-ring shutter button,
 * and circular camera switch button with left-handed ergonomic mirroring.
 */
@Composable
fun MinimalistShutterRow(
    mode: CameraMode,
    isRecording: Boolean,
    latestMedia: MediaItem?,
    canSwitchCamera: Boolean,
    onShutterPressed: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    isLeftHanded: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Gallery Thumbnail (Rounded Square)
        val galleryThumbnail: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCharcoal)
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onOpenGallery
                    ),
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
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }

        // 2. Camera Switch Button (Dark Circular Button)
        val cameraSwitchButton: @Composable () -> Unit = {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(DarkCharcoal)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = canSwitchCamera,
                        onClick = onSwitchCamera
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Cameraswitch,
                    contentDescription = "Switch Camera",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        // 3. Central Dual-Ring Shutter Button
        val shutterButton: @Composable () -> Unit = {
            val isVideo = mode.isVideoMode
            val innerSize by animateDpAsState(
                targetValue = if (isRecording) 32.dp else 58.dp,
                animationSpec = tween(durationMillis = 200),
                label = "shutterInnerSize"
            )
            val innerCornerRadius by animateDpAsState(
                targetValue = if (isRecording) 8.dp else 29.dp,
                animationSpec = tween(durationMillis = 200),
                label = "shutterInnerCorner"
            )

            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .border(3.5.dp, ShutterRingWhite, CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShutterPressed
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Inner solid core disc/square
                Box(
                    modifier = Modifier
                        .size(innerSize)
                        .clip(RoundedCornerShape(innerCornerRadius))
                        .background(
                            when {
                                isRecording -> ErrorRed
                                isVideo -> ErrorRed
                                else -> ShutterRingWhite
                            }
                        )
                )
            }
        }

        if (isLeftHanded) {
            cameraSwitchButton()
            shutterButton()
            galleryThumbnail()
        } else {
            galleryThumbnail()
            shutterButton()
            cameraSwitchButton()
        }
    }
}
