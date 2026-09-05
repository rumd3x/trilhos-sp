package com.rumd3x.trilhossp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "metro.api")
data class MetroApiProperties(
    val baseUrl: String,
)
