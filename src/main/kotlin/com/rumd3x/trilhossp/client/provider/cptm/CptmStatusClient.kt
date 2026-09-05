package com.rumd3x.trilhossp.client.provider.cptm

import com.rumd3x.trilhossp.client.provider.LineStatusProvider
import com.rumd3x.trilhossp.config.CptmApiProperties
import com.rumd3x.trilhossp.domain.Line
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class CptmStatusClient(
    properties: CptmApiProperties,
    webClientBuilder: WebClient.Builder,
    private val mapper: CptmStatusMapper,
) : LineStatusProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = webClientBuilder.baseUrl(properties.baseUrl).build()

    override fun fetchLines(): Mono<List<Line>> =
        webClient
            .get()
            .retrieve()
            .bodyToMono(object : ParameterizedTypeReference<List<CptmLineStatus>>() {})
            .doOnSubscribe { log.info("Fetching transit status from CPTM API...") }
            .doOnSuccess { log.info("CPTM API response received: {} lines", it?.size) }
            .doOnError { log.error("CPTM API fetch failed: {}", it.message) }
            .map { mapper.toLines(it) }
}
