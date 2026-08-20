package com.rumd3x.trilhossp.mapper

import com.rumd3x.trilhossp.model.Empresa
import com.rumd3x.trilhossp.model.Estacoes
import com.rumd3x.trilhossp.model.Linha
import com.rumd3x.trilhossp.model.LinhaStatus
import com.rumd3x.trilhossp.model.TransitMeta
import com.rumd3x.trilhossp.model.TransitStatusResponse
import kotlin.test.Test
import kotlin.test.assertEquals

class TransitStatusMapperTest {

    private val mapper = TransitStatusMapper()

    private fun response(vararg empresas: Empresa) = TransitStatusResponse(
        meta = TransitMeta(versao = "1.0.0", timestamp = "", filtrosAplicados = null, totalLinhas = 1, totalEmpresas = 1),
        empresas = empresas.toList(),
    )

    private fun empresa(
        id: Int = 1,
        nome: String = "ViaQuatro",
        fiscalizacaoArtesp: Boolean = true,
        linhas: List<Linha>,
    ) = Empresa(id = id, nome = nome, fiscalizacaoArtesp = fiscalizacaoArtesp, linhas = linhas)

    private fun linha(
        nome: String = "Linha 4-Amarela",
        codigo: String = "4",
        situacao: String = "Operação Normal",
        classificacao: String = "operacional",
        operacaoNormal: Boolean = true,
        estacoes: List<String> = listOf("Luz", "República"),
    ) = Linha(
        nome = nome,
        codigo = codigo,
        ativa = true,
        status = LinhaStatus(
            situacao = situacao,
            classificacao = classificacao,
            operacaoNormal = operacaoNormal,
            atualizadoEm = "2024-01-01T10:00:00-03:00",
            atualizadoHa = "5 minutos",
        ),
        estacoes = Estacoes(total = estacoes.size, nomes = estacoes),
    )

    @Test fun `maps line code and name`() {
        val lines = mapper.toLines(response(empresa(linhas = listOf(linha()))))
        assertEquals("4", lines.first().code)
        assertEquals("Linha 4-Amarela", lines.first().name)
    }

    @Test fun `maps company id, name and artesp flag`() {
        val lines = mapper.toLines(response(empresa(id = 2, nome = "ViaMobilidade", fiscalizacaoArtesp = false, linhas = listOf(linha()))))
        val company = lines.first().company
        assertEquals(2, company.id)
        assertEquals("ViaMobilidade", company.name)
        assertEquals(false, company.isArtespMonitored)
    }

    @Test fun `maps line status fields`() {
        val lines = mapper.toLines(response(empresa(linhas = listOf(linha(situacao = "Lentidão", classificacao = "parcial", operacaoNormal = false)))))
        val status = lines.first().status
        assertEquals("Lentidão", status.situation)
        assertEquals("parcial", status.classification)
        assertEquals(false, status.isNormal)
    }

    @Test fun `maps stations from estacoes nomes`() {
        val stations = mapper.toLines(response(empresa(linhas = listOf(linha(estacoes = listOf("Luz", "República", "Paulista")))))).first().stations
        assertEquals(3, stations.size)
        assertEquals("Luz", stations[0].name)
        assertEquals("Paulista", stations[2].name)
    }

    @Test fun `flattens lines from multiple companies`() {
        val lines = mapper.toLines(
            response(
                empresa(id = 1, linhas = listOf(linha(codigo = "4"))),
                empresa(id = 2, nome = "ViaMobilidade", linhas = listOf(linha(codigo = "5"), linha(codigo = "9"))),
            )
        )
        assertEquals(3, lines.size)
        assertEquals(listOf("4", "5", "9"), lines.map { it.code })
    }

    @Test fun `all lines from same company share the same company reference`() {
        val lines = mapper.toLines(response(empresa(nome = "CPTM", linhas = listOf(linha(codigo = "7"), linha(codigo = "8")))))
        assertEquals("CPTM", lines[0].company.name)
        assertEquals("CPTM", lines[1].company.name)
    }
}
