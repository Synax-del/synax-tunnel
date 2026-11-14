package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PayloadGeneratorDialog(
    initialPayload: String,
    initialSni: String,
    onDismiss: () -> Unit,
    onApplyPayload: (String) -> Unit,
) {
    var bugHost by remember { mutableStateOf(initialSni.ifBlank { "m.tiktok.com" }) }
    var method by remember { mutableStateOf("CONNECT") }
    var injectionMode by remember { mutableStateOf("Normal") }
    var addSplit by remember { mutableStateOf(false) }
    var addKeepAlive by remember { mutableStateOf(true) }
    var addUserAgent by remember { mutableStateOf(true) }
    var rawPayload by remember { mutableStateOf(initialPayload) }

    fun rebuildPayload() {
        val h = bugHost.trim().ifBlank { "[host]" }
        val lines = mutableListOf<String>()
        when (injectionMode) {
            "Front Inject" -> {
                lines += "$method http://$h/ [protocol]"
                lines += "Host: $h"
                if (addKeepAlive) lines += "Connection: Keep-Alive"
                if (addUserAgent) lines += "User-Agent: [ua]"
                val prefix = lines.joinToString("[crlf]") + "[crlf][crlf]"
                val splitTag = if (addSplit) "[split]" else ""
                rawPayload = "$prefix${splitTag}CONNECT [host_port] [protocol][crlf][crlf]"
            }
            "Back Inject" -> {
                val splitTag = if (addSplit) "[split]" else ""
                lines += "CONNECT [host_port] [protocol]"
                lines += "Host: $h"
                if (addKeepAlive) lines += "Connection: Keep-Alive"
                if (addUserAgent) lines += "User-Agent: [ua]"
                rawPayload = "${lines.joinToString("[crlf]")}[crlf][crlf]$splitTag$method http://$h/ [protocol][crlf]Host: $h[crlf][crlf]"
            }
            "WebSocket" -> {
                val splitTag = if (addSplit) "[split]" else ""
                rawPayload = "GET / HTTP/1.1[crlf]Host: $h[crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]" +
                    (if (addUserAgent) "User-Agent: [ua][crlf]" else "") +
                    "[crlf]$splitTag"
            }
            else -> {
                val splitTag = if (addSplit) "[split]" else ""
                lines += "$method [host_port] [protocol]"
                lines += "Host: $h"
                if (addKeepAlive) lines += "Connection: Keep-Alive"
                if (addUserAgent) lines += "User-Agent: [ua]"
                rawPayload = "$splitTag${lines.joinToString("[crlf]")}[crlf][crlf]"
            }
        }
    }

    val tunnelColors = LocalTunnelColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = tunnelColors.topBarContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Payload Generator",
                        style = MaterialTheme.typography.titleLarge,
                        color = tunnelColors.onTopBar,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = bugHost,
                        onValueChange = { bugHost = it },
                        label = { Text("Payload Host / Bug Host") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Text("Request Method", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("CONNECT", "GET", "POST", "HEAD").forEach { m ->
                            FilterChip(
                                selected = method == m,
                                onClick = { method = m; rebuildPayload() },
                                label = { Text(m) },
                            )
                        }
                    }

                    Text("Injection Mode", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Normal", "Front Inject", "Back Inject", "WebSocket").forEach { mode ->
                            FilterChip(
                                selected = injectionMode == mode,
                                onClick = { injectionMode = mode; rebuildPayload() },
                                label = { Text(mode) },
                            )
                        }
                    }

                    Text("Header Flags", style = MaterialTheme.typography.labelMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = addSplit,
                            onClick = { addSplit = !addSplit; rebuildPayload() },
                            label = { Text("[split]") },
                        )
                        FilterChip(
                            selected = addKeepAlive,
                            onClick = { addKeepAlive = !addKeepAlive; rebuildPayload() },
                            label = { Text("Keep-Alive") },
                        )
                        FilterChip(
                            selected = addUserAgent,
                            onClick = { addUserAgent = !addUserAgent; rebuildPayload() },
                            label = { Text("User-Agent") },
                        )
                    }

                    OutlinedTextField(
                        value = rawPayload,
                        onValueChange = { rawPayload = it },
                        label = { Text("Generated Raw Payload") },
                        textStyle = TextStyle(
                            fontFamily = JetBrainsMonoFamily,
                            fontSize = 12.sp,
                        ),
                        minLines = 4,
                        maxLines = 6,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Cancel")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Button(
                            onClick = { rebuildPayload() },
                            colors = ButtonDefaults.outlinedButtonColors(),
                        ) {
                            Text("Generate", fontWeight = FontWeight.SemiBold)
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                        Button(
                            onClick = { onApplyPayload(rawPayload) },
                            modifier = Modifier.testTag("apply_payload_button"),
                        ) {
                            Text("Apply")
                        }
                    }
                }
            }
        }
    }
}
