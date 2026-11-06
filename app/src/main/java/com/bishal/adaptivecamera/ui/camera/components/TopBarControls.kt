package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.GridOff
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Timer10
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.camera.config.HdrPlusMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@Composable
fun TopBarControls(
    config: CameraConfiguration,
    onToggleFlash: () -> Unit,
    onToggleTimer: () -> Unit,
    onToggleExposure: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    onCycleAspectRatio: () -> Unit = {},
    onCycleGrid: () -> Unit = {},
    isLookActive: Boolean = false,
    onToggleColorLooks: () -> Unit = {},
    onToggleHdrPlus: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Flash Mode
        IconButton(
            onClick = onToggleFlash,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            when (config.flashMode) {
                FlashMode.OFF -> Icon(Icons.Default.FlashOff, contentDescription = "Flash Off", tint = TextSecondary)
                FlashMode.AUTO -> Icon(Icons.Default.FlashAuto, contentDescription = "Flash Auto", tint = AccentAmber)
                FlashMode.ON -> Icon(Icons.Default.FlashOn, contentDescription = "Flash On", tint = AccentAmber)
                FlashMode.TORCH -> Icon(Icons.Default.Highlight, contentDescription = "Torch", tint = PrimaryCyan)
            }
        }

        // 1b. HDR+ Quick Toggle (Photo mode only)
        if (config.mode == CameraMode.PHOTO) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.45f))
                    .border(
                        1.dp,
                        if (config.hdrPlusMode == HdrPlusMode.ON) PrimaryCyan
                        else if (config.hdrPlusMode == HdrPlusMode.AUTO) AccentAmber.copy(alpha = 0.6f)
                        else Color.Transparent,
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(onClick = onToggleHdrPlus)
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (config.hdrPlusMode) {
                        HdrPlusMode.OFF -> "HDR+"
                        HdrPlusMode.AUTO -> "HDR+ A"
                        HdrPlusMode.ON -> "HDR+ ON"
                    },
                    color = when (config.hdrPlusMode) {
                        HdrPlusMode.OFF -> TextSecondary
                        HdrPlusMode.AUTO -> AccentAmber
                        HdrPlusMode.ON -> PrimaryCyan
                    },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 2. Aspect Ratio Quick Pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onCycleAspectRatio)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = config.aspectRatio.displayName,
                color = if (config.aspectRatio != com.bishal.adaptivecamera.camera.config.AspectRatioConfig.RATIO_4_3) PrimaryCyan else TextPrimary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // 3. Composition Grid Quick Button
        IconButton(
            onClick = onCycleGrid,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            val isGridActive = config.gridType != CompositionGridType.OFF
            Icon(
                imageVector = if (isGridActive) Icons.Default.GridOn else Icons.Default.GridOff,
                contentDescription = "Composition Grid: ${config.gridType.displayName}",
                tint = if (isGridActive) PrimaryCyan else TextSecondary
            )
        }

        // 4. Timer
        IconButton(
            onClick = onToggleTimer,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            when (config.timer) {
                PhotoTimer.OFF -> Icon(Icons.Default.Timer, contentDescription = "Timer Off", tint = TextSecondary)
                PhotoTimer.SEC_3 -> Icon(Icons.Default.Timer3, contentDescription = "Timer 3s", tint = PrimaryCyan)
                PhotoTimer.SEC_10 -> Icon(Icons.Default.Timer10, contentDescription = "Timer 10s", tint = PrimaryCyan)
            }
        }

        // 5. Exposure indicator / toggle
        val isExposureAvailable = config.isExposureControlAvailable
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(onClick = onToggleExposure)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = "Exposure Compensation",
                    tint = if (!isExposureAvailable) TextSecondary.copy(alpha = 0.4f)
                    else if (config.exposureCompensationIndex != 0) PrimaryCyan
                    else TextSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = if (!isExposureAvailable) "EV" else config.exposureEvDisplay,
                    color = if (!isExposureAvailable) TextSecondary.copy(alpha = 0.4f)
                    else if (config.exposureCompensationIndex != 0) PrimaryCyan
                    else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // 6. Color Looks & Film Effects Button
        IconButton(
            onClick = onToggleColorLooks,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Color Looks & Film Effects",
                tint = if (isLookActive) AccentAmber else TextSecondary
            )
        }

        // 7. Settings Screen Button
        IconButton(
            onClick = onOpenSettings,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "Camera Settings",
                tint = PrimaryCyan
            )
        }
    }
}
