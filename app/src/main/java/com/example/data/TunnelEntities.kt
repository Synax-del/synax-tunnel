package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tunnel_profiles")
data class TunnelProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val author: String,
    val notes: String,
    val protocol: String,
    val serverId: String,
    val customHost: String,
    val customPort: Int,
    val useSniHost: Boolean,
    val sniHost: String,
    val usePayload: Boolean,
    val payload: String,
    val usePayloadAfterTls: Boolean,
    val payloadAfterTls: String,
    val useProxy: Boolean,
    val proxyHost: String,
    val proxyPort: Int,
    val obfuscatedTlsCh: Boolean,
    val tcpFastOpen: Boolean,
    val preferIpv6: Boolean,
    val isPreset: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
)

@Entity(tableName = "connection_sessions")
data class ConnectionSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val serverCode: String,
    val serverCountry: String,
    val methodSummary: String,
    val sniOrHost: String,
    val startedAtMs: Long,
    val durationSeconds: Long,
    val rxBytes: Long,
    val txBytes: Long,
    val handshakeMs: Long?,
)
