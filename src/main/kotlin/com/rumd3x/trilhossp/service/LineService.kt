package com.rumd3x.trilhossp.service

import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.mapper.LineEntityMapper
import com.rumd3x.trilhossp.repository.LineRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import reactor.core.publisher.Mono

@Service
class LineService(
    private val repository: LineRepository,
    private val mapper: LineEntityMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun replaceAll(lines: List<Line>): Mono<Void> {
        log.info("Persisting {} lines to database...", lines.size)
        return repository
            .deleteAll()
            .doOnSuccess { log.debug("Cleared existing lines") }
            .thenMany(repository.saveAll(lines.map { mapper.toEntity(it) }))
            .doOnComplete { log.info("All {} lines saved successfully", lines.size) }
            .then()
    }
}
