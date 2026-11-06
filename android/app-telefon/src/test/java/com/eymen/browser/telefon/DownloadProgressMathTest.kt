package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class DownloadProgressMathTest {
    @Test fun unknownSizeNeverClaimsPercentage() {
        assertNull(DownloadProgressMath.percent(0L, null))
        assertNull(DownloadProgressMath.percent(100L, null))
        assertNull(DownloadProgressMath.percent(100L, 0L))
        assertNull(DownloadProgressMath.percent(100L, -1L))
    }

    @Test fun knownSizeUsesActualWrittenBytes() {
        assertEquals(0, DownloadProgressMath.percent(0L, 100L))
        assertEquals(25, DownloadProgressMath.percent(25L, 100L))
        assertEquals(50, DownloadProgressMath.percent(50L, 100L))
        assertEquals(100, DownloadProgressMath.percent(100L, 100L))
    }

    @Test fun neverDisplaysMoreThanOneHundredPercent() {
        // Content-Length may not match decoded/streamed body length.
        assertEquals(100, DownloadProgressMath.percent(150L, 100L))
        assertNull(DownloadProgressMath.percent(-1L, 100L))
    }

    @Test fun avoidsOverflowForLargeFiles() {
        assertEquals(50, DownloadProgressMath.percent(
            Long.MAX_VALUE / 2, Long.MAX_VALUE
        ))
        assertEquals(100, DownloadProgressMath.percent(
            Long.MAX_VALUE, Long.MAX_VALUE
        ))
    }
}
