package app.blanktv.tv.data.stalker

import org.junit.Assert.assertEquals
import org.junit.Test

class StalkerCmdTest {

    @Test
    fun stripsKnownTransportPrefixes() {
        assertEquals(
            "http://cdn.example/ch/1.m3u8",
            StalkerCmd.normalize("ffmpeg http://cdn.example/ch/1.m3u8"),
        )
        assertEquals(
            "http://cdn.example/ch/2.m3u8",
            StalkerCmd.normalize("ffrt http://cdn.example/ch/2.m3u8"),
        )
    }

    @Test
    fun prefixMatchIsCaseInsensitiveAndOnlyAtTheStart() {
        assertEquals(
            "http://cdn.example/ffmpeg/x.m3u8",
            StalkerCmd.normalize("FFMPEG http://cdn.example/ffmpeg/x.m3u8"),
        )
    }

    @Test
    fun leavesBareUrlsUntouched() {
        assertEquals(
            "http://cdn.example/ch/3.m3u8",
            StalkerCmd.normalize("  http://cdn.example/ch/3.m3u8  "),
        )
    }
}
