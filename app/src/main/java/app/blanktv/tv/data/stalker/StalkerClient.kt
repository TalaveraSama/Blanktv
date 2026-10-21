package app.blanktv.tv.data.stalker

import app.blanktv.tv.data.parser.ParsedEntry
import app.blanktv.tv.domain.MediaKind
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

/**
 * Client for Stalker / MAG portals (Infomir Ministra middleware).
 *
 * This is the least standardised of the three protocols, so the client is defensive by design:
 *
 *  - **Portal path** — deployments live at `…/portal.php`, `…/server/load.php` or
 *    `…/stalker_portal/server/load.php`. Candidates are tried in order until a handshake answers.
 *  - **Session** — a `handshake` with only the MAC cookie returns a token; every later call carries
 *    the MAC *and* the token (cookie plus `Authorization: Bearer`, since firmware generations
 *    disagree on which one they read). `get_profile` is called once because some portals only
 *    activate the token after it.
 *  - **`cmd` fields** — carry a transport prefix (`ffmpeg `, `ffrt ` …) that [StalkerCmd] strips.
 *  - **Optional modules** — VOD/series endpoints 404 on portals licensed for live-only, so those
 *    branches degrade to an empty list instead of failing the whole sync.
 */
class StalkerClient(private val http: OkHttpClient) {

    fun fetchAll(portalUrl: String, macAddress: String): List<ParsedEntry> {
        val mac = macAddress.trim()
        var lastError: IOException? = null

        for (candidate in portalCandidates(portalUrl)) {
            try {
                val token = handshake(candidate, mac)
                runCatching { get(candidate, mac, token, "type=stb&action=get_profile") }

                val genres = genres(candidate, mac, token)
                val out = ArrayList<ParsedEntry>()
                out += channels(candidate, mac, token, genres)
                out += items(candidate, mac, token, genres, TYPE_VOD, MediaKind.Movie)
                out += items(candidate, mac, token, genres, TYPE_SERIES, MediaKind.Series)
                return out
            } catch (e: IOException) {
                lastError = e
            }
        }
        throw lastError ?: IOException("Stalker portal did not answer the handshake")
    }

    private fun portalCandidates(raw: String): List<String> {
        val trimmed = raw.trim().trimEnd('/')
        return if (trimmed.endsWith(".php")) {
            listOf(trimmed)
        } else {
            listOf(
                "$trimmed/portal.php",
                "$trimmed/server/load.php",
                "$trimmed/stalker_portal/server/load.php",
            )
        }
    }

    private fun handshake(base: String, mac: String): String {
        val body = get(base, mac, token = null, query = "type=stb&action=handshake")
        val js = JSONObject(body).optJSONObject("js")
            ?: throw IOException("Stalker handshake returned no payload")
        val token = js.optString("token")
        if (token.isEmpty()) throw IOException("Stalker handshake returned no token")
        return token
    }

    private fun genres(base: String, mac: String, token: String): Map<String, String> {
        val map = HashMap<String, String>()
        runCatching {
            val js = JSONObject(get(base, mac, token, "type=itv&action=get_genres")).opt("js")
            when (js) {
                is JSONArray -> for (i in 0 until js.length()) {
                    val o = js.optJSONObject(i) ?: continue
                    map[o.optString("id")] = o.optString("title")
                }
                is JSONObject -> for (key in js.keys()) map[key] = js.optString(key)
            }
        }
        return map
    }

    private fun channels(
        base: String,
        mac: String,
        token: String,
        genres: Map<String, String>,
    ): List<ParsedEntry> {
        val data = JSONObject(get(base, mac, token, "type=itv&action=get_all_channels"))
            .optJSONObject("js")
            ?.optJSONArray("data")
            ?: return emptyList()

        return (0 until data.length()).mapNotNull { i ->
            val o = data.optJSONObject(i) ?: return@mapNotNull null
            val url = StalkerCmd.normalize(o.optString("cmd"))
            if (url.isEmpty()) return@mapNotNull null
            ParsedEntry(
                title = o.optString("name").ifEmpty { url },
                url = url,
                logoUrl = o.optString("logo").ifEmpty { null },
                tvgId = null,
                groupName = genres[o.optString("tv_genre_id")],
                kind = MediaKind.Live,
                channelNumber = o.optString("number").toIntOrNull(),
            )
        }
    }

    private fun items(
        base: String,
        mac: String,
        token: String,
        genres: Map<String, String>,
        type: String,
        kind: MediaKind,
    ): List<ParsedEntry> = runCatching {
        val data = JSONObject(get(base, mac, token, "type=$type&action=get_all_items&genre_id=*"))
            .optJSONObject("js")
            ?.optJSONArray("data")
            ?: return@runCatching emptyList()

        (0 until data.length()).mapNotNull { i ->
            val o = data.optJSONObject(i) ?: return@mapNotNull null
            val url = StalkerCmd.normalize(o.optString("cmd"))
            if (url.isEmpty()) return@mapNotNull null
            ParsedEntry(
                title = o.optString("name").ifEmpty { url },
                url = url,
                logoUrl = o.optString("logo").ifEmpty { null },
                tvgId = null,
                groupName = genres[o.optString("category_id")],
                kind = kind,
                channelNumber = o.optString("number").toIntOrNull(),
            )
        }
    }.getOrDefault(emptyList())

    private fun get(base: String, mac: String, token: String?, query: String): String {
        val cookie = buildString {
            append("mac=").append(mac)
            append("; stb_lang=en; timezone=Etc%2FUTC")
            if (token != null) append("; token=").append(token)
        }
        val builder = Request.Builder()
            .url("$base?$query&JsHttpRequest=1-xml")
            .header("User-Agent", MAG_USER_AGENT)
            .header("Cookie", cookie)
            .header("Accept", "*/*")
        if (token != null) builder.header("Authorization", "Bearer $token")

        http.newCall(builder.build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Stalker HTTP ${response.code}")
            return response.body?.string().orEmpty()
        }
    }

    private companion object {
        const val TYPE_VOD = "vod"
        const val TYPE_SERIES = "series"

        /** Mimics a real MAG box so portals that user-agent-gate still serve us. */
        const val MAG_USER_AGENT =
            "Mozilla/5.0 (QtEmbedded; U; Linux; C) AppleWebKit/533.3 (KHTML, like Gecko) " +
                "MAG200/5.5.0 Safari/533.3"
    }
}
