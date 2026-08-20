package com.rumd3x.trilhossp.domain

data class LineStatus(
    val situation: String,
    val classification: String,
    val isNormal: Boolean,
    val updatedAt: String,
)
