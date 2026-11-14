package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.ui.ToolsUiState
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsScreen(
    state: ToolsUiState,
    onHostQueryChange: (String) -> Unit,
    onHostPortChange: (Int) -> Unit,
    onHostMethodChange: (String) -> Unit,
    onRunHostCheck: () -> Unit,
    onScanHostChange: (String) -> Unit,
    onRunPortScan: () -> Unit,
    onTargetPrefixChange: (String) -> Unit,
    onRefreshIp: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedSubTab by remember { mutableIntStateOf(0) }
    val subTabs = listOf("Host Checker", "IP Hunter", "Port Scanner")
    val tunnelColors = LocalTunnelColors.current

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedSubTab) {
            subTabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedSubTab == index,
                    onClick = { selectedSubTab = index },
                    text = { Text(title, style = MaterialTheme.typography.labelLarge) },
                )
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            when (selectedSubTab) {
                0 -> {
                    // Host / SNI Checker
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "SNI & HTTP Host Checker",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Performs a real DNS + TLS SNI handshake and HTTP response probe",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            OutlinedTextField(
                                value = state.hostQuery,
                                onValueChange = onHostQueryChange,
                                label = { Text("Domain / Bug Host") },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("host_checker_input"),
                            )

                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(443, 80, 8443).forEach { p ->
                                    FilterChip(
                                        selected = state.hostPort == p,
                                        onClick = { onHostPortChange(p) },
                                        label = { Text("Port $p") },
                                    )
                                }
                                listOf("HEAD", "GET").forEach { m ->
                                    FilterChip(
                                        selected = state.hostMethod == m,
                                        onClick = { onHostMethodChange(m) },
                                        label = { Text(m) },
                                    )
                                }
                            }

                            Button(
                                onClick = onRunHostCheck,
                                enabled = !state.isCheckingHost,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_host_check_button"),
                            ) {
                                if (state.isCheckingHost) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Probing Host...")
                                } else {
                                    Icon(Icons.Rounded.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Check Host & SNI")
                                }
                            }
                        }
                    }

                    state.hostResult?.let { res ->
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (res.error == null) Icons.Rounded.CheckCircle else Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (res.error == null) tunnelColors.signalGood else tunnelColors.signalWeak,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = res.host,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                                HorizontalDivider()
                                DiagnosticRow("Resolved IP", res.resolvedIp ?: "Unresolved")
                                DiagnosticRow("HTTP Status", res.httpStatusCode?.toString() ?: "N/A")
                                DiagnosticRow("Server Header", res.serverHeader ?: "Hidden")
                                DiagnosticRow("TLS Protocol", res.tlsVersion ?: "N/A")
                                DiagnosticRow("Cipher Suite", res.tlsCipher ?: "N/A")
                                DiagnosticRow("Latency (RTT)", "${res.rttMs} ms")
                                if (res.error != null) {
                                    Text(
                                        text = "Detail: ${res.error}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // IP Hunter
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "Local & Public IP Hunter",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "Inspect active carrier/Wi-Fi interface IPs and match against target subnet prefixes",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )

                            OutlinedTextField(
                                value = state.targetIpPrefix,
                                onValueChange = onTargetPrefixChange,
                                label = { Text("Target IP Prefix (e.g. 10.0. or 100.64.)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                                modifier = Modifier.fillMaxWidth(),
                            )

                            val matched = state.localInterfaces.any { (_, ip) ->
                                state.targetIpPrefix.isNotBlank() && ip.startsWith(state.targetIpPrefix.trim())
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (matched) {
                                    tunnelColors.signalGood.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = if (matched) "PREFIX MATCH FOUND" else "NO PREFIX MATCH YET",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = if (matched) tunnelColors.signalGood else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = "Public: ${state.publicIp ?: "..."}",
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }

                            Button(
                                onClick = onRefreshIp,
                                enabled = !state.isHuntingIp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("refresh_ip_hunter_button"),
                            ) {
                                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (state.isHuntingIp) "Scanning Interfaces..." else "Refresh Device IPs")
                            }

                            if (state.localInterfaces.isNotEmpty()) {
                                HorizontalDivider()
                                state.localInterfaces.forEach { (iface, ip) ->
                                    DiagnosticRow(iface, ip)
                                }
                            }
                        }
                    }
                }

                else -> {
                    // Port Scanner
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Text(
                                text = "TCP Port Scanner",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            OutlinedTextField(
                                value = state.scanHostQuery,
                                onValueChange = onScanHostChange,
                                label = { Text("Target Host / IP") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Button(
                                onClick = onRunPortScan,
                                enabled = !state.isScanningPorts,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_port_scan_button"),
                            ) {
                                if (state.isScanningPorts) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scanning Ports...")
                                } else {
                                    Icon(Icons.Rounded.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Scan Tunnel Ports")
                                }
                            }
                        }
                    }

                    if (state.portScanResults.isNotEmpty()) {
                        Card(
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                state.portScanResults.forEach { portRes ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Column {
                                            Text(
                                                text = "Port ${portRes.port}",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Text(
                                                text = portRes.serviceName,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (portRes.isOpen) {
                                                tunnelColors.signalGood.copy(alpha = 0.14f)
                                            } else {
                                                MaterialTheme.colorScheme.surfaceVariant
                                            },
                                        ) {
                                            Text(
                                                text = if (portRes.isOpen) "OPEN (${portRes.rttMs}ms)" else "CLOSED",
                                                fontFamily = JetBrainsMonoFamily,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (portRes.isOpen) tunnelColors.signalGood else MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                            )
                                        }
                                    }
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
                                }
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open DNS, MTU & Split Tunneling Settings")
            }
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            fontFamily = JetBrainsMonoFamily,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
