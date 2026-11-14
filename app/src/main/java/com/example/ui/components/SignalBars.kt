package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalTunnelColors

@Composable
fun SignalStrengthBars(
    latencyMs: Long?,
    modifier: Modifier = Modifier,
    width: Dp = 24.dp,
    height: Dp = 20.dp,
) {
    val tunnelColors = LocalTunnelColors.current
    val inactiveColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)

    val activeBars = when {
        latencyMs == null -> 4
        latencyMs < 90 -> 4
        latencyMs < 180 -> 3
        latencyMs < 320 -> 2
        else -> 1
    }

    val activeColor = when {
        latencyMs == null -> tunnelColors.signalGood
        latencyMs < 90 -> tunnelColors.signalGood
        latencyMs < 180 -> tunnelColors.signalMedium
        else -> tunnelColors.signalWeak
    }

    Canvas(modifier = modifier.size(width = width, height = height)) {
        val barCount = 4
        val gap = this.size.width * 0.11f
        val barWidth = (this.size.width - gap * (barCount - 1)) / barCount
        val radius = CornerRadius(barWidth * 0.28f, barWidth * 0.28f)

        for (i in 0 until barCount) {
            val fraction = (i + 1) / barCount.toFloat()
            val barHeight = this.size.height * (0.28f + 0.72f * fraction)
            val left = i * (barWidth + gap)
            val top = this.size.height - barHeight
            drawRoundRect(
                color = if (i < activeBars) activeColor else inactiveColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = radius,
            )
        }
    }
}
