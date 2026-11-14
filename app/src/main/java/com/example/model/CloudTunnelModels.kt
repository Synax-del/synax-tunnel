package com.example.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class CloudTweak(
    @DocumentId val id: String = "",
    val name: String = "",
    val carrierTag: String = "",
    val notes: String = "",
    val protocol: String = "TCP",
    val serverId: String = "sg1",
    val customHost: String = "1.1.1.1",
    val customPort: Long = 443L,
    val useSniHost: Boolean = true,
    val sniHost: String = "",
    val usePayload: Boolean = false,
    val payload: String = "",
    val usePayloadAfterTls: Boolean = false,
    val payloadAfterTls: String = "",
    val useProxy: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Long = 8080L,
    val obfuscatedTlsCh: Boolean = false,
    val tcpFastOpen: Boolean = false,
    val preferIpv6: Boolean = false,
    val createdBy: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class AdminNotificationMessage(
    @DocumentId val id: String = "",
    val title: String = "",
    val message: String = "",
    val actionUrl: String = "",
    val createdBy: String = "",
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
)

@IgnoreExtraProperties
data class CloudConfigDocument(
    @DocumentId val id: String = "",
    val shareCode: String = "",
    val ownerId: String = "",
    val name: String = "",
    val author: String = "",
    val notes: String = "",
    val protocol: String = "TCP",
    val serverId: String = "sg1",
    val customHost: String = "1.1.1.1",
    val customPort: Long = 443L,
    val useSniHost: Boolean = true,
    val sniHost: String = "",
    val usePayload: Boolean = false,
    val payload: String = "",
    val usePayloadAfterTls: Boolean = false,
    val payloadAfterTls: String = "",
    val useProxy: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Long = 8080L,
    val obfuscatedTlsCh: Boolean = false,
    val tcpFastOpen: Boolean = false,
    val preferIpv6: Boolean = false,
    val isPublic: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

enum class AppThemeMode(val label: String) {
    SYSTEM("System"),
    DAY("Day"),
    NIGHT("Night"),
}
