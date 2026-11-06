package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bishal.adaptivecamera.photography.pro.ProAppliedState
import com.bishal.adaptivecamera.photography.pro.ProConfiguration
import com.bishal.adaptivecamera.ui.theme.AccentAmber
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.SuccessGreen
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@Composable
fun ProAppliedTelemetryBadge(
    appliedState: ProAppliedState,
    proConfig: ProConfiguration,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(DarkBackground.copy(alpha = 0.75f))
            .border(1.dp, PrimaryCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // ISO
            Text(
                text = appliedState.appliedIsoDisplay,
                color = if (proConfig.isManualIso) PrimaryCyan else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (proConfig.isManualIso) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
            Text("·", color = TextMuted, fontSize = 10.sp)

            // Shutter
            Text(
                text = appliedState.appliedShutterDisplay,
                color = if (proConfig.isManualShutter) PrimaryCyan else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (proConfig.isManualShutter) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )
            Text("·", color = TextMuted, fontSize = 10.sp)

            // Aperture & Focal length
            appliedState.aperture?.let { ap ->
                Text("f/$ap", color = TextSecondary, fontSize = 10.sp, fontFamily = FontFamily.Monospace)
                Text("·", color = TextMuted, fontSize = 10.sp)
            }

            // Focus
            Text(
                text = if (proConfig.isManualFocus) "MF: ${appliedState.appliedFocusDistanceDisplay}"
                       else if (proConfig.isAfLocked) "AF-LOCKED"
                       else "AF",
                color = if (proConfig.isManualFocus || proConfig.isAfLocked) AccentAmber else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (proConfig.isManualFocus || proConfig.isAfLocked) FontWeight.Bold else FontWeight.Normal
            )

            // White Balance / Kelvin
            Text("·", color = TextMuted, fontSize = 10.sp)
            Text(
                text = appliedState.appliedWbDisplay,
                color = if (proConfig.isManualKelvin || proConfig.isAwbLocked) PrimaryCyan else TextSecondary,
                fontSize = 10.sp,
                fontWeight = if (proConfig.isManualKelvin || proConfig.isAwbLocked) FontWeight.Bold else FontWeight.Normal,
                fontFamily = FontFamily.Monospace
            )

            // AE Lock Badge
            if (proConfig.isAeLocked) {
                Text("·", color = TextMuted, fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(AccentAmber.copy(alpha = 0.25f))
                        .border(0.5.dp, AccentAmber, RoundedCornerShape(3.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text("AE-LOCK", color = AccentAmber, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }

            // RAW Badge
            if (proConfig.isRawCaptureEnabled) {
                Text("·", color = TextMuted, fontSize = 10.sp)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(SuccessGreen.copy(alpha = 0.2f))
                        .border(1.dp, SuccessGreen, RoundedCornerShape(3.dp))
                        .padding(horizontal = 3.dp, vertical = 1.dp)
                ) {
                    Text("RAW+JPG", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
