package com.bishal.adaptivecamera.ui.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.camera.lens.DirectPhysicalLens
import com.bishal.adaptivecamera.ui.theme.DarkCharcoal
import com.bishal.adaptivecamera.ui.theme.MinimalistGold
import com.bishal.adaptivecamera.ui.theme.PureBlack

@Composable
fun DirectLensSelectorBar(
    lenses: List<DirectPhysicalLens>,
    selectedCameraId: String?,
    onSelectLens: (DirectPhysicalLens) -> Unit,
    modifier: Modifier = Modifier
) {
    if (lenses.isEmpty() || lenses.size <= 1) return

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.Black.copy(alpha = 0.65f))
            .border(0.5.dp, MinimalistGold.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 4.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        lenses.forEach { lens ->
            val isSelected = (selectedCameraId == null && !lens.isPhysicalSubCamera) || (selectedCameraId == lens.cameraId)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isSelected) MinimalistGold else Color.Transparent)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onSelectLens(lens) }
                    .padding(horizontal = 10.dp, vertical = 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lens.label,
                    color = if (isSelected) PureBlack else Color.White.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}
