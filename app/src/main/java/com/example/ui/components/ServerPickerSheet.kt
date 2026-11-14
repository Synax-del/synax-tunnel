package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.DefaultServers
import com.example.model.ConnectionConfig
import com.example.model.TunnelServer
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ServerPickerSheet(
    config: ConnectionConfig,
    latencies: Map<String, Long?>,
    isPinging: Boolean,
    onPingAll: () -> Unit,
    onSelectServer: (TunnelServer, String?, Int?) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var customHost by remember(config) { mutableStateOf(config.customHost) }
    var customPortText by remember(config) { mutableStateOf(config.customPort.toString()) }
    var editingCustom by remember { mutableStateOf(config.serverId == "custom") }
    val tunnelColors = LocalTunnelColors.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Tunnel Servers",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Select a global relay or enter a custom host",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                OutlinedButton(
                    onClick = onPingAll,
                    enabled = !isPinging,
                    modifier = Modifier.testTag("ping_all_servers_button"),
                ) {
                    if (isPinging) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pinging")
                    } else {
                        Icon(
                            imageVector = Icons.Rounded.NetworkCheck,
                            contentDescription = "Ping All",
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Ping All")
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f, fill = false),
            ) {
                items(DefaultServers.presetServers, key = { it.id }) { server ->
                    val selected = config.serverId == server.id
                    val rtt = latencies[server.id]

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (server.isCustom) {
                                    editingCustom = !editingCustom
                                } else {
                                    onSelectServer(server, null, null)
                                    onDismiss()
                                }
                            }
                            .testTag("server_item_${server.id}"),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                CircularFlagIcon(flagCode = server.flagCode, size = 40.dp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = server.code,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                        )
                                        if (selected) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Rounded.CheckCircle,
                                                contentDescription = "Selected",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                    Text(
                                        text = if (server.isCustom) "Custom Host: ${config.customHost}:${config.customPort}" else "${server.country} • ${server.city}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        SignalStrengthBars(latencyMs = rtt, width = 20.dp, height = 16.dp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (rtt != null) "${rtt}ms" else "${config.protocol.shortCode} ${server.port}",
                                            fontFamily = JetBrainsMonoFamily,
                                            style = MaterialTheme.typography.labelMedium,
                                            color = when {
                                                rtt == null -> MaterialTheme.colorScheme.onSurface
                                                rtt < 90 -> tunnelColors.signalGood
                                                rtt < 180 -> tunnelColors.signalMedium
                                                else -> tunnelColors.signalWeak
                                            },
                                        )
                                    }
                                    if (!server.isCustom) {
                                        Text(
                                            text = "Load ${server.loadPercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }

                            AnimatedVisibility(visible = server.isCustom && editingCustom) {
                                Column(modifier = Modifier.padding(top = 12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = customHost,
                                            onValueChange = { customHost = it },
                                            label = { Text("Server Host / IP") },
                                            singleLine = true,
                                            modifier = Modifier.weight(2f),
                                        )
                                        OutlinedTextField(
                                            value = customPortText,
                                            onValueChange = { customPortText = it.filter(Char::isDigit).take(5) },
                                            label = { Text("Port") },
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        listOf(443, 80, 8443, 8080, 53).forEach { p ->
                                            FilterChip(
                                                selected = customPortText == p.toString(),
                                                onClick = { customPortText = p.toString() },
                                                label = { Text("$p") },
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Button(
                                        onClick = {
                                            val port = customPortText.toIntOrNull()?.coerceIn(1, 65535) ?: 443
                                            onSelectServer(server, customHost.trim().ifBlank { "1.1.1.1" }, port)
                                            onDismiss()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text("Use Custom Server")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
