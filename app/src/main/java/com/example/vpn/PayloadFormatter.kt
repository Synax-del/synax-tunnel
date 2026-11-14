package com.example.vpn

object PayloadFormatter {
    private const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 14; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    /**
     * Formats HTTP injector tokens ([crlf], [lf], [cr], [host], [host_port], [port], [protocol], [ua], [split])
     * and splits into segments whenever [split] appears.
     */
    fun formatPayloadSegments(
        template: String,
        targetHost: String,
        targetPort: Int,
        sniHost: String,
    ): List<ByteArray> {
        val effectiveHost = sniHost.ifBlank { targetHost }
        val resolved = template
            .replace("[crlf]", "\r\n", ignoreCase = true)
            .replace("[lf]", "\n", ignoreCase = true)
            .replace("[cr]", "\r", ignoreCase = true)
            .replace("[host_port]", "$effectiveHost:$targetPort", ignoreCase = true)
            .replace("[host]", effectiveHost, ignoreCase = true)
            .replace("[port]", targetPort.toString(), ignoreCase = true)
            .replace("[protocol]", "HTTP/1.1", ignoreCase = true)
            .replace("[ua]", DEFAULT_UA, ignoreCase = true)
            .replace("[netdata]", "CONNECT $effectiveHost:$targetPort HTTP/1.1", ignoreCase = true)

        return resolved
            .split("[split]", ignoreCase = true)
            .filter { it.isNotEmpty() }
            .map { it.toByteArray(Charsets.ISO_8859_1) }
    }
}
