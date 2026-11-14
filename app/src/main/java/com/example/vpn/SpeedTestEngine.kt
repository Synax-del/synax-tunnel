package com.example.vpn

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.abs

enum class SpeedTestPhase {
    IDLE,
    PINGING,
    DOWNLOADING,
    UPLOADING,
    COMPLETED,
    ERROR,
}

data class SpeedTestState(
    val phase: SpeedTestPhase = SpeedTestPhase.IDLE,
    val liveMbps: Float = 0f,
    val downloadMbps: Float = 0f,
    val uploadMbps: Float = 0f,
    val pingMs: Long? = null,
    val jitterMs: Long? = null,
    val progress: Float = 0f,
    val serverLabel: String = "Cloudflare Edge",
    val errorMessage: String? = null,
)

object SpeedTestEngine {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    fun runSpeedTest(): Flow<SpeedTestState> = flow {
        emit(SpeedTestState(phase = SpeedTestPhase.PINGING, progress = 0.05f))

        // 1. Measure Ping & Jitter over 4 TCP probes
        val rtts = mutableListOf<Long>()
        for (i in 0 until 4) {
            if (!currentCoroutineContext().isActive) return@flow
            val start = System.nanoTime()
            val ok = runCatching {
                Socket().use { s ->
                    s.connect(InetSocketAddress("1.1.1.1", 443), 2500)
                }
            }.isSuccess
            if (ok) {
                rtts += ((System.nanoTime() - start) / 1_000_000L).coerceAtLeast(1L)
            }
        }
        if (rtts.isEmpty()) {
            emit(
                SpeedTestState(
                    phase = SpeedTestPhase.ERROR,
                    errorMessage = "Network unreachable. Check your internet connection.",
                ),
            )
            return@flow
        }

        val avgPing = rtts.average().toLong()
        val jitter = if (rtts.size > 1) {
            rtts.zipWithNext { a, b -> abs(a - b) }.average().toLong()
        } else {
            1L
        }

        emit(
            SpeedTestState(
                phase = SpeedTestPhase.DOWNLOADING,
                pingMs = avgPing,
                jitterMs = jitter,
                progress = 0.15f,
            ),
        )

        // 2. Real Download Throughput via Cloudflare Speed Edge (5 MB stream)
        var measuredDownMbps = 0f
        try {
            val downReq = Request.Builder()
                .url("https://speed.cloudflare.com/__down?bytes=5000000")
                .header("Cache-Control", "no-cache")
                .get()
                .build()

            client.newCall(downReq).execute().use { resp ->
                if (!resp.isSuccessful) error("Download HTTP ${resp.code}")
                val body = resp.body ?: error("Empty download body")
                val stream = body.byteStream()
                val buf = ByteArray(16384)
                var totalBytes = 0L
                val startNs = System.nanoTime()
                var lastEmitNs = startNs

                while (currentCoroutineContext().isActive) {
                    val read = stream.read(buf)
                    if (read <= 0) break
                    totalBytes += read
                    val nowNs = System.nanoTime()
                    val elapsedSec = ((nowNs - startNs) / 1_000_000_000f).coerceAtLeast(0.05f)
                    measuredDownMbps = (totalBytes * 8f) / (elapsedSec * 1_000_000f)
                    if (nowNs - lastEmitNs > 120_000_000L) {
                        lastEmitNs = nowNs
                        val frac = (totalBytes / 5_000_000f).coerceIn(0f, 1f)
                        emit(
                            SpeedTestState(
                                phase = SpeedTestPhase.DOWNLOADING,
                                liveMbps = measuredDownMbps,
                                downloadMbps = measuredDownMbps,
                                pingMs = avgPing,
                                jitterMs = jitter,
                                progress = 0.15f + frac * 0.45f,
                            ),
                        )
                    }
                    // Cap max test duration to ~5s so slow networks stay snappy
                    if (elapsedSec > 5.5f) break
                }
            }
        } catch (e: Exception) {
            emit(
                SpeedTestState(
                    phase = SpeedTestPhase.ERROR,
                    pingMs = avgPing,
                    jitterMs = jitter,
                    errorMessage = "Download test failed: ${e.localizedMessage ?: "Network error"}",
                ),
            )
            return@flow
        }

        // 3. Real Upload Throughput (1.5 MB chunked stream)
        emit(
            SpeedTestState(
                phase = SpeedTestPhase.UPLOADING,
                liveMbps = 0f,
                downloadMbps = measuredDownMbps,
                pingMs = avgPing,
                jitterMs = jitter,
                progress = 0.65f,
            ),
        )

        var measuredUpMbps = 0f
        try {
            val uploadTotal = 1_500_000L
            val chunk = ByteArray(16384) { 0x5A }
            val startUpNs = System.nanoTime()

            val uploadBody = object : RequestBody() {
                override fun contentType() = "application/octet-stream".toMediaType()
                override fun contentLength(): Long = uploadTotal
                override fun writeTo(sink: BufferedSink) {
                    var sent = 0L
                    while (sent < uploadTotal) {
                        val toWrite = minOf(chunk.size.toLong(), uploadTotal - sent).toInt()
                        sink.write(chunk, 0, toWrite)
                        sent += toWrite
                    }
                    sink.flush()
                }
            }

            val upReq = Request.Builder()
                .url("https://speed.cloudflare.com/__up")
                .post(uploadBody)
                .build()

            client.newCall(upReq).execute().use { resp ->
                val elapsedSec = ((System.nanoTime() - startUpNs) / 1_000_000_000f).coerceAtLeast(0.05f)
                if (resp.isSuccessful) {
                    measuredUpMbps = (uploadTotal * 8f) / (elapsedSec * 1_000_000f)
                }
            }
        } catch (_: Exception) {
            // Upload endpoint fallback: keep download & ping results intact
            measuredUpMbps = 0f
        }

        emit(
            SpeedTestState(
                phase = SpeedTestPhase.COMPLETED,
                liveMbps = measuredDownMbps,
                downloadMbps = measuredDownMbps,
                uploadMbps = measuredUpMbps,
                pingMs = avgPing,
                jitterMs = jitter,
                progress = 1f,
            ),
        )
    }.flowOn(Dispatchers.IO)
}
