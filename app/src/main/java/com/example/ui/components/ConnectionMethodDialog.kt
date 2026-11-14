package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ConnectionConfig
import com.example.model.TunnelProtocol
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectionMethodDialog(
    initialConfig: ConnectionConfig,
    onDismiss: () -> Unit,
    onSave: (ConnectionConfig) -> Unit,
) {
    var draft by remember(initialConfig) { mutableStateOf(initialConfig) }
    var proxyPortText by remember(initialConfig) { mutableStateOf(initialConfig.proxyPort.toString()) }
    var showPayloadGenerator by remember { mutableStateOf(false) }

    val tunnelColors = LocalTunnelColors.current

    if (showPayloadGenerator) {
        PayloadGeneratorDialog(
            initialPayload = draft.payload,
            initialSni = draft.sniHost,
            onDismiss = { showPayloadGenerator = false },
            onApplyPayload = { generated ->
                draft = draft.copy(
                    protocol = TunnelProtocol.TCP,
                    usePayload = true,
                    payload = generated,
                )
                showPayloadGenerator = false
            },
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header bar matching the reference screenshot: < > | Connection Method | Save icon
                Surface(
                    color = tunnelColors.topBarContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(
                            onClick = { showPayloadGenerator = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("open_payload_generator_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Code,
                                contentDescription = "Open Payload Generator",
                                tint = tunnelColors.onTopBar,
                            )
                        }

                        Text(
                            text = "Connection Method",
                            style = MaterialTheme.typography.titleLarge,
                            color = tunnelColors.onTopBar,
                            modifier = Modifier.weight(1f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        )

                        IconButton(
                            onClick = {
                                val parsedPort = proxyPortText.toIntOrNull()?.coerceIn(1, 65535) ?: 8080
                                onSave(draft.copy(proxyPort = parsedPort))
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("save_connection_method_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Save,
                                contentDescription = "Save Connection Method",
                                tint = tunnelColors.onTopBar,
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 18.dp),
                ) {
                    // Segmented TCP | UDP Toggle Pill
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        color = MaterialTheme.colorScheme.surface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                    ) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            TunnelProtocol.entries.forEachIndexed { index, proto ->
                                val selected = draft.protocol == proto
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(
                                            if (index == 0) {
                                                RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp)
                                            } else {
                                                RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp)
                                            },
                                        )
                                        .background(
                                            if (selected) tunnelColors.segmentSelected else MaterialTheme.colorScheme.surface,
                                        )
                                        .clickable { draft = draft.copy(protocol = proto) }
                                        .testTag("protocol_segment_${proto.name.lowercase()}"),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = proto.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (draft.protocol == TunnelProtocol.TCP) {
                        MethodCheckboxRow(
                            label = "Prefer IPV6",
                            checked = draft.preferIpv6,
                            testTag = "checkbox_prefer_ipv6",
                            onCheckedChange = { draft = draft.copy(preferIpv6 = it) },
                        )
                        MethodCheckboxRow(
                            label = "TCP Fast Open",
                            checked = draft.tcpFastOpen,
                            testTag = "checkbox_tcp_fast_open",
                            onCheckedChange = { draft = draft.copy(tcpFastOpen = it) },
                        )
                        MethodCheckboxRow(
                            label = "Obfuscated TLS-CH",
                            checked = draft.obfuscatedTlsCh,
                            testTag = "checkbox_obfuscated_tls",
                            onCheckedChange = { draft = draft.copy(obfuscatedTlsCh = it) },
                        )
                        MethodCheckboxRow(
                            label = "Use Payload",
                            checked = draft.usePayload,
                            testTag = "checkbox_use_payload",
                            onCheckedChange = { draft = draft.copy(usePayload = it) },
                        )

                        AnimatedVisibility(visible = draft.usePayload) {
                            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                OutlinedTextField(
                                    value = draft.payload,
                                    onValueChange = { draft = draft.copy(payload = it) },
                                    label = { Text("Custom Payload") },
                                    textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 12.sp),
                                    minLines = 2,
                                    maxLines = 4,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_custom_payload"),
                                )
                            }
                        }

                        MethodCheckboxRow(
                            label = "Use SNI Host",
                            checked = draft.useSniHost,
                            testTag = "checkbox_use_sni_host",
                            onCheckedChange = { draft = draft.copy(useSniHost = it) },
                        )

                        AnimatedVisibility(visible = draft.useSniHost) {
                            Column(modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)) {
                                OutlinedTextField(
                                    value = draft.sniHost,
                                    onValueChange = { draft = draft.copy(sniHost = it) },
                                    label = { Text("SNI Host") },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                                        focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("input_sni_host"),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    listOf("m.tiktok.com", "v.whatsapp.net", "speed.cloudflare.com").forEach { preset ->
                                        FilterChip(
                                            selected = draft.sniHost == preset,
                                            onClick = { draft = draft.copy(sniHost = preset) },
                                            label = { Text(preset, style = MaterialTheme.typography.labelSmall) },
                                        )
                                    }
                                }
                            }
                        }

                        MethodCheckboxRow(
                            label = "Use Payload after TLS",
                            checked = draft.usePayloadAfterTls,
                            testTag = "checkbox_payload_after_tls",
                            onCheckedChange = { draft = draft.copy(usePayloadAfterTls = it) },
                        )

                        AnimatedVisibility(visible = draft.usePayloadAfterTls) {
                            OutlinedTextField(
                                value = draft.payloadAfterTls,
                                onValueChange = { draft = draft.copy(payloadAfterTls = it) },
                                label = { Text("Post-TLS Payload") },
                                textStyle = TextStyle(fontFamily = JetBrainsMonoFamily, fontSize = 12.sp),
                                minLines = 2,
                                maxLines = 4,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 10.dp),
                            )
                        }

                        MethodCheckboxRow(
                            label = "Use Proxy",
                            checked = draft.useProxy,
                            testTag = "checkbox_use_proxy",
                            onCheckedChange = { draft = draft.copy(useProxy = it) },
                        )

                        AnimatedVisibility(visible = draft.useProxy) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = draft.proxyHost,
                                    onValueChange = { draft = draft.copy(proxyHost = it) },
                                    label = { Text("Proxy Host / IP") },
                                    singleLine = true,
                                    modifier = Modifier.weight(2f),
                                )
                                OutlinedTextField(
                                    value = proxyPortText,
                                    onValueChange = { proxyPortText = it.filter(Char::isDigit).take(5) },
                                    label = { Text("Port") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    } else {
                        // UDP Mode options
                        MethodCheckboxRow(
                            label = "Prefer IPV6",
                            checked = draft.preferIpv6,
                            testTag = "checkbox_udp_ipv6",
                            onCheckedChange = { draft = draft.copy(preferIpv6 = it) },
                        )
                        MethodCheckboxRow(
                            label = "UDP Forwarding (UDPGW)",
                            checked = draft.udpForwarding,
                            testTag = "checkbox_udp_forwarding",
                            onCheckedChange = { draft = draft.copy(udpForwarding = it) },
                        )
                        MethodCheckboxRow(
                            label = "DNS Forwarding (Port 53)",
                            checked = draft.dnsForwarding,
                            testTag = "checkbox_dns_forwarding",
                            onCheckedChange = { draft = draft.copy(dnsForwarding = it) },
                        )
                        MethodCheckboxRow(
                            label = "QUIC / Hysteria Obfuscation",
                            checked = draft.quicObfuscation,
                            testTag = "checkbox_quic_obfuscation",
                            onCheckedChange = { draft = draft.copy(quicObfuscation = it) },
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "UDP Tunnel MTU",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(1280, 1360, 1400, 1500).forEach { mtuOption ->
                                FilterChip(
                                    selected = draft.mtu == mtuOption,
                                    onClick = { draft = draft.copy(mtu = mtuOption) },
                                    label = { Text("$mtuOption B") },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MethodCheckboxRow(
    label: String,
    checked: Boolean,
    testTag: String,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
