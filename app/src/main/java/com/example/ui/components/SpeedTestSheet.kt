package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors
import com.example.vpn.SpeedTestPhase
import com.example.vpn.SpeedTestState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestSheet(
    state: SpeedTestState,
    onRunTest: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val tunnelColors = LocalTunnelColors.current
    val isRunning = state.phase == SpeedTestPhase.PINGING ||
        state.phase == SpeedTestPhase.DOWNLOADING ||
        state.phase == SpeedTestPhase.UPLOADING

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Internet Speed Checker",
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
                Text(
                    text = state.serverLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Fast.com inspired radial gauge + huge live Mbps readout
            val displayMbps = when (state.phase) {
                SpeedTestPhase.UPLOADING -> state.uploadMbps.takeIf { it > 0f } ?: state.liveMbps
                else -> state.liveMbps
            }

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(210.dp),
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                val primaryColor = MaterialTheme.colorScheme.primary
                val accentColor = tunnelColors.signalGood
                val sweepFraction = ((displayMbps / 100f).coerceIn(0.05f, 1f)) * state.progress.coerceAtLeast(0.15f)

                Canvas(modifier = Modifier.size(200.dp)) {
                    val stroke = 14.dp.toPx()
                    val arcSize = Size(size.width - stroke, size.height - stroke)
                    val topLeft = Offset(stroke / 2f, stroke / 2f)

                    drawArc(
                        color = trackColor,
                        startAngle = 140f,
                        sweepAngle = 260f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawArc(
                        brush = Brush.horizontalGradient(listOf(primaryColor, accentColor)),
                        startAngle = 140f,
                        sweepAngle = 260f * sweepFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (displayMbps >= 100f) "%.0f".format(displayMbps) else "%.1f".format(displayMbps),
                        fontFamily = JetBrainsMonoFamily,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Mbps",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (state.phase) {
                            SpeedTestPhase.IDLE -> "Tap Start Test"
                            SpeedTestPhase.PINGING -> "Measuring Ping..."
                            SpeedTestPhase.DOWNLOADING -> "Testing Download..."
                            SpeedTestPhase.UPLOADING -> "Testing Upload..."
                            SpeedTestPhase.COMPLETED -> "Test Complete"
                            SpeedTestPhase.ERROR -> "Test Failed"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Metrics row: Download, Upload, Ping, Jitter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpeedMetricCard(
                    label = "DOWNLOAD",
                    value = if (state.downloadMbps > 0f) "%.1f Mbps".format(state.downloadMbps) else "--",
                    modifier = Modifier.weight(1f),
                )
                SpeedMetricCard(
                    label = "UPLOAD",
                    value = if (state.uploadMbps > 0f) "%.1f Mbps".format(state.uploadMbps) else "--",
                    modifier = Modifier.weight(1f),
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SpeedMetricCard(
                    label = "LATENCY",
                    value = state.pingMs?.let { "$it ms" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
                SpeedMetricCard(
                    label = "JITTER",
                    value = state.jitterMs?.let { "$it ms" } ?: "--",
                    modifier = Modifier.weight(1f),
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onRunTest,
                enabled = !isRunning,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("start_speed_test_button"),
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isRunning) "Testing Connection..." else "Run Speed Test")
            }
        }
    }
}

@Composable
private fun SpeedMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
