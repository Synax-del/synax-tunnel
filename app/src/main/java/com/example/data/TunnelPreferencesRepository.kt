package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.AppThemeMode
import com.example.model.ConnectionConfig
import com.example.model.RoutingMode
import com.example.model.TunnelProtocol
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONObject

private val Context.tunnelDataStore by preferencesDataStore(name = "synax_tunnel_prefs")

data class AppUserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val remainingTimeSeconds: Long = 4L * 3600L, // 4 hours starter pack
    val telegramPopupDismissed: Boolean = false,
    val telegramUrl: String = "https://t.me/synaxtunnel",
    val adminPin: String = "2026",
)

class TunnelPreferencesRepository(private val context: Context) {

    private object Keys {
        val SERVER_ID = stringPreferencesKey("server_id")
        val CUSTOM_HOST = stringPreferencesKey("custom_host")
        val CUSTOM_PORT = intPreferencesKey("custom_port")
        val PROTOCOL = stringPreferencesKey("protocol")
        val PREFER_IPV6 = booleanPreferencesKey("prefer_ipv6")
        val TCP_FAST_OPEN = booleanPreferencesKey("tcp_fast_open")
        val OBFUSCATED_TLS_CH = booleanPreferencesKey("obfuscated_tls_ch")
        val USE_PAYLOAD = booleanPreferencesKey("use_payload")
        val PAYLOAD = stringPreferencesKey("payload")
        val USE_SNI_HOST = booleanPreferencesKey("use_sni_host")
        val SNI_HOST = stringPreferencesKey("sni_host")
        val USE_PAYLOAD_AFTER_TLS = booleanPreferencesKey("use_payload_after_tls")
        val PAYLOAD_AFTER_TLS = stringPreferencesKey("payload_after_tls")
        val USE_PROXY = booleanPreferencesKey("use_proxy")
        val PROXY_HOST = stringPreferencesKey("proxy_host")
        val PROXY_PORT = intPreferencesKey("proxy_port")
        val UDP_FORWARDING = booleanPreferencesKey("udp_forwarding")
        val DNS_FORWARDING = booleanPreferencesKey("dns_forwarding")
        val QUIC_OBFUSCATION = booleanPreferencesKey("quic_obfuscation")
        val MTU = intPreferencesKey("mtu")
        val PRIMARY_DNS = stringPreferencesKey("primary_dns")
        val SECONDARY_DNS = stringPreferencesKey("secondary_dns")
        val ROUTING_MODE = stringPreferencesKey("routing_mode")
        val WAKE_LOCK = booleanPreferencesKey("wake_lock")
        val EXCLUDED_PACKAGES = stringSetPreferencesKey("excluded_packages")

        // App-level Synax Tunnel settings
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val REMAINING_TIME_SECONDS = longPreferencesKey("remaining_time_seconds")
        val TELEGRAM_DISMISSED = booleanPreferencesKey("telegram_popup_dismissed")
        val TELEGRAM_URL = stringPreferencesKey("telegram_url")
        val ADMIN_PIN = stringPreferencesKey("admin_pin")
    }

    val configFlow: Flow<ConnectionConfig> = context.tunnelDataStore.data.map { prefs ->
        val default = ConnectionConfig()
        ConnectionConfig(
            serverId = prefs[Keys.SERVER_ID] ?: default.serverId,
            customHost = prefs[Keys.CUSTOM_HOST] ?: default.customHost,
            customPort = prefs[Keys.CUSTOM_PORT] ?: default.customPort,
            protocol = runCatching {
                TunnelProtocol.valueOf(prefs[Keys.PROTOCOL] ?: default.protocol.name)
            }.getOrDefault(default.protocol),
            preferIpv6 = prefs[Keys.PREFER_IPV6] ?: default.preferIpv6,
            tcpFastOpen = prefs[Keys.TCP_FAST_OPEN] ?: default.tcpFastOpen,
            obfuscatedTlsCh = prefs[Keys.OBFUSCATED_TLS_CH] ?: default.obfuscatedTlsCh,
            usePayload = prefs[Keys.USE_PAYLOAD] ?: default.usePayload,
            payload = prefs[Keys.PAYLOAD] ?: default.payload,
            useSniHost = prefs[Keys.USE_SNI_HOST] ?: default.useSniHost,
            sniHost = prefs[Keys.SNI_HOST] ?: default.sniHost,
            usePayloadAfterTls = prefs[Keys.USE_PAYLOAD_AFTER_TLS] ?: default.usePayloadAfterTls,
            payloadAfterTls = prefs[Keys.PAYLOAD_AFTER_TLS] ?: default.payloadAfterTls,
            useProxy = prefs[Keys.USE_PROXY] ?: default.useProxy,
            proxyHost = prefs[Keys.PROXY_HOST] ?: default.proxyHost,
            proxyPort = prefs[Keys.PROXY_PORT] ?: default.proxyPort,
            udpForwarding = prefs[Keys.UDP_FORWARDING] ?: default.udpForwarding,
            dnsForwarding = prefs[Keys.DNS_FORWARDING] ?: default.dnsForwarding,
            quicObfuscation = prefs[Keys.QUIC_OBFUSCATION] ?: default.quicObfuscation,
            mtu = prefs[Keys.MTU] ?: default.mtu,
            primaryDns = prefs[Keys.PRIMARY_DNS] ?: default.primaryDns,
            secondaryDns = prefs[Keys.SECONDARY_DNS] ?: default.secondaryDns,
            routingMode = runCatching {
                RoutingMode.valueOf(prefs[Keys.ROUTING_MODE] ?: default.routingMode.name)
            }.getOrDefault(default.routingMode),
            wakeLock = prefs[Keys.WAKE_LOCK] ?: default.wakeLock,
            excludedPackages = prefs[Keys.EXCLUDED_PACKAGES] ?: default.excludedPackages,
        )
    }

