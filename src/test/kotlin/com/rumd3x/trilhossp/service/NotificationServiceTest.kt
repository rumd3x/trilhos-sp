package com.rumd3x.trilhossp.service

import com.rumd3x.trilhossp.client.notification.Notifier
import com.rumd3x.trilhossp.config.NotificationProperties
import com.rumd3x.trilhossp.domain.Company
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import com.rumd3x.trilhossp.domain.LineStatusDiff
import org.junit.jupiter.api.Test
import reactor.core.publisher.Mono
import reactor.test.StepVerifier
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NotificationServiceTest {

    private fun service(
        days: String = "all",
        level: Int = 0,
        lines: String = "all",
        notifiers: List<Notifier> = listOf(notifier()),
    ) = NotificationService(NotificationProperties(days = days, level = level, lines = lines), notifiers)

    private fun notifier(configured: Boolean = true, onSend: () -> Unit = {}) = object : Notifier {
        override fun isConfigured() = configured
        override fun send(line: Line, diff: LineStatusDiff): Mono<Void> { onSend(); return Mono.empty() }
    }

    private val line = Line(
        code = "4",
        name = "Linha 4-Amarela",
        status = LineStatus("Operação Normal", "operacional", true, ""),
        company = Company(1, "ViaQuatro", true),
        stations = emptyList(),
    )

    private fun diff(old: String, new: String): LineStatusDiff {
        val s = { sit: String -> LineStatus(sit, "", true, "") }
        return LineStatusDiff(s(old), s(new))
    }

    // line filter

    @Test fun `notify returns empty when line is not in allowed list`() {
        StepVerifier.create(service(lines = "5,9").notify(line, diff("Operação Normal", "Lentidão")))
            .verifyComplete()
    }

    @Test fun `notify calls send when line is in allowed list`() {
        val called = AtomicBoolean(false)
        service(lines = "4,5", notifiers = listOf(notifier { called.set(true) }))
            .notify(line, diff("Operação Normal", "Lentidão")).block()
        assertTrue(called.get())
    }

    // day filter

    @Test fun `notify returns empty when today is not in allowed days`() {
        val today = LocalDate.now().dayOfWeek.value
        val otherDays = (1..7).filter { it != today }.joinToString(",")
        StepVerifier.create(service(days = otherDays).notify(line, diff("Operação Normal", "Lentidão")))
            .verifyComplete()
    }

    @Test fun `notify calls send when today is in allowed days`() {
        val called = AtomicBoolean(false)
        val today = LocalDate.now().dayOfWeek.value.toString()
        service(days = today, notifiers = listOf(notifier { called.set(true) }))
            .notify(line, diff("Operação Normal", "Lentidão")).block()
        assertTrue(called.get())
    }

    // level filter

    @Test fun `notify returns empty when diff level is below threshold`() {
        // positive change is level 1, threshold 3 → blocked
        StepVerifier.create(service(level = 3).notify(line, diff("Lentidão", "Operação Normal")))
            .verifyComplete()
    }

    @Test fun `notify calls send when diff level meets threshold`() {
        val called = AtomicBoolean(false)
        // paralisada is level 3, threshold 2 → passes
        service(level = 2, notifiers = listOf(notifier { called.set(true) }))
            .notify(line, diff("Operação Normal", "Paralisada")).block()
        assertTrue(called.get())
    }

    @Test fun `notify returns empty when multiple filters fail`() {
        val today = LocalDate.now().dayOfWeek.value
        val otherDays = (1..7).filter { it != today }.joinToString(",")
        StepVerifier.create(
            service(days = otherDays, lines = "9,11", level = 3)
                .notify(line, diff("Operação Normal", "Paralisada"))
        ).verifyComplete()
    }

    // startup validation

    @Test fun `throws when notifiers list is empty`() {
        assertFailsWith<IllegalArgumentException> { service(notifiers = emptyList()) }
    }

    @Test fun `throws when all notifiers are unconfigured`() {
        assertFailsWith<IllegalArgumentException> { service(notifiers = listOf(notifier(configured = false))) }
    }
}

class NotificationServiceTest {

    private fun service(
        days: String = "all",
        level: Int = 0,
        lines: String = "all",
        gotifyUrl: String = "http://127.0.0.1:1",
        gotifyToken: String = "test",
    ) = NotificationService(
        NotificationProperties(days = days, level = level, lines = lines),
        GotifyProperties(url = gotifyUrl, token = gotifyToken),
        WebClient.builder(),
    )

    private val line = Line(
        code = "4",
        name = "Linha 4-Amarela",
        status = LineStatus("Operação Normal", "operacional", true, ""),
        company = Company(1, "ViaQuatro", true),
        stations = emptyList(),
    )

    private fun diff(old: String, new: String): LineStatusDiff {
        val s = { sit: String -> LineStatus(sit, "", true, "") }
        return LineStatusDiff(s(old), s(new))
    }

    // line filter

    @Test fun `notify returns empty when line is not in allowed list`() {
        val result = service(lines = "5,9").notify(line, diff("Operação Normal", "Lentidão"))
        StepVerifier.create(result).verifyComplete()
    }

    @Test fun `notify attempts call when line is in allowed list`() {
        val result = service(lines = "4,5").notify(line, diff("Operação Normal", "Lentidão"))
        StepVerifier.create(result).verifyError()
    }

    // day filter

    @Test fun `notify returns empty when today is not in allowed days`() {
        val today = LocalDate.now().dayOfWeek.value
        val otherDays = (1..7).filter { it != today }.joinToString(",")
        val result = service(days = otherDays).notify(line, diff("Operação Normal", "Lentidão"))
        StepVerifier.create(result).verifyComplete()
    }

    @Test fun `notify attempts call when today is in allowed days`() {
        val today = LocalDate.now().dayOfWeek.value.toString()
        val result = service(days = today).notify(line, diff("Operação Normal", "Lentidão"))
        StepVerifier.create(result).verifyError()
    }

    // level filter

    @Test fun `notify returns empty when diff level is below threshold`() {
        // level=3 means only notify for level >= 3; positive change is level 1
        val result = service(level = 3).notify(line, diff("Lentidão", "Operação Normal"))
        StepVerifier.create(result).verifyComplete()
    }

    @Test fun `notify attempts call when diff level meets threshold`() {
        // level=2 means notify for level >= 2; paralisada is level 3
        val result = service(level = 2).notify(line, diff("Operação Normal", "Paralisada"))
        StepVerifier.create(result).verifyError()
    }

    @Test fun `notify returns empty when multiple filters fail`() {
        val today = LocalDate.now().dayOfWeek.value
        val otherDays = (1..7).filter { it != today }.joinToString(",")
        val result = service(days = otherDays, lines = "9,11", level = 3)
            .notify(line, diff("Operação Normal", "Paralisada"))
        StepVerifier.create(result).verifyComplete()
    }

    // startup validation

    @Test fun `throws when no notification method is configured`() {
        assertFailsWith<IllegalArgumentException> {
            service(gotifyUrl = "", gotifyToken = "")
        }
    }

    @Test fun `throws when gotify url is blank even if token is set`() {
        assertFailsWith<IllegalArgumentException> {
            service(gotifyUrl = "", gotifyToken = "some-token")
        }
    }

    @Test fun `throws when gotify token is blank even if url is set`() {
        assertFailsWith<IllegalArgumentException> {
            service(gotifyUrl = "http://gotify.example.com", gotifyToken = "")
        }
    }
}
