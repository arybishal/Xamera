package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Highlight
import androidx.compose.material.icons.filled.Memory
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
import com.bishal.adaptivecamera.camera.config.FlashMode
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
    onOpenDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Flash Mode
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

        // Timer
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

        // Exposure indicator / toggle
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
                    contentDescription = "Exposure",
                    tint = if (config.exposureCompensationIndex != 0) PrimaryCyan else TextSecondary,
                    modifier = Modifier.padding(end = 4.dp)
                )
                Text(
                    text = config.exposureEvDisplay,
                    color = if (config.exposureCompensationIndex != 0) PrimaryCyan else TextPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Diagnostics Screen Button (Preserving Phase 0)
        IconButton(
            onClick = onOpenDiagnostics,
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.45f))
        ) {
            Icon(
                imageVector = Icons.Default.Memory,
                contentDescription = "Hardware Diagnostics",
                tint = PrimaryCyan
            )
        }
    }
}
