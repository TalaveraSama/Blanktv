package app.blanktv.tv.data.xtream

import app.blanktv.tv.data.parser.ParsedEntry
import app.blanktv.tv.domain.MediaKind
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Client for the Xtream Codes panel API — the JSON interface behind xui.one and the vast majority
 * of reseller panels.
 *
 * The protocol is a single endpoint, `player_api.php`, switched by `action`:
 *   (none)                 user_info / server_info  → used as the authentication probe
 *   get_live_categories    get_vod_categories  get_series_categories
 *   get_live_streams       get_vod_streams     get_series
 *   get_series_info        per-series episode expansion
 *
 * Responses are plain JSON documents, so `org.json` is used rather than a streaming reader: even a
 * 50k-stream panel answers in single-digit megabytes, well inside a comfortable heap, and the far
 * bigger cost is the per-series `get_series_info` fan-out, which is capped below.
 */
class XtreamClient(private val http: OkHttpClient) {

    fun fetchAll(baseUrl: String, username: String, password: String): List<ParsedEntry> {
        authenticate(baseUrl, username, password)

        val liveCats = categories(baseUrl, username, password, ACTION_LIVE_CATEGORIES)
        val vodCats = categories(baseUrl, username, password, ACTION_VOD_CATEGORIES)
        val seriesCats = categories(baseUrl, username, password, ACTION_SERIES_CATEGORIES)

        val out = ArrayList<ParsedEntry>()
        out += live(baseUrl, username, password, liveCats)
        out += movies(baseUrl, username, password, vodCats)
        out += series(baseUrl, username, password, seriesCats)
        return out
    }

    /** Throws [IOException] when the panel rejects the credentials, so a bad login fails fast. */
    fun authenticate(baseUrl: String, username: String, password: String) {
        val info = getObject(baseUrl, username, password, "").optJSONObject("user_info")
        val authed = info?.optString("auth") == "1"
        if (!authed) throw IOException("Xtream panel rejected these credentials")
    }

    private fun categories(
        baseUrl: String,
        username: String,
        password: String,
        action: String,
    ): Map<String, String> {
        val array = getArray(baseUrl, username, password, action)
        val map = HashMap<String, String>(array.length())
        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val id = o.optString("category_id")
            if (id.isNotEmpty()) map[id] = o.optString("category_name")
        }
        return map
    }

    private fun live(
        baseUrl: String,
        username: String,
        password: String,
        categories: Map<String, String>,
    ): List<ParsedEntry> {
        val array = getArray(baseUrl, username, password, ACTION_LIVE_STREAMS)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val streamId = o.optString("stream_id")
            if (streamId.isEmpty()) return@mapNotNull null
            ParsedEntry(
                title = o.optString("name").ifEmpty { "Stream $streamId" },
                url = XtreamUrls.live(baseUrl, username, password, streamId),
                logoUrl = o.optString("stream_icon").ifEmpty { null },
                tvgId = o.optString("epg_channel_id").ifEmpty { null },
                groupName = categories[o.optString("category_id")],
                kind = MediaKind.Live,
                channelNumber = o.optString("num").toIntOrNull(),
            )
        }
    }

    private fun movies(
        baseUrl: String,
        username: String,
        password: String,
        categories: Map<String, String>,
    ): List<ParsedEntry> {
        val array = getArray(baseUrl, username, password, ACTION_VOD_STREAMS)
        return (0 until array.length()).mapNotNull { i ->
            val o = array.optJSONObject(i) ?: return@mapNotNull null
            val streamId = o.optString("stream_id")
            if (streamId.isEmpty()) return@mapNotNull null
            val ext = o.optString("container_extension").ifEmpty { "mp4" }
            ParsedEntry(
                title = o.optString("name").ifEmpty { "Title $streamId" },
                url = XtreamUrls.movie(baseUrl, username, password, streamId, ext),
                logoUrl = o.optString("stream_icon").ifEmpty { null },
                tvgId = null,
                groupName = categories[o.optString("category_id")],
                kind = MediaKind.Movie,
                channelNumber = o.optString("num").toIntOrNull(),
            )
        }
    }

    /**
     * Series need one extra call each (`get_series_info`) to reach the episodes, so a panel with
     * thousands of series would mean thousands of requests. The expansion is capped: beyond the
     * cap the series is still listed by title but its episodes are not fetched this sync.
     */
    private fun series(
        baseUrl: String,
        username: String,
        password: String,
        categories: Map<String, String>,
    ): List<ParsedEntry> {
        val array = getArray(baseUrl, username, password, ACTION_SERIES)
        val out = ArrayList<ParsedEntry>()
        var expanded = 0

        for (i in 0 until array.length()) {
            val o = array.optJSONObject(i) ?: continue
            val seriesId = o.optString("series_id")
            if (seriesId.isEmpty()) continue
            val seriesName = o.optString("name").ifEmpty { "Series $seriesId" }
            val group = categories[o.optString("category_id")] ?: seriesName
            val cover = o.optString("cover").ifEmpty { null }

            if (expanded >= MAX_SERIES_EXPANSION) continue
            expanded++

            val info = getObject(baseUrl, username, password, "$ACTION_SERIES_INFO&series_id=$seriesId")
            val seasons = info.optJSONObject("seasons") ?: continue
            for (seasonKey in seasons.keys()) {
                val episodes = seasons.optJSONArray(seasonKey) ?: continue
                for (j in 0 until episodes.length()) {
                    val ep = episodes.optJSONObject(j) ?: continue
                    val episodeId = ep.optString("id")
                    if (episodeId.isEmpty()) continue
                    val ext = ep.optString("container_extension").ifEmpty { "mp4" }
                    val epTitle = ep.optString("title").ifEmpty {
                        "S${seasonKey}E${ep.optString("episode_num")}"
                    }
                    out += ParsedEntry(
                        title = "$seriesName — $epTitle",
                        url = XtreamUrls.seriesEpisode(baseUrl, username, password, episodeId, ext),
                        logoUrl = cover,
                        tvgId = null,
                        groupName = group,
                        kind = MediaKind.Series,
                        channelNumber = null,
                    )
                }
            }
        }
        return out
    }

    private fun getArray(baseUrl: String, username: String, password: String, action: String): JSONArray {
        val body = get(baseUrl, username, password, action).trim()
        return if (body.startsWith("[")) JSONArray(body) else JSONArray()
    }

    private fun getObject(baseUrl: String, username: String, password: String, action: String): JSONObject {
        val body = get(baseUrl, username, password, action).trim()
        return if (body.startsWith("{")) JSONObject(body) else JSONObject()
    }

    private fun get(baseUrl: String, username: String, password: String, action: String): String {
        val request = Request.Builder()
            .url(XtreamUrls.api(baseUrl, username, password, action))
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Xtream HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private companion object {
        const val ACTION_LIVE_CATEGORIES = "get_live_categories"
        const val ACTION_VOD_CATEGORIES = "get_vod_categories"
        const val ACTION_SERIES_CATEGORIES = "get_series_categories"
        const val ACTION_LIVE_STREAMS = "get_live_streams"
        const val ACTION_VOD_STREAMS = "get_vod_streams"
        const val ACTION_SERIES = "get_series"
        const val ACTION_SERIES_INFO = "get_series_info"

        /** Bounds the per-series fan-out; see [series]. */
        const val MAX_SERIES_EXPANSION = 200
    }
}
