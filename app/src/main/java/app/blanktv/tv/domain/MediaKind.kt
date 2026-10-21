package app.blanktv.tv.domain

/**
 * The three content families a playlist can carry.
 *
 * M3U playlists do not declare a type per entry, so the parser infers one (see
 * [app.blanktv.tv.data.parser.M3uParser]) and stores it as [code]. Persisting the code rather than
 * the enum name keeps the column a plain integer and survives enum renames.
 */
enum class MediaKind(val code: Int) {
    Live(0),
    Movie(1),
    Series(2),
    ;

    companion object {
        fun fromCode(code: Int): MediaKind = entries.firstOrNull { it.code == code } ?: Live
    }
}
