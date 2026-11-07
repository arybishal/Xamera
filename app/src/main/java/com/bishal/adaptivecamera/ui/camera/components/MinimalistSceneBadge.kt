package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.intelligence.scene.SceneCategory
import com.bishal.adaptivecamera.photography.auto.SmartAutoState
import com.bishal.adaptivecamera.ui.theme.MinimalistGold

/**
 * Minimalist floating scene recommendation badge inspired by the reference screenshot (e.g. [🌙 1s]).
 * Renders an amber-gold pill containing the scene icon and recommended exposure/mode parameter.
 */
@Composable
fun MinimalistSceneBadge(
    autoState: SmartAutoState,
    isDismissed: Boolean,
    onApplySuggestion: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sceneResult = autoState.sceneResult
    val category = sceneResult?.primaryCategory ?: SceneCategory.STANDARD
    val isLowLight = autoState.latestMetrics?.isLowLight == true

    val shouldShow = !isDismissed && (
        category != SceneCategory.STANDARD || isLowLight
    )

    AnimatedVisibility(
        visible = shouldShow,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        val (icon, label) = when {
            category == SceneCategory.NIGHT || isLowLight -> {
                Icons.Default.NightsStay to "1s"
            }
            category == SceneCategory.PORTRAIT -> {
                Icons.Default.Face to "Portrait"
            }
            category == SceneCategory.LANDSCAPE -> {
                Icons.Default.Landscape to "Landscape"
            }
            category == SceneCategory.ACTION_MOVEMENT -> {
                Icons.Default.Warning to "Action"
            }
            category == SceneCategory.BACKLIT -> {
                Icons.Default.AutoAwesome to "HDR"
            }
            category == SceneCategory.MACRO -> {
                Icons.Default.AutoAwesome to "Macro"
            }
            else -> {
                Icons.Default.AutoAwesome to "Auto"
            }
        }

        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MinimalistGold)
                .clickable(onClick = onApplySuggestion)
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.Black,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                color = Color.Black,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
