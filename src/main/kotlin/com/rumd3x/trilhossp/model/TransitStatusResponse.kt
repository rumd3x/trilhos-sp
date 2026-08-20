package com.rumd3x.trilhossp.model

import tools.jackson.annotation.JsonProperty

data class TransitStatusResponse(
    val meta: TransitMeta,
    val empresas: List<Empresa>,
)

data class TransitMeta(
    val versao: String,
    val timestamp: String,
    @JsonProperty("filtros_aplicados") val filtrosAplicados: Map<String, Any>?,
    @JsonProperty("total_linhas") val totalLinhas: Int,
    @JsonProperty("total_empresas") val totalEmpresas: Int,
)

data class Empresa(
    val id: Int,
    val nome: String,
    @JsonProperty("fiscalizacao_artesp") val fiscalizacaoArtesp: Boolean,
    val linhas: List<Linha>,
)

data class Linha(
    val nome: String,
    val codigo: String,
    val ativa: Boolean,
    val status: LinhaStatus,
    val estacoes: Estacoes,
)

data class LinhaStatus(
    val situacao: String,
    val classificacao: String,
    @JsonProperty("operacao_normal") val operacaoNormal: Boolean,
    @JsonProperty("atualizado_em") val atualizadoEm: String,
    @JsonProperty("atualizado_ha") val atualizadoHa: String,
)

data class Estacoes(
    val total: Int,
    val nomes: List<String>,
)
