package com.example.data

import com.example.model.TunnelServer

object DefaultServers {
    val presetServers: List<TunnelServer> = listOf(
        TunnelServer(
            id = "sg1",
            code = "SG1",
            country = "Singapore",
            city = "Singapore",
            host = "1.1.1.1",
            port = 443,
            flagCode = "SG",
            loadPercent = 24,
        ),
        TunnelServer(
            id = "us1",
            code = "US1",
            country = "United States",
            city = "New York",
            host = "8.8.8.8",
            port = 443,
            flagCode = "US",
            loadPercent = 31,
        ),
        TunnelServer(
            id = "uk1",
            code = "UK1",
            country = "United Kingdom",
            city = "London",
            host = "9.9.9.9",
            port = 443,
            flagCode = "GB",
            loadPercent = 29,
        ),
        TunnelServer(
            id = "de1",
            code = "DE1",
            country = "Germany",
            city = "Frankfurt",
            host = "1.0.0.1",
            port = 443,
            flagCode = "DE",
            loadPercent = 22,
        ),
        TunnelServer(
            id = "jp1",
            code = "JP1",
            country = "Japan",
            city = "Tokyo",
            host = "8.8.4.4",
            port = 443,
            flagCode = "JP",
            loadPercent = 37,
        ),
        TunnelServer(
            id = "nl1",
            code = "NL1",
            country = "Netherlands",
            city = "Amsterdam",
            host = "149.112.112.112",
            port = 443,
            flagCode = "NL",
            loadPercent = 19,
        ),
        TunnelServer(
            id = "za1",
            code = "ZA1",
            country = "South Africa",
            city = "Johannesburg",
            host = "197.242.144.102",
            port = 443,
            flagCode = "ZA",
            loadPercent = 27,
        ),
        TunnelServer(
            id = "br1",
            code = "BR1",
            country = "Brazil",
            city = "São Paulo",
            host = "208.67.222.222",
            port = 443,
            flagCode = "BR",
            loadPercent = 41,
        ),
        TunnelServer(
            id = "custom",
            code = "CUS",
            country = "Custom Endpoint",
            city = "User Defined",
            host = "1.1.1.1",
            port = 443,
            flagCode = "CUSTOM",
            loadPercent = 0,
            isCustom = true,
        ),
    )

    fun findById(id: String): TunnelServer =
        presetServers.firstOrNull { it.id == id } ?: presetServers.first()
}
