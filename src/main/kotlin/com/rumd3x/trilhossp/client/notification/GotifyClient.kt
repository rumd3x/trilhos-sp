package com.rumd3x.trilhossp.client.notification

import com.rumd3x.trilhossp.config.GotifyProperties
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatusDiff
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class GotifyClient(
    private val props: GotifyProperties,
    webClientBuilder: WebClient.Builder,
) : Notifier {
    private val log = LoggerFactory.getLogger(javaClass)

    // initialized lazily so an unconfigured client never builds an invalid WebClient
    private val httpClient: WebClient by lazy {
        webClientBuilder
            .baseUrl(props.url)
            .defaultHeader("X-Gotify-Key", props.token)
            .build()
    }

    override fun isConfigured(): Boolean = props.isConfigured().also { configured ->
        if (!configured) log.warn("GotifyClient not configured: set GOTIFY_URL and GOTIFY_TOKEN")
    }

    override fun send(line: Line, diff: LineStatusDiff): Mono<Void> =
        httpClient.post()
            .uri("/message")
            .bodyValue(
                mapOf(
                    "title" to "Linha ${line.code} – ${line.name}",
                    "message" to "${diff.oldStatus.situation} → ${diff.newStatus.situation}",
                    "priority" to diff.level,
                )
            )
            .retrieve()
            .toBodilessEntity()
            .doOnSuccess { log.info("Gotify notification sent for line {} [{}]: '{}' -> '{}' (level {})", line.code, line.name, diff.oldStatus.situation, diff.newStatus.situation, diff.level) }
            .doOnError { log.error("Failed to send Gotify notification for line {}: {}", line.code, it.message) }
            .then()
}
