package com.eymen.browser.telefon
import org.junit.Assert.*
import org.junit.Test
class BookmarkPolicyTest {
    @Test fun onlySafeHttpsBookmarksAreAccepted() {
        assertEquals("https://example.com/a?q=1#x",BookmarkPolicy.url(" https://example.com/a?q=1#x "))
        for(url in listOf("javascript:alert(1)","file:///tmp/test","https://user:password@example.com/","http://example.com/","https://example.com/\nnext","https://")) assertNull(url,BookmarkPolicy.url(url))
    }
    @Test fun namesAreUnicodeNormalizedAndBounded() {
        assertEquals("Café",BookmarkPolicy.name(" Cafe\u0301\n "))
        assertEquals(120,BookmarkPolicy.name("x".repeat(300)).length)
        assertEquals("",BookmarkPolicy.name("\n\t"))
    }
    @Test fun storedTitleHasAStableFallbackIndependentOfHistory() {
        assertEquals("example.com",BookmarkPolicy.title(" ","https://example.com/private-path"))
        assertEquals("Kayıtlı başlık",BookmarkPolicy.title("Kayıtlı başlık","https://example.com/"))
    }
}
