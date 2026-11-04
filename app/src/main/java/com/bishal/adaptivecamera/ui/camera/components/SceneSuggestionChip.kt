package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.bishal.adaptivecamera.intelligence.scene.SceneCategory
import com.bishal.adaptivecamera.photography.auto.SmartAutoState
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@Composable
fun SceneSuggestionChip(
    autoState: SmartAutoState,
    isDismissed: Boolean,
    onApplySuggestion: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sceneResult = autoState.sceneResult ?: return
    val suggestion = autoState.configSuggestion ?: return

    // Do not show chip if dismissed or if it's purely a standard scene with 0 EV bias
    if (isDismissed || (sceneResult.primaryCategory == SceneCategory.STANDARD && suggestion.recommendedEvIndex == 0)) {
        return
    }

    var isExpanded by remember { mutableStateOf(false) }

    val accentColor = when (sceneResult.primaryCategory) {
        SceneCategory.NIGHT -> PrimaryCyan
        SceneCategory.BACKLIT -> AccentAmber
        SceneCategory.ACTION_MOVEMENT -> Color(0xFFFF5252)
        SceneCategory.PORTRAIT -> Color(0xFFFF80AB)
        SceneCategory.LANDSCAPE -> SuccessGreen
        SceneCategory.MACRO -> Color(0xFFB388FF)
        SceneCategory.STANDARD -> PrimaryCyan
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(DarkBackground.copy(alpha = 0.85f))
            .border(1.dp, accentColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .clickable { isExpanded = !isExpanded }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = sceneResult.primaryCategory.iconEmoji,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = sceneResult.primaryCategory.displayName,
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            // Confidence badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.2f))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "${(sceneResult.confidence * 100).toInt()}%",
                                    color = accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Text(
                            text = suggestion.actionLabel,
                            color = accentColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Action Apply Button
                    Button(
                        onClick = onApplySuggestion,
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = ButtonDefaults.ContentPadding,
                        modifier = Modifier
                            .height(30.dp)
                            .padding(end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Apply",
                            color = Color.Black,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Dismiss Button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Dismiss",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            // Expanded Explanation & Telemetry Details
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        text = suggestion.summaryRationale,
                        color = TextPrimary,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )

                    autoState.latestMetrics?.let { metrics ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            MetricItem("Luma", "${(metrics.meanLuminance * 100).toInt()}%")
                            MetricItem("Contrast", String.format("%.2f", metrics.rmsContrast))
                            MetricItem("Shadows", "${(metrics.shadowRatio * 100).toInt()}%")
                            MetricItem("Dynamic Range", "${String.format("%.1f", metrics.estimatedDynamicRangeStops)} stops")
                        }
                    }

                    autoState.lensRecommendation?.let { lens ->
                        if (lens.requiresLensSwitch) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "💡 ${lens.reason}",
                                color = AccentAmber,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = PrimaryCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Text(label, color = TextSecondary, fontSize = 9.sp)
    }
}
