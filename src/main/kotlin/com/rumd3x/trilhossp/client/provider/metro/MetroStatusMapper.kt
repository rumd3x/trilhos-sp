package com.rumd3x.trilhossp.client.provider.metro

import com.rumd3x.trilhossp.client.provider.ProviderNames
import com.rumd3x.trilhossp.domain.Line
import com.rumd3x.trilhossp.domain.LineStatus
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

@Component
class MetroStatusMapper {
    private val name = ProviderNames.METRO

    // dd/MM/yyyy HH:mm[:ss], seconds are sometimes omitted by the source page
    private val inputFormatter =
        DateTimeFormatterBuilder()
            .appendPattern("dd/MM/yyyy HH:mm")
            .optionalStart()
            .appendPattern(":ss")
            .optionalEnd()
            .toFormatter()

    private val datePattern = Regex("""\d{2}/\d{2}/\d{4} \d{2}:\d{2}(?::\d{2})?""")

    fun toLines(html: String): List<Line> =
        Jsoup
            .parse(html)
            .select("ol.direto-metro li.linha")
            .mapNotNull { toLine(it) }

    private fun toLine(li: Element): Line? {
        val code =
            li
                .selectFirst(".linha-numero")
                ?.text()
                ?.trim()
                ?.takeIf { it.isNotBlank() } ?: return null
        val nome =
            li
                .selectFirst(".linha-nome")
                ?.text()
                ?.trim()
                .orEmpty()
        val situacao =
            li
                .selectFirst(".linha-situacao")
                ?.text()
                ?.trim()
                .orEmpty()

        // extra details are embedded as an HTML fragment inside this tooltip attribute
        val infoHtml = li.selectFirst(".linha-info")?.attr("data-bs-title").orEmpty()
        val infoDoc = Jsoup.parseBodyFragment(infoHtml)
        val descricao =
            infoDoc
                .selectFirst(".description")
                ?.text()
                ?.trim()
                .orEmpty()
        val dateText = infoDoc.selectFirst(".date")?.text().orEmpty()

        return Line(
            name = "Linha $code-$nome",
            code = code,
            company = null,
            status =
                LineStatus(
                    situation = situacao,
                    classification = "",
                    descricao = descricao,
                    isNormal = descricao == "Situação Normal",
                    updatedAt = parseUpdatedAt(dateText),
                ),
            stations = emptyList(),
            source = name,
        )
    }

    private fun parseUpdatedAt(raw: String): String {
        val match = datePattern.find(raw)?.value ?: return ""
        return try {
            LocalDateTime.parse(match, inputFormatter).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        } catch (_: Exception) {
            ""
        }
    }
}
