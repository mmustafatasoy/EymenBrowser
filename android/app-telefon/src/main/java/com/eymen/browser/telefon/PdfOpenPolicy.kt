package com.eymen.browser.telefon

/** Treat a validated PDF MIME or file suffix as a document, not as an external app handoff. */
internal object PdfOpenPolicy {
    fun isPdf(name:String, mime:String):Boolean =
        mime.substringBefore(';').trim().equals("application/pdf",true) ||
        name.trim().endsWith(".pdf",true)
}
