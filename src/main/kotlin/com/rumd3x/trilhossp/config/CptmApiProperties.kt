package com.rumd3x.trilhossp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "cptm.api")
data class CptmApiProperties(
    val baseUrl: String,
)
