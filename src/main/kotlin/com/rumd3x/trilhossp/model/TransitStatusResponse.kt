package com.rumd3x.trilhossp.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitStatusResponse(
    val meta: TransitMeta,
    val empresas: List<Empresa>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TransitMeta(
    val versao: String,
    val timestamp: String,
    @JsonProperty("total_linhas") val totalLinhas: Int,
    @JsonProperty("total_empresas") val totalEmpresas: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Empresa(
    val id: Int,
    val nome: String,
    @JsonProperty("fiscalizacao_artesp") val fiscalizacaoArtesp: Boolean,
    val linhas: List<Linha>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Linha(
    val nome: String,
    val codigo: String,
    val ativa: Boolean,
    val status: LinhaStatus,
    val estacoes: Estacoes? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class LinhaStatus(
    val situacao: String,
    val classificacao: String,
    @JsonProperty("operacao_normal") val operacaoNormal: Boolean,
    @JsonProperty("atualizado_em") val atualizadoEm: String,
    @JsonProperty("atualizado_ha") val atualizadoHa: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Estacoes(
    val total: Int? = 0,
    val nomes: List<String>? = emptyList(),
)
