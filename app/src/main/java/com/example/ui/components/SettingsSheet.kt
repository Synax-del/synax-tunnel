package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.dp
import com.example.model.AppThemeMode
import com.example.model.ConnectionConfig
import com.example.model.InstalledAppItem
import com.example.model.RoutingMode
import com.example.ui.theme.JetBrainsMonoFamily
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SettingsSheet(
    config: ConnectionConfig,
    themeMode: AppThemeMode,
    installedApps: List<InstalledAppItem>,
    onThemeModeChange: (AppThemeMode) -> Unit,
    onUpdateConfig: (ConnectionConfig) -> Unit,
    onToggleExcludedApp: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var appFilterQuery by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Tunnel Settings",
                style = MaterialTheme.typography.titleLarge,
            )

            // Appearance: Day / Night Mode
            Text("Appearance (Day & Night Mode)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AppThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        label = { Text("${mode.label} Mode") },
                        modifier = Modifier.testTag("settings_theme_${mode.name.lowercase()}"),
                    )
                }
            }

            HorizontalDivider()

            // Routing Mode
            Text("TUN Routing Mode", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            RoutingMode.entries.forEach { mode ->
                val selected = config.routingMode == mode
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    ),
                    color = if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUpdateConfig(config.copy(routingMode = mode)) },
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onUpdateConfig(config.copy(routingMode = mode)) },
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(mode.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                mode.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // DNS Forwarding
            Text("Custom DNS Resolvers", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "Cloudflare" to ("1.1.1.1" to "1.0.0.1"),
                    "Google" to ("8.8.8.8" to "8.8.4.4"),
                    "Quad9" to ("9.9.9.9" to "149.112.112.112"),
                    "AdGuard" to ("94.140.14.14" to "94.140.15.15"),
                ).forEach { (label, pair) ->
                    FilterChip(
                        selected = config.primaryDns == pair.first,
                        onClick = {
                            onUpdateConfig(config.copy(primaryDns = pair.first, secondaryDns = pair.second))
                        },
                        label = { Text(label) },
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = config.primaryDns,
                    onValueChange = { onUpdateConfig(config.copy(primaryDns = it)) },
                    label = { Text("Primary DNS") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = config.secondaryDns,
                    onValueChange = { onUpdateConfig(config.copy(secondaryDns = it)) },
                    label = { Text("Secondary DNS") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            HorizontalDivider()

            // MTU Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("TUN Interface MTU", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${config.mtu} bytes",
                    fontFamily = JetBrainsMonoFamily,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Slider(
                value = config.mtu.toFloat(),
                onValueChange = { onUpdateConfig(config.copy(mtu = it.roundToInt())) },
                valueRange = 1280f..1500f,
                steps = 10,
            )

            // WakeLock
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("CPU WakeLock", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "Keep tunnel socket responsive when screen turns off",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = config.wakeLock,
                    onCheckedChange = { onUpdateConfig(config.copy(wakeLock = it)) },
                    modifier = Modifier.testTag("switch_wakelock"),
                )
            }

            HorizontalDivider()

            // Split Tunneling (Bypass VPN apps)
            Text(
                text = "Split Tunneling (Bypass VPN)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = "${config.excludedPackages.size} app(s) excluded from VPN tunnel",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = appFilterQuery,
                onValueChange = { appFilterQuery = it },
                label = { Text("Filter installed apps") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            val filteredApps = remember(installedApps, appFilterQuery) {
                if (appFilterQuery.isBlank()) installedApps.take(15)
                else installedApps.filter {
                    it.label.contains(appFilterQuery, ignoreCase = true) ||
                        it.packageName.contains(appFilterQuery, ignoreCase = true)
                }.take(20)
            }

            if (filteredApps.isEmpty()) {
                Text(
                    text = "No matching apps found",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    filteredApps.forEach { app ->
                        val excluded = app.packageName in config.excludedPackages
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleExcludedApp(app.packageName) }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = excluded,
                                onCheckedChange = { onToggleExcludedApp(app.packageName) },
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(
                                    app.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = JetBrainsMonoFamily,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
