package app.blanktv.tv.data.net

import android.content.Context
import android.net.Uri
import java.io.BufferedReader
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Opens playlist documents as a forward-only character stream.
 *
 * Both sources are handed to the caller as a [BufferedReader] inside a [use]-scoped block so the
 * underlying socket or file descriptor is always released, even when parsing throws halfway
 * through a 40 MB playlist.
 */
class PlaylistFetcher(private val client: OkHttpClient) {

    fun <T> withRemoteReader(url: String, block: (BufferedReader) -> T): T {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "*/*")
            .build()
        val response = client.newCall(request).execute()
        response.use { resp ->
            if (!resp.isSuccessful) throw IOException("HTTP ${resp.code} fetching playlist")
            val body = resp.body ?: throw IOException("Empty playlist response")
            return body.byteStream().bufferedReader(Charsets.UTF_8).use(block)
        }
    }

    fun <T> withLocalReader(context: Context, uri: Uri, block: (BufferedReader) -> T): T {
        val stream = context.contentResolver.openInputStream(uri)
            ?: throw IOException("Cannot open $uri")
        return stream.use { input -> input.bufferedReader(Charsets.UTF_8).use(block) }
    }
}
