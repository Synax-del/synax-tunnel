package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.LogLevel
import com.example.model.TunnelLogEntry
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LogBgDark
import com.example.ui.theme.LogDnsColor
import com.example.ui.theme.LogErrorColor
import com.example.ui.theme.LogInfoColor
import com.example.ui.theme.LogTlsColor
import com.example.ui.theme.LogTunColor
import com.example.ui.theme.LogWarnColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LogsScreen(
    logs: List<TunnelLogEntry>,
    onClearLogs: () -> Unit,
    onCopiedToast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedFilter by remember { mutableStateOf<LogLevel?>(null) }
    val clipboard = LocalClipboardManager.current
    val listState = rememberLazyListState()

    val filteredLogs = remember(logs, selectedFilter) {
        if (selectedFilter == null) logs else logs.filter { it.level == selectedFilter }
    }

    LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
            listState.animateScrollToItem(filteredLogs.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Tunnel Event Log (${filteredLogs.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        val text = filteredLogs.joinToString("\n") { "[${it.timestamp}] [${it.level.tag}] ${it.message}" }
                        clipboard.setText(AnnotatedString(text))
                        onCopiedToast()
                    },
                    modifier = Modifier.testTag("copy_logs_button"),
                ) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Copy")
                }
                OutlinedButton(
                    onClick = onClearLogs,
                    modifier = Modifier.testTag("clear_logs_button"),
                ) {
                    Icon(Icons.Outlined.DeleteSweep, contentDescription = "Clear logs", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear")
                }
            }
        }

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            FilterChip(
                selected = selectedFilter == null,
                onClick = { selectedFilter = null },
                label = { Text("All") },
            )
            listOf(LogLevel.TLS, LogLevel.DNS, LogLevel.TUN, LogLevel.ERROR).forEach { lvl ->
                FilterChip(
                    selected = selectedFilter == lvl,
                    onClick = { selectedFilter = if (selectedFilter == lvl) null else lvl },
                    label = { Text(lvl.tag) },
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = LogBgDark,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            if (filteredLogs.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "No matching log events",
                        fontFamily = JetBrainsMonoFamily,
                        color = Color(0xFF94A8A5),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    items(filteredLogs, key = { it.id }) { entry ->
                        val levelColor = when (entry.level) {
                            LogLevel.INFO -> LogInfoColor
                            LogLevel.TLS -> LogTlsColor
                            LogLevel.DNS -> LogDnsColor
                            LogLevel.TUN -> LogTunColor
                            LogLevel.WARN -> LogWarnColor
                            LogLevel.ERROR -> LogErrorColor
                        }
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "${entry.timestamp} ",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                color = Color(0xFF7B918E),
                            )
                            Text(
                                text = "[${entry.level.tag}] ",
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = levelColor,
                            )
                            Text(
                                text = entry.message,
                                fontFamily = JetBrainsMonoFamily,
                                fontSize = 11.5.sp,
                                lineHeight = 16.sp,
                                color = Color(0xFFE5F0EE),
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(2.dp))
    }
}
