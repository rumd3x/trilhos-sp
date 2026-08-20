package com.rumd3x.trilhossp.view

import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.LineStatusDiff
import com.rumd3x.trilhossp.entity.LineEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LineDashboardItem(
    val name: String,
    val situation: String,
    val companyName: String,
    val levelCss: String,
    val updatedAt: String,
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

        fun from(entity: LineEntity) = LineDashboardItem(
            name = entity.name,
            situation = entity.situation,
            companyName = entity.companyName,
            levelCss = levelCssFor(entity),
            updatedAt = formatDate(entity.updatedAt),
        )

        private fun levelCssFor(entity: LineEntity): String {
            val status = LineStatus(entity.situation, entity.classification, entity.isNormal, entity.updatedAt)
            return "line-level-${LineStatusDiff(status, status).level}"
        }

        private fun formatDate(raw: String): String {
            if (raw.isBlank()) return ""
            return try {
                LocalDateTime.parse(raw).format(formatter)
            } catch (_: Exception) {
                raw
            }
        }
    }
}
