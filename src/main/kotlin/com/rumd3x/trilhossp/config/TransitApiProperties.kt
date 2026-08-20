package com.rumd3x.trilhossp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "transit.api")
data class TransitApiProperties(
    val baseUrl: String,
    val key: String,
) {
    private val tokenPrefix = "cci_metro_status_live_"

    fun normalizedKey(): String = if (key.startsWith(tokenPrefix)) key else "$tokenPrefix$key"
}
