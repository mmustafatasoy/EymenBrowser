package com.eymen.browser.telefon

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CancellationException
import org.junit.Assert.*
import org.junit.Test

class DownloadStreamCopyTest {
    @Test fun copiesExactBytesAcrossBuffersAndCountsActualWrites() {
        val input = ByteArray(200_123) { (it % 251).toByte() }
        val output = ByteArrayOutputStream()
        var last = 0L
        val count = DownloadStreamCopy.copy(ByteArrayInputStream(input), output, { false }) {
            assertTrue(it > last)
            assertEquals(output.size().toLong(), it)
            last = it
        }
        assertArrayEquals(input, output.toByteArray())
        assertEquals(input.size.toLong(), count)
    }
    @Test fun cancellationDuringReadNeverWritesTheReturnedBuffer() {
        var cancelled = false
        val source = object : ByteArrayInputStream(ByteArray(100)) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                val count = super.read(b, off, len)
                cancelled = true
                return count
            }
        }
        val output = ByteArrayOutputStream()
        assertThrows(CancellationException::class.java) {
            DownloadStreamCopy.copy(source, output, { cancelled }) { fail("No bytes were written") }
        }
        assertEquals(0, output.size())
    }
    @Test fun diskFailureDoesNotAdvanceProgressOrReportSuccess() {
        val output = object : OutputStream() {
            override fun write(b: Int) { throw IOException("disk full") }
        }
        assertThrows(IOException::class.java) {
            DownloadStreamCopy.copy(ByteArrayInputStream(ByteArray(100)), output, { false }) {
                fail("A failed write must not advance progress")
            }
        }
    }
    @Test fun brokenResponsePropagatesInsteadOfBecomingACompletedFile() {
        val source = object : InputStream() {
            override fun read(): Int { throw IOException("connection lost") }
        }
        assertThrows(IOException::class.java) {
            DownloadStreamCopy.copy(source, ByteArrayOutputStream(), { false }) {}
        }
    }
    @Test fun cancellationAtFlushIsStillCancellation() {
        var cancelled = false
        val output = object : ByteArrayOutputStream() {
            override fun flush() { cancelled = true }
        }
        assertThrows(CancellationException::class.java) {
            DownloadStreamCopy.copy(ByteArrayInputStream(byteArrayOf(1)), output, { cancelled }) {}
        }
    }
}
