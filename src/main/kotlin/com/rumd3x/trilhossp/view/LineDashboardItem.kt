package com.rumd3x.trilhossp.view

import com.rumd3x.trilhossp.client.provider.ProviderDisplayName
import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.LineStatusDiff
import com.rumd3x.trilhossp.entity.LineEntity
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class LineDashboardItem(
    val name: String,
    val situation: String,
    val descricao: String,
    val companyName: String,
    val source: String,
    val levelCss: String,
    val updatedAt: String,
) {
    companion object {
        private val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

        fun from(entity: LineEntity) =
            LineDashboardItem(
                name = entity.name,
                situation = entity.situation,
                descricao = entity.descricao,
                companyName = entity.companyName,
                source =
                    entity.source
                        .takeIf { it.isNotBlank() }
                        ?.let { ProviderDisplayName.labelFor(it) }
                        .orEmpty(),
                levelCss = levelCssFor(entity),
                updatedAt = formatDate(entity.updatedAt),
            )

        private fun levelCssFor(entity: LineEntity): String {
            val status = LineStatus(entity.situation, entity.classification, entity.descricao, entity.isNormal, entity.updatedAt)
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
