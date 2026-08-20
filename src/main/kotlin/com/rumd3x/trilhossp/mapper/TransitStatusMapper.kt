package com.rumd3x.trilhossp.mapper

import com.rumd3x.trilhossp.domain.Company
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.Station
import com.rumd3x.trilhossp.model.TransitStatusResponse
import org.springframework.stereotype.Component

@Component
class TransitStatusMapper {

    fun toLines(response: TransitStatusResponse): List<Line> =
        response.empresas.flatMap { empresa ->
            val company = Company(id = empresa.id, name = empresa.nome, isArtespMonitored = empresa.fiscalizacaoArtesp)
            empresa.linhas.map { linha ->
                Line(
                    name = linha.nome,
                    code = linha.codigo,
                    company = company,
                    status = LineStatus(
                        situation = linha.status.situacao,
                        classification = linha.status.classificacao,
                        isNormal = linha.status.operacaoNormal,
                        updatedAt = linha.status.atualizadoEm,
                    ),
                    stations = linha.estacoes.nomes.map { Station(name = it) },
                )
            }
        }
}
