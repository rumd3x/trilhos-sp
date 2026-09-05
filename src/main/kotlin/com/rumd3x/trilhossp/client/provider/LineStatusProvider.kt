package com.rumd3x.trilhossp.client.provider

import com.rumd3x.trilhossp.domain.Line
import reactor.core.publisher.Mono

/** A source of line status information (e.g. an external API). */
interface LineStatusProvider {
    fun fetchLines(): Mono<List<Line>>
}
