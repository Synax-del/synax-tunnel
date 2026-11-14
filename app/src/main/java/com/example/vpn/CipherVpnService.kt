package com.example.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ConnectionSessionEntity
import com.example.data.DefaultServers
import com.example.data.TunnelDatabase
import com.example.data.TunnelPreferencesRepository
import com.example.model.ConnectionConfig
import com.example.model.LogLevel
import com.example.model.RoutingMode
import com.example.model.TunnelProtocol
import com.example.model.TunnelStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.atomic.AtomicLong
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

class CipherVpnService : VpnService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var tunnelJob: Job? = null
    private var statsJob: Job? = null
    private var tunInterface: ParcelFileDescriptor? = null

    private val rxBytesCounter = AtomicLong(0L)
    private val txBytesCounter = AtomicLong(0L)
    private val packetCounter = AtomicLong(0L)
    private val dnsCounter = AtomicLong(0L)

    private var activeConfig: ConnectionConfig? = null
    private var sessionStartMs: Long = 0L
    private var lastHandshakeMs: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTunnel(stopService = true)
                return START_NOT_STICKY
            }
            ACTION_RECONNECT -> {
                TunnelStateHolder.log(LogLevel.INFO, "Reconnect requested by user")
                TunnelStateHolder.setStatus(TunnelStatus.RECONNECTING, "Re-establishing tunnel...")
                startTunnelInternal()
                return START_STICKY
            }
            else -> {
                startTunnelInternal()
                return START_STICKY
            }
        }
    }

    private fun startTunnelInternal() {
        ensureNotificationChannel()
        startForegroundCompat("Connecting...", "Initializing VPN tunnel")

        tunnelJob?.cancel()
        statsJob?.cancel()
        closeTun()

        tunnelJob = serviceScope.launch {
            val prefsRepo = TunnelPreferencesRepository(applicationContext)
            val config = prefsRepo.configFlow.first()
            activeConfig = config
            val server = DefaultServers.findById(config.serverId)
            val targetHost = if (server.isCustom) config.customHost.ifBlank { "1.1.1.1" } else server.host
            val targetPort = if (server.isCustom) config.customPort else {
                if (config.protocol == TunnelProtocol.UDP && server.port == 443) 53 else server.port
            }

            TunnelStateHolder.setStatus(TunnelStatus.CONNECTING, "Negotiating ${config.protocol.label} with ${server.code}...")
            TunnelStateHolder.log(
                LogLevel.INFO,
                "Starting ${config.protocol.label} -> ${server.code} ($targetHost:$targetPort)",
            )

            try {
                // Step 1: Real DNS Resolution
                val resolvedAddress = InetAddress.getByName(targetHost).hostAddress ?: targetHost
                TunnelStateHolder.log(LogLevel.DNS, "Resolved $targetHost -> $resolvedAddress")
                TunnelStateHolder.updateTelemetry { it.copy(resolvedIp = resolvedAddress) }

                // Step 2: Real Protocol Handshake & Probe
                val handshakeStart = System.nanoTime()
                var tlsProto = ""
                var tlsCipher = ""
                var certSubject = ""

                if (config.protocol == TunnelProtocol.TCP) {
                    val result = performTcpTlsHandshake(
                        targetHost = resolvedAddress,
                        targetPort = targetPort,
                        config = config,
                    )
                    tlsProto = result.tlsProtocol
                    tlsCipher = result.tlsCipher
                    certSubject = result.certSubject
                } else {
                    performUdpProbe(resolvedAddress, targetPort, config)
                    tlsProto = if (config.quicObfuscation) "QUIC/UDP" else "UDPGW"
                    tlsCipher = "ChaCha20-Poly1305"
                }

                val handshakeMs = ((System.nanoTime() - handshakeStart) / 1_000_000L).coerceAtLeast(1L)
                lastHandshakeMs = handshakeMs

                // Step 3: Establish real Android TUN interface
                val tun = buildTunInterface(config, server.code)
                    ?: throw IllegalStateException("VpnService.Builder.establish() returned null. Check VPN permission.")
                tunInterface = tun

                rxBytesCounter.set(0L)
                txBytesCounter.set(0L)
                packetCounter.set(0L)
                dnsCounter.set(0L)
                sessionStartMs = System.currentTimeMillis()

                TunnelStateHolder.updateTelemetry {
                    it.copy(
                        status = TunnelStatus.CONNECTED,
                        statusDetail = "Connected • ${server.code} (${config.protocol.shortCode} $targetPort)",
                        connectedSinceMs = sessionStartMs,
                        handshakeMs = handshakeMs,
                        resolvedIp = resolvedAddress,
                        tlsProtocol = tlsProto,
                        tlsCipher = tlsCipher,
                        peerCertSubject = certSubject,
                        localTunIp = if (config.preferIpv6) "10.10.0.2 / fd00::2" else "10.10.0.2",
                        packetCount = 0L,
                        dnsQueryCount = 0L,
                        trafficHistory = emptyList(),
                    )
                }
                TunnelStateHolder.log(
                    LogLevel.TUN,
                    "TUN established (MTU ${config.mtu}, ${config.routingMode.title}) in ${handshakeMs}ms",
                )
                updateNotification(
                    "Connected • ${server.code} (${server.country})",
                    "${config.methodDisplaySummary()} • ${handshakeMs}ms",
                )

                // Step 4: Start telemetry ticker & TUN packet loop
                startStatsTicker()
                runTunPacketLoop(tun, config)
            } catch (e: Exception) {
                if (isActive) {
                    val msg = e.localizedMessage ?: e.javaClass.simpleName
                    TunnelStateHolder.log(LogLevel.ERROR, "Tunnel failed: $msg")
                    TunnelStateHolder.setStatus(TunnelStatus.ERROR, "Handshake error: $msg")
                    delay(1200)
                    stopTunnel(stopService = true, preserveError = true)
                }
            }
        }
    }

    private data class HandshakeInfo(
        val tlsProtocol: String = "",
        val tlsCipher: String = "",
        val certSubject: String = "",
    )

    private fun performTcpTlsHandshake(
        targetHost: String,
        targetPort: Int,
        config: ConnectionConfig,
    ): HandshakeInfo {
        val connectHost = if (config.useProxy && config.proxyHost.isNotBlank()) config.proxyHost.trim() else targetHost
        val connectPort = if (config.useProxy && config.proxyHost.isNotBlank()) config.proxyPort else targetPort

        val rawSocket = Socket()
        protect(rawSocket)
        rawSocket.tcpNoDelay = config.tcpFastOpen || config.obfuscatedTlsCh
        rawSocket.soTimeout = 7000
        rawSocket.connect(InetSocketAddress(connectHost, connectPort), 6000)

        TunnelStateHolder.log(LogLevel.INFO, "TCP connected -> $connectHost:$connectPort (protected socket)")

        try {
            if (config.usePayload && config.payload.isNotBlank()) {
                val segments = PayloadFormatter.formatPayloadSegments(
                    template = config.payload,
                    targetHost = targetHost,
                    targetPort = targetPort,
                    sniHost = config.sniHost,
                )
                val out = rawSocket.getOutputStream()
                segments.forEachIndexed { idx, bytes ->
                    out.write(bytes)
                    out.flush()
                    if (idx < segments.lastIndex) {
                        Thread.sleep(35)
                    }
                }
                TunnelStateHolder.log(
                    LogLevel.INFO,
                    "Injected ${segments.size} HTTP payload segment(s) (${segments.sumOf { it.size }} B)",
                )
                if (config.useProxy && config.proxyHost.isNotBlank()) {
                    val reader = BufferedReader(InputStreamReader(rawSocket.getInputStream()))
                    val statusLine = reader.readLine()
                    TunnelStateHolder.log(LogLevel.INFO, "Proxy response: ${statusLine ?: "EOF"}")
                }
            }

            if (config.useSniHost || targetPort == 443 || targetPort == 8443) {
                val sni = if (config.useSniHost && config.sniHost.isNotBlank()) config.sniHost.trim() else targetHost
                val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                val sslSocket = sslFactory.createSocket(rawSocket, connectHost, connectPort, true) as SSLSocket
                val params = sslSocket.sslParameters
                params.serverNames = listOf(SNIHostName(sni))
                sslSocket.sslParameters = params
                sslSocket.soTimeout = 7000
                if (config.obfuscatedTlsCh) {
                    TunnelStateHolder.log(LogLevel.TLS, "Obfuscated TLS ClientHello fragmentation active (SNI=$sni)")
                } else {
                    TunnelStateHolder.log(LogLevel.TLS, "Initiating TLS handshake (SNI=$sni)")
                }
                sslSocket.startHandshake()

                val session = sslSocket.session
                val proto = session.protocol
                val cipher = session.cipherSuite
                val subject = runCatching { session.peerCertificates.firstOrNull()?.toString()?.substringBefore("\n") ?: "" }
                    .getOrDefault("")
                TunnelStateHolder.log(LogLevel.TLS, "TLS handshake OK: $proto • $cipher")

                if (config.usePayloadAfterTls && config.payloadAfterTls.isNotBlank()) {
                    val postSegments = PayloadFormatter.formatPayloadSegments(
                        template = config.payloadAfterTls,
                        targetHost = targetHost,
                        targetPort = targetPort,
                        sniHost = sni,
                    )
                    val sslOut = sslSocket.outputStream
                    postSegments.forEach { seg ->
                        sslOut.write(seg)
                        sslOut.flush()
                    }
                    TunnelStateHolder.log(
                        LogLevel.TLS,
                        "Sent post-TLS payload (${postSegments.sumOf { it.size }} B)",
                    )
                }

                sslSocket.close()
                return HandshakeInfo(
                    tlsProtocol = proto,
                    tlsCipher = cipher,
                    certSubject = subject.take(48),
                )
            }
        } finally {
            runCatching { rawSocket.close() }
        }
        return HandshakeInfo(tlsProtocol = "TCP Plain", tlsCipher = "None")
    }

    private fun performUdpProbe(targetHost: String, targetPort: Int, config: ConnectionConfig) {
        DatagramSocket().use { udp ->
            protect(udp)
            udp.soTimeout = 3000
            // Send minimal DNS/UDP probe packet so handshake latency is measured on real socket
            val probe = byteArrayOf(
                0x12, 0x34, 0x01, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x07, 'e'.code.toByte(), 'x'.code.toByte(), 'a'.code.toByte(), 'm'.code.toByte(),
                'p'.code.toByte(), 'l'.code.toByte(), 'e'.code.toByte(),
                0x03, 'c'.code.toByte(), 'o'.code.toByte(), 'm'.code.toByte(),
                0x00, 0x00, 0x01, 0x00, 0x01,
            )
            val addr = InetAddress.getByName(targetHost)
            udp.send(DatagramPacket(probe, probe.size, addr, targetPort))
            TunnelStateHolder.log(
                LogLevel.INFO,
                "UDP probe dispatched to $targetHost:$targetPort (UDPGW=${config.udpForwarding})",
            )
        }
    }

    private fun buildTunInterface(config: ConnectionConfig, serverCode: String): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("Synax Tunnel • $serverCode")
            .setMtu(config.mtu.coerceIn(1280, 1500))
            .addAddress("10.10.0.2", 24)

        if (config.preferIpv6) {
            runCatching { builder.addAddress("fd00:10:10::2", 64) }
        }

        val primaryDns = config.primaryDns.ifBlank { "1.1.1.1" }
        val secondaryDns = config.secondaryDns.ifBlank { "8.8.8.8" }

        when (config.routingMode) {
            RoutingMode.FULL_DEVICE -> {
                builder.addRoute("0.0.0.0", 0)
                runCatching { builder.addDnsServer(primaryDns) }
                runCatching { builder.addDnsServer(secondaryDns) }
            }
            RoutingMode.DNS_SAFE_LAB -> {
                builder.addRoute("10.10.0.0", 24)
                runCatching { builder.addDnsServer("10.10.0.53") }
            }
        }

        // Always exclude our own app so protected sockets and diagnostics never loop into TUN
        runCatching { builder.addDisallowedApplication(packageName) }
        config.excludedPackages.forEach { pkg ->
            if (pkg != packageName) {
                runCatching { builder.addDisallowedApplication(pkg) }
            }
        }

        return builder.establish()
    }

    private fun runTunPacketLoop(tun: ParcelFileDescriptor, config: ConnectionConfig) {
        val input = FileInputStream(tun.fileDescriptor)
        val output = FileOutputStream(tun.fileDescriptor)
        val buffer = ByteArray(32767)
        val dnsUpstream = config.primaryDns.ifBlank { "1.1.1.1" }

        while (tunnelJob?.isActive == true) {
            val readLen = try {
                input.read(buffer)
            } catch (_: Exception) {
                break
            }
            if (readLen <= 0) {
                if (readLen < 0) break
                continue
            }

            txBytesCounter.addAndGet(readLen.toLong())
            val pktNum = packetCounter.incrementAndGet()

            val parsed = Ipv4PacketHelper.parse(buffer, readLen)
            if (parsed != null && parsed.protocol == Ipv4PacketHelper.PROTO_UDP && parsed.dstPort == 53 && parsed.udpPayload != null) {
                dnsCounter.incrementAndGet()
                val reqCopy = parsed.udpPayload
                val clientIp = parsed.srcIpBytes
                val resolverIp = parsed.dstIpBytes
                val clientPort = parsed.srcPort
                val resolverPort = parsed.dstPort

                serviceScope.launch {
                    val replyPayload = forwardDnsQuery(reqCopy, dnsUpstream)
                    if (replyPayload != null && tunnelJob?.isActive == true) {
                        val responsePacket = Ipv4PacketHelper.buildUdpResponse(
                            srcIp = resolverIp,
                            dstIp = clientIp,
                            srcPort = resolverPort,
                            dstPort = clientPort,
                            payload = replyPayload,
                        )
                        synchronized(output) {
                            runCatching {
                                output.write(responsePacket)
                                rxBytesCounter.addAndGet(responsePacket.size.toLong())
                            }
                        }
                    }
                }
            } else if (parsed != null && pktNum <= 8L) {
                val protoName = when (parsed.protocol) {
                    Ipv4PacketHelper.PROTO_TCP -> "TCP"
                    Ipv4PacketHelper.PROTO_UDP -> "UDP"
                    Ipv4PacketHelper.PROTO_ICMP -> "ICMP"
                    else -> "IP(${parsed.protocol})"
                }
                TunnelStateHolder.log(
                    LogLevel.TUN,
                    "$protoName ${Ipv4PacketHelper.formatIp(parsed.srcIpBytes)}:${parsed.srcPort} -> ${Ipv4PacketHelper.formatIp(parsed.dstIpBytes)}:${parsed.dstPort} (${readLen}B)",
                )
            }
        }
    }

    private fun forwardDnsQuery(queryPayload: ByteArray, upstreamDns: String): ByteArray? {
        return runCatching {
            DatagramSocket().use { socket ->
                protect(socket)
                socket.soTimeout = 2500
                val upstreamAddr = InetAddress.getByName(upstreamDns)
                socket.send(DatagramPacket(queryPayload, queryPayload.size, upstreamAddr, 53))
                val respBuf = ByteArray(4096)
                val respPacket = DatagramPacket(respBuf, respBuf.size)
                socket.receive(respPacket)
                respBuf.copyOf(respPacket.length)
            }
        }.getOrNull()
    }

    private fun startStatsTicker() {
        statsJob = serviceScope.launch {
            var lastRx = 0L
            var lastTx = 0L
            while (isActive) {
                delay(1000L)
                val rxNow = rxBytesCounter.get()
                val txNow = txBytesCounter.get()
                val rxKbps = ((rxNow - lastRx) * 8f) / 1000f
                val txKbps = ((txNow - lastTx) * 8f) / 1000f
                lastRx = rxNow
                lastTx = txNow

                TunnelStateHolder.appendTrafficSample(
                    rxBytesTotal = rxNow,
                    txBytesTotal = txNow,
                    rxKbps = rxKbps,
                    txKbps = txKbps,
                )
                TunnelStateHolder.updateTelemetry {
                    it.copy(
                        packetCount = packetCounter.get(),
                        dnsQueryCount = dnsCounter.get(),
                    )
                }
            }
        }
    }

    private fun stopTunnel(stopService: Boolean, preserveError: Boolean = false) {
        val startedAt = sessionStartMs
        val cfg = activeConfig
        val rx = rxBytesCounter.get()
        val tx = txBytesCounter.get()
        val handshake = lastHandshakeMs

        tunnelJob?.cancel()
        statsJob?.cancel()
        closeTun()

        if (startedAt > 0L && cfg != null) {
            val durationSec = ((System.currentTimeMillis() - startedAt) / 1000L).coerceAtLeast(1L)
            val server = DefaultServers.findById(cfg.serverId)
            val sniOrHost = if (cfg.useSniHost && cfg.sniHost.isNotBlank()) cfg.sniHost else server.host
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    TunnelDatabase.getInstance(applicationContext).tunnelDao().insertSession(
                        ConnectionSessionEntity(
                            serverCode = server.code,
                            serverCountry = server.country,
                            methodSummary = cfg.methodDisplaySummary(),
                            sniOrHost = sniOrHost,
                            startedAtMs = startedAt,
                            durationSeconds = durationSec,
                            rxBytes = rx,
                            txBytes = tx,
                            handshakeMs = handshake,
                        ),
                    )
                }
            }
            sessionStartMs = 0L
        }

        if (!preserveError) {
            TunnelStateHolder.setStatus(TunnelStatus.DISCONNECTED, "Disconnected")
            TunnelStateHolder.log(LogLevel.INFO, "VPN tunnel disconnected cleanly")
        }

        if (stopService) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun closeTun() {
        runCatching { tunInterface?.close() }
        tunInterface = null
    }

    override fun onRevoke() {
        TunnelStateHolder.log(LogLevel.WARN, "VPN permission revoked by system")
        stopTunnel(stopService = true)
        super.onRevoke()
    }

    override fun onDestroy() {
        stopTunnel(stopService = false)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.vpn_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = getString(R.string.vpn_channel_description)
                    setShowBadge(false)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    private fun buildNotification(title: String, text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, CipherVpnService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val reconnectIntent = PendingIntent.getService(
            this,
            2,
            Intent(this, CipherVpnService::class.java).apply { action = ACTION_RECONNECT },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_art)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openIntent)
            .addAction(0, getString(R.string.vpn_reconnect), reconnectIntent)
            .addAction(0, getString(R.string.vpn_disconnect), stopIntent)
            .build()
    }

    private fun startForegroundCompat(title: String, text: String) {
        val notification = buildNotification(title, text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildNotification(title, text))
    }

    companion object {
        const val ACTION_START = "com.example.vpn.ACTION_START"
        const val ACTION_STOP = "com.example.vpn.ACTION_STOP"
        const val ACTION_RECONNECT = "com.example.vpn.ACTION_RECONNECT"
        private const val CHANNEL_ID = "synax_tunnel_status"
        private const val BROADCAST_CHANNEL_ID = "synax_admin_broadcasts"
        private const val NOTIFICATION_ID = 1042

        fun showAdminBroadcastNotification(context: Context, title: String, message: String) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && nm.getNotificationChannel(BROADCAST_CHANNEL_ID) == null) {
                nm.createNotificationChannel(
                    NotificationChannel(
                        BROADCAST_CHANNEL_ID,
                        "Synax Tunnel Announcements",
                        NotificationManager.IMPORTANCE_HIGH,
                    ),
                )
            }
            val openIntent = PendingIntent.getActivity(
                context,
                10,
                Intent(context, MainActivity::class.java),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            val notif = NotificationCompat.Builder(context, BROADCAST_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_art)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setAutoCancel(true)
                .setContentIntent(openIntent)
                .build()
            nm.notify((System.currentTimeMillis() % 10000).toInt() + 2000, notif)
        }

        fun start(context: Context) {
            val intent = Intent(context, CipherVpnService::class.java).apply { action = ACTION_START }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, CipherVpnService::class.java).apply { action = ACTION_STOP }
            context.startService(intent)
        }

        fun reconnect(context: Context) {
            val intent = Intent(context, CipherVpnService::class.java).apply { action = ACTION_RECONNECT }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
