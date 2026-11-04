package com.bishal.adaptivecamera.ui.camera.pro

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bishal.adaptivecamera.photography.pro.HistogramData
import com.bishal.adaptivecamera.ui.theme.DarkBackground
import com.bishal.adaptivecamera.ui.theme.ErrorRed
import com.bishal.adaptivecamera.ui.theme.PrimaryCyan
import com.bishal.adaptivecamera.ui.theme.TextMuted
import com.bishal.adaptivecamera.ui.theme.TextPrimary
import com.bishal.adaptivecamera.ui.theme.TextSecondary

@Composable
fun HistogramView(
    data: HistogramData,
    isExpanded: Boolean,
    onToggleExpanded: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DarkBackground.copy(alpha = 0.85f))
            .border(1.dp, PrimaryCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
            .clickable { onToggleExpanded() }
            .padding(6.dp)
    ) {
        if (!isExpanded) {
            // Compact Mode: Single combined RGB/Luma waveform graph
            Column(modifier = Modifier.width(110.dp).height(50.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("HIST (Preview)", color = TextMuted, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                    if (data.highlightClippingRatio > 0.05f) {
                        Text("CLIP", color = ErrorRed, fontSize = 7.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawHistogramPath(data.rBins, Color(0xFFFF4444).copy(alpha = 0.6f))
                    drawHistogramPath(data.gBins, Color(0xFF00E676).copy(alpha = 0.6f))
                    drawHistogramPath(data.bBins, Color(0xFF2979FF).copy(alpha = 0.6f))
                    drawHistogramPath(data.lumaBins, Color.White.copy(alpha = 0.85f))
                }
            }
        } else {
            // Expanded Mode: Individual R, G, B, Luma channels + clipping indicators
            Column(modifier = Modifier.width(220.dp).padding(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("LIVE RGB HISTOGRAM", color = PrimaryCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text("Tap to collapse", color = TextMuted, fontSize = 8.sp)
                }

                Spacer(modifier = Modifier.height(6.dp))
                HistogramChannelRow("Luminance", data.lumaBins, Color.White)
                HistogramChannelRow("Red", data.rBins, Color(0xFFFF5252))
                HistogramChannelRow("Green", data.gBins, Color(0xFF00E676))
                HistogramChannelRow("Blue", data.bBins, Color(0xFF448AFF))

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Shadows: ${(data.shadowClippingRatio * 100).toInt()}%",
                        color = if (data.shadowClippingRatio > 0.15f) ErrorRed else TextSecondary,
                        fontSize = 8.sp
                    )
                    Text(
                        text = "Highlights: ${(data.highlightClippingRatio * 100).toInt()}%",
                        color = if (data.highlightClippingRatio > 0.10f) ErrorRed else TextSecondary,
                        fontSize = 8.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun HistogramChannelRow(
    label: String,
    bins: FloatArray,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(24.dp)
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label.take(1),
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(14.dp)
        )
        Canvas(modifier = Modifier.weight(1f).fillMaxHeight()) {
            drawHistogramPath(bins, color)
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHistogramPath(
    bins: FloatArray,
    color: Color
) {
    if (bins.isEmpty()) return
    val path = Path()
    val widthStep = size.width / (bins.size - 1).toFloat()

    bins.forEachIndexed { i, normValue ->
        val x = i * widthStep
        val y = size.height - (normValue.coerceIn(0f, 1f) * size.height)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }

    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round)
    )
}
