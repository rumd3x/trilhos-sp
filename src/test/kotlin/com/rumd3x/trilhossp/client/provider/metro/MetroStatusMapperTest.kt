package com.rumd3x.trilhossp.client.provider.metro

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MetroStatusMapperTest {
    private val mapper = MetroStatusMapper()

    private fun fixture() = javaClass.getResourceAsStream("/mock-metro-response.html")!!.reader().readText()

    @Test fun `maps code and name for every line`() {
        val lines = mapper.toLines(fixture())
        assertEquals(
            mapOf("1" to "Linha 1-Azul", "4" to "Linha 4-Amarela", "17" to "Linha 17-Ouro"),
            lines.associate { it.code to it.name },
        )
    }

    @Test fun `maps situation from linha-situacao`() {
        val line = mapper.toLines(fixture()).first { it.code == "1" }
        assertEquals("Operação Normal", line.status.situation)
    }

    @Test fun `uses fixed empty classification`() {
        assertTrue(mapper.toLines(fixture()).all { it.status.classification == "" })
    }

    @Test fun `isNormal is true only when description equals Situação Normal`() {
        val azul = mapper.toLines(fixture()).first { it.code == "1" }
        val ouro = mapper.toLines(fixture()).first { it.code == "17" }
        assertTrue(azul.status.isNormal)
        assertFalse(ouro.status.isNormal)
    }

    @Test fun `company is null since this page does not report company data`() {
        assertTrue(mapper.toLines(fixture()).all { it.company == null })
    }

    @Test fun `parses updatedAt with seconds`() {
        val line = mapper.toLines(fixture()).first { it.code == "1" }
        assertEquals("2026-09-04T20:22:45", line.status.updatedAt)
    }

    @Test fun `parses updatedAt without seconds`() {
        val line = mapper.toLines(fixture()).first { it.code == "4" }
        assertEquals("2026-09-04T20:23:00", line.status.updatedAt)
    }

    @Test fun `maps to empty stations since page does not provide them`() {
        assertTrue(mapper.toLines(fixture()).all { it.stations.isEmpty() })
    }

    @Test fun `handles blank description as not normal with empty descricao`() {
        val ouro = mapper.toLines(fixture()).first { it.code == "17" }
        assertEquals("", ouro.status.descricao)
    }
}
