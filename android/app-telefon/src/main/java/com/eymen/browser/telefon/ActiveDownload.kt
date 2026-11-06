package com.eymen.browser.telefon

/**
 * Only the real number of bytes written from GeckoView's response stream.
 * Content-Length is a hint, never a source of fabricated progress.
 */
internal data class ActiveDownload(
    val name: String,
    val bytesWritten: Long,
    val totalBytes: Long?,
    val cancelling: Boolean = false
)

internal object DownloadProgressMath {
    /** Unknown/invalid Content-Length has no percentage. */
    fun percent(written: Long, expected: Long?): Int? {
        if (expected == null || expected <= 0L || written < 0L) return null
        return ((written.coerceAtMost(expected).toDouble() / expected.toDouble()) * 100)
            .toInt().coerceIn(0, 100)
    }
}
