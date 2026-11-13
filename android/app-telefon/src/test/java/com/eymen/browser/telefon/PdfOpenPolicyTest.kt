package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test

class PdfOpenPolicyTest {
    @Test fun acceptsPdfFromMimeOrFilename() {
        assertTrue(PdfOpenPolicy.isPdf("download", "application/pdf"))
        assertTrue(PdfOpenPolicy.isPdf("document.PDF", "application/octet-stream"))
        assertTrue(PdfOpenPolicy.isPdf("a.pdf", "application/pdf; charset=binary"))
        assertFalse(PdfOpenPolicy.isPdf("image.jpg", "image/jpeg"))
        assertFalse(PdfOpenPolicy.isPdf("fake.pdf.exe", "application/octet-stream"))
    }
}
