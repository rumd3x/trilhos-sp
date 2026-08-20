package com.rumd3x.trilhossp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "gotify")
data class GotifyProperties(
    val url: String = "",
    val token: String = "",
) {
    fun isConfigured(): Boolean = url.isNotBlank() && token.isNotBlank()
}
