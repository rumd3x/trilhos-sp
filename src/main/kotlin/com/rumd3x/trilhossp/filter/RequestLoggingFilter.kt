package com.rumd3x.trilhossp.filter

import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class RequestLoggingFilter : WebFilter {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun filter(
        exchange: ServerWebExchange,
        chain: WebFilterChain,
    ): Mono<Void> {
        val request = exchange.request
        val uri = request.uri.path
        val method = request.method

        if (uri.startsWith("/actuator/")) return chain.filter(exchange)

        val ip = resolveClientIp(request)
        val start = System.currentTimeMillis()

        return chain.filter(exchange).doFinally {
            val status = exchange.response.statusCode?.value() ?: "-"
            val ms = System.currentTimeMillis() - start
            log.info("{} {} {} {} {}ms", ip, method, uri, status, ms)
        }
    }

    private fun resolveClientIp(request: ServerHttpRequest): String {
        // X-Forwarded-For may contain a comma-separated chain; first entry is the original client
        request.headers
            .getFirst("X-Forwarded-For")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.split(",").first().trim() }
        request.headers
            .getFirst("X-Real-IP")
            ?.takeIf { it.isNotBlank() }
            ?.let { return it.trim() }
        return request.remoteAddress?.address?.hostAddress ?: "unknown"
    }
}
