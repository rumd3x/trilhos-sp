package com.rumd3x.trilhossp.job

import com.rumd3x.trilhossp.client.notification.Notifier
import com.rumd3x.trilhossp.client.provider.LineStatusProvider
import com.rumd3x.trilhossp.client.provider.ProviderNames
import com.rumd3x.trilhossp.config.NotificationProperties
import com.rumd3x.trilhossp.domain.Company
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.LineStatusDiff
import com.rumd3x.trilhossp.domain.Station
import com.rumd3x.trilhossp.service.LineService
import com.rumd3x.trilhossp.service.NotificationService
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import reactor.core.publisher.Mono
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class TransitMonitoringJobTest {
    private val provider = mockk<LineStatusProvider>()
    private val lineService = mockk<LineService>()

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

    private val job = TransitMonitoringJob(listOf(provider), lineService, notificationService)

    init {
        every { lineService.replaceAll(any()) } returns Mono.empty()
    }

    private fun line(
        code: String,
        situation: String,
    ) = Line(
        code = code,
        name = "Linha $code",
        status = LineStatus(situation, "", "", situation.contains("Normal"), ""),
        company = Company(1, "Test", true),
        stations = emptyList(),
        source = listOf("test"),
    )

    private fun lineFull(
        code: String,
        situation: String,
        classification: String = "",
        descricao: String = "",
        updatedAt: String = "",
        companyName: String = "Test",
        stations: List<String> = emptyList(),
        source: List<String> = listOf("test"),
    ) = Line(
        code = code,
        name = "Linha $code",
        status = LineStatus(situation, classification, descricao, situation.contains("Normal"), updatedAt),
        company = Company(1, companyName, true),
        stations = stations.map { Station(it) },
        source = source,
    )

    @Test fun `does not notify on first poll regardless of status`() {
        every { provider.fetchLines() } returns Mono.just(listOf(line("4", "Lentidão")))

        job.pollTransitStatus()

        assertEquals(0, sendCount.get())
    }

    @Test fun `does not notify when status is unchanged between polls`() {
        every { provider.fetchLines() } returns Mono.just(listOf(line("4", "Operação Normal"), line("5", "Lentidão")))

        job.pollTransitStatus()
        job.pollTransitStatus()

        assertEquals(0, sendCount.get())
    }

    @Test fun `notifies only the changed line between polls`() {
        every { provider.fetchLines() } returnsMany
            listOf(
                Mono.just(listOf(line("4", "Operação Normal"), line("5", "Operação Normal"))),
                Mono.just(listOf(line("4", "Lentidão"), line("5", "Operação Normal"))),
            )

        job.pollTransitStatus()
        assertEquals(0, sendCount.get())

        job.pollTransitStatus()
        assertEquals(1, sendCount.get())
    }

    @Test fun `notifies all lines that changed in same poll`() {
        every { provider.fetchLines() } returnsMany
            listOf(
                Mono.just(listOf(line("4", "Operação Normal"), line("5", "Operação Normal"))),
                Mono.just(listOf(line("4", "Lentidão"), line("5", "Paralisada"))),
            )

        job.pollTransitStatus()
        job.pollTransitStatus()

        assertEquals(2, sendCount.get())
    }

    @Test fun `does not notify again if status reverts and polls without change`() {
        every { provider.fetchLines() } returnsMany
            listOf(
                Mono.just(listOf(line("4", "Operação Normal"))),
                Mono.just(listOf(line("4", "Lentidão"))),
                Mono.just(listOf(line("4", "Lentidão"))),
            )

        job.pollTransitStatus()
        job.pollTransitStatus()
        val afterChange = sendCount.get()

        job.pollTransitStatus()
        assertEquals(afterChange, sendCount.get())
    }

    @Test fun `merges lines from multiple providers`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns Mono.just(listOf(line("4", "Operação Normal")))
        every { secondProvider.fetchLines() } returns Mono.just(listOf(line("20", "Operação Normal")))

        val multiProviderJob = TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService)
        multiProviderJob.pollTransitStatus()

        assertEquals(0, sendCount.get())
    }

    @Test fun `continues with other providers when one fails`() {
        val failingProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns Mono.just(listOf(line("4", "Operação Normal")))
        every { failingProvider.fetchLines() } returns Mono.error(RuntimeException("boom"))

        val multiProviderJob = TransitMonitoringJob(listOf(provider, failingProvider), lineService, notificationService)
        multiProviderJob.pollTransitStatus()

        assertEquals(0, sendCount.get())
    }

    // merge logic

    @Test fun `merges duplicate line codes from multiple providers into a single line`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns Mono.just(listOf(lineFull("4", "Operação Normal")))
        every { secondProvider.fetchLines() } returns Mono.just(listOf(lineFull("4", "Operação Normal")))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(1, captured.captured.size)
    }

    @Test fun `does not merge lines with different codes`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns Mono.just(listOf(lineFull("4", "Operação Normal")))
        every { secondProvider.fetchLines() } returns Mono.just(listOf(lineFull("5", "Operação Normal")))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(setOf("4", "5"), captured.captured.map { it.code }.toSet())
    }

    @Test fun `merged line keeps the status from the most recently updated provider`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Lentidão", updatedAt = "2024-01-01T10:00:00")))
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", updatedAt = "2024-01-01T12:00:00")))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(
            "Operação Normal",
            captured.captured
                .first()
                .status.situation,
        )
    }

    @Test fun `prefers artesp source over a more recently updated line from another provider`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Lentidão", updatedAt = "2024-01-01T09:00:00", source = listOf(ProviderNames.ARTESP))))
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", updatedAt = "2024-01-01T12:00:00", source = listOf(ProviderNames.CPTM))))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(
            "Lentidão",
            captured.captured
                .first()
                .status.situation,
        )
    }

    @Test fun `falls back to most recently updated line when neither source is artesp`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Lentidão", updatedAt = "2024-01-01T09:00:00", source = listOf(ProviderNames.CPTM))))
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", updatedAt = "2024-01-01T12:00:00", source = listOf(ProviderNames.METRO))))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(
            "Operação Normal",
            captured.captured
                .first()
                .status.situation,
        )
    }

    @Test fun `backfills blank fields from alternate even when artesp is preferred`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(
                listOf(
                    lineFull(
                        "4",
                        "Operação Normal",
                        classification = "",
                        descricao = "",
                        updatedAt = "2024-01-01T09:00:00",
                        source = listOf(ProviderNames.ARTESP),
                    ),
                ),
            )
        every { secondProvider.fetchLines() } returns
            Mono.just(
                listOf(
                    lineFull(
                        "4",
                        "Operação Normal",
                        classification = "C",
                        descricao = "Tudo certo",
                        updatedAt = "2024-01-01T12:00:00",
                        source = listOf(ProviderNames.CPTM),
                    ),
                ),
            )
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        val merged = captured.captured.first()
        assertEquals("C", merged.status.classification)
        assertEquals("Tudo certo", merged.status.descricao)
    }

    @Test fun `merged line backfills blank fields from the less recently updated provider`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(
                listOf(
                    lineFull(
                        "4",
                        "Operação Normal",
                        classification = "operacional",
                        descricao = "Tudo certo",
                        updatedAt = "2024-01-01T09:00:00",
                    ),
                ),
            )
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", updatedAt = "2024-01-01T10:00:00")))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        val merged = captured.captured.first()
        assertEquals("operacional", merged.status.classification)
        assertEquals("Tudo certo", merged.status.descricao)
    }

    @Test fun `merged line unions stations from both providers without duplicates`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", stations = listOf("Luz", "Sé"))))
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", stations = listOf("Sé", "República"))))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(
            listOf("Luz", "Sé", "República"),
            captured.captured
                .first()
                .stations
                .map { it.name },
        )
    }

    @Test fun `merged line combines sources from both providers without duplicates`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", source = listOf(ProviderNames.CPTM))))
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", source = listOf(ProviderNames.METRO))))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(listOf(ProviderNames.CPTM, ProviderNames.METRO), captured.captured.first().source)
    }

    @Test fun `merged line does not duplicate a source reported by both providers`() {
        val secondProvider = mockk<LineStatusProvider>()
        every { provider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", source = listOf(ProviderNames.CPTM))))
        every { secondProvider.fetchLines() } returns
            Mono.just(listOf(lineFull("4", "Operação Normal", source = listOf(ProviderNames.CPTM))))
        val captured = slot<List<Line>>()
        every { lineService.replaceAll(capture(captured)) } returns Mono.empty()

        TransitMonitoringJob(listOf(provider, secondProvider), lineService, notificationService).pollTransitStatus()

        assertEquals(listOf(ProviderNames.CPTM), captured.captured.first().source)
    }
}
