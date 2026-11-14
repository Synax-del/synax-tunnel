package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

@Dao
interface TunnelDao {
    @Query("SELECT * FROM tunnel_profiles ORDER BY isPreset ASC, createdAtMs DESC")
    fun observeProfiles(): Flow<List<TunnelProfileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: TunnelProfileEntity): Long

    @Query("DELETE FROM tunnel_profiles WHERE id = :id")
    suspend fun deleteProfile(id: Long)

    @Query("SELECT COUNT(*) FROM tunnel_profiles")
    suspend fun profileCount(): Int

    @Query("SELECT * FROM connection_sessions ORDER BY startedAtMs DESC LIMIT 50")
    fun observeSessions(): Flow<List<ConnectionSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ConnectionSessionEntity): Long

    @Query("DELETE FROM connection_sessions")
    suspend fun clearSessions()
}

@Database(
    entities = [TunnelProfileEntity::class, ConnectionSessionEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class TunnelDatabase : RoomDatabase() {
    abstract fun tunnelDao(): TunnelDao

    companion object {
        @Volatile private var instance: TunnelDatabase? = null

        fun getInstance(context: Context): TunnelDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext).also { instance = it }
            }
        }

        private fun buildDatabase(appContext: Context): TunnelDatabase {
            lateinit var db: TunnelDatabase
            db = Room.databaseBuilder(
                appContext,
                TunnelDatabase::class.java,
                "cipher_tunnel_db",
            ).addCallback(object : Callback() {
                override fun onCreate(sqliteDb: SupportSQLiteDatabase) {
                    super.onCreate(sqliteDb)
                    CoroutineScope(Dispatchers.IO).launch {
                        seedDefaults(db.tunnelDao())
                    }
                }
            }).build()
            return db
        }

        suspend fun seedDefaultsIfEmpty(dao: TunnelDao) {
            if (dao.profileCount() == 0) {
                seedDefaults(dao)
            }
        }

        private suspend fun seedDefaults(dao: TunnelDao) {
            val presets = listOf(
                TunnelProfileEntity(
                    name = "TikTok SNI Direct (TLS 443)",
                    author = "CipherLab",
                    notes = "Standard TLS handshake with SNI host spoofing on port 443",
                    protocol = "TCP",
                    serverId = "sg1",
                    customHost = "1.1.1.1",
                    customPort = 443,
                    useSniHost = true,
                    sniHost = "m.tiktok.com",
                    usePayload = false,
                    payload = "",
                    usePayloadAfterTls = false,
                    payloadAfterTls = "",
                    useProxy = false,
                    proxyHost = "",
                    proxyPort = 8080,
                    obfuscatedTlsCh = false,
                    tcpFastOpen = true,
                    preferIpv6 = false,
                    isPreset = true,
                ),
                TunnelProfileEntity(
                    name = "Cloudflare WS + Split Payload",
                    author = "CipherLab",
                    notes = "WebSocket upgrade payload with [split] injection & obfuscated ClientHello",
                    protocol = "TCP",
                    serverId = "us1",
                    customHost = "1.1.1.1",
                    customPort = 443,
                    useSniHost = true,
                    sniHost = "speed.cloudflare.com",
                    usePayload = true,
                    payload = "GET / HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf]Connection: Upgrade[crlf]User-Agent: [ua][crlf][crlf]",
                    usePayloadAfterTls = true,
                    payloadAfterTls = "GET /ws HTTP/1.1[crlf]Host: [host][crlf]Upgrade: websocket[crlf][crlf]",
                    useProxy = false,
                    proxyHost = "",
                    proxyPort = 8080,
                    obfuscatedTlsCh = true,
                    tcpFastOpen = true,
                    preferIpv6 = false,
                    isPreset = true,
                ),
                TunnelProfileEntity(
                    name = "WhatsApp Zero-Rate SNI",
                    author = "CipherLab",
                    notes = "Obfuscated TLS ClientHello targeting messaging CDN SNI",
                    protocol = "TCP",
                    serverId = "uk1",
                    customHost = "9.9.9.9",
                    customPort = 443,
                    useSniHost = true,
                    sniHost = "v.whatsapp.net",
                    usePayload = false,
                    payload = "",
                    usePayloadAfterTls = false,
                    payloadAfterTls = "",
                    useProxy = false,
                    proxyHost = "",
                    proxyPort = 8080,
                    obfuscatedTlsCh = true,
                    tcpFastOpen = false,
                    preferIpv6 = false,
                    isPreset = true,
                ),
                TunnelProfileEntity(
                    name = "UDP 53 Fast DNS Relay",
                    author = "CipherLab",
                    notes = "Low-latency UDP tunnel forwarding with DNS-over-UDP relay",
                    protocol = "UDP",
                    serverId = "de1",
                    customHost = "1.0.0.1",
                    customPort = 53,
                    useSniHost = false,
                    sniHost = "",
                    usePayload = false,
                    payload = "",
                    usePayloadAfterTls = false,
                    payloadAfterTls = "",
                    useProxy = false,
                    proxyHost = "",
                    proxyPort = 8080,
                    obfuscatedTlsCh = false,
                    tcpFastOpen = false,
                    preferIpv6 = false,
                    isPreset = true,
                ),
            )
            presets.forEach { dao.insertProfile(it) }
        }
    }
}
