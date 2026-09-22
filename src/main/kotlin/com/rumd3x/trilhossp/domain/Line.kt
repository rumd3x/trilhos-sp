package com.rumd3x.trilhossp.domain

import com.rumd3x.trilhossp.entity.LineEntity

data class Line(
    val name: String,
    val code: String,
    val status: LineStatus,
    val company: Company?,
    val stations: List<Station>,
    val source: List<String>,
) {
    // stations are not stored in LineEntity and must be loaded separately if needed
    constructor(entity: LineEntity) : this(
        name = entity.name,
        code = entity.code,
        status =
            LineStatus(
                situation = entity.situation,
                classification = entity.classification,
                descricao = entity.descricao,
                isNormal = entity.isNormal,
                updatedAt = entity.updatedAt,
            ),
        company =
            entity.companyName
                .takeIf { it.isNotBlank() }
                ?.let {
                    Company(
                        id = entity.companyId,
                        name = it,
                        isArtespMonitored = entity.isArtespMonitored,
                    )
                },
        stations = emptyList(),
        source = entity.source.split(",").map { it.trim() }.filter { it.isNotBlank() },
    )
}
