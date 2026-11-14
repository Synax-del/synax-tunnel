package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.model.AdminNotificationMessage
import com.example.model.CloudConfigDocument
import com.example.model.CloudTweak
import com.example.model.ConnectionConfig
import com.example.model.TunnelProtocol
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import kotlin.random.Random

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write"),
}

fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?,
    auth: FirebaseAuth = Firebase.auth,
): String {
    val causes = generateSequence(exception) { it.cause }
    val rootError = causes.firstOrNull { it is FirebaseFirestoreException }
        ?: causes.firstOrNull { it !is CancellationException }
        ?: exception

    val currentUser = auth.currentUser
    val providerInfoList = currentUser?.providerData?.map { provider ->
        JSONObject().apply {
            put("providerId", provider.providerId)
            put("email", provider.email)
        }
    } ?: emptyList()

    val authInfoJson = JSONObject().apply {
        put("userId", currentUser?.uid)
        put("email", currentUser?.email)
        put("emailVerified", currentUser?.isEmailVerified)
        put("tenantId", currentUser?.tenantId)
        put("providerInfo", JSONArray(providerInfoList))
    }

    val errorInfoJson = JSONObject().apply {
        put("error", rootError.message ?: rootError.toString())
        put("operationType", operationType.value)
        put("path", path)
        put("authInfo", authInfoJson)
    }

    val jsonString = errorInfoJson.toString()
    Log.e("FirestoreError", "Firestore Error: $jsonString")
    return jsonString
}

class CloudTunnelRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth,
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id),
        ),
        auth = Firebase.auth,
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing Cloud Configs.")
    }

    fun observeActiveTweaks(): Flow<List<CloudTweak>> {
        val path = "cloud_tweaks"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CloudTweak::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    fun observeActiveNotifications(): Flow<List<AdminNotificationMessage>> {
        val path = "admin_notifications"
        return db.collection(path)
            .whereEqualTo("isActive", true)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    AdminNotificationMessage::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
    }

    fun observeUserCloudConfigs(userId: String): Flow<List<CloudConfigDocument>> {
        val path = "cloud_configs"
        return db.collection(path)
            .whereEqualTo("ownerId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(
                    CloudConfigDocument::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                ).sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
    }

    suspend fun publishCloudTweak(
        name: String,
        carrierTag: String,
        notes: String,
        config: ConnectionConfig,
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("cloud_tweaks").document()
        val payload = mapOf(
            "name" to name.trim().take(80),
            "carrierTag" to carrierTag.trim().ifBlank { "All Networks" }.take(40),
            "notes" to notes.trim().take(250),
            "protocol" to config.protocol.name,
            "serverId" to config.serverId,
            "customHost" to config.customHost.take(120),
            "customPort" to config.customPort.coerceIn(1, 65535),
            "useSniHost" to config.useSniHost,
            "sniHost" to config.sniHost.take(120),
            "usePayload" to config.usePayload,
            "payload" to config.payload.take(1000),
            "usePayloadAfterTls" to config.usePayloadAfterTls,
            "payloadAfterTls" to config.payloadAfterTls.take(1000),
            "useProxy" to config.useProxy,
            "proxyHost" to config.proxyHost.take(120),
            "proxyPort" to config.proxyPort.coerceIn(1, 65535),
            "obfuscatedTlsCh" to config.obfuscatedTlsCh,
            "tcpFastOpen" to config.tcpFastOpen,
            "preferIpv6" to config.preferIpv6,
            "createdBy" to uid,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudTweak(tweakId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("cloud_tweaks").document(tweakId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun publishAdminNotification(
        title: String,
        message: String,
        actionUrl: String = "",
    ): Result<String> {
        val uid = requireUserId()
        val docRef = db.collection("admin_notifications").document()
        val payload = mapOf(
            "title" to title.trim().take(100),
            "message" to message.trim().take(500),
            "actionUrl" to actionUrl.trim().take(300),
            "createdBy" to uid,
            "isActive" to true,
            "createdAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun exportCloudConfig(
        name: String,
        author: String,
        notes: String,
        config: ConnectionConfig,
        isPublic: Boolean = true,
        requestedCode: String? = null,
    ): Result<String> {
        val uid = requireUserId()
        val code = requestedCode?.trim()?.uppercase()?.take(24)
            ?.takeIf { it.matches(Regex("^[A-Za-z0-9_-]{4,24}$")) }
            ?: "SYNAX-${Random.nextInt(1000, 9999)}"
        val docRef = db.collection("cloud_configs").document(code)
        val payload = mapOf(
            "shareCode" to code,
            "ownerId" to uid,
            "name" to name.trim().ifBlank { "Synax Cloud Config" }.take(80),
            "author" to author.trim().ifBlank { "Synax User" }.take(50),
            "notes" to notes.trim().take(250),
            "protocol" to config.protocol.name,
            "serverId" to config.serverId,
            "customHost" to config.customHost.take(120),
            "customPort" to config.customPort.coerceIn(1, 65535),
            "useSniHost" to config.useSniHost,
            "sniHost" to config.sniHost.take(120),
            "usePayload" to config.usePayload,
            "payload" to config.payload.take(1000),
            "usePayloadAfterTls" to config.usePayloadAfterTls,
            "payloadAfterTls" to config.payloadAfterTls.take(1000),
            "useProxy" to config.useProxy,
            "proxyHost" to config.proxyHost.take(120),
            "proxyPort" to config.proxyPort.coerceIn(1, 65535),
            "obfuscatedTlsCh" to config.obfuscatedTlsCh,
            "tcpFastOpen" to config.tcpFastOpen,
            "preferIpv6" to config.preferIpv6,
            "isPublic" to isPublic,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        return try {
            docRef.set(payload).await()
            Result.success(code)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun importCloudConfigByCode(rawCode: String): Result<CloudConfigDocument> {
        requireUserId()
        val code = rawCode.trim().uppercase()
        if (!code.matches(Regex("^[A-Za-z0-9_-]{4,24}$"))) {
            return Result.failure(IllegalArgumentException("Enter a valid cloud code (e.g. SYNAX-1234)"))
        }
        val docRef = db.collection("cloud_configs").document(code)
        return try {
            val snap = docRef.get().await()
            val parsed = snap.toObject(
                CloudConfigDocument::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
            ) ?: return Result.failure(NoSuchElementException("No cloud config found for code \"$code\""))
            Result.success(parsed)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path, auth)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudConfig(shareCode: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("cloud_configs").document(shareCode)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path, auth)
            Result.failure(e)
        }
    }

    companion object {
        fun tweakToConnectionConfig(tweak: CloudTweak, base: ConnectionConfig): ConnectionConfig {
            val proto = runCatching { TunnelProtocol.valueOf(tweak.protocol) }.getOrDefault(TunnelProtocol.TCP)
            return base.copy(
                protocol = proto,
                serverId = tweak.serverId,
                customHost = tweak.customHost,
                customPort = tweak.customPort.toInt().coerceIn(1, 65535),
                useSniHost = tweak.useSniHost,
                sniHost = tweak.sniHost,
                usePayload = tweak.usePayload,
                payload = tweak.payload.ifBlank { base.payload },
                usePayloadAfterTls = tweak.usePayloadAfterTls,
                payloadAfterTls = tweak.payloadAfterTls.ifBlank { base.payloadAfterTls },
                useProxy = tweak.useProxy,
                proxyHost = tweak.proxyHost,
                proxyPort = tweak.proxyPort.toInt().coerceIn(1, 65535),
                obfuscatedTlsCh = tweak.obfuscatedTlsCh,
                tcpFastOpen = tweak.tcpFastOpen,
                preferIpv6 = tweak.preferIpv6,
            )
        }

        fun cloudConfigToConnectionConfig(doc: CloudConfigDocument, base: ConnectionConfig): ConnectionConfig {
            val proto = runCatching { TunnelProtocol.valueOf(doc.protocol) }.getOrDefault(TunnelProtocol.TCP)
            return base.copy(
                protocol = proto,
                serverId = doc.serverId,
                customHost = doc.customHost,
                customPort = doc.customPort.toInt().coerceIn(1, 65535),
                useSniHost = doc.useSniHost,
                sniHost = doc.sniHost,
                usePayload = doc.usePayload,
                payload = doc.payload.ifBlank { base.payload },
                usePayloadAfterTls = doc.usePayloadAfterTls,
                payloadAfterTls = doc.payloadAfterTls.ifBlank { base.payloadAfterTls },
                useProxy = doc.useProxy,
                proxyHost = doc.proxyHost,
                proxyPort = doc.proxyPort.toInt().coerceIn(1, 65535),
                obfuscatedTlsCh = doc.obfuscatedTlsCh,
                tcpFastOpen = doc.tcpFastOpen,
                preferIpv6 = doc.preferIpv6,
            )
        }
    }
}
