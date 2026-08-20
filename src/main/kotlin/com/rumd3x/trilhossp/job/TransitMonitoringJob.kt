package com.rumd3x.trilhossp.job

import com.rumd3x.trilhossp.client.TransitStatusClient
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatusDiff
import com.rumd3x.trilhossp.mapper.TransitStatusMapper
import com.rumd3x.trilhossp.service.LineService
import com.rumd3x.trilhossp.service.NotificationService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicReference

@Component
class TransitMonitoringJob(
    private val client: TransitStatusClient,
    private val mapper: TransitStatusMapper,
    private val lineService: LineService,
    private val notificationService: NotificationService,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val previousLines = AtomicReference<List<Line>>(emptyList())

    @Scheduled(cron = "0 */10 * * * *")
    fun pollTransitStatus() {
        log.info("--- Poll started ---")
        client
            .fetchStatus()
            .map { mapper.toLines(it) }
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
                val changed = pairs.filter { (_, diff) -> diff.oldStatus != diff.newStatus }
                log.info("Status comparison: {}/{} lines changed", changed.size, newLines.size)
                changed.forEach { (line, diff) ->
                    log.info(
                        "  Line {} [{}]: '{}' -> '{}' (level {})",
                        line.code,
                        line.name,
                        diff.oldStatus.situation,
                        diff.newStatus.situation,
                        diff.level,
                    )
                }
                changed.forEach { (line, diff) -> notificationService.notify(line, diff).subscribe() }
            }.doOnError { log.error("Failed to poll transit status: {}", it.message) }
            .subscribe()
    }
}
