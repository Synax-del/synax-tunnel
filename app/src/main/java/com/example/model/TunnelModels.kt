package com.example.model

enum class TunnelProtocol(val shortCode: String, val label: String) {
    TCP("T", "TCP Method"),
    UDP("U", "UDP Method"),
}

enum class RoutingMode(val title: String, val subtitle: String) {
    DNS_SAFE_LAB(
        "DNS & SNI Lab Tunnel",
        "Routes tunnel DNS (10.10.0.53) & probes without blocking local app internet",
    ),
    FULL_DEVICE(
        "Full Device Tunnel (0.0.0.0/0)",
        "Captures all device IP traffic through TUN interface with live DNS relay",
    ),
}

enum class TunnelStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
}

data class TunnelServer(
    val id: String,
    val code: String,
    val country: String,
    val city: String,
    val host: String,
    val port: Int,
    val flagCode: String,
    val loadPercent: Int = 28,
    val isCustom: Boolean = false,
)

data class ConnectionConfig(
    val serverId: String = "sg1",
    val customHost: String = "1.1.1.1",
    val customPort: Int = 443,
    val protocol: TunnelProtocol = TunnelProtocol.TCP,
    val preferIpv6: Boolean = false,
    val tcpFastOpen: Boolean = false,
    val obfuscatedTlsCh: Boolean = false,
    val usePayload: Boolean = false,
    val payload: String = "CONNECT [host_port] [protocol][crlf]Host: [host][crlf]Connection: Keep-Alive[crlf][crlf]",
    val useSniHost: Boolean = true,
    val sniHost: String = "m.tiktok.com",
    val usePayloadAfterTls: Boolean = false,
    val payloadAfterTls: String = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf][crlf]",
    val useProxy: Boolean = false,
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val udpForwarding: Boolean = true,
    val dnsForwarding: Boolean = true,
    val quicObfuscation: Boolean = false,
    val mtu: Int = 1400,
    val primaryDns: String = "1.1.1.1",
    val secondaryDns: String = "8.8.8.8",
    val routingMode: RoutingMode = RoutingMode.DNS_SAFE_LAB,
    val wakeLock: Boolean = true,
    val excludedPackages: Set<String> = emptySet(),
) {
    fun methodDisplaySummary(): String {
        val parts = mutableListOf(protocol.label)
        if (protocol == TunnelProtocol.TCP) {
            if (useSniHost && sniHost.isNotBlank()) parts += "SNI"
            if (usePayload) parts += "Payload"
            if (useProxy && proxyHost.isNotBlank()) parts += "Proxy"
            if (obfuscatedTlsCh) parts += "TLS-CH"
        } else {
            if (udpForwarding) parts += "UDPGW"
            if (quicObfuscation) parts += "QUIC"
        }
        return parts.joinToString(" • ")
    }
}

enum class LogLevel(val tag: String) {
    INFO("INFO"),
    TLS("TLS"),
    DNS("DNS"),
    TUN("TUN"),
    WARN("WARN"),
    ERROR("ERR"),
}

data class TunnelLogEntry(
    val id: Long,
    val timestamp: String,
    val level: LogLevel,
    val message: String,
)

data class TrafficPoint(
    val rxKbps: Float,
    val txKbps: Float,
    val timestampMs: Long,
)

data class TunnelTelemetry(
    val status: TunnelStatus = TunnelStatus.DISCONNECTED,
    val statusDetail: String = "Ready to connect",
    val connectedSinceMs: Long = 0L,
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val rxKbps: Float = 0f,
    val txKbps: Float = 0f,
    val handshakeMs: Long? = null,
    val resolvedIp: String = "",
    val tlsProtocol: String = "",
    val tlsCipher: String = "",
    val peerCertSubject: String = "",
    val localTunIp: String = "10.10.0.2",
    val packetCount: Long = 0L,
    val dnsQueryCount: Long = 0L,
    val trafficHistory: List<TrafficPoint> = emptyList(),
)

data class HostCheckResult(
    val host: String,
    val resolvedIp: String?,
    val httpStatusCode: Int?,
    val serverHeader: String?,
    val tlsVersion: String?,
    val tlsCipher: String?,
    val rttMs: Long,
    val error: String? = null,
)

data class PortScanResult(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val rttMs: Long?,
)

data class InstalledAppItem(
    val packageName: String,
    val label: String,
)
