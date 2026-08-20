package com.rumd3x.trilhossp.controller

import com.rumd3x.trilhossp.repository.LineRepository
import com.rumd3x.trilhossp.view.LineDashboardItem
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.result.view.Rendering
import reactor.core.publisher.Mono

@Controller
class DashboardController(
    private val repository: LineRepository,
) {
    @GetMapping("/")
    fun dashboard(): Mono<Rendering> =
        repository
            .findAll()
            .collectList()
            .map { lines ->
                Rendering
                    .view("dashboard")
                    .modelAttribute(
                        "lines",
                        lines
                            .sortedBy { it.code.toIntOrNull() ?: Int.MAX_VALUE }
                            .map { LineDashboardItem.from(it) },
                    ).build()
            }
}
