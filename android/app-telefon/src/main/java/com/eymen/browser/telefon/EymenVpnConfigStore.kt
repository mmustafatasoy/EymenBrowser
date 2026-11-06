package com.eymen.browser.telefon

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import com.wireguard.config.Config
import com.wireguard.config.Interface
import java.io.ByteArrayInputStream
import java.net.Inet4Address
import java.net.Inet6Address
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Real WireGuard credentials are never shipped with the APK.
 * A user imports their OWN validated, dual-stack full-tunnel .conf.
 *
 * This intentionally does NOT share keys in SharedPreferences, logs, GitHub,
 * backups or analytics. The normalized config is AES-256/GCM-encrypted with
 * a non-exportable Android Keystore key in noBackupFilesDir.
 */
internal class EymenVpnConfigStore(private val context: Context) {
    private val file = AtomicFile(context.noBackupFilesDir.resolve("eymen-wg-v1.bin"))
    private val alias = "eymen_browser_wireguard_config_v1"

    companion object {
        private const val MAX_CONF_BYTES = 24 * 1024
        private const val MAX_ENCRYPTED_BYTES = 32 * 1024
        private const val IV_LENGTH = 12
    }

    fun exists(): Boolean = file.baseFile.isFile

    private fun encryptionKey(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val saved = ks.getKey(alias, null)
        if (saved is SecretKey) return saved
        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore"
        )
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    /**
     * All supplied IncludedApplications/ExcludedApplications are discarded;
     * GoBackend will be forced to include ONLY this browser's own package.
     * Block split-tunnel configs to avoid IPv4/IPv6/DNS escaping the VPN.
     */
    private fun safeConfig(source: Config): Config {
        require(source.peers.size == 1) {
            "Yalnızca tek sunuculu WireGuard yapılandırması destekleniyor."
        }
        val peer = source.peers.single()
        require(peer.endpoint.isPresent) { "WireGuard sunucu adresi eksik." }
        val routes = peer.allowedIps
        require(routes.any { it.address is Inet4Address && it.mask == 0 } &&
                routes.any { it.address is Inet6Address && it.mask == 0 }) {
            "IPv4 ve IPv6 için tam tünel rotaları (0.0.0.0/0 ve ::/0) gerekli."
        }
        val original = source.`interface`
        require(original.addresses.any { it.address is Inet4Address } &&
                original.addresses.any { it.address is Inet6Address }) {
            "Tünel yapılandırmasında IPv4 ve IPv6 adresleri gerekli."
        }
        require(original.dnsServers.isNotEmpty()) { "Tünel DNS sunucusu eksik." }

        val iface = Interface.Builder()
            .addAddresses(original.addresses)
            .addDnsServers(original.dnsServers)
            .addDnsSearchDomains(original.dnsSearchDomains)
            .setKeyPair(original.keyPair)
            .includeApplication(context.packageName)
        original.listenPort.ifPresent { iface.setListenPort(it) }
        original.mtu.ifPresent { iface.setMtu(it) }
        return Config.Builder()
            .setInterface(iface.build())
            .addPeers(source.peers)
            .build()
    }

    private fun parse(bytes: ByteArray): Config =
        safeConfig(Config.parse(ByteArrayInputStream(bytes)))

    fun importBytes(raw: ByteArray) {
        require(raw.size in 1..MAX_CONF_BYTES) {
            "WireGuard dosyası boş veya 24 KB sınırını aşıyor."
        }
        val parsed = parse(raw)
        val plaintext = parsed.toWgQuickString().toByteArray(Charsets.UTF_8)
        try {
            require(plaintext.size in 1..MAX_CONF_BYTES) {
                "VPN yapılandırması izin verilen boyutu aşıyor."
            }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey())
            val encrypted = cipher.doFinal(plaintext)
            val iv = cipher.iv
            check(iv.size == IV_LENGTH)
            val blob = byteArrayOf(1) + iv + encrypted
            require(blob.size <= MAX_ENCRYPTED_BYTES)

            val output = file.startWrite()
            try {
                output.write(blob)
                file.finishWrite(output)
            } catch (error: Exception) {
                file.failWrite(output)
                throw error
            } finally {
                encrypted.fill(0)
                blob.fill(0)
            }
        } finally {
            plaintext.fill(0)
        }
    }

    fun load(): Config {
        val raw = file.readFully()
        require(raw.size in (IV_LENGTH + 18)..MAX_ENCRYPTED_BYTES && raw[0] == 1.toByte()) {
            "Şifreli WireGuard yapılandırması bozulmuş."
        }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.DECRYPT_MODE,
            encryptionKey(),
            GCMParameterSpec(128, raw.copyOfRange(1, 1 + IV_LENGTH))
        )
        val plaintext = cipher.doFinal(raw.copyOfRange(1 + IV_LENGTH, raw.size))
        try {
            return parse(plaintext)
        } finally {
            plaintext.fill(0)
            raw.fill(0)
        }
    }

    fun clear() {
        file.delete()
    }
}
