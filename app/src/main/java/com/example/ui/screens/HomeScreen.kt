package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ListAlt
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.DefaultServers
import com.example.model.AdminNotificationMessage
import com.example.model.ConnectionConfig
import com.example.model.TunnelProtocol
import com.example.model.TunnelStatus
import com.example.model.TunnelTelemetry
import com.example.ui.components.CircularFlagIcon
import com.example.ui.components.SignalStrengthBars
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    config: ConnectionConfig,
    telemetry: TunnelTelemetry,
    serverLatencies: Map<String, Long?>,
    remainingTimeSeconds: Long,
    activeAnnouncement: AdminNotificationMessage?,
    onOpenServerPicker: () -> Unit,
    onOpenConnectionMethod: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenAddTime: () -> Unit,
    onDismissAnnouncement: (String) -> Unit,
    onOpenAnnouncementUrl: (String) -> Unit,
    onStartOrStop: () -> Unit,
    onReconnect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val server = remember(config.serverId) { DefaultServers.findById(config.serverId) }
    val displayPort = if (server.isCustom) {
        config.customPort
    } else if (config.protocol == TunnelProtocol.UDP && server.port == 443) {
        53
    } else {
        server.port
    }
    val serverLatency = serverLatencies[server.id]
    val tunnelColors = LocalTunnelColors.current
    val isActive = telemetry.status == TunnelStatus.CONNECTED ||
        telemetry.status == TunnelStatus.CONNECTING ||
        telemetry.status == TunnelStatus.RECONNECTING
    val isConnecting = telemetry.status == TunnelStatus.CONNECTING ||
        telemetry.status == TunnelStatus.RECONNECTING

    // Live elapsed timer tick
    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(telemetry.status, telemetry.connectedSinceMs) {
        while (telemetry.status == TunnelStatus.CONNECTED && telemetry.connectedSinceMs > 0L) {
            nowMs = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val elapsedSec = if (telemetry.status == TunnelStatus.CONNECTED && telemetry.connectedSinceMs > 0L) {
        ((nowMs - telemetry.connectedSinceMs) / 1000L).coerceAtLeast(0L)
    } else {
        0L
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Optional Admin Broadcast Notification Banner
        AnimatedVisibility(visible = activeAnnouncement != null) {
            activeAnnouncement?.let { notice ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("admin_announcement_banner"),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Campaign,
                            contentDescription = "Announcement",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = notice.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = notice.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (notice.actionUrl.isNotBlank()) {
                                TextButton(
                                    onClick = { onOpenAnnouncementUrl(notice.actionUrl) },
                                    modifier = Modifier.padding(top = 2.dp),
                                ) {
                                    Text("Open Link")
                                }
                            }
                        }
                        IconButton(
                            onClick = { onDismissAnnouncement(notice.id) },
                            modifier = Modifier.size(32.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Dismiss announcement",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }

        // Card 1: SERVER & PORT + START/STOP/RECONNECT Bar (Signature Reference Layout)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "SERVER",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "PORT",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Interactive Server & Port Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onOpenServerPicker)
                        .padding(vertical = 4.dp)
                        .testTag("server_selector_row"),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularFlagIcon(flagCode = server.flagCode, size = 44.dp)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = server.code,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = if (server.isCustom) config.customHost else server.country,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        SignalStrengthBars(latencyMs = serverLatency)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${config.protocol.shortCode} $displayPort",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = JetBrainsMonoFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Divider / Progress Line
                if (isConnecting) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.5.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    )
                } else {
                    val barColor by animateColorAsState(
                        targetValue = when (telemetry.status) {
                            TunnelStatus.CONNECTED -> MaterialTheme.colorScheme.primary
                            TunnelStatus.ERROR -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
                        },
                        label = "status_bar_color",
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(barColor),
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Primary START / STOP Pill + Integrated Crimson Reconnect Button
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clickable(onClick = onStartOrStop)
                                .padding(horizontal = 14.dp)
                                .testTag("vpn_start_stop_button"),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = if (isActive) Icons.Rounded.Stop else Icons.Rounded.PlayArrow,
                                contentDescription = if (isActive) "Stop VPN" else "Start VPN",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(22.dp),
                            )
                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = when (telemetry.status) {
                                        TunnelStatus.CONNECTED -> "STOP"
                                        TunnelStatus.CONNECTING -> "CONNECTING..."
                                        TunnelStatus.RECONNECTING -> "RECONNECTING..."
                                        else -> "START"
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                            }
                        }

                        AnimatedVisibility(visible = isActive) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = tunnelColors.reconnectButton,
                                modifier = Modifier
                                    .width(54.dp)
                                    .height(48.dp)
                                    .clickable(onClick = onReconnect)
                                    .testTag("vpn_reconnect_button"),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = "Reconnect Tunnel",
                                        tint = Color.White,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // HA Tunnel Plus Style Session Timer & + Add Time Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(
                width = if (remainingTimeSeconds <= 0L) 1.dp else 0.5.dp,
                color = if (remainingTimeSeconds <= 0L) {
                    MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                } else {
                    MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                },
            ),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = CircleShape,
                    color = if (remainingTimeSeconds > 0L) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.errorContainer
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTimeFilled,
                            contentDescription = "Session Time Remaining",
                            tint = if (remainingTimeSeconds > 0L) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.error
                            },
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "TUNNEL TIME REMAINING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDuration(remainingTimeSeconds),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (remainingTimeSeconds > 0L) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.testTag("remaining_time_text"),
                    )
                }
                FilledTonalButton(
                    onClick = onOpenAddTime,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier.testTag("open_add_time_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Time", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Card 2: CONNECTION METHOD Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClick = onOpenConnectionMethod),
                    ) {
                        Text(
                            text = "CONNECTION METHOD",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = config.protocol.label,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }

                    IconButton(
                        onClick = onOpenConnectionMethod,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_connection_method_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Edit Connection Method",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }

                    IconButton(
                        onClick = onOpenPresets,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("open_presets_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ListAlt,
                            contentDescription = "Open Saved Configs",
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (config.protocol == TunnelProtocol.TCP && config.useSniHost && config.sniHost.isNotBlank()) {
                        AssistChip(
                            onClick = onOpenConnectionMethod,
                            label = {
                                Text(
                                    text = "SNI: ${config.sniHost}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                    if (config.protocol == TunnelProtocol.TCP && config.usePayload) {
                        AssistChip(
                            onClick = onOpenConnectionMethod,
                            label = { Text("Payload Active", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                    if (config.protocol == TunnelProtocol.TCP && config.useProxy && config.proxyHost.isNotBlank()) {
                        AssistChip(
                            onClick = onOpenConnectionMethod,
                            label = {
                                Text(
                                    "Proxy: ${config.proxyHost}:${config.proxyPort}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                        )
                    }
                    if (config.obfuscatedTlsCh) {
                        AssistChip(
                            onClick = onOpenConnectionMethod,
                            label = { Text("TLS-CH Obfs", style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                    AssistChip(
                        onClick = onOpenConnectionMethod,
                        label = {
                            Text(
                                text = "DNS ${config.primaryDns}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                    )
                }
            }
        }

        // Card 3: Live Tunnel Telemetry & Handshake Info
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val badgeColor = when (telemetry.status) {
                            TunnelStatus.CONNECTED -> tunnelColors.signalGood
                            TunnelStatus.CONNECTING, TunnelStatus.RECONNECTING -> tunnelColors.signalMedium
                            TunnelStatus.ERROR -> tunnelColors.signalWeak
                            TunnelStatus.DISCONNECTED -> MaterialTheme.colorScheme.outline
                        }
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(badgeColor),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = telemetry.statusDetail,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Text(
                        text = formatDuration(elapsedSec),
                        fontFamily = JetBrainsMonoFamily,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                // Speed & Byte Counters
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TelemetryTile(
                        title = "DOWNLOAD",
                        value = formatSpeed(telemetry.rxKbps),
                        subtitle = formatBytes(telemetry.rxBytes),
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowDownward,
                                contentDescription = "Download",
                                tint = tunnelColors.signalGood,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                    TelemetryTile(
                        title = "UPLOAD",
                        value = formatSpeed(telemetry.txKbps),
                        subtitle = formatBytes(telemetry.txBytes),
                        icon = {
                            Icon(
                                imageVector = Icons.Rounded.ArrowUpward,
                                contentDescription = "Upload",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        modifier = Modifier.weight(1f),
                    )
                }

                // Handshake & Routing Metadata
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Shield,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = buildString {
                                    append(if (telemetry.tlsProtocol.isNotBlank()) telemetry.tlsProtocol else config.protocol.name)
                                    if (telemetry.handshakeMs != null) append(" • ${telemetry.handshakeMs}ms")
                                    if (telemetry.resolvedIp.isNotBlank()) append(" • ${telemetry.resolvedIp}")
                                },
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = "TUN ${telemetry.localTunIp}",
                            fontFamily = JetBrainsMonoFamily,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryTile(
    title: String,
    value: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Total $subtitle",
                fontFamily = JetBrainsMonoFamily,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

fun formatSpeed(kbps: Float): String {
    return if (kbps >= 1000f) {
        "%.2f Mbps".format(kbps / 1000f)
    } else {
        "%.1f kbps".format(kbps)
    }
}

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024f)
        bytes < 1024 * 1024 * 1024 -> "%.2f MB".format(bytes / (1024f * 1024f))
        else -> "%.2f GB".format(bytes / (1024f * 1024f * 1024f))
    }
}
