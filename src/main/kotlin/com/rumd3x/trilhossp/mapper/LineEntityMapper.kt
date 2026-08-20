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
            companyId = line.company.id,
            companyName = line.company.name,
            isArtespMonitored = line.company.isArtespMonitored,
            situation = line.status.situation,
            classification = line.status.classification,
            isNormal = line.status.isNormal,
            updatedAt = line.status.updatedAt,
        )
}
