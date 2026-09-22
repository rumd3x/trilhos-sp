package com.rumd3x.trilhossp.client.provider.ccr

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CcrStatusResponse(
    val status: Boolean,
    val message: String,
    val errorCode: String,
    val data: CcrData,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CcrData(
    val dataAtualizacao: String,
    val concessoes: List<CcrConcessao>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CcrConcessao(
    val uid: String,
    val nome: String,
    val linhas: List<CcrLinha>,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CcrLinha(
    val uid: String,
    // the API returns this field as either a JSON string (e.g. "4") or a number (e.g. 1), Jackson coerces both into String
    val numero: String,
    val nome: String,
    val statusLinha: CcrLineStatus,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CcrLineStatus(
    val codigo: String,
    val status: String,
    val descricao: String? = null,
)
