package com.rumd3x.trilhossp.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table

@Table("lines")
data class LineEntity(
    @Id val id: Long? = null,
    val code: String,
    val name: String,
    val companyId: Int,
    val companyName: String,
    val isArtespMonitored: Boolean,
    val situation: String,
    val classification: String,
    val isNormal: Boolean,
    val updatedAt: String,
)
