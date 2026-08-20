package com.rumd3x.trilhossp.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notify")
data class NotificationProperties(
    val days: String = "all",
    val level: Int = 0,
    val lines: String = "all",
)
