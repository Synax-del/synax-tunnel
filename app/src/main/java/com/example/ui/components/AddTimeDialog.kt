package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.AccessTimeFilled
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.CardGiftcard
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.ui.screens.formatDuration
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.theme.LocalTunnelColors

@Composable
fun AddTimeDialog(
    remainingSeconds: Long,
    onAddTimeSeconds: (Long, String) -> Unit,
    onRedeemCode: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var promoCode by remember { mutableStateOf("") }
    val tunnelColors = LocalTunnelColors.current

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Surface(
                    color = tunnelColors.topBarContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTimeFilled,
                            contentDescription = null,
                            tint = tunnelColors.onTopBar,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Add Tunnel Time",
                                style = MaterialTheme.typography.titleLarge,
                                color = tunnelColors.onTopBar,
                            )
                            Text(
                                text = "Remaining: ${formatDuration(remainingSeconds)}",
                                fontFamily = JetBrainsMonoFamily,
                                style = MaterialTheme.typography.labelMedium,
                                color = tunnelColors.onTopBar.copy(alpha = 0.85f),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    TimePackRow(
                        title = "+2 Hours",
                        subtitle = "Standard Session Renew",
                        testTag = "add_time_2h",
                        onClick = {
                            onAddTimeSeconds(2L * 3600L, "+2h added")
                            onDismiss()
                        },
                    )
                    TimePackRow(
                        title = "+4 Hours",
                        subtitle = "HA-Style Extended Boost",
                        testTag = "add_time_4h",
                        onClick = {
                            onAddTimeSeconds(4L * 3600L, "+4h added")
                            onDismiss()
                        },
                    )
                    TimePackRow(
                        title = "+6 Hours",
                        subtitle = "Long-Haul Tunnel Pass",
                        testTag = "add_time_6h",
                        onClick = {
                            onAddTimeSeconds(6L * 3600L, "+6h added")
                            onDismiss()
                        },
                    )

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Redeem Time Voucher",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = promoCode,
                            onValueChange = { promoCode = it },
                            label = { Text("Code (e.g. SYNAX24)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = {
                                if (promoCode.isNotBlank()) {
                                    onRedeemCode(promoCode)
                                    promoCode = ""
                                }
                            },
                            modifier = Modifier.testTag("redeem_time_code_button"),
                        ) {
                            Icon(Icons.Rounded.CardGiftcard, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss) {
                            Text("Close")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimePackRow(
    title: String,
    subtitle: String,
    testTag: String,
    onClick: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag(testTag),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "CLAIM",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
