package com.rumd3x.trilhossp.client.provider.ccr

import tools.jackson.module.kotlin.jacksonObjectMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CcrStatusMapperTest {
    private val mapper = CcrStatusMapper()

    private fun response(vararg concessoes: CcrConcessao) =
        CcrStatusResponse(
            status = true,
            message = "",
            errorCode = "",
            data = CcrData(dataAtualizacao = "2026-09-22T19:37:55", concessoes = concessoes.toList()),
        )

    private fun concessao(
        nome: String = "Motiva Linha Quatro",
        linhas: List<CcrLinha>,
    ) = CcrConcessao(uid = "uid", nome = nome, linhas = linhas)

    private fun linha(
        numero: String = "4",
        nome: String = "Amarela",
        codigo: String = "OperacaoNormal",
        status: String = "Operação Normal",
        descricao: String? = "",
    ) = CcrLinha(
        uid = "uid-$numero",
        numero = numero,
        nome = nome,
        statusLinha = CcrLineStatus(codigo = codigo, status = status, descricao = descricao),
    )

    @Test fun `maps line code and name`() {
        val lines = mapper.toLines(response(concessao(linhas = listOf(linha()))))
        assertEquals("4", lines.first().code)
        assertEquals("Linha 4-Amarela", lines.first().name)
    }

    @Test fun `maps company from concessao with id 0 and isArtespMonitored false`() {
        val lines = mapper.toLines(response(concessao(nome = "ViaMobilidade 5", linhas = listOf(linha(numero = "5", nome = "Lilás")))))
        val company = lines.first().company!!
        assertEquals(0, company.id)
        assertEquals("ViaMobilidade 5", company.name)
        assertFalse(company.isArtespMonitored)
    }

    @Test fun `maps status fields`() {
        val line =
            mapper
                .toLines(
                    response(
                        concessao(
                            linhas =
                                listOf(
                                    linha(codigo = "OperacaoEncerrada", status = "Operação Encerrada", descricao = "Operação Encerrada"),
                                ),
                        ),
                    ),
                ).first()
        assertEquals("Operação Encerrada", line.status.situation)
        assertEquals("OperacaoEncerrada", line.status.classification)
        assertEquals("Operação Encerrada", line.status.descricao)
    }

    @Test fun `isNormal is true only when codigo is OperacaoNormal`() {
        assertTrue(mapper.toLines(response(concessao(linhas = listOf(linha(codigo = "OperacaoNormal"))))).first().status.isNormal)
        assertFalse(mapper.toLines(response(concessao(linhas = listOf(linha(codigo = "OperacaoEncerrada"))))).first().status.isNormal)
    }

    @Test fun `treats null descricao as empty string`() {
        val line = mapper.toLines(response(concessao(linhas = listOf(linha(descricao = null))))).first()
        assertEquals("", line.status.descricao)
    }

    @Test fun `uses shared dataAtualizacao as updatedAt for every line`() {
        val lines = mapper.toLines(response(concessao(linhas = listOf(linha(numero = "4"), linha(numero = "5")))))
        assertTrue(lines.all { it.status.updatedAt == "2026-09-22T19:37:55" })
    }

    @Test fun `sets source to ccr`() {
        assertEquals(listOf("ccr"), mapper.toLines(response(concessao(linhas = listOf(linha())))).first().source)
    }

    @Test fun `maps to empty stations since api does not provide them`() {
        assertEquals(emptyList(), mapper.toLines(response(concessao(linhas = listOf(linha())))).first().stations)
    }

    @Test fun `flattens lines from multiple concessoes`() {
        val lines =
            mapper.toLines(
                response(
                    concessao(nome = "A", linhas = listOf(linha(numero = "4"))),
                    concessao(nome = "B", linhas = listOf(linha(numero = "5"), linha(numero = "9"))),
                ),
            )
        assertEquals(3, lines.size)
        assertEquals(listOf("4", "5", "9"), lines.map { it.code })
    }

    @Test fun `deserializes and maps full mock ccr response with mixed numero types`() {
        val json = javaClass.getResourceAsStream("/mock-ccr-response.json")!!.reader().readText()
        val response = jacksonObjectMapper().readValue(json, CcrStatusResponse::class.java)
        val lines = mapper.toLines(response)

        assertEquals(15, lines.size)
        assertEquals(
            setOf("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "15", "17"),
            lines.map { it.code }.toSet(),
        )

        // numero was a JSON string ("4") for this line
        val linha4 = lines.first { it.code == "4" }
        assertEquals("Linha 4-Amarela", linha4.name)
        assertEquals("Motiva Linha Quatro", linha4.company?.name)

        // numero was a raw JSON number (1) for this line
        val linha1 = lines.first { it.code == "1" }
        assertEquals("Linha 1-Azul", linha1.name)
        assertEquals("Metro SP", linha1.company?.name)
        assertTrue(linha1.status.isNormal)

        // encerrada line
        val linha17 = lines.first { it.code == "17" }
        assertFalse(linha17.status.isNormal)
    }
}