    val appSettingsFlow: Flow<AppUserSettings> = context.tunnelDataStore.data.map { prefs ->
        val default = AppUserSettings()
        AppUserSettings(
            themeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: default.themeMode.name)
            }.getOrDefault(default.themeMode),
            remainingTimeSeconds = prefs[Keys.REMAINING_TIME_SECONDS] ?: default.remainingTimeSeconds,
            telegramPopupDismissed = prefs[Keys.TELEGRAM_DISMISSED] ?: default.telegramPopupDismissed,
            telegramUrl = prefs[Keys.TELEGRAM_URL] ?: default.telegramUrl,
            adminPin = prefs[Keys.ADMIN_PIN] ?: default.adminPin,
        )
    }

    suspend fun saveConfig(config: ConnectionConfig) {
        context.tunnelDataStore.edit { prefs ->
            prefs[Keys.SERVER_ID] = config.serverId
            prefs[Keys.CUSTOM_HOST] = config.customHost
            prefs[Keys.CUSTOM_PORT] = config.customPort
            prefs[Keys.PROTOCOL] = config.protocol.name
            prefs[Keys.PREFER_IPV6] = config.preferIpv6
            prefs[Keys.TCP_FAST_OPEN] = config.tcpFastOpen
            prefs[Keys.OBFUSCATED_TLS_CH] = config.obfuscatedTlsCh
            prefs[Keys.USE_PAYLOAD] = config.usePayload
            prefs[Keys.PAYLOAD] = config.payload
            prefs[Keys.USE_SNI_HOST] = config.useSniHost
            prefs[Keys.SNI_HOST] = config.sniHost
            prefs[Keys.USE_PAYLOAD_AFTER_TLS] = config.usePayloadAfterTls
            prefs[Keys.PAYLOAD_AFTER_TLS] = config.payloadAfterTls
            prefs[Keys.USE_PROXY] = config.useProxy
            prefs[Keys.PROXY_HOST] = config.proxyHost
            prefs[Keys.PROXY_PORT] = config.proxyPort
            prefs[Keys.UDP_FORWARDING] = config.udpForwarding
            prefs[Keys.DNS_FORWARDING] = config.dnsForwarding
            prefs[Keys.QUIC_OBFUSCATION] = config.quicObfuscation
            prefs[Keys.MTU] = config.mtu
            prefs[Keys.PRIMARY_DNS] = config.primaryDns
            prefs[Keys.SECONDARY_DNS] = config.secondaryDns
            prefs[Keys.ROUTING_MODE] = config.routingMode.name
            prefs[Keys.WAKE_LOCK] = config.wakeLock
            prefs[Keys.EXCLUDED_PACKAGES] = config.excludedPackages
        }
    }

    suspend fun setThemeMode(mode: AppThemeMode) {
        context.tunnelDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun addTimeSeconds(deltaSeconds: Long) {
        context.tunnelDataStore.edit { prefs ->
            val current = prefs[Keys.REMAINING_TIME_SECONDS] ?: (4L * 3600L)
            prefs[Keys.REMAINING_TIME_SECONDS] = (current + deltaSeconds).coerceIn(0L, 30L * 86400L)
        }
    }

    suspend fun setRemainingTimeSeconds(seconds: Long) {
        context.tunnelDataStore.edit { prefs ->
            prefs[Keys.REMAINING_TIME_SECONDS] = seconds.coerceAtLeast(0L)
        }
    }

    suspend fun setTelegramPopupDismissed(dismissed: Boolean) {
        context.tunnelDataStore.edit { it[Keys.TELEGRAM_DISMISSED] = dismissed }
    }

    suspend fun setTelegramUrl(url: String) {
        context.tunnelDataStore.edit { it[Keys.TELEGRAM_URL] = url.trim().ifBlank { "https://t.me/synaxtunnel" } }
    }

    suspend fun setAdminPin(pin: String) {
        val clean = pin.trim()
        if (clean.length >= 4) {
            context.tunnelDataStore.edit { it[Keys.ADMIN_PIN] = clean }
        }
    }

    companion object {
        fun exportToJson(config: ConnectionConfig, profileName: String, author: String, notes: String): String {
            val json = JSONObject()
            json.put("format", "synax-tunnel-v1")
            json.put("name", profileName)
            json.put("author", author)
            json.put("notes", notes)
            json.put("serverId", config.serverId)
            json.put("customHost", config.customHost)
            json.put("customPort", config.customPort)
            json.put("protocol", config.protocol.name)
            json.put("preferIpv6", config.preferIpv6)
            json.put("tcpFastOpen", config.tcpFastOpen)
            json.put("obfuscatedTlsCh", config.obfuscatedTlsCh)
            json.put("usePayload", config.usePayload)
            json.put("payload", config.payload)
            json.put("useSniHost", config.useSniHost)
            json.put("sniHost", config.sniHost)
            json.put("usePayloadAfterTls", config.usePayloadAfterTls)
            json.put("payloadAfterTls", config.payloadAfterTls)
            json.put("useProxy", config.useProxy)
            json.put("proxyHost", config.proxyHost)
            json.put("proxyPort", config.proxyPort)
            json.put("udpForwarding", config.udpForwarding)
            json.put("dnsForwarding", config.dnsForwarding)
            json.put("quicObfuscation", config.quicObfuscation)
            json.put("mtu", config.mtu)
            json.put("primaryDns", config.primaryDns)
            json.put("secondaryDns", config.secondaryDns)
            return json.toString(2)
        }

        fun importFromJson(rawJson: String): Result<Pair<TunnelProfileEntity, ConnectionConfig>> = runCatching {
            val obj = JSONObject(rawJson.trim())
            val default = ConnectionConfig()
            val protocol = runCatching {
                TunnelProtocol.valueOf(obj.optString("protocol", default.protocol.name))
            }.getOrDefault(default.protocol)
            val config = default.copy(
                serverId = obj.optString("serverId", default.serverId),
                customHost = obj.optString("customHost", default.customHost),
                customPort = obj.optInt("customPort", default.customPort),
                protocol = protocol,
                preferIpv6 = obj.optBoolean("preferIpv6", default.preferIpv6),
                tcpFastOpen = obj.optBoolean("tcpFastOpen", default.tcpFastOpen),
                obfuscatedTlsCh = obj.optBoolean("obfuscatedTlsCh", default.obfuscatedTlsCh),
                usePayload = obj.optBoolean("usePayload", default.usePayload),
                payload = obj.optString("payload", default.payload),
                useSniHost = obj.optBoolean("useSniHost", default.useSniHost),
                sniHost = obj.optString("sniHost", default.sniHost),
                usePayloadAfterTls = obj.optBoolean("usePayloadAfterTls", default.usePayloadAfterTls),
                payloadAfterTls = obj.optString("payloadAfterTls", default.payloadAfterTls),
                useProxy = obj.optBoolean("useProxy", default.useProxy),
                proxyHost = obj.optString("proxyHost", default.proxyHost),
                proxyPort = obj.optInt("proxyPort", default.proxyPort),
                udpForwarding = obj.optBoolean("udpForwarding", default.udpForwarding),
                dnsForwarding = obj.optBoolean("dnsForwarding", default.dnsForwarding),
                quicObfuscation = obj.optBoolean("quicObfuscation", default.quicObfuscation),
                mtu = obj.optInt("mtu", default.mtu),
                primaryDns = obj.optString("primaryDns", default.primaryDns),
                secondaryDns = obj.optString("secondaryDns", default.secondaryDns),
            )
            val profile = TunnelProfileEntity(
                name = obj.optString("name", "Imported Synax Config").ifBlank { "Imported Synax Config" },
                author = obj.optString("author", "External"),
                notes = obj.optString("notes", "Imported .synax configuration"),
                protocol = config.protocol.name,
                serverId = config.serverId,
                customHost = config.customHost,
                customPort = config.customPort,
                useSniHost = config.useSniHost,
                sniHost = config.sniHost,
                usePayload = config.usePayload,
                payload = config.payload,
                usePayloadAfterTls = config.usePayloadAfterTls,
                payloadAfterTls = config.payloadAfterTls,
                useProxy = config.useProxy,
                proxyHost = config.proxyHost,
                proxyPort = config.proxyPort,
                obfuscatedTlsCh = config.obfuscatedTlsCh,
                tcpFastOpen = config.tcpFastOpen,
                preferIpv6 = config.preferIpv6,
                isPreset = false,
            )
            profile to config
        }
    }
}
