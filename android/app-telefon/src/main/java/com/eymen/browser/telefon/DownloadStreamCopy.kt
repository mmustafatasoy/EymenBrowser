package com.eymen.browser.telefon

import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.CancellationException

/** Counts only successful writes; cancellation and read/write errors propagate. */
internal object DownloadStreamCopy {
    fun copy(
        source: InputStream, output: OutputStream,
        cancelled: () -> Boolean, progress: (Long) -> Unit
    ): Long {
        val buffer = ByteArray(64 * 1024)
        var written = 0L
        while (true) {
            if (cancelled()) throw CancellationException("User cancelled")
            val count = source.read(buffer)
            if (cancelled()) throw CancellationException("User cancelled")
            if (count < 0) break
            if (count == 0) continue
            output.write(buffer, 0, count)
            written += count
            progress(written)
        }
        output.flush()
        if (cancelled()) throw CancellationException("User cancelled")
        return written
    }
}
