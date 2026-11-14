package com.example.vpn

import java.net.InetAddress

data class ParsedIpv4Packet(
    val version: Int,
    val headerLength: Int,
    val totalLength: Int,
    val protocol: Int,
    val srcIpBytes: ByteArray,
    val dstIpBytes: ByteArray,
    val srcPort: Int,
    val dstPort: Int,
    val udpPayload: ByteArray?,
)

object Ipv4PacketHelper {
    const val PROTO_ICMP = 1
    const val PROTO_TCP = 6
    const val PROTO_UDP = 17

    fun parse(packet: ByteArray, length: Int): ParsedIpv4Packet? {
        if (length < 20) return null
        val version = (packet[0].toInt() ushr 4) and 0x0F
        if (version != 4) return null
        val ihl = (packet[0].toInt() and 0x0F) * 4
        if (ihl < 20 || length < ihl) return null

        val totalLen = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
        val effectiveLen = minOf(length, totalLen)
        val protocol = packet[9].toInt() and 0xFF
        val srcIp = packet.copyOfRange(12, 16)
        val dstIp = packet.copyOfRange(16, 20)

        var srcPort = 0
        var dstPort = 0
        var udpPayload: ByteArray? = null

        if ((protocol == PROTO_UDP || protocol == PROTO_TCP) && effectiveLen >= ihl + 4) {
            srcPort = ((packet[ihl].toInt() and 0xFF) shl 8) or (packet[ihl + 1].toInt() and 0xFF)
            dstPort = ((packet[ihl + 2].toInt() and 0xFF) shl 8) or (packet[ihl + 3].toInt() and 0xFF)
            if (protocol == PROTO_UDP && effectiveLen >= ihl + 8) {
                val udpLen = ((packet[ihl + 4].toInt() and 0xFF) shl 8) or (packet[ihl + 5].toInt() and 0xFF)
                val payloadStart = ihl + 8
                val payloadEnd = minOf(effectiveLen, ihl + udpLen)
                if (payloadEnd > payloadStart) {
                    udpPayload = packet.copyOfRange(payloadStart, payloadEnd)
                }
            }
        }

        return ParsedIpv4Packet(
            version = version,
            headerLength = ihl,
            totalLength = effectiveLen,
            protocol = protocol,
            srcIpBytes = srcIp,
            dstIpBytes = dstIp,
            srcPort = srcPort,
            dstPort = dstPort,
            udpPayload = udpPayload,
        )
    }

    fun buildUdpResponse(
        srcIp: ByteArray,
        dstIp: ByteArray,
        srcPort: Int,
        dstPort: Int,
        payload: ByteArray,
    ): ByteArray {
        val ipHeaderLen = 20
        val udpLen = 8 + payload.size
        val totalLen = ipHeaderLen + udpLen
        val out = ByteArray(totalLen)

        // IPv4 Header
        out[0] = 0x45.toByte()
        out[1] = 0x00
        out[2] = ((totalLen ushr 8) and 0xFF).toByte()
        out[3] = (totalLen and 0xFF).toByte()
        out[4] = 0x00
        out[5] = 0x01
        out[6] = 0x40.toByte() // Don't Fragment
        out[7] = 0x00
        out[8] = 64.toByte() // TTL
        out[9] = PROTO_UDP.toByte()
        // Checksum [10..11] filled after
        System.arraycopy(srcIp, 0, out, 12, 4)
        System.arraycopy(dstIp, 0, out, 16, 4)

        val ipChecksum = computeChecksum(out, 0, ipHeaderLen)
        out[10] = ((ipChecksum ushr 8) and 0xFF).toByte()
        out[11] = (ipChecksum and 0xFF).toByte()

        // UDP Header
        out[20] = ((srcPort ushr 8) and 0xFF).toByte()
        out[21] = (srcPort and 0xFF).toByte()
        out[22] = ((dstPort ushr 8) and 0xFF).toByte()
        out[23] = (dstPort and 0xFF).toByte()
        out[24] = ((udpLen ushr 8) and 0xFF).toByte()
        out[25] = (udpLen and 0xFF).toByte()
        // UDP checksum optional in IPv4 (0x0000), or computed with pseudo-header
        out[26] = 0x00
        out[27] = 0x00
        System.arraycopy(payload, 0, out, 28, payload.size)

        val udpChecksum = computeUdpChecksum(srcIp, dstIp, out, 20, udpLen)
        out[26] = ((udpChecksum ushr 8) and 0xFF).toByte()
        out[27] = (udpChecksum and 0xFF).toByte()

        return out
    }

    fun formatIp(bytes: ByteArray): String =
        runCatching { InetAddress.getByAddress(bytes).hostAddress ?: "0.0.0.0" }.getOrDefault("0.0.0.0")

    private fun computeChecksum(buf: ByteArray, offset: Int, len: Int): Int {
        var sum = 0L
        var i = offset
        val end = offset + len
        while (i + 1 < end) {
            val word = ((buf[i].toInt() and 0xFF) shl 8) or (buf[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (buf[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFFL) + (sum ushr 16)
        }
        return (sum.inv() and 0xFFFFL).toInt()
    }

    private fun computeUdpChecksum(
        srcIp: ByteArray,
        dstIp: ByteArray,
        udpSegment: ByteArray,
        udpOffset: Int,
        udpLen: Int,
    ): Int {
        var sum = 0L
        // Pseudo header
        sum += ((srcIp[0].toInt() and 0xFF) shl 8) or (srcIp[1].toInt() and 0xFF)
        sum += ((srcIp[2].toInt() and 0xFF) shl 8) or (srcIp[3].toInt() and 0xFF)
        sum += ((dstIp[0].toInt() and 0xFF) shl 8) or (dstIp[1].toInt() and 0xFF)
        sum += ((dstIp[2].toInt() and 0xFF) shl 8) or (dstIp[3].toInt() and 0xFF)
        sum += PROTO_UDP
        sum += udpLen

        var i = udpOffset
        val end = udpOffset + udpLen
        while (i + 1 < end) {
            val word = ((udpSegment[i].toInt() and 0xFF) shl 8) or (udpSegment[i + 1].toInt() and 0xFF)
            sum += word
            i += 2
        }
        if (i < end) {
            sum += (udpSegment[i].toInt() and 0xFF) shl 8
        }
        while ((sum ushr 16) != 0L) {
            sum = (sum and 0xFFFFL) + (sum ushr 16)
        }
        val result = (sum.inv() and 0xFFFFL).toInt()
        return if (result == 0) 0xFFFF else result
    }
}
