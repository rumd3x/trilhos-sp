package com.rumd3x.trilhossp.client.provider.cptm

import com.rumd3x.trilhossp.client.provider.ProviderNames
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import org.springframework.stereotype.Component

@Component
class CptmStatusMapper {

    // this API does not report line names, so they must be mapped manually by id
    private val lineNames =
        mapOf(
            10 to "Linha 10-Turquesa",
            11 to "Linha 11-Coral",
            12 to "Linha 12-Safira",
            13 to "Linha 13-Jade",
        )

    fun toLines(response: List<CptmLineStatus>): List<Line> =
        response.mapNotNull { item ->
            val lineName = lineNames[item.linhaId] ?: return@mapNotNull null
            Line(
                name = lineName,
                code = item.linhaId.toString(),
                company = null,
                status =
                    LineStatus(
                        situation = item.status,
                        classification = item.tipo,
                        descricao = item.descricao ?: "",
                        isNormal = item.tipo == "C",
                        updatedAt = item.dataGeracao,
                    ),
                stations = emptyList(),
                source = listOf(ProviderNames.CPTM),
            )
        }
}
