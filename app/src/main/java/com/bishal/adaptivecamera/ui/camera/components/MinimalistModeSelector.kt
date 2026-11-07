package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.config.CameraMode
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.DarkCharcoalElevated
import com.bishal.adaptivecamera.ui.theme.MinimalistGold

/**
 * Minimalist horizontal camera mode selector matching the reference screenshot.
 * Displays primary modes: PRO, VIDEO, PHOTO, PORTRAIT, MORE.
 * Active mode is framed in a dark rounded capsule with warm gold typography.
 */
@Composable
fun MinimalistModeSelector(
    currentMode: CameraMode,
    onModeChanged: (CameraMode) -> Unit,
    onHapticTick: () -> Unit = {},
    onOpenMoreModes: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Primary modes directly visible in the carousel
    val primaryModes = listOf(
        CameraMode.PRO,
        CameraMode.VIDEO,
        CameraMode.PHOTO,
        CameraMode.PORTRAIT
    )

    // Additional modes revealed under "MORE"
    val moreModes = listOf(
        CameraMode.NIGHT,
        CameraMode.TIME_LAPSE,
        CameraMode.SLOW_MO,
        CameraMode.DUAL
    )

    val isMoreActive = currentMode in moreModes

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Main Horizontal Carousel: PRO | VIDEO | PHOTO | PORTRAIT | MORE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            primaryModes.forEach { mode ->
                val isSelected = currentMode == mode && !isMoreActive

                val textColor by animateColorAsState(
                    targetValue = if (isSelected) MinimalistGold else Color.White.copy(alpha = 0.55f),
                    animationSpec = tween(durationMillis = 150),
                    label = "modeTextColor"
                )

                val pillBg by animateColorAsState(
                    targetValue = if (isSelected) DarkCharcoal else Color.Transparent,
                    animationSpec = tween(durationMillis = 150),
                    label = "modePillBg"
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(pillBg)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            if (!isSelected) {
                                onHapticTick()
                                onModeChanged(mode)
                            }
                        }
                        .padding(horizontal = 9.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mode.displayName.uppercase(),
                        color = textColor,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            // "MORE" Capsule
            val isMoreSelected = isMoreActive
            val moreTextColor by animateColorAsState(
                targetValue = if (isMoreSelected) MinimalistGold else Color.White.copy(alpha = 0.55f),
                animationSpec = tween(durationMillis = 150),
                label = "moreTextColor"
            )
            val morePillBg by animateColorAsState(
                targetValue = if (isMoreSelected) DarkCharcoal else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "morePillBg"
            )

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(morePillBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onHapticTick()
                        onOpenMoreModes()
                    }
                .padding(horizontal = 9.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "MORE",
                    color = moreTextColor,
                    fontSize = 12.sp,
                    fontWeight = if (isMoreSelected) FontWeight.Bold else FontWeight.Medium,
                    letterSpacing = 0.4.sp,
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}
