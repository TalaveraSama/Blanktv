package app.blanktv.tv.data.parser

import app.blanktv.tv.domain.MediaKind
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Test

class M3uParserTest {

    @Test
    fun parsesExtInfAttributesTitleAndUrl() {
        val m3u = """
            #EXTM3U
            #EXTINF:-1 tvg-id="cctv1" tvg-logo="http://x/logo.png" group-title="News",CCTV 1 HD
            http://x/live/1.m3u8
        """.trimIndent()

        val entries = M3uParser.parse(StringReader(m3u).buffered())

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("CCTV 1 HD", entry.title)
        assertEquals("http://x/live/1.m3u8", entry.url)
        assertEquals("cctv1", entry.tvgId)
        assertEquals("http://x/logo.png", entry.logoUrl)
        assertEquals("News", entry.groupName)
        assertEquals(MediaKind.Live, entry.kind)
    }

    @Test
    fun classifiesXtreamMovieAndSeriesUrls() {
        val m3u = """
            #EXTM3U
            #EXTINF:120,Some Movie
            http://x/movie/u/p/42.mkv
            #EXTINF:120,Some Episode
            http://x/series/u/p/77.mkv
        """.trimIndent()

        val entries = M3uParser.parse(StringReader(m3u).buffered())

        assertEquals(2, entries.size)
        assertEquals(MediaKind.Movie, entries[0].kind)
        assertEquals(MediaKind.Series, entries[1].kind)
    }

    @Test
    fun extGrpFallsBackWhenGroupTitleIsMissing() {
        val m3u = """
            #EXTM3U
            #EXTGRP:Sports
            #EXTINF:-1,Sports One
            http://x/live/9.ts
        """.trimIndent()

        val entries = M3uParser.parse(StringReader(m3u).buffered())

        assertEquals("Sports", entries[0].groupName)
    }

    @Test
    fun skipsBlankAndCommentLinesAndKeepsChannelNumbers() {
        val m3u = """
            #EXTM3U

            # a plain comment
            #EXTINF:-1 tvg-chno="7",Seven
            http://x/live/7.ts
        """.trimIndent()

        val entries = M3uParser.parse(StringReader(m3u).buffered())

        assertEquals(1, entries.size)
        assertEquals(7, entries[0].channelNumber)
    }
}
