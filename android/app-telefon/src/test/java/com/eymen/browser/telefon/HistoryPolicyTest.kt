package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class HistoryPolicyTest {
    @Test fun privateAndNonWebDocumentsNeverReachStorage() {
        assertNull(HistoryPolicy.safeUrl("https://example.com/private",true))
        listOf("about:blank","data:text/html,secret","javascript:alert(1)","file:///secret",
            "moz-extension://abc/options.html","https://name:password@example.com/","https://example.com/\nsecret")
            .forEach { assertNull(it,HistoryPolicy.safeUrl(it,false)) }
    }
    @Test fun realWebAddressesKeepTheirQueryButDropFragment() {
        assertEquals("https://example.com/find?q=test",HistoryPolicy.safeUrl("https://example.com/find?q=test#section",false))
        assertEquals("http://example.com/",HistoryPolicy.safeUrl("http://example.com/",false))
        assertNull(HistoryPolicy.safeUrl("https://example.com/"+"a".repeat(8192),false))
    }
    @Test fun titlesArePlainBoundedTextAndNeverFabricated() {
        assertEquals("A real title",HistoryPolicy.title(" A\n real\t title ","https://example.com/"))
        assertEquals("example.com",HistoryPolicy.title("   ","https://example.com/path"))
        assertEquals(512,HistoryPolicy.title("x".repeat(1000),"https://example.com/").length)
    }
    @Test fun historyUsesLocalCalendarDaysAndNinetyDayRetention() {
        val at=Instant.parse("2026-11-06T22:30:00Z").toEpochMilli()
        assertEquals("2026-11-07",HistoryPolicy.dayKey(at,ZoneId.of("Europe/Istanbul")))
        assertEquals("2026-11-06",HistoryPolicy.dayKey(at,ZoneId.of("UTC")))
        assertEquals(TimeUnit.DAYS.toMillis(90),at-HistoryPolicy.cutoff(at))
    }
}
