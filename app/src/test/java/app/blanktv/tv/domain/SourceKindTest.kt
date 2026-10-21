package app.blanktv.tv.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class SourceKindTest {

    @Test
    fun codesRoundTrip() {
        SourceKind.entries.forEach { kind ->
            assertEquals(kind, SourceKind.fromCode(kind.code))
        }
    }

    @Test
    fun unknownCodeFallsBackToM3u() {
        assertEquals(SourceKind.M3U, SourceKind.fromCode(99))
    }

    @Test
    fun mediaKindCodesRoundTrip() {
        MediaKind.entries.forEach { kind ->
            assertEquals(kind, MediaKind.fromCode(kind.code))
        }
    }
}
