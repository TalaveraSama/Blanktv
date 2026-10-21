package app.blanktv.tv.domain

/**
 * The provider protocols blankTV can talk to.
 *
 * They are stored as [code] on the playlist row so a re-open knows which client to drive; the three
 * are radically different (one document vs. an authenticated JSON API vs. a token+cookie session),
 * which is why each has its own client under `data/`.
 */
enum class SourceKind(val code: Int) {
    /** A single M3U/M3U8 document fetched over HTTP. */
    M3U(0),

    /** Xtream Codes panel API (`player_api.php`), as served by xui.one and most reseller panels. */
    Xtream(1),

    /** Stalker / MAG Ministra portal (`portal.php` / `server/load.php`) authenticated by MAC. */
    Stalker(2),
    ;

    companion object {
        fun fromCode(code: Int): SourceKind = entries.firstOrNull { it.code == code } ?: M3U
    }
}
