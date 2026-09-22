package com.rumd3x.trilhossp.client.provider.ccr

import com.rumd3x.trilhossp.client.provider.LineStatusProvider
import com.rumd3x.trilhossp.config.CcrApiProperties
import com.rumd3x.trilhossp.domain.Line
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class CcrStatusClient(
    properties: CcrApiProperties,
    webClientBuilder: WebClient.Builder,
    private val mapper: CcrStatusMapper,
) : LineStatusProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = webClientBuilder.baseUrl(properties.baseUrl).build()

    override fun fetchLines(): Mono<List<Line>> =
        webClient
            .get()
            .retrieve()
            .bodyToMono(CcrStatusResponse::class.java)
            .doOnSubscribe { log.info("Fetching transit status from Grupo CCR API...") }
            .doOnSuccess { log.info("Grupo CCR API response received: {} concessions", it?.data?.concessoes?.size) }
            .doOnError { log.error("Grupo CCR API fetch failed: {}", it.message) }
            .map { mapper.toLines(it) }
}
