package app.blanktv.tv.data.xtream

import java.net.URLEncoder

/**
 * Pure URL construction for the Xtream Codes player API.
 *
 * Kept separate from the network client so it is trivially unit-testable: the path layout
 * (`/live/user/pass/id.m3u8`, `/movie/…`, `/series/…`) is the contract every panel implements, and
 * getting the credential encoding wrong here is the classic "works in browser, 401 in app" bug.
 */
object XtreamUrls {

    fun api(baseUrl: String, username: String, password: String, action: String): String =
        "${base(baseUrl)}/player_api.php" +
            "?username=${encode(username)}&password=${encode(password)}" +
            if (action.isEmpty()) "" else "&action=$action"

    fun live(baseUrl: String, username: String, password: String, streamId: String): String =
        "${base(baseUrl)}/live/${encode(username)}/${encode(password)}/$streamId.m3u8"

    fun movie(
        baseUrl: String,
        username: String,
        password: String,
        streamId: String,
        containerExtension: String,
    ): String = "${base(baseUrl)}/movie/${encode(username)}/${encode(password)}/$streamId.$containerExtension"

    fun seriesEpisode(
        baseUrl: String,
        username: String,
        password: String,
        episodeId: String,
        containerExtension: String,
    ): String = "${base(baseUrl)}/series/${encode(username)}/${encode(password)}/$episodeId.$containerExtension"

    private fun base(baseUrl: String): String = baseUrl.trim().trimEnd('/')

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
