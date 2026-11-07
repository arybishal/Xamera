package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.FlashAuto
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MotionPhotosOn
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.FlashMode
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.MinimalistGold

/**
 * Minimalist top control bar matching the reference screenshot design.
 * Features a dark rounded capsule on the left (Flash, Secondary, EV Readout)
 * and AI / Quick Settings grid buttons on the right.
 */
@Composable
fun MinimalistTopBar(
    config: CameraConfiguration,
    onToggleFlash: () -> Unit,
    onToggleExposure: () -> Unit,
    onToggleQuickSettings: () -> Unit,
    onToggleAiMode: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .statusBarsPadding()
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Dark Pill Capsule [Flash | Secondary | EV 0.0]
        Row(
            modifier = Modifier
                .height(38.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(DarkCharcoal.copy(alpha = 0.85f))
                .border(0.5.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(19.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 1. Flash Toggle Icon
            val (flashIcon, flashTint) = when (config.flashMode) {
                FlashMode.OFF -> Icons.Default.FlashOff to Color.White.copy(alpha = 0.9f)
                FlashMode.ON -> Icons.Default.FlashOn to MinimalistGold
                FlashMode.AUTO -> Icons.Default.FlashAuto to MinimalistGold
                FlashMode.TORCH -> Icons.Default.FlashOn to MinimalistGold
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleFlash),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = flashIcon,
                    contentDescription = "Flash: ${config.flashMode.displayName}",
                    tint = flashTint,
                    modifier = Modifier.size(17.dp)
                )
            }

            // 2. Secondary Feature Icon (Motion/Assist Indicator)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onToggleAiMode),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MotionPhotosOn,
                    contentDescription = "Capture Assist",
                    tint = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.size(17.dp)
                )
            }

            // 3. Live Exposure Compensation Value (EV 0.0)
            val evVal = config.exposureCompensationIndex * config.exposureStepEv
            val evFormatted = if (evVal > 0.001f) String.format(java.util.Locale.US, "+%.1f", evVal) else String.format(java.util.Locale.US, "%.1f", evVal)
            val isNeutral = config.exposureCompensationIndex == 0

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onToggleExposure)
                    .padding(horizontal = 4.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "EV",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = if (isNeutral) "0.0" else evFormatted,
                    color = if (isNeutral) Color.White else MinimalistGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Right: [ AI / Smart Scan ]  [ Quick Settings Grid (:::) ]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // AI Focus / Scan Button
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkCharcoal.copy(alpha = 0.85f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(onClick = onToggleAiMode),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CenterFocusStrong,
                    contentDescription = "AI Scanner & Lens",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.size(19.dp)
                )
            }

            // Quick Settings Grid Button (:::)
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(DarkCharcoal.copy(alpha = 0.85f))
                    .border(0.5.dp, Color.White.copy(alpha = 0.08f), CircleShape)
                    .clickable(onClick = onToggleQuickSettings),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Apps,
                    contentDescription = "Quick Settings",
                    tint = Color.White,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
    }
}
