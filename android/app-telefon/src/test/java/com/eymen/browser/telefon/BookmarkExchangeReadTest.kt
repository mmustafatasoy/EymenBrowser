package com.eymen.browser.telefon

import org.junit.Assert.*
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.InputStream

class BookmarkExchangeReadTest {
    @Test fun readsUtf8AndOptionalBomWithoutChangingContent() {
        val text="Türkçe kitaplık / Café"
        assertEquals(text,BookmarkExchange.read(ByteArrayInputStream(("\uFEFF"+text).toByteArray(Charsets.UTF_8))))
    }
    @Test fun rejectsMalformedUtf8AndClosesTheInput() {
        var closed=false
        val input=object:ByteArrayInputStream(byteArrayOf(0xC3.toByte(),0x28)){override fun close(){closed=true;super.close()}}
        try{BookmarkExchange.read(input);fail("Malformed UTF-8 must be rejected")}catch(_:java.nio.charset.CharacterCodingException){}
        assertTrue(closed)
    }
    @Test fun oversizedStreamsAreBoundedAndClosedBeforeParsing() {
        var returned=0;var closed=false
        val input=object:InputStream(){
            override fun read():Int=0
            override fun read(buffer:ByteArray,offset:Int,length:Int):Int {returned+=length;return length}
            override fun close(){closed=true}
        }
        try{BookmarkExchange.read(input);fail("Oversized file must be rejected")}catch(_:IllegalArgumentException){}
        assertTrue(closed);assertTrue(returned<=BookmarkExchange.MAX_BYTES+8192)
    }
}
