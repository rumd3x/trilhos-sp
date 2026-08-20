package com.rumd3x.trilhossp.client

import com.rumd3x.trilhossp.config.TransitApiProperties
import com.rumd3x.trilhossp.model.TransitStatusResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class TransitStatusClient(
    properties: TransitApiProperties,
    webClientBuilder: WebClient.Builder,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient = webClientBuilder
        .baseUrl(properties.baseUrl)
        .defaultHeader("Authorization", "Api-Key ${properties.normalizedKey()}")
        .build()

    fun fetchStatus(): Mono<TransitStatusResponse> =
        webClient.get()
            .retrieve()
            .bodyToMono(TransitStatusResponse::class.java)
            .doOnSubscribe { log.info("Fetching transit status from API...") }
            .doOnSuccess { log.info("API response received: {} companies, {} total lines", it.empresas.size, it.meta.totalLinhas) }
            .doOnError { log.error("API fetch failed: {}", it.message) }
}
