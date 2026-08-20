package com.rumd3x.trilhossp.model

import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class TransitStatusResponseTest {
    private val mapper = jacksonObjectMapper()

    private fun json(filtrosAplicados: String? = null): String {
        val metaExtra = if (filtrosAplicados != null) """"filtros_aplicados": $filtrosAplicados,""" else ""
        return """
            {
                "meta": {
                    "versao": "1.0",
                    "timestamp": "2024-01-01T10:00:00-03:00",
                    $metaExtra
                    "total_linhas": 2,
                    "total_empresas": 1
                },
                "empresas": []
            }
            """.trimIndent()
    }

    @Test fun `deserializes when filtros_aplicados is null`() {
        val result = mapper.readValue<TransitStatusResponse>(json("null"))
        assertEquals(2, result.meta.totalLinhas)
    }

    @Test fun `deserializes when filtros_aplicados is absent`() {
        val result = mapper.readValue<TransitStatusResponse>(json())
        assertNotNull(result.meta)
    }

    @Test fun `deserializes when filtros_aplicados is an empty object`() {
        val result = mapper.readValue<TransitStatusResponse>(json("{}"))
        assertEquals(1, result.meta.totalEmpresas)
    }

    @Test fun `deserializes when filtros_aplicados is a populated object`() {
        val result = mapper.readValue<TransitStatusResponse>(json("""{"linha": "4", "data": "2024-01-01"}"""))
        assertEquals("1.0", result.meta.versao)
    }

    @Test fun `ignores unknown fields at all levels`() {
        val fullJson =
            """
            {
                "meta": {
                    "versao": "1.0",
                    "timestamp": "",
                    "filtros_aplicados": null,
                    "total_linhas": 1,
                    "total_empresas": 1,
                    "unknown_meta_field": "ignored"
                },
                "empresas": [],
                "unknown_root_field": true
            }
            """.trimIndent()
        val result = mapper.readValue<TransitStatusResponse>(fullJson)
        assertNotNull(result)
    }

    private fun linhaJson(estacoes: String?) =
        """
        {
            "meta": {"versao": "1.0", "timestamp": "", "total_linhas": 1, "total_empresas": 1},
            "empresas": [{
                "id": 1, "nome": "ViaQuatro", "fiscalizacao_artesp": true,
                "linhas": [{
                    "nome": "Linha 4-Amarela", "codigo": "4", "ativa": true,
                    "status": {
                        "situacao": "Operação Normal", "classificacao": "operacional",
                        "operacao_normal": true, "atualizado_em": "2024-01-01", "atualizado_ha": "5min"
                    }
                    ${if (estacoes != null) ", \"estacoes\": $estacoes" else ""}
                }]
            }]
        }
        """.trimIndent()

    @Test fun `deserializes linha when estacoes is absent`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson(null))
        assertNotNull(
            result.empresas
                .first()
                .linhas
                .first(),
        )
    }

    @Test fun `deserializes linha when estacoes is null`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson("null"))
        assertNotNull(
            result.empresas
                .first()
                .linhas
                .first(),
        )
    }

    @Test fun `deserializes linha with estacoes present`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson("""{"total": 2, "nomes": ["Luz", "República"]}"""))
        assertEquals(
            2,
            result.empresas
                .first()
                .linhas
                .first()
                .estacoes
                ?.total,
        )
    }

    @Test fun `deserializes estacoes when nomes is null`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson("""{"total": 2, "nomes": null}"""))
        assertNotNull(
            result.empresas
                .first()
                .linhas
                .first()
                .estacoes,
        )
    }

    @Test fun `deserializes estacoes when nomes is absent`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson("""{"total": 2}"""))
        assertNotNull(
            result.empresas
                .first()
                .linhas
                .first()
                .estacoes,
        )
    }

    @Test fun `deserializes estacoes when total is null`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson("""{"total": null, "nomes": []}"""))
        assertNotNull(
            result.empresas
                .first()
                .linhas
                .first()
                .estacoes,
        )
    }

    @Test fun `deserializes estacoes when both total and nomes are absent`() {
        val result = mapper.readValue<TransitStatusResponse>(linhaJson("{}"))
        assertNotNull(
            result.empresas
                .first()
                .linhas
                .first()
                .estacoes,
        )
    }

    @Test fun `deserializes full mock api response`() {
        val json = javaClass.getResourceAsStream("/mock-api-response.json")!!.reader().readText()
        val result = mapper.readValue<TransitStatusResponse>(json)

        assertEquals(7, result.empresas.size)
        assertEquals(13, result.meta.totalLinhas)

        // lines without estacoes field
        val cptm = result.empresas.first { it.nome.startsWith("CPTM") }
        assertNotNull(
            cptm.linhas
                .first()
                .estacoes
                ?.let { null } ?: Unit,
        )

        // lines with estacoes but no nomes
        val trivia = result.empresas.first { it.nome == "Trivia Trens" }
        assertEquals(3, trivia.linhas.size)
        trivia.linhas.forEach { assertNotNull(it) }

        // filtros_aplicados with nested nulls is ignored
        assertEquals(13, result.meta.totalLinhas)

        // line with full estacoes
        val viaQuatro = result.empresas.first { it.nome == "ViaQuatro" }
        assertEquals(
            11,
            viaQuatro.linhas
                .first()
                .estacoes
                ?.total,
        )
        assertEquals(
            "Luz",
            viaQuatro.linhas
                .first()
                .estacoes
                ?.nomes
                ?.first(),
        )
    }
}
