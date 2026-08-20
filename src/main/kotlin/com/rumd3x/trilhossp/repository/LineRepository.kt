package com.rumd3x.trilhossp.repository

import com.rumd3x.trilhossp.entity.LineEntity
import org.springframework.data.repository.reactive.ReactiveCrudRepository

interface LineRepository : ReactiveCrudRepository<LineEntity, Long>
