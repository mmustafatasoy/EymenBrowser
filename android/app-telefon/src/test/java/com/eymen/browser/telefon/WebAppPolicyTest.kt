package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class WebAppPolicyTest {
    @Test fun launchAndScopeCannotEscapeTheApprovedOrigin() {
        assertNotNull(WebAppPolicy.candidate("https://example.com/app/","index.html","./","App"))
        for(start in listOf("https://other.example/","javascript:alert(1)","https://user:pass@example.com/","http://example.com/"))
            assertNull(start,WebAppPolicy.candidate("https://example.com/app/",start,"/","App"))
        assertNull(WebAppPolicy.candidate("https://example.com/app/","/elsewhere","/app/","App"))
    }
    @Test fun navigationHonorsDirectoryBoundariesAndCanonicalTraversal() {
        assertTrue(WebAppPolicy.withinScope("https://example.com/app/","https://example.com/app/detail?q=1#x"))
        for(url in listOf("https://example.com/apple/","https://example.com/app/../outside","https://example.com/app/%2e%2e/outside","https://example.com/app/%2foutside","https://example.com:444/app/"))
            assertFalse(url,WebAppPolicy.withinScope("https://example.com/app/",url))
    }
    @Test fun labelsAreBoundedNormalizedAndCannotContainBidiControls() {
        assertEquals("Café",WebAppPolicy.candidate("https://example.com/","/","/","Cafe\u0301\u202e").let{it!!.name})
        assertEquals("example.com",WebAppPolicy.candidate("https://example.com/","/","/","\n").let{it!!.name})
        assertFalse(WebAppPolicy.candidate("https://example.com/","/","/","x".repeat(39)+"\ud83d\ude00")!!.name.last().isHighSurrogate())
    }
}
