package com.rumd3x.trilhossp.client.provider.artesp

import com.rumd3x.trilhossp.client.provider.LineStatusProvider
import com.rumd3x.trilhossp.config.ArtespApiProperties
import com.rumd3x.trilhossp.domain.Line
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class ArtespStatusClient(
    properties: ArtespApiProperties,
    webClientBuilder: WebClient.Builder,
    private val mapper: ArtespStatusMapper,
) : LineStatusProvider {
    private val log = LoggerFactory.getLogger(javaClass)

    private val webClient =
        webClientBuilder
            .baseUrl(properties.baseUrl)
            .defaultHeader("Authorization", "Api-Key ${properties.normalizedKey()}")
            .build()

    override fun fetchLines(): Mono<List<Line>> =
        webClient
            .get()
            .retrieve()
            .bodyToMono(ArtespStatusResponse::class.java)
            .doOnSubscribe { log.info("Fetching transit status from ARTESP API...") }
            .doOnSuccess {
                log.info(
                    "ARTESP API response received: {} companies, {} total lines",
                    it?.empresas?.size,
                    it?.meta?.totalLinhas,
                )
            }.doOnError { log.error("ARTESP API fetch failed: {}", it.message) }
            .map { mapper.toLines(it) }
}
