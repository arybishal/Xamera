package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PureBlack

/**
 * Minimalist bottom camera deck matching the reference screenshot.
 * Structures the shutter control row prominently ABOVE the horizontal mode selector
 * on an immersive pure black canvas.
 */
@Composable
fun MinimalistBottomDeck(
    mode: CameraMode,
    sessionState: CameraSessionState,
    latestMedia: MediaItem?,
    recordingDurationSec: Long,
    onModeChanged: (CameraMode) -> Unit,
    onShutterPressed: () -> Unit,
    onSwitchCamera: () -> Unit,
    onOpenGallery: () -> Unit,
    modifier: Modifier = Modifier,
    isLeftHanded: Boolean = false,
    onHapticTick: () -> Unit = {},
    onOpenMoreModes: () -> Unit = {}
) {
    val isRecording = sessionState == CameraSessionState.RECORDING

    Column(
        modifier = modifier
            .navigationBarsPadding()
            .fillMaxWidth()
            .background(PureBlack)
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Recording duration indicator (if active)
        if (isRecording) {
            val minutes = recordingDurationSec / 60
            val seconds = recordingDurationSec % 60
            val timeString = String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds)

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
        }

        // 1. Primary Shutter Row: Gallery (Left) | Shutter Button (Center) | Switch Camera (Right)
        MinimalistShutterRow(
            mode = mode,
            isRecording = isRecording,
            latestMedia = latestMedia,
            canSwitchCamera = sessionState.canSwitchCamera,
            onShutterPressed = onShutterPressed,
            onSwitchCamera = onSwitchCamera,
            onOpenGallery = onOpenGallery,
            isLeftHanded = isLeftHanded
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Horizontal Mode Selector: PRO | VIDEO | PHOTO | PORTRAIT | MORE
        MinimalistModeSelector(
            currentMode = mode,
            onModeChanged = onModeChanged,
            onHapticTick = onHapticTick,
            onOpenMoreModes = onOpenMoreModes
        )
    }
}
