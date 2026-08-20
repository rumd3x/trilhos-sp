package com.rumd3x.trilhossp.job

import com.rumd3x.trilhossp.client.TransitStatusClient
import com.rumd3x.trilhossp.client.notification.Notifier
import com.rumd3x.trilhossp.config.NotificationProperties
import com.rumd3x.trilhossp.domain.Company
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.LineStatusDiff
import com.rumd3x.trilhossp.mapper.TransitStatusMapper
import com.rumd3x.trilhossp.model.TransitStatusResponse
import com.rumd3x.trilhossp.service.LineService
import com.rumd3x.trilhossp.service.NotificationService
import io.mockk.every
import io.mockk.mockk
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class TransitMonitoringJobTest {
    private val client = mockk<TransitStatusClient>()
    private val mapper = mockk<TransitStatusMapper>()
    private val lineService = mockk<LineService>()
    private val response = mockk<TransitStatusResponse>()

    private val sendCount = AtomicInteger(0)
    private val notificationService =
        NotificationService(
            NotificationProperties(days = "all", level = 0, lines = "all"),
            listOf(
                object : Notifier {
                    override fun isConfigured() = true

                    override fun send(
                        line: Line,
                        diff: LineStatusDiff,
                    ): Mono<Void> {
                        sendCount.incrementAndGet()
                        return Mono.empty()
                    }
                },
            ),
        )

    private val job = TransitMonitoringJob(client, mapper, lineService, notificationService)

    init {
        every { client.fetchStatus() } returns Mono.just(response)
        every { lineService.replaceAll(any()) } returns Mono.empty()
    }

    private fun line(
        code: String,
        situation: String,
    ) = Line(
        code = code,
        name = "Linha $code",
        status = LineStatus(situation, "", situation.contains("Normal"), ""),
        company = Company(1, "Test", true),
        stations = emptyList(),
    )

    @Test fun `does not notify on first poll regardless of status`() {
        every { mapper.toLines(response) } returns listOf(line("4", "Lentidão"))

        job.pollTransitStatus()

        assertEquals(0, sendCount.get())
    }

    @Test fun `does not notify when status is unchanged between polls`() {
        every { mapper.toLines(response) } returns listOf(line("4", "Operação Normal"), line("5", "Lentidão"))

        job.pollTransitStatus()
        job.pollTransitStatus()

        assertEquals(0, sendCount.get())
    }

    @Test fun `notifies only the changed line between polls`() {
        every { mapper.toLines(response) } returnsMany
            listOf(
                listOf(line("4", "Operação Normal"), line("5", "Operação Normal")),
                listOf(line("4", "Lentidão"), line("5", "Operação Normal")),
            )

        job.pollTransitStatus()
        assertEquals(0, sendCount.get())

        job.pollTransitStatus()
        assertEquals(1, sendCount.get())
    }

    @Test fun `notifies all lines that changed in same poll`() {
        every { mapper.toLines(response) } returnsMany
            listOf(
                listOf(line("4", "Operação Normal"), line("5", "Operação Normal")),
                listOf(line("4", "Lentidão"), line("5", "Paralisada")),
            )

        job.pollTransitStatus()
        job.pollTransitStatus()

        assertEquals(2, sendCount.get())
    }

    @Test fun `does not notify again if status reverts and polls without change`() {
        every { mapper.toLines(response) } returnsMany
            listOf(
                listOf(line("4", "Operação Normal")),
                listOf(line("4", "Lentidão")),
                listOf(line("4", "Lentidão")),
            )

        job.pollTransitStatus()
        job.pollTransitStatus()
        val afterChange = sendCount.get()

        job.pollTransitStatus()
        assertEquals(afterChange, sendCount.get())
    }
}
