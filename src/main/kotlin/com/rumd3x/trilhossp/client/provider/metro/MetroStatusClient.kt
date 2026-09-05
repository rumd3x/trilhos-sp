package com.rumd3x.trilhossp.client.provider.metro

import com.rumd3x.trilhossp.client.provider.LineStatusProvider
import com.rumd3x.trilhossp.config.MetroApiProperties
import com.rumd3x.trilhossp.domain.Line
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class MetroStatusClient(
    properties: MetroApiProperties,
    webClientBuilder: WebClient.Builder,
    private val mapper: MetroStatusMapper,
) : LineStatusProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = webClientBuilder.baseUrl(properties.baseUrl).build()

    override fun fetchLines(): Mono<List<Line>> =
        webClient
            .get()
            .retrieve()
            .bodyToMono(String::class.java)
            .doOnSubscribe { log.info("Fetching transit status from Metrô SP page...") }
            .doOnSuccess { log.info("Metrô SP page fetched successfully") }
            .doOnError { log.error("Metrô SP page fetch failed: {}", it.message) }
            .map { mapper.toLines(it) }
}
