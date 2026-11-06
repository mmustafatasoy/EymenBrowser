package com.eymen.browser.telefon

/**
 * WireGuard peer-handshake freshness check.
 *
 * A successful local TUN establishment is not proof of a reachable peer.
 * Avoid treating future clock-skewed or stale handshake times as live.
 */
internal object EymenHandshakePolicy {
    const val MAX_HANDSHAKE_AGE_MS = 180_000L

    fun ageSeconds(handshakeEpochMillis: Long, nowEpochMillis: Long): Long? {
        if (handshakeEpochMillis <= 0L || nowEpochMillis < handshakeEpochMillis) return null
        return (nowEpochMillis - handshakeEpochMillis) / 1_000L
    }

    fun isRecent(handshakeEpochMillis: Long, nowEpochMillis: Long): Boolean {
        val age = ageSeconds(handshakeEpochMillis, nowEpochMillis) ?: return false
        return age < MAX_HANDSHAKE_AGE_MS / 1_000L
    }
}
