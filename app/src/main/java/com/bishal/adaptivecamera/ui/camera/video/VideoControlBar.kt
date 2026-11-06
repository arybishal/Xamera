package com.bishal.adaptivecamera.ui.camera.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.video.audio.AudioVuState
import com.bishal.adaptivecamera.video.config.VideoConfig
import com.bishal.adaptivecamera.video.recording.VideoRecordingState
import com.bishal.adaptivecamera.video.recording.VideoRecordingStatus
import kotlinx.coroutines.delay

@Composable
fun VideoControlBar(
    videoConfig: VideoConfig,
    recordingState: VideoRecordingState,
    audioVuState: AudioVuState,
    onOpenQuickSettings: () -> Unit,
    onTogglePauseResume: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isRecording = recordingState.status.isRecordingOrPaused

    // Blinking effect for recording indicator
    var isBlinkVisible by remember { mutableStateOf(true) }
    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(600)
                isBlinkVisible = !isBlinkVisible
            }
        } else {
            isBlinkVisible = true
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Resolution & FPS badge
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x99000000))
                .clickable(enabled = !isRecording, onClick = onOpenQuickSettings)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = videoConfig.badgeLabel,
                color = if (isRecording) Color.LightGray else Color(0xFFFFCC00),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = videoConfig.videoCodec.shortLabel,
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        // Center: Recording timer or exposure info
        if (isRecording) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xB3000000))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Red blink dot
                if (isBlinkVisible && recordingState.status == VideoRecordingStatus.RECORDING) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFF3B30), CircleShape)
                    )
                } else if (recordingState.status == VideoRecordingStatus.PAUSED) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(Color(0xFFFFCC00), CircleShape)
                    )
                } else {
                    Box(modifier = Modifier.size(8.dp))
                }

                Text(
                    text = recordingState.formattedDuration,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = recordingState.formattedSize,
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                // Pause / Resume button
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF333333))
                        .clickable(onClick = onTogglePauseResume),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (recordingState.status == VideoRecordingStatus.PAUSED) Icons.Default.PlayArrow else Icons.Default.Pause,
                        contentDescription = if (recordingState.status == VideoRecordingStatus.PAUSED) "Resume" else "Pause",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else {
            // Pro info badge: Shutter angle + Stabilization
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0x99000000))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (videoConfig.shutterAngle != null) {
                    Text(
                        text = "${videoConfig.shutterAngle.toInt()}° (${videoConfig.formattedShutterSpeed})",
                        color = Color(0xFFAAAAAA),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = videoConfig.stabilizationMode.shortLabel,
                    color = if (videoConfig.stabilizationMode.isStabilized) Color(0xFF34C759) else Color.Gray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right: Audio VU Meter
        AnimatedVisibility(
            visible = videoConfig.audioEnabled,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            AudioVuMeterView(vuState = audioVuState)
        }
    }
}
