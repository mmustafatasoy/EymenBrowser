package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class TabSearchPolicyTest {
    @Test fun emptyOrWhitespaceQueryKeepsAllScopedTabs() {
        for (input in listOf("", "  ", "\t\n")) {
            assertTrue(TabSearchPolicy.matches("", "", input))
            assertTrue(TabSearchPolicy.matches("Example", "https://example.com/", input))
        }
    }

    @Test fun titleSearchIgnoresCaseIncludingTurkishLetters() {
        assertTrue(TabSearchPolicy.matches("İzmir Haberleri", "https://example.com/", "izmir"))
        assertTrue(TabSearchPolicy.matches("Işık", "https://example.com/", "ışık"))
        assertTrue(TabSearchPolicy.matches("Açık Sekme", "https://example.com/", " AÇIK "))
        assertFalse(TabSearchPolicy.matches("İzmir", "https://example.com/", "Ankara"))
    }

    @Test fun addressSearchIncludesPathQueryAndFragment() {
        val address = "https://example.com/Docs?q=TabSearch#Result"
        assertTrue(TabSearchPolicy.matches("A page", address, "EXAMPLE.COM/docs"))
        assertTrue(TabSearchPolicy.matches("A page", address, "q=tabsearch#result"))
    }

    @Test fun eitherTitleOrAddressCanMatchButAnUnrelatedTabCannot() {
        assertTrue(TabSearchPolicy.matches("Saved research", "https://example.com/", "research"))
        assertTrue(TabSearchPolicy.matches("Other page", "https://research.example/", "research"))
        assertFalse(TabSearchPolicy.matches("Other page", "https://example.com/", "research"))
    }

    @Test fun canonicallyEquivalentUnicodeTitlesMatch() {
        assertTrue(TabSearchPolicy.matches("Caf\u0065\u0301", "", "Café"))
        assertTrue(TabSearchPolicy.matches("Café", "", "Cafe\u0301"))
    }

    @Test fun anEmptyHomeTabHasNoInventedSearchText() {
        assertFalse(TabSearchPolicy.matches("", "", "example"))
        assertTrue(TabSearchPolicy.matches("Yeni sekme", "", "sekme"))
    }

    @Test fun queryInputIsBoundedWithoutSplittingASurrogatePair() {
        assertEquals(256, TabSearchPolicy.boundedInput("x".repeat(300)).length)
        val emojiAtBoundary = "x".repeat(255) + "\uD83D\uDE00" + "trailing"
        assertEquals("x".repeat(255), TabSearchPolicy.boundedInput(emojiAtBoundary))
        assertEquals("  café  ", TabSearchPolicy.boundedInput("  café  "))
        assertEquals("café", TabSearchPolicy.query("  café  "))
    }
}
