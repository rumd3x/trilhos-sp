package com.rumd3x.trilhossp.mapper

import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.entity.LineEntity
import org.springframework.stereotype.Component

@Component
class LineEntityMapper {
    fun toEntity(line: Line): LineEntity =
        LineEntity(
            code = line.code,
            name = line.name,
            companyId = line.company?.id ?: 0,
            companyName = line.company?.name ?: "",
            isArtespMonitored = line.company?.isArtespMonitored ?: false,
            situation = line.status.situation,
            classification = line.status.classification,
            descricao = line.status.descricao,
            isNormal = line.status.isNormal,
            updatedAt = line.status.updatedAt,
            source = line.source,
        )
}
