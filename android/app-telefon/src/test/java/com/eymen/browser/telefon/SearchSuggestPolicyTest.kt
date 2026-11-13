package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class SearchSuggestPolicyTest {
    @Test fun normalGoogleTermsAreEligibleFromFirstCharacter() {
        assertEquals("a",SearchSuggestPolicy.eligible("a","Google",false,false,true))
        assertEquals("bebek",SearchSuggestPolicy.eligible(" bebek ","Google",false,false,true))
        assertNull(SearchSuggestPolicy.eligible("https://example.com","Google",false,false,true))
        assertNull(SearchSuggestPolicy.eligible("google.com","Google",false,false,true))
    }
    @Test fun privateChildOrDisabledNeverSendsTypedTerms() {
        assertNull(SearchSuggestPolicy.eligible("beb","Google",true,false,true))
        assertNull(SearchSuggestPolicy.eligible("beb","Google",false,true,true))
        assertNull(SearchSuggestPolicy.eligible("beb","Google",false,false,false))
        assertNull(SearchSuggestPolicy.eligible("beb","DuckDuckGo",false,false,true))
        assertNull(SearchSuggestPolicy.eligible("a\nsecret","Google",false,false,true))
    }
}
