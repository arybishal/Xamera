package com.bishal.adaptivecamera.ui.camera.portrait

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.photography.portrait.PortraitAperture
import com.bishal.adaptivecamera.photography.portrait.PortraitConfig
import com.bishal.adaptivecamera.photography.portrait.PortraitLightingStyle
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkSurface
import com.bishal.adaptivecamera.ui.theme.DarkSurfaceVariant
import com.bishal.adaptivecamera.ui.theme.DividerColor
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

/**
 * Ergonomic, translucent bottom control bar for Dedicated Portrait Mode.
 * Provides quick selection of simulated optical aperture f-stop and lighting styles.
 */
@Composable
fun PortraitControlBar(
    portraitConfig: PortraitConfig,
    onApertureChanged: (PortraitAperture) -> Unit,
    onLightingStyleChanged: (PortraitLightingStyle) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        DarkSurface.copy(alpha = 0.85f),
                        DarkSurface.copy(alpha = 0.95f)
                    )
                )
            )
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Row 1: Portrait Lighting Style Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortraitLightingStyle.values().forEach { style ->
                val isSelected = portraitConfig.lightingStyle == style
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (isSelected) AccentAmber.copy(alpha = 0.22f) else DarkSurfaceVariant.copy(alpha = 0.6f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AccentAmber else DividerColor,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .clickable { onLightingStyleChanged(style) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.WbSunny,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .height(12.dp)
                            )
                        }
                        Text(
                            text = style.label.uppercase(),
                            color = if (isSelected) AccentAmber else TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Row 2: Aperture f-stop Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            PortraitAperture.values().forEach { ap ->
                val isSelected = portraitConfig.aperture == ap
                Box(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) AccentAmber else DarkSurfaceVariant.copy(alpha = 0.8f)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isSelected) AccentAmber else DividerColor,
                            shape = CircleShape
                        )
                        .clickable { onApertureChanged(ap) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = ap.label,
                        color = if (isSelected) Color.Black else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))
    }
}
