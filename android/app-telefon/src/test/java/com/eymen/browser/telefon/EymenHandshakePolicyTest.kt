package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class EymenHandshakePolicyTest {
    private val now = 1_800_000_000_000L

    @Test fun noHandshakeIsNeverVerified() {
        assertFalse(EymenHandshakePolicy.isRecent(0L, now))
        assertFalse(EymenHandshakePolicy.isRecent(-1L, now))
        assertNull(EymenHandshakePolicy.ageSeconds(0L, now))
    }

    @Test fun futureHandshakeIsNeverVerified() {
        assertFalse(EymenHandshakePolicy.isRecent(now + 1L, now))
        assertFalse(EymenHandshakePolicy.isRecent(now + 120_000L, now))
        assertNull(EymenHandshakePolicy.ageSeconds(now + 1L, now))
    }

    @Test fun veryRecentHandshakeCanBeVerified() {
        assertTrue(EymenHandshakePolicy.isRecent(now, now))
        assertTrue(EymenHandshakePolicy.isRecent(now - 10_000L, now))
        assertEquals(10L,
            EymenHandshakePolicy.ageSeconds(now - 10_999L, now))
    }

    @Test fun expirationIsStrictAtThreeMinutes() {
        assertTrue(EymenHandshakePolicy.isRecent(now - 179_999L, now))
        assertFalse(EymenHandshakePolicy.isRecent(now - 180_000L, now))
        assertFalse(EymenHandshakePolicy.isRecent(now - 3_600_000L, now))
    }

    @Test fun boundaryDoesNotOverflow() {
        assertFalse(EymenHandshakePolicy.isRecent(Long.MAX_VALUE, now))
        assertNull(EymenHandshakePolicy.ageSeconds(Long.MAX_VALUE, now))
    }
}
