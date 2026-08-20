package com.rumd3x.trilhossp.client.notification

import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatusDiff
import reactor.core.publisher.Mono

interface Notifier {
    fun isConfigured(): Boolean
    fun send(line: Line, diff: LineStatusDiff): Mono<Void>
}
