package com.rumd3x.trilhossp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "ccr.api")
data class CcrApiProperties(
    val baseUrl: String,
)
