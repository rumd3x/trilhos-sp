package com.rumd3x.trilhossp.client.provider.ccr

import com.rumd3x.trilhossp.client.provider.ProviderNames
import com.rumd3x.trilhossp.domain.Company
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import org.springframework.stereotype.Component

@Component
class CcrStatusMapper {
    fun toLines(response: CcrStatusResponse): List<Line> =
        response.data.concessoes.flatMap { concessao ->
            val company = Company(id = 0, name = concessao.nome, isArtespMonitored = false)
            concessao.linhas.map { linha ->
                Line(
                    name = "Linha ${linha.numero}-${linha.nome}",
                    code = linha.numero,
                    company = company,
                    status =
                        LineStatus(
                            situation = linha.statusLinha.status,
                            classification = linha.statusLinha.codigo,
                            descricao = linha.statusLinha.descricao ?: "",
                            isNormal = linha.statusLinha.codigo == "OperacaoNormal",
                            updatedAt = response.data.dataAtualizacao,
                        ),
                    stations = emptyList(),
                    source = listOf(ProviderNames.CCR),
                )
            }
        }
}
