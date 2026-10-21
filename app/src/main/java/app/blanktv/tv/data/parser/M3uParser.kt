package app.blanktv.tv.data.parser

import app.blanktv.tv.domain.MediaKind
import java.io.BufferedReader

/**
 * One entry read out of a playlist, before it is persisted.
 */
data class ParsedEntry(
    val title: String,
    val url: String,
    val logoUrl: String?,
    val tvgId: String?,
    val groupName: String?,
    val kind: MediaKind,
    val channelNumber: Int?,
)

/**
 * Streaming M3U / M3U8 reader.
 *
 * The playlist is consumed line by line and never buffered whole: provider playlists regularly run
 * to tens of thousands of entries, and holding the raw text plus the parsed model at once is what
 * turns a large import into an out-of-memory crash.
 *
 * Recognised directives:
 *  - `#EXTINF:<seconds> key="value" …,Display title`
 *  - `#EXTGRP:<group>`            (fallback group when EXTINF carries none)
 *  - `#EXTVLCOPT:` / `#KODIPROP:` (ignored)
 *  - any other non-`#` line is treated as the entry URL
 */
object M3uParser {

    private val ATTRIBUTE = Regex("([A-Za-z0-9_-]+)=\"([^\"]*)\"")

    private val MOVIE_HINT = Regex("(?i)(/movie/|\\bvod\\b|movies?|pel[ií]culas?)")
    private val SERIES_HINT = Regex("(?i)(/series?/|series?|temporada|season)")

    fun parse(reader: BufferedReader): List<ParsedEntry> {
        val out = ArrayList<ParsedEntry>(512)

        var title: String? = null
        var logo: String? = null
        var tvgId: String? = null
        var group: String? = null
        var number: Int? = null

        fun reset() {
            title = null
            logo = null
            tvgId = null
            group = null
            number = null
        }

        reader.useLines { lines ->
            for (raw in lines) {
                val line = raw.trim()
                if (line.isEmpty()) continue

                when {
                    line.startsWith("#EXTINF", ignoreCase = true) -> {
                        val comma = line.indexOf(',')
                        val attrPart = if (comma >= 0) line.substring(0, comma) else line
                        title = if (comma >= 0) line.substring(comma + 1).trim().ifEmpty { null }
                        for (m in ATTRIBUTE.findAll(attrPart)) {
                            val key = m.groupValues[1].lowercase()
                            val value = m.groupValues[2].trim()
                            if (value.isEmpty()) continue
                            when (key) {
                                "tvg-id" -> tvgId = value
                                "tvg-logo" -> logo = value
                                "group-title" -> group = value
                                "tvg-chno", "channel-number" -> number = value.toIntOrNull()
                                "tvg-name" -> if (title == null) title = value
                            }
                        }
                    }

                    line.startsWith("#EXTGRP", ignoreCase = true) -> {
                        val value = line.substringAfter(':').trim()
                        if (value.isNotEmpty()) group = value
                    }

                    line.startsWith("#") -> Unit // EXTM3U, EXTVLCOPT, KODIPROP, comments

                    else -> {
                        val entryTitle = title ?: line.substringAfterLast('/').ifEmpty { line }
                        out += ParsedEntry(
                            title = entryTitle,
                            url = line,
                            logoUrl = logo,
                            tvgId = tvgId,
                            groupName = group,
                            kind = classify(line, group),
                            channelNumber = number,
                        )
                        reset()
                    }
                }
            }
        }
        return out
    }

    /**
     * M3U carries no type field, so infer one. Xtream-style URLs are unambiguous
     * (`…/movie/user/pass/id.ext`, `…/series/…`); everything else falls back to the group name and
     * finally to Live, which is the safe default for a tuner-style entry.
     */
    private fun classify(url: String, group: String?): MediaKind {
        val hay = "$url ${group.orEmpty()}"
        return when {
            SERIES_HINT.containsMatchIn(hay) -> MediaKind.Series
            MOVIE_HINT.containsMatchIn(hay) -> MediaKind.Movie
            else -> MediaKind.Live
        }
    }
}
