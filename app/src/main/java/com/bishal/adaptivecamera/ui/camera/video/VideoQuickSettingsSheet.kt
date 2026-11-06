package com.bishal.adaptivecamera.ui.camera.video

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.video.config.VideoBitrateProfile
import com.bishal.adaptivecamera.video.config.VideoCodec
import com.bishal.adaptivecamera.video.config.VideoConfig
import com.bishal.adaptivecamera.video.config.VideoFrameRate
import com.bishal.adaptivecamera.video.config.VideoResolution
import com.bishal.adaptivecamera.video.config.VideoStabilizationMode
import com.bishal.adaptivecamera.video.exposure.ShutterAngleCalculator

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoQuickSettingsSheet(
    config: VideoConfig,
    onConfigChanged: (VideoConfig) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .background(Color(0xE6141414))
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = null,
                        tint = Color(0xFFFFCC00),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Professional Video Settings",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White
                    )
                }
            }

            // 1. Resolution
            SettingSection(title = "RESOLUTION") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        VideoResolution.UHD_4K,
                        VideoResolution.FHD_1080P,
                        VideoResolution.HD_720P
                    ).forEach { res ->
                        OptionPill(
                            label = res.label,
                            selected = config.resolution == res,
                            onClick = { onConfigChanged(config.copy(resolution = res).syncBitrate()) }
                        )
                    }
                }
            }

            // 2. Frame Rate
            SettingSection(title = "FRAME RATE") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        VideoFrameRate.FPS_24,
                        VideoFrameRate.FPS_30,
                        VideoFrameRate.FPS_60
                    ).forEach { fps ->
                        OptionPill(
                            label = fps.label,
                            selected = config.frameRate == fps,
                            onClick = { onConfigChanged(config.copy(frameRate = fps).syncBitrate()) }
                        )
                    }
                }
            }

            // 3. Shutter Angle
            SettingSection(title = "SHUTTER ANGLE") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OptionPill(
                        label = "Auto",
                        selected = config.shutterAngle == null,
                        onClick = { onConfigChanged(config.copy(shutterAngle = null)) }
                    )

                    ShutterAngleCalculator.PRESET_SHUTTER_ANGLES.forEach { (angle, label) ->
                        val targetSec = ShutterAngleCalculator.calculateExposureSeconds(config.frameRate.fps, angle)
                        val denom = kotlin.math.round(1.0 / targetSec).toInt()
                        OptionPill(
                            label = "${angle.toInt()}° (1/${denom}s)",
                            selected = config.shutterAngle == angle,
                            onClick = { onConfigChanged(config.copy(shutterAngle = angle)) }
                        )
                    }
                }
            }

            // 4. Video Codec
            SettingSection(title = "VIDEO CODEC") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        VideoCodec.H264_AVC,
                        VideoCodec.H265_HEVC
                    ).forEach { codec ->
                        OptionPill(
                            label = codec.label,
                            selected = config.videoCodec == codec,
                            onClick = { onConfigChanged(config.copy(videoCodec = codec)) }
                        )
                    }
                }
            }

            // 5. Bitrate Profile
            SettingSection(title = "BITRATE PROFILE") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        VideoBitrateProfile.STANDARD,
                        VideoBitrateProfile.HIGH,
                        VideoBitrateProfile.ALL_I
                    ).forEach { profile ->
                        val bps = profile.getTargetBitrateBps(config.resolution, config.frameRate)
                        val mbps = bps / 1_000_000
                        OptionPill(
                            label = "${profile.label} (${mbps} Mbps)",
                            selected = config.bitrateProfile == profile,
                            onClick = { onConfigChanged(config.copy(bitrateProfile = profile).syncBitrate()) }
                        )
                    }
                }
            }

            // 6. Stabilization Mode
            SettingSection(title = "STABILIZATION") {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        VideoStabilizationMode.OFF,
                        VideoStabilizationMode.OIS,
                        VideoStabilizationMode.EIS,
                        VideoStabilizationMode.COMBINED
                    ).forEach { stab ->
                        OptionPill(
                            label = stab.label,
                            selected = config.stabilizationMode == stab,
                            onClick = { onConfigChanged(config.copy(stabilizationMode = stab)) }
                        )
                    }
                }
            }

            // 7. Audio Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF222222))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Record Audio",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (config.audioEnabled) "Studio 48kHz Stereo AAC" else "Muted / Video Only",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                Switch(
                    checked = config.audioEnabled,
                    onCheckedChange = { onConfigChanged(config.copy(audioEnabled = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF34C759)
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = Color(0xFFAAAAAA),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
        content()
    }
}

@Composable
private fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) Color(0xFFFFCC00) else Color(0xFF222222))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            color = if (selected) Color.Black else Color.White,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
