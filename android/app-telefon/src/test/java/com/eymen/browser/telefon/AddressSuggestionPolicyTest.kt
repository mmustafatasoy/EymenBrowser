package com.eymen.browser.telefon
import org.junit.Assert.*
import org.junit.Test
class AddressSuggestionPolicyTest {
    private val saved=AddressSuggestion("https://example.com/izmir","İzmir notları")
    @Test fun privateEditingCannotExposeTheNormalLibrary() {
        assertTrue(AddressSuggestionPolicy.find("izmir",listOf(saved),true).isEmpty())
    }
    @Test fun localTitlesAndAddressesMatchWithoutChangingSavedOrder() {
        assertEquals(listOf(saved),AddressSuggestionPolicy.find("İZMİR",listOf(saved,saved),false))
        assertEquals(listOf(saved),AddressSuggestionPolicy.find("EXAMPLE.COM/IZMIR",listOf(saved),false))
    }
    @Test fun suggestionsAreBoundedAndExcludeUnsafeSchemes() {
        val data=(1..10).map {AddressSuggestion("https://example.com/$it","Page $it")}
        assertEquals(5,AddressSuggestionPolicy.find("page",data,false).size)
        assertTrue(AddressSuggestionPolicy.find("page",listOf(AddressSuggestion("javascript:page","Page")),false).isEmpty())
        assertTrue(AddressSuggestionPolicy.find("p",data,false).isEmpty())
    }
}
