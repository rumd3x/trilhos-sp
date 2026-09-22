package com.rumd3x.trilhossp.client.provider

/** Human-friendly labels for each [ProviderNames] source code. */
enum class ProviderDisplayName(
    val code: String,
    val label: String,
) {
    ARTESP(ProviderNames.ARTESP, "ARTESP"),
    CPTM(ProviderNames.CPTM, "CPTM"),
    METRO(ProviderNames.METRO, "Metrô SP"),
    CCR(ProviderNames.CCR, "Grupo CCR"),
    ;

    companion object {
        fun labelFor(code: String): String = entries.find { it.code == code }?.label ?: code
    }
}
