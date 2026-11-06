package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class ChildModePolicyTest {
    @Test fun approvedHostDoesNotAllowLookalikesOrCredentials() {
        val sites = setOf("example.com")
        assertTrue(ChildModePolicy.allows("https://example.com/child", sites))
        assertTrue(ChildModePolicy.allows("https://video.example.com/", sites))
        for (url in listOf("https://example.com.attacker.test/", "https://notexample.com/",
            "http://example.com/", "https://example.com@attacker.test/", "https://user@example.com/",
            "javascript:location='https://example.com'", "data:text/html,test", "https://example.com:8443/"))
            assertFalse(url, ChildModePolicy.allows(url, sites))
    }
    @Test fun inputsAreDomainsRatherThanArbitraryUrls() {
        assertEquals("example.com", ChildModePolicy.host("HTTPS://EXAMPLE.COM")?.lowercase())
        assertNull(ChildModePolicy.host("https://example.com/login?token=secret"))
        assertNull(ChildModePolicy.host("https://user@example.com"))
        assertNull(ChildModePolicy.host("http://example.com"))
    }
    @Test fun pinIsSaltedAndWrongPinIsRejected() {
        val salt = ByteArray(16) { it.toByte() }
        val pin = "284619".toCharArray()
        val hash = ChildModePolicy.derive(pin, salt)
        assertTrue(pin.all { it == '\u0000' })
        assertTrue(ChildModePolicy.matches("284619".toCharArray(), salt, hash))
        assertFalse(ChildModePolicy.matches("284618".toCharArray(), salt, hash))
        assertFalse(hash.contentEquals(ChildModePolicy.derive("284619".toCharArray(), ByteArray(16))))
    }
}
