package app.blanktv.tv.data.xtream

import org.junit.Assert.assertEquals
import org.junit.Test

class XtreamUrlsTest {

    @Test
    fun liveUrlFollowsThePanelLayout() {
        assertEquals(
            "http://panel.example:8080/live/alice/s3cret/42.m3u8",
            XtreamUrls.live("http://panel.example:8080/", "alice", "s3cret", "42"),
        )
    }

    @Test
    fun movieAndSeriesUseTheirContainerExtension() {
        assertEquals(
            "http://panel.example:8080/movie/alice/s3cret/7.mkv",
            XtreamUrls.movie("http://panel.example:8080", "alice", "s3cret", "7", "mkv"),
        )
        assertEquals(
            "http://panel.example:8080/series/alice/s3cret/99.mp4",
            XtreamUrls.seriesEpisode("http://panel.example:8080", "alice", "s3cret", "99", "mp4"),
        )
    }

    @Test
    fun credentialsArePercentEncodedInPathsAndQuery() {
        assertEquals(
            "http://h/live/a%40b/p%20q/1.m3u8",
            XtreamUrls.live("http://h", "a@b", "p q", "1"),
        )
        assertEquals(
            "http://h/player_api.php?username=a%40b&password=p%20q&action=get_live_streams",
            XtreamUrls.api("http://h", "a@b", "p q", "get_live_streams"),
        )
    }

    @Test
    fun emptyActionYieldsTheAuthProbeUrl() {
        assertEquals(
            "http://h/player_api.php?username=u&password=p",
            XtreamUrls.api("http://h", "u", "p", ""),
        )
    }
}
