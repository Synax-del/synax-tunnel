package com.example.vpn

import com.example.model.LogLevel
import com.example.model.TrafficPoint
import com.example.model.TunnelLogEntry
import com.example.model.TunnelStatus
import com.example.model.TunnelTelemetry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

object TunnelStateHolder {

    private val logIdGen = AtomicLong(1L)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.US)

    private val _telemetry = MutableStateFlow(TunnelTelemetry())
    val telemetry: StateFlow<TunnelTelemetry> = _telemetry.asStateFlow()

    private val _logs = MutableStateFlow<List<TunnelLogEntry>>(
        listOf(
            TunnelLogEntry(
                id = logIdGen.getAndIncrement(),
                timestamp = currentTimeStr(),
                level = LogLevel.INFO,
                message = "Cipher Tunnel core initialized • Ready for TLS/SNI or UDP routing",
            ),
        ),
    )
    val logs: StateFlow<List<TunnelLogEntry>> = _logs.asStateFlow()

    private val _serverLatencies = MutableStateFlow<Map<String, Long?>>(emptyMap())
    val serverLatencies: StateFlow<Map<String, Long?>> = _serverLatencies.asStateFlow()

    fun updateLatency(serverId: String, latencyMs: Long?) {
        _serverLatencies.update { it + (serverId to latencyMs) }
    }

    fun updateTelemetry(transform: (TunnelTelemetry) -> TunnelTelemetry) {
        _telemetry.update(transform)
    }

    fun appendTrafficSample(rxBytesTotal: Long, txBytesTotal: Long, rxKbps: Float, txKbps: Float) {
        val now = System.currentTimeMillis()
        _telemetry.update { current ->
            val updatedHistory = (current.trafficHistory + TrafficPoint(rxKbps, txKbps, now)).takeLast(45)
            current.copy(
                rxBytes = rxBytesTotal,
                txBytes = txBytesTotal,
                rxKbps = rxKbps,
                txKbps = txKbps,
                trafficHistory = updatedHistory,
            )
        }
    }

    fun setStatus(status: TunnelStatus, detail: String) {
        _telemetry.update { current ->
            current.copy(
                status = status,
                statusDetail = detail,
                connectedSinceMs = if (status == TunnelStatus.CONNECTED && current.connectedSinceMs == 0L) {
                    System.currentTimeMillis()
                } else if (status == TunnelStatus.DISCONNECTED || status == TunnelStatus.ERROR) {
                    0L
                } else {
                    current.connectedSinceMs
                },
            )
        }
    }

    fun log(level: LogLevel, message: String) {
        val entry = TunnelLogEntry(
            id = logIdGen.getAndIncrement(),
            timestamp = currentTimeStr(),
            level = level,
            message = message,
        )
        _logs.update { current -> (current + entry).takeLast(400) }
    }

    fun clearLogs() {
        _logs.value = emptyList()
        log(LogLevel.INFO, "Log buffer cleared")
    }

    private fun currentTimeStr(): String = synchronized(timeFormat) {
        timeFormat.format(Date())
    }
}
