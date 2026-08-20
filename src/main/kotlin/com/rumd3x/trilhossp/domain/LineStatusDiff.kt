package com.rumd3x.trilhossp.domain

data class LineStatusDiff(
    val oldStatus: LineStatus,
    val newStatus: LineStatus,
) {
    val level: Int
        get() =
            when {
                isPositive() -> 1
                isReallyBad() -> 3
                isNeutral() -> 0
                isNegative() -> 2
                else -> 4
            }

    fun isNeutral(): Boolean {
        val startedNow = oldStatus.situation.lowercase().contains("encerrada")
        val isCurrentlyNormal = newStatus.situation.lowercase().contains("normal")
        val finishedNow = newStatus.situation.lowercase().contains("encerrada")
        return (startedNow && isCurrentlyNormal) || finishedNow
    }

    fun isPositive(): Boolean = newStatus.situation.lowercase().contains("normal") && !isNeutral()

    fun isNegative(): Boolean = !isPositive() && !isNeutral() && !isUnknown()

    fun isReallyBad(): Boolean {
        val newSituation = newStatus.situation.lowercase()
        return newSituation.contains("paralisad") ||
            newSituation.contains("paralizad") ||
            newSituation.contains("parcial")
    }

    // case-sensitive to match the original behaviour
    private fun isUnknown(): Boolean = newStatus.situation.contains("Dados Indisponíveis")
}
