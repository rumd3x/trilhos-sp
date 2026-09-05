package com.rumd3x.trilhossp.client.provider.cptm

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CptmStatusMapperTest {
    private val mapper = CptmStatusMapper()

    private fun status(
        linhaId: Int,
        status: String = "Operação Normal",
        tipo: String = "C",
        descricao: String? = "",
        dataGeracao: String = "2026-09-04T19:00:48",
    ) = CptmLineStatus(linhaId = linhaId, dataGeracao = dataGeracao, descricao = descricao, tipo = tipo, status = status)

    @Test fun `maps known line ids to their names and codes`() {
        val lines = mapper.toLines(listOf(status(10), status(11), status(12), status(13)))
        assertEquals(
            mapOf(
                "10" to "Linha 10-Turquesa",
                "11" to "Linha 11-Coral",
                "12" to "Linha 12-Safira",
                "13" to "Linha 13-Jade",
            ),
            lines.associate { it.code to it.name },
        )
    }

    @Test fun `skips unknown line ids`() {
        assertEquals(emptyList(), mapper.toLines(listOf(status(99))))
    }

    @Test fun `maps status fields`() {
        val line = mapper.toLines(listOf(status(10, status = "Paralisada", tipo = "P", descricao = "Falha no sistema"))).first()
        assertEquals("Paralisada", line.status.situation)
        assertEquals("P", line.status.classification)
        assertEquals("Falha no sistema", line.status.descricao)
        assertEquals("2026-09-04T19:00:48", line.status.updatedAt)
    }

    @Test fun `isNormal is true only when tipo is C`() {
        assertTrue(
            mapper
                .toLines(listOf(status(10, tipo = "C")))
                .first()
                .status.isNormal,
        )
        assertFalse(
            mapper
                .toLines(listOf(status(10, tipo = "P")))
                .first()
                .status.isNormal,
        )
    }

    @Test fun `company is null since this api does not report company data`() {
        assertEquals(null, mapper.toLines(listOf(status(10))).first().company)
    }

    @Test fun `maps to empty stations since api does not provide them`() {
        assertEquals(emptyList(), mapper.toLines(listOf(status(10))).first().stations)
    }

    @Test fun `treats null descricao as empty string`() {
        val line = mapper.toLines(listOf(status(10, descricao = null))).first()
        assertEquals("", line.status.descricao)
    }
}
