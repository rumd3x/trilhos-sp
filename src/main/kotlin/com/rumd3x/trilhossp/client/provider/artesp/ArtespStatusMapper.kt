package com.rumd3x.trilhossp.client.provider.artesp

import com.rumd3x.trilhossp.client.provider.ProviderNames
import com.rumd3x.trilhossp.domain.Company
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.Station
import org.springframework.stereotype.Component

@Component
class ArtespStatusMapper {

    fun toLines(response: ArtespStatusResponse): List<Line> =
        response.empresas.flatMap { empresa ->
            val company = Company(id = empresa.id, name = empresa.nome, isArtespMonitored = empresa.fiscalizacaoArtesp)
            empresa.linhas.map { linha ->
                Line(
                    name = linha.nome,
                    code = linha.codigo,
                    company = company,
                    status =
                        LineStatus(
                            situation = linha.status.situacao,
                            classification = linha.status.classificacao,
                            descricao = linha.status.descricao ?: "",
                            isNormal = linha.status.operacaoNormal,
                            updatedAt = linha.status.atualizadoEm,
                        ),
                    stations = linha.estacoes?.nomes?.map { Station(name = it) } ?: emptyList(),
                    source = listOf(ProviderNames.ARTESP),
                )
            }
        }
}
