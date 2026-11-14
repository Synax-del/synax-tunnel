package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.CloudTweak
import com.example.model.ConnectionConfig
import com.example.model.TunnelProtocol
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AdminPanelDialog(
    expectedPin: String,
    currentConfig: ConnectionConfig,
    cloudTweaks: List<CloudTweak>,
    signedInEmail: String?,
    telegramUrl: String,
    onSignInGoogleClick: () -> Unit,
    onPublishTweak: (name: String, carrier: String, notes: String, config: ConnectionConfig) -> Unit,
    onDeleteCloudTweak: (String) -> Unit,
    onSendNotification: (title: String, message: String, actionUrl: String) -> Unit,
    onUpdateTelegramUrl: (String) -> Unit,
    onUpdateAdminPin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var unlocked by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    var adminTab by remember { mutableIntStateOf(0) }

    // Tweak editor state
    var tweakName by remember { mutableStateOf("") }
    var carrierTag by remember { mutableStateOf("All Networks") }
    var tweakNotes by remember { mutableStateOf("") }
    var tweakProto by remember(currentConfig) { mutableStateOf(currentConfig.protocol) }
    var tweakSni by remember(currentConfig) { mutableStateOf(currentConfig.sniHost) }
    var tweakPayload by remember(currentConfig) { mutableStateOf(currentConfig.payload) }
    var useTweakPayload by remember(currentConfig) { mutableStateOf(currentConfig.usePayload) }

    // Notification composer state
    var notifTitle by remember { mutableStateOf("") }
    var notifMessage by remember { mutableStateOf("") }
    var notifActionUrl by remember { mutableStateOf(telegramUrl) }

    // Settings tab state
    var tgUrlDraft by remember(telegramUrl) { mutableStateOf(telegramUrl) }
    var newPinDraft by remember { mutableStateOf("") }

    val tunnelColors = LocalTunnelColors.current

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 24.dp),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Hidden Admin Header
                Surface(
                    color = tunnelColors.topBarContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AdminPanelSettings,
                            contentDescription = null,
                            tint = tunnelColors.onTopBar,
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Synax Admin Console",
                            style = MaterialTheme.typography.titleLarge,
                            color = tunnelColors.onTopBar,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Close Admin Panel",
                                tint = tunnelColors.onTopBar,
                            )
                        }
                    }
                }

                if (!unlocked) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(44.dp),
                        )
                        Text(
                            text = "Administrator Access",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Enter Admin PIN to manage VPN tweaks and broadcast notifications (default: 2026)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedTextField(
                            value = pinInput,
                            onValueChange = {
                                pinInput = it
                                pinError = false
                            },
                            label = { Text("Admin PIN") },
                            isError = pinError,
                            supportingText = if (pinError) {
                                { Text("Incorrect Admin PIN") }
                            } else {
                                null
                            },
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_pin_input"),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = onDismiss) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    if (pinInput.trim() == expectedPin) {
                                        unlocked = true
                                    } else {
                                        pinError = true
                                    }
                                },
                                modifier = Modifier.testTag("admin_unlock_button"),
                            ) {
                                Text("Unlock")
                            }
                        }
                    }
                } else {
                    // Cloud Auth banner inside Admin Panel
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (signedInEmail != null) "Cloud Sync: $signedInEmail" else "Local Admin Mode",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text(
                                    text = if (signedInEmail != null) {
                                        "Tweaks & notifications sync to Firestore"
                                    } else {
                                        "Sign in with admin Google account to push to Cloud"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            if (signedInEmail == null) {
                                OutlinedButton(onClick = onSignInGoogleClick) {
                                    Text("Sign in")
                                }
                            }
                        }
                    }

                    TabRow(selectedTabIndex = adminTab) {
                        Tab(
                            selected = adminTab == 0,
                            onClick = { adminTab = 0 },
                            text = { Text("Add Tweaks") },
                        )
                        Tab(
                            selected = adminTab == 1,
                            onClick = { adminTab = 1 },
                            text = { Text("Notify") },
                        )
                        Tab(
                            selected = adminTab == 2,
                            onClick = { adminTab = 2 },
                            text = { Text("Channel") },
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        when (adminTab) {
                            0 -> {
                                OutlinedTextField(
                                    value = tweakName,
                                    onValueChange = { tweakName = it },
                                    label = { Text("Tweak Name (e.g. MTN TikTok Zero 443)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedTextField(
                                    value = carrierTag,
                                    onValueChange = { carrierTag = it },
                                    label = { Text("Network / ISP Tag") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TunnelProtocol.entries.forEach { p ->
                                        FilterChip(
                                            selected = tweakProto == p,
                                            onClick = { tweakProto = p },
                                            label = { Text(p.name) },
                                        )
                                    }
                                    FilterChip(
                                        selected = useTweakPayload,
                                        onClick = { useTweakPayload = !useTweakPayload },
                                        label = { Text("Include Payload") },
                                    )
                                }
                                OutlinedTextField(
                                    value = tweakSni,
                                    onValueChange = { tweakSni = it },
                                    label = { Text("SNI Bug Host") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                if (useTweakPayload) {
                                    OutlinedTextField(
                                        value = tweakPayload,
                                        onValueChange = { tweakPayload = it },
                                        label = { Text("Custom Payload") },
                                        minLines = 2,
                                        maxLines = 4,
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                }
                                OutlinedTextField(
                                    value = tweakNotes,
                                    onValueChange = { tweakNotes = it },
                                    label = { Text("Admin Notes") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        if (tweakName.isNotBlank()) {
                                            val built = currentConfig.copy(
                                                protocol = tweakProto,
                                                useSniHost = tweakSni.isNotBlank(),
                                                sniHost = tweakSni.trim(),
                                                usePayload = useTweakPayload,
                                                payload = tweakPayload,
                                            )
                                            onPublishTweak(tweakName, carrierTag, tweakNotes, built)
                                            tweakName = ""
                                            tweakNotes = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_publish_tweak_button"),
                                ) {
                                    Icon(Icons.Rounded.Tune, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Publish Tweak")
                                }

                                if (cloudTweaks.isNotEmpty()) {
                                    HorizontalDivider()
                                    Text(
                                        text = "PUBLISHED CLOUD TWEAKS (${cloudTweaks.size})",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    cloudTweaks.forEach { tw ->
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(tw.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                                    Text(
                                                        "${tw.carrierTag} • ${tw.protocol} • ${tw.sniHost}",
                                                        fontFamily = JetBrainsMonoFamily,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    )
                                                }
                                                IconButton(onClick = { onDeleteCloudTweak(tw.id) }) {
                                                    Icon(
                                                        Icons.Outlined.Delete,
                                                        contentDescription = "Delete Cloud Tweak",
                                                        tint = MaterialTheme.colorScheme.error,
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            1 -> {
                                OutlinedTextField(
                                    value = notifTitle,
                                    onValueChange = { notifTitle = it },
                                    label = { Text("Notification Title") },
                                    singleLine = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_notif_title_input"),
                                )
                                OutlinedTextField(
                                    value = notifMessage,
                                    onValueChange = { notifMessage = it },
                                    label = { Text("Broadcast Message") },
                                    minLines = 3,
                                    maxLines = 5,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_notif_message_input"),
                                )
                                OutlinedTextField(
                                    value = notifActionUrl,
                                    onValueChange = { notifActionUrl = it },
                                    label = { Text("Optional Link (Telegram / Update URL)") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = {
                                        if (notifTitle.isNotBlank() && notifMessage.isNotBlank()) {
                                            onSendNotification(notifTitle, notifMessage, notifActionUrl)
                                            notifTitle = ""
                                            notifMessage = ""
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("admin_send_notification_button"),
                                ) {
                                    Icon(Icons.Rounded.Campaign, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Send Notification Message")
                                }
                            }

                            else -> {
                                OutlinedTextField(
                                    value = tgUrlDraft,
                                    onValueChange = { tgUrlDraft = it },
                                    label = { Text("Telegram Channel URL") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Button(
                                    onClick = { onUpdateTelegramUrl(tgUrlDraft) },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Save Telegram Link")
                                }

                                HorizontalDivider()

                                OutlinedTextField(
                                    value = newPinDraft,
                                    onValueChange = { newPinDraft = it.filter(Char::isDigit).take(8) },
                                    label = { Text("New Admin PIN (4+ digits)") },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (newPinDraft.length >= 4) {
                                            onUpdateAdminPin(newPinDraft)
                                            newPinDraft = ""
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("Update Admin PIN")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
