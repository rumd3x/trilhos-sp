package com.rumd3x.trilhossp.job

import com.rumd3x.trilhossp.client.provider.LineStatusProvider
import com.rumd3x.trilhossp.client.provider.ProviderNames
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatusDiff
import com.rumd3x.trilhossp.service.LineService
import com.rumd3x.trilhossp.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@Component
class TransitMonitoringJob(
    private val providers: List<LineStatusProvider>,
    private val lineService: LineService,
    private val notificationService: NotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val previousLines = AtomicReference<List<Line>>(emptyList())

    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.MINUTES)
    fun pollTransitStatus() {
        log.info("--- Poll started ---")
        Flux
            .fromIterable(providers)
            .flatMap { provider ->
                provider
                    .fetchLines()
                    .doOnError { log.error("Provider {} failed: {}", provider::class.simpleName, it.message) }
                    .onErrorResume { Mono.just(emptyList()) }
            }.collectList()
            .map { perProvider -> mergeLines(perProvider.flatten()) }
            .flatMap { newLines ->
                val oldByCode = previousLines.get().associateBy { it.code }
                val diffs =
                    newLines.map { newLine ->
                        val oldStatus = oldByCode[newLine.code]?.status ?: newLine.status
                        LineStatusDiff(oldStatus = oldStatus, newStatus = newLine.status)
                    }
                lineService.replaceAll(newLines).thenReturn(newLines to diffs)
            }.doOnNext { (newLines, diffs) ->
                previousLines.set(newLines)
                val pairs = newLines.zip(diffs)
                val changed = pairs.filter { (_, diff) -> diff.hasChange() }
                log.info("Status comparison: {}/{} lines changed", changed.size, newLines.size)
                changed.forEach { (line, diff) ->
                    log.info(
                        "  Line {} [{}]: '{}' -> '{}' (level {})",
                        line.code,
                        line.name,
                        diff.oldStatus.situation + if (diff.oldStatus.descricao.isNotEmpty()) " - ${diff.oldStatus.descricao}" else "",
                        diff.newStatus.situation + if (diff.newStatus.descricao.isNotEmpty()) " - ${diff.newStatus.descricao}" else "",
                        diff.level,
                    )
                }
                changed.forEach { (line, diff) -> notificationService.notify(line, diff).subscribe() }
            }.doOnError { log.error("Failed to poll transit status: {}", it.message) }
            .subscribe()
    }

    // when multiple providers report the same line code, keep the most recently updated status and union the stations
    private fun mergeLines(lines: List<Line>): List<Line> =
        lines
            .groupBy { it.code }
            .map { (code, duplicates) ->
                if (duplicates.size > 1) log.info("Line {} reported by {} providers, merging", code, duplicates.size)
                duplicates.reduce(::mergeDuplicateLine)
            }

    private fun mergeDuplicateLine(
        a: Line,
        b: Line,
    ): Line {
        // ARTESP is preferred regardless of recency; otherwise, the most recently updated line wins
        val preferred =
            when (ProviderNames.ARTESP) {
                a.source -> a
                b.source -> b
                else -> if (parseUpdatedAt(b.status.updatedAt) > parseUpdatedAt(a.status.updatedAt)) b else a
            }
        val alternate = if (preferred === a) b else a

        // fill in any blank field on the preferred status/company with data from the alternate source
        val mergedStatus =
            preferred.status.copy(
                situation = preferred.status.situation.ifBlank { alternate.status.situation },
                classification = preferred.status.classification.ifBlank { alternate.status.classification },
                descricao = preferred.status.descricao.ifBlank { alternate.status.descricao },
                updatedAt = preferred.status.updatedAt.ifBlank { alternate.status.updatedAt },
            )
        val mergedCompany =
            when {
                preferred.company != null && alternate.company != null ->
                    preferred.company.copy(name = preferred.company.name.ifBlank { alternate.company.name })
                else -> preferred.company ?: alternate.company
            }
        val mergedStations = (a.stations + b.stations).distinctBy { it.name }

        return preferred.copy(
            name = preferred.name.ifBlank { alternate.name },
            status = mergedStatus,
            company = mergedCompany,
            stations = mergedStations,
        )
    }

    private fun parseUpdatedAt(raw: String): LocalDateTime =
        try {
            LocalDateTime.parse(raw)
        } catch (_: Exception) {
            LocalDateTime.MIN
        }
}
