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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.TunnelProfileEntity
import com.example.model.CloudConfigDocument
import com.example.model.CloudTweak
import com.example.ui.theme.JetBrainsMonoFamily
import com.google.firebase.auth.FirebaseUser

private enum class ConfigDialogMode {
    NONE,
    SAVE_LOCAL,
    EXPORT_FILE,
    EXPORT_CLOUD,
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigsScreen(
    profiles: List<TunnelProfileEntity>,
    currentUser: FirebaseUser?,
    cloudTweaks: List<CloudTweak>,
    userCloudConfigs: List<CloudConfigDocument>,
    isCloudBusy: Boolean,
    lastExportedShareCode: String?,
    onSignInGoogleClick: () -> Unit,
    onApplyProfile: (TunnelProfileEntity) -> Unit,
    onDeleteProfile: (Long) -> Unit,
    onSaveCurrentProfile: (name: String, author: String, notes: String) -> Unit,
    onImportFileClick: () -> Unit,
    onExportFileRequest: (name: String, author: String, notes: String) -> Unit,
    onExportCloudRequest: (name: String, author: String, notes: String) -> Unit,
    onImportCloudCode: (String) -> Unit,
    onClearExportedShareCode: () -> Unit,
    onApplyCloudTweak: (CloudTweak) -> Unit,
    onApplyCloudConfig: (CloudConfigDocument) -> Unit,
    onDeleteCloudConfig: (String) -> Unit,
    onCopiedToast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val clipboard = LocalClipboardManager.current
    var dialogMode by remember { mutableStateOf(ConfigDialogMode.NONE) }
    var profileName by remember { mutableStateOf("") }
    var profileAuthor by remember(currentUser) {
        mutableStateOf(currentUser?.displayName?.takeIf { it.isNotBlank() } ?: "Synax User")
    }
    var profileNotes by remember { mutableStateOf("") }
    var cloudCodeInput by remember { mutableStateOf("") }

    if (lastExportedShareCode != null) {
        AlertDialog(
            onDismissRequest = onClearExportedShareCode,
            icon = { Icon(Icons.Rounded.CloudDone, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Cloud Config Published") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Share this Cloud Code so anyone on Synax Tunnel can import your configuration:")
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = lastExportedShareCode,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(lastExportedShareCode))
                                    onCopiedToast("Copied share code $lastExportedShareCode")
                                },
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Share Code")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onClearExportedShareCode) { Text("Done") }
            },
        )
    }

    if (dialogMode != ConfigDialogMode.NONE) {
        AlertDialog(
            onDismissRequest = { dialogMode = ConfigDialogMode.NONE },
            title = {
                Text(
                    when (dialogMode) {
                        ConfigDialogMode.EXPORT_FILE -> "Export .synax Config File"
                        ConfigDialogMode.EXPORT_CLOUD -> "Export to Synax Cloud"
                        else -> "Save Tunnel Profile"
                    },
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = profileName,
                        onValueChange = { profileName = it },
                        label = { Text("Config / Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = profileAuthor,
                        onValueChange = { profileAuthor = it },
                        label = { Text("Author") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = profileNotes,
                        onValueChange = { profileNotes = it },
                        label = { Text("Notes / ISP Hint") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        when (dialogMode) {
                            ConfigDialogMode.EXPORT_FILE -> onExportFileRequest(profileName, profileAuthor, profileNotes)
                            ConfigDialogMode.EXPORT_CLOUD -> onExportCloudRequest(profileName, profileAuthor, profileNotes)
                            ConfigDialogMode.SAVE_LOCAL -> onSaveCurrentProfile(profileName, profileAuthor, profileNotes)
                            ConfigDialogMode.NONE -> Unit
                        }
                        dialogMode = ConfigDialogMode.NONE
                        profileName = ""
                        profileNotes = ""
                    },
                ) {
                    Text(
                        when (dialogMode) {
                            ConfigDialogMode.EXPORT_FILE -> "Choose File"
                            ConfigDialogMode.EXPORT_CLOUD -> "Upload to Cloud"
                            else -> "Save"
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { dialogMode = ConfigDialogMode.NONE }) {
                    Text("Cancel")
                }
            },
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Card 1: File Import & Export (.synax / .ctun)
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "File Import & Export (.synax)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Share or load SNI, HTTP payload, and proxy setups via file picker",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedButton(
                            onClick = onImportFileClick,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("import_config_button"),
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Import")
                        }
                        OutlinedButton(
                            onClick = { dialogMode = ConfigDialogMode.EXPORT_FILE },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("export_config_button"),
                        ) {
                            Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export")
                        }
                        Button(
                            onClick = { dialogMode = ConfigDialogMode.SAVE_LOCAL },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("save_profile_button"),
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save")
                        }
                    }
                }
            }
        }

        // Card 2: Synax Cloud Config Import & Export
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Cloud Config Sync & Share",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = if (currentUser != null) {
                                    "Signed in as ${currentUser.email ?: currentUser.displayName ?: "Cloud User"}"
                                } else {
                                    "Sign in with Google to sync Cloud Configs & Tweaks"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isCloudBusy) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        }
                    }

                    if (currentUser == null) {
                        Button(
                            onClick = onSignInGoogleClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("cloud_google_signin_button"),
                        ) {
                            Icon(Icons.Rounded.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sign in with Google for Cloud")
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            OutlinedTextField(
                                value = cloudCodeInput,
                                onValueChange = { cloudCodeInput = it.uppercase() },
                                label = { Text("Cloud Code (e.g. SYNAX-1234)") },
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("cloud_code_input"),
                            )
                            Button(
                                onClick = {
                                    if (cloudCodeInput.isNotBlank()) {
                                        onImportCloudCode(cloudCodeInput)
                                        cloudCodeInput = ""
                                    }
                                },
                                enabled = !isCloudBusy,
                                modifier = Modifier.testTag("import_cloud_code_button"),
                            ) {
                                Icon(Icons.Rounded.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Fetch")
                            }
                        }

                        FilledTonalButton(
                            onClick = { dialogMode = ConfigDialogMode.EXPORT_CLOUD },
                            enabled = !isCloudBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("export_cloud_config_button"),
                        ) {
                            Icon(Icons.Rounded.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Export Current Config to Cloud")
                        }
                    }
                }
            }
        }

        // Cloud Tweaks published by Admin
        if (cloudTweaks.isNotEmpty()) {
            item {
                Text(
                    text = "SYNAX CLOUD TWEAKS (${cloudTweaks.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(cloudTweaks, key = { "tw_${it.id}" }) { tweak ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Tune,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = tweak.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f),
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Text(
                                    text = tweak.carrierTag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                        if (tweak.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tweak.notes,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = { onApplyCloudTweak(tweak) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Apply Cloud Tweak")
                        }
                    }
                }
            }
        }

        // User's Published Cloud Configs
        if (userCloudConfigs.isNotEmpty()) {
            item {
                Text(
                    text = "MY CLOUD CONFIGS (${userCloudConfigs.size})",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            items(userCloudConfigs, key = { "cc_${it.shareCode}" }) { doc ->
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(doc.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "Code: ${doc.shareCode} • ${doc.protocol} • ${doc.sniHost.ifBlank { doc.customHost }}",
                                    fontFamily = JetBrainsMonoFamily,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboard.setText(AnnotatedString(doc.shareCode))
                                    onCopiedToast("Copied code ${doc.shareCode}")
                                },
                            ) {
                                Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Share Code")
                            }
                            IconButton(onClick = { onDeleteCloudConfig(doc.shareCode) }) {
                                Icon(
                                    Icons.Outlined.Delete,
                                    contentDescription = "Delete Cloud Config",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        FilledTonalButton(
                            onClick = { onApplyCloudConfig(doc) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Load Cloud Config")
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "SAVED & PRESET PROFILES (${profiles.size})",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 4.dp),
            )
        }

        items(profiles, key = { it.id }) { profile ->
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = profile.name,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = "by ${profile.author} • ${profile.notes}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!profile.isPreset) {
                            IconButton(onClick = { onDeleteProfile(profile.id) }) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete profile",
                                    tint = MaterialTheme.colorScheme.error,
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Text(
                                text = profile.protocol,
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            )
                        }
                        if (profile.useSniHost && profile.sniHost.isNotBlank()) {
                            AssistChip(
                                onClick = { onApplyProfile(profile) },
                                label = {
                                    Text(
                                        profile.sniHost,
                                        fontFamily = JetBrainsMonoFamily,
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                },
                            )
                        }
                        if (profile.usePayload) {
                            AssistChip(
                                onClick = { onApplyProfile(profile) },
                                label = { Text("Payload", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                        if (profile.obfuscatedTlsCh) {
                            AssistChip(
                                onClick = { onApplyProfile(profile) },
                                label = { Text("TLS-CH", style = MaterialTheme.typography.labelSmall) },
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    FilledTonalButton(
                        onClick = { onApplyProfile(profile) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("apply_profile_${profile.id}"),
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Load Into Tunnel")
                    }
                }
            }
        }
    }
}
