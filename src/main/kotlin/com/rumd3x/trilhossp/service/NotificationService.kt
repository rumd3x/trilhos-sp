package com.rumd3x.trilhossp.service

import com.rumd3x.trilhossp.client.notification.Notifier
import com.rumd3x.trilhossp.config.NotificationProperties
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatusDiff
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.time.LocalDate

@Service
class NotificationService(
    notifyProps: NotificationProperties,
    notifiers: List<Notifier>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    // null means "all" — parsed once at startup for efficient lookup
    private val allowedDays: Set<Int>? =
        notifyProps.days
            .takeUnless { it == "all" }
            ?.split(",")
            ?.map { it.trim().toInt() }
            ?.toSet()

    private val allowedLines: Set<String>? =
        notifyProps.lines
            .takeUnless { it == "all" }
            ?.split(",")
            ?.map { it.trim() }
            ?.toSet()

    private val notifyLevel = notifyProps.level

    private val senders: List<Notifier>

    init {
        val (active, inactive) = notifiers.partition { it.isConfigured() }
        inactive.forEach { log.warn("Notification provider skipped (not configured): {}", it::class.simpleName) }
        active.forEach { log.info("Notification provider ready: {}", it::class.simpleName) }
        require(active.isNotEmpty()) { "No notification methods configured." }
        senders = active
    }

    fun notify(
        line: Line,
        diff: LineStatusDiff,
    ): Mono<Void> {
        if (!shouldNotify(line, diff)) return Mono.empty()
        return Flux
            .fromIterable(senders)
            .flatMap { it.send(line, diff) }
            .then()
    }

    private fun shouldNotify(
        line: Line,
        diff: LineStatusDiff,
    ): Boolean {
        val lineMatches = allowedLines == null || line.code in allowedLines
        if (!lineMatches) {
            log.info("Line {} skipped: not in allowed lines {}", line.code, allowedLines)
            return false
        }
        // DayOfWeek.value: Mon=1, Tue=2, Wed=3, Thu=4, Fri=5, Sat=6, Sun=7
        val today = LocalDate.now().dayOfWeek.value
        val dayMatches = allowedDays == null || today in allowedDays
        if (!dayMatches) {
            log.info("Line {} skipped: today ({}) not in allowed days {}", line.code, today, allowedDays)
            return false
        }
        val levelMatches = diff.level >= notifyLevel
        if (!levelMatches) {
            log.info("Line {} skipped: diff level {} below threshold {}", line.code, diff.level, notifyLevel)
            return false
        }
        return true
    }
}
