package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Crop
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.config.AspectRatioConfig
import com.bishal.adaptivecamera.camera.config.CameraConfiguration
import com.bishal.adaptivecamera.camera.config.CompositionGridType
import com.bishal.adaptivecamera.camera.config.HdrPlusMode
import com.bishal.adaptivecamera.camera.config.PhotoTimer
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.DarkCharcoalElevated
import com.bishal.adaptivecamera.ui.theme.MinimalistGold

/**
 * Floating 2-row modal bottom sheet inspired by the reference screenshot.
 * Houses 6 large circular quick-action tiles:
 * Aspect ratio, Soft light, Auto HDR, Timer, Grid / Composition, and Settings.
 */
@Composable
fun MinimalistQuickSettingsSheet(
    isVisible: Boolean,
    config: CameraConfiguration,
    isScreenFlashActive: Boolean,
    onCycleAspectRatio: () -> Unit,
    onToggleSoftLight: () -> Unit,
    onCycleHdrPlus: () -> Unit,
    onCycleTimer: () -> Unit,
    onCycleGrid: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isVisible) return

    Box(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Semi-transparent backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.55f))
        )

        // Floating Sheet Card
        AnimatedVisibility(
            visible = isVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 14.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(DarkCharcoal)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {} // Prevent click-through to backdrop
                    .padding(horizontal = 18.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Row 1: Aspect ratio | Soft light | Auto HDR | Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.Top
                ) {
                    // 1. Aspect Ratio
                    QuickActionTile(
                        icon = Icons.Default.Crop,
                        label = "Aspect ratio",
                        badge = config.aspectRatio.displayName,
                        onClick = onCycleAspectRatio
                    )

                    // 2. Soft Light (Screen Flash)
                    QuickActionTile(
                        icon = Icons.Default.Lightbulb,
                        label = "Soft light",
                        isActive = isScreenFlashActive,
                        badge = if (isScreenFlashActive) "ON" else null,
                        onClick = onToggleSoftLight
                    )

                    // 3. Auto HDR (Distinctive Gold Button when active, matching screenshot)
                    val isHdrActive = config.hdrPlusMode != HdrPlusMode.OFF
                    val hdrText = when (config.hdrPlusMode) {
                        HdrPlusMode.OFF -> "HDR\nOFF"
                        HdrPlusMode.AUTO -> "HDR\nAUTO"
                        HdrPlusMode.ON -> "HDR\nON"
                    }
                    QuickHdrTile(
                        label = "Auto HDR",
                        badgeText = hdrText,
                        isActive = isHdrActive,
                        onClick = onCycleHdrPlus
                    )

                    // 4. Timer
                    val timerLabel = when (config.timer) {
                        PhotoTimer.OFF -> "Off"
                        else -> "${config.timer.seconds}s"
                    }
                    QuickActionTile(
                        icon = Icons.Default.Timer,
                        label = "Timer",
                        badge = timerLabel,
                        isActive = config.timer != PhotoTimer.OFF,
                        onClick = onCycleTimer
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Row 2: Grid / Composition | Settings
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    Spacer(modifier = Modifier.width(12.dp))

                    // 5. Grid / Composition Overlay
                    val gridLabel = when (config.gridType) {
                        CompositionGridType.OFF -> "Off"
                        CompositionGridType.RULE_OF_THIRDS -> "3x3"
                        CompositionGridType.GOLDEN_RATIO -> "Golden"
                        CompositionGridType.GOLDEN_SPIRAL -> "Spiral"
                        CompositionGridType.CENTER_CROSSHAIRS -> "Cross"
                    }
                    QuickActionTile(
                        icon = Icons.Default.GridOn,
                        label = "Grid",
                        badge = gridLabel,
                        isActive = config.gridType != CompositionGridType.OFF,
                        onClick = onCycleGrid
                    )

                    Spacer(modifier = Modifier.width(36.dp))

                    // 6. Settings
                    QuickActionTile(
                        icon = Icons.Default.Settings,
                        label = "Settings",
                        onClick = {
                            onDismiss()
                            onOpenSettings()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    badge: String? = null,
    isActive: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) MinimalistGold else DarkCharcoalElevated)
                .border(
                    1.dp,
                    if (isActive) MinimalistGold else Color.White.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color.Black else Color.White.copy(alpha = 0.9f),
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )

        if (badge != null) {
            Text(
                text = badge,
                color = if (isActive) MinimalistGold else Color.White.copy(alpha = 0.5f),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Dedicated Auto HDR Tile matching the prominent solid-gold circular design in the screenshot.
 */
@Composable
private fun QuickHdrTile(
    label: String,
    badgeText: String,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(if (isActive) MinimalistGold else DarkCharcoalElevated)
                .border(
                    1.dp,
                    if (isActive) MinimalistGold else Color.White.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = badgeText,
                color = if (isActive) Color.Black else Color.White.copy(alpha = 0.85f),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 13.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = label,
            color = Color.White.copy(alpha = 0.9f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}
