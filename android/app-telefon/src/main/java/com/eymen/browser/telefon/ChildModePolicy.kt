package com.eymen.browser.telefon

import java.net.IDN
import java.net.URI
import java.util.Locale
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

internal object ChildModePolicy {
    fun host(input: String): String? = try {
        val text = input.trim()
        val uri = URI(if (text.contains("://")) text else "https://$text")
        if (!uri.scheme.equals("https", true) || uri.userInfo != null || uri.port !in setOf(-1, 443) ||
            uri.rawQuery != null || uri.fragment != null || uri.path !in setOf("", "/")) null
        else uri.host?.let { IDN.toASCII(it.lowercase(Locale.ROOT)).trimEnd('.') }
            ?.takeIf { it.length in 1..253 && '.' in it }
    } catch (_: Exception) { null }

    fun allows(url: String, allowed: Set<String>): Boolean {
        if (url == "about:blank") return true
        return try {
            val uri = URI(url)
            if (!uri.scheme.equals("https", true) || uri.userInfo != null || uri.port !in setOf(-1, 443)) return false
            val value = uri.host?.let { IDN.toASCII(it.lowercase(Locale.ROOT)).trimEnd('.') } ?: return false
            allowed.any { value == it || value.endsWith(".$it") }
        } catch (_: Exception) { false }
    }

    fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, 120_000, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded }
        finally { spec.clearPassword(); pin.fill('\u0000') }
    }
    fun matches(pin: CharArray, salt: ByteArray, expected: ByteArray) =
        MessageDigest.isEqual(derive(pin, salt), expected)
}
