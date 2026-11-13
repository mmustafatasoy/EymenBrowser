package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class DesktopSitePolicyTest {
    @Test fun onlyExactHttpsWhatsAppWebUsesDesktop() {
        assertTrue(DesktopSitePolicy.preferDesktop("https://web.whatsapp.com/"))
        assertTrue(DesktopSitePolicy.preferDesktop("https://WEB.WHATSAPP.COM/?lang=tr"))
        assertFalse(DesktopSitePolicy.preferDesktop("https://whatsapp.com/"))
        assertFalse(DesktopSitePolicy.preferDesktop("https://web.whatsapp.com.evil.example/"))
        assertFalse(DesktopSitePolicy.preferDesktop("http://web.whatsapp.com/"))
        assertFalse(DesktopSitePolicy.preferDesktop("https://username@web.whatsapp.com/"))
        assertFalse(DesktopSitePolicy.preferDesktop(null))
    }
}
