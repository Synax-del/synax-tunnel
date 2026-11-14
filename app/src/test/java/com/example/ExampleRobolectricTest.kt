package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.TunnelPreferencesRepository
import com.example.model.ConnectionConfig
import com.example.model.TunnelProtocol
import com.example.vpn.Ipv4PacketHelper
import com.example.vpn.PayloadFormatter
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `app name matches Synax Tunnel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Synax Tunnel", appName)
    }

    @Test
    fun `payload formatter resolves tokens and splits segments`() {
        val segments = PayloadFormatter.formatPayloadSegments(
            template = "CONNECT [host_port] [protocol][crlf][split]Host: [host][crlf][crlf]",
            targetHost = "1.1.1.1",
            targetPort = 443,
            sniHost = "m.tiktok.com",
        )
        assertEquals(2, segments.size)
        assertEquals("CONNECT m.tiktok.com:443 HTTP/1.1\r\n", String(segments[0], Charsets.ISO_8859_1))
        assertEquals("Host: m.tiktok.com\r\n\r\n", String(segments[1], Charsets.ISO_8859_1))
    }

    @Test
    fun `ipv4 udp packet builder and parser roundtrip`() {
        val srcIp = byteArrayOf(10, 10, 0, 53)
        val dstIp = byteArrayOf(10, 10, 0, 2)
        val dnsPayload = byteArrayOf(0x12, 0x34, 0x01, 0x00, 0x00, 0x01)

        val packet = Ipv4PacketHelper.buildUdpResponse(
            srcIp = srcIp,
            dstIp = dstIp,
            srcPort = 53,
            dstPort = 54321,
            payload = dnsPayload,
        )

        val parsed = Ipv4PacketHelper.parse(packet, packet.size)
        assertNotNull(parsed)
        assertEquals(Ipv4PacketHelper.PROTO_UDP, parsed!!.protocol)
        assertEquals(53, parsed.srcPort)
        assertEquals(54321, parsed.dstPort)
        assertArrayEquals(dnsPayload, parsed.udpPayload)
    }

    @Test
    fun `ctun config export and import roundtrip`() {
        val original = ConnectionConfig(
            serverId = "us1",
            protocol = TunnelProtocol.TCP,
            useSniHost = true,
            sniHost = "speed.cloudflare.com",
            usePayload = true,
            obfuscatedTlsCh = true,
            useProxy = true,
            proxyHost = "127.0.0.1",
            proxyPort = 8888,
        )

        val json = TunnelPreferencesRepository.exportToJson(
            config = original,
            profileName = "Test Profile",
            author = "Tester",
            notes = "Unit test roundtrip",
        )

        val imported = TunnelPreferencesRepository.importFromJson(json)
        assertTrue(imported.isSuccess)
        val (profile, cfg) = imported.getOrThrow()
        assertEquals("Test Profile", profile.name)
        assertEquals("speed.cloudflare.com", cfg.sniHost)
        assertEquals("127.0.0.1", cfg.proxyHost)
        assertEquals(8888, cfg.proxyPort)
        assertTrue(cfg.obfuscatedTlsCh)
    }
}
