package com.rumd3x.trilhossp.client.provider.cptm

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class CptmLineStatus(
    val linhaId: Int,
    val dataGeracao: String,
    val descricao: String? = "",
    val tipo: String,
    val status: String,
)
