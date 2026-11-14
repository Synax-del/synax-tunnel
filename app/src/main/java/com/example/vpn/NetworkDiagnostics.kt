package com.example.vpn

import com.example.model.HostCheckResult
import com.example.model.PortScanResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit
import javax.net.ssl.SNIHostName
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object NetworkDiagnostics {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(false)
        .build()

    suspend fun pingTcp(host: String, port: Int, timeoutMs: Int = 2500): Long? =
        withContext(Dispatchers.IO) {
            runCatching {
                val startNs = System.nanoTime()
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(host, port), timeoutMs)
                }
                val elapsedMs = (System.nanoTime() - startNs) / 1_000_000L
                elapsedMs.coerceAtLeast(1L)
            }.getOrNull()
        }

    suspend fun checkHost(rawHost: String, port: Int = 443, method: String = "HEAD"): HostCheckResult =
        withContext(Dispatchers.IO) {
            val cleanHost = rawHost.trim()
                .removePrefix("https://")
                .removePrefix("http://")
                .substringBefore("/")
                .substringBefore(":")
            if (cleanHost.isBlank()) {
                return@withContext HostCheckResult(
                    host = rawHost,
                    resolvedIp = null,
                    httpStatusCode = null,
                    serverHeader = null,
                    tlsVersion = null,
                    tlsCipher = null,
                    rttMs = 0L,
                    error = "Enter a valid hostname or domain",
                )
            }

            val startNs = System.nanoTime()
            var resolvedIp: String? = null
            var tlsVersion: String? = null
            var tlsCipher: String? = null
            var httpCode: Int? = null
            var serverHeader: String? = null
            var errorMsg: String? = null

            try {
                resolvedIp = InetAddress.getByName(cleanHost).hostAddress

                if (port == 443 || port == 8443) {
                    val factory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    (factory.createSocket() as SSLSocket).use { ssl ->
                        val params = ssl.sslParameters
                        params.serverNames = listOf(SNIHostName(cleanHost))
                        ssl.sslParameters = params
                        ssl.soTimeout = 5000
                        ssl.connect(InetSocketAddress(resolvedIp ?: cleanHost, port), 5000)
                        ssl.startHandshake()
                        tlsVersion = ssl.session.protocol
                        tlsCipher = ssl.session.cipherSuite
                    }
                }

                val scheme = if (port == 80 || port == 8080) "http" else "https"
                val urlPort = if ((scheme == "https" && port == 443) || (scheme == "http" && port == 80)) "" else ":$port"
                val requestBuilder = Request.Builder()
                    .url("$scheme://$cleanHost$urlPort/")
                    .header("User-Agent", "CipherTunnel-HostChecker/1.0")
                    .header("Connection", "close")
                when (method.uppercase()) {
                    "GET" -> requestBuilder.get()
                    else -> requestBuilder.head()
                }
                httpClient.newCall(requestBuilder.build()).execute().use { resp ->
                    httpCode = resp.code
                    serverHeader = resp.header("Server") ?: resp.header("Via")
                }
            } catch (e: Exception) {
                errorMsg = e.localizedMessage ?: e.javaClass.simpleName
            }

            val rtt = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(1L)
            HostCheckResult(
                host = cleanHost,
                resolvedIp = resolvedIp,
                httpStatusCode = httpCode,
                serverHeader = serverHeader,
                tlsVersion = tlsVersion,
                tlsCipher = tlsCipher,
                rttMs = rtt,
                error = if (httpCode == null && tlsVersion == null) errorMsg else null,
            )
        }

    suspend fun scanPorts(host: String): List<PortScanResult> = withContext(Dispatchers.IO) {
        val cleanHost = host.trim().removePrefix("https://").removePrefix("http://").substringBefore("/")
        val targets = listOf(
            22 to "SSH",
            53 to "DNS",
            80 to "HTTP",
            443 to "HTTPS / TLS",
            3128 to "Squid Proxy",
            8080 to "HTTP Alt",
            8443 to "TLS Alt",
            1080 to "SOCKS5",
        )
        coroutineScope {
            targets.map { (port, label) ->
                async {
                    val rtt = pingTcp(cleanHost, port, timeoutMs = 1800)
                    PortScanResult(
                        port = port,
                        serviceName = label,
                        isOpen = rtt != null,
                        rttMs = rtt,
                    )
                }
            }.awaitAll()
        }
    }

    suspend fun getLocalInterfaceIps(): List<Pair<String, String>> = withContext(Dispatchers.IO) {
        val list = mutableListOf<Pair<String, String>>()
        runCatching {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces != null && interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (!iface.isUp || iface.isLoopback) continue
                val addrs = iface.inetAddresses
                while (addrs.hasMoreElements()) {
                    val addr = addrs.nextElement()
                    val ip = addr.hostAddress?.substringBefore('%') ?: continue
                    val kind = if (addr is Inet4Address) "IPv4" else "IPv6"
                    list += "${iface.name} ($kind)" to ip
                }
            }
        }
        list
    }

    suspend fun fetchPublicIp(): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url("https://api.ipify.org?format=text")
                .get()
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("HTTP ${resp.code}")
                resp.body?.string()?.trim()?.takeIf { it.isNotBlank() } ?: error("Empty response")
            }
        }
    }
}
