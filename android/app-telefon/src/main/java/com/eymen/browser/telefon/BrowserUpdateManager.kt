package com.eymen.browser.telefon

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/** Production-only, user-consented sideload updater. No silent APK installation. */
internal class BrowserUpdateManager(private val activity: Activity) {
    private data class Candidate(
        val version: String, val versionCode: Long, val downloadUrl: String,
        val bytes: Long, val sha256: String
    )

    private val executor = Executors.newSingleThreadExecutor()
    private val prefs by lazy {
        activity.getSharedPreferences("eymen_browser_updates", Activity.MODE_PRIVATE)
    }
    private val downloading = AtomicBoolean(false)
    private val checking = AtomicBoolean(false)
    private var pendingInstall: File? = null
    private var cancelled: AtomicBoolean? = null
    private var disposed = false

    fun checkOnForeground() {
        if (disposed || isDebuggableInstall() ||
            !activity.getSharedPreferences("eymen_browser_setup", Activity.MODE_PRIVATE)
                .getBoolean("setup_completed", false)) return
        if (System.currentTimeMillis() - prefs.getLong("last_check", 0L) < CHECK_INTERVAL_MS) return
        check(manual = false)
    }

    fun check(manual: Boolean) {
        if (disposed || downloading.get()) return
        if (isDebuggableInstall()) {
            if (manual) showInfo(R.string.update_debug_build, R.string.settings_about_update)
            return
        }
        if (!checking.compareAndSet(false, true)) return
        if (manual) toast(R.string.update_checking)
        executor.execute {
            try {
                val candidate = fetchCandidate()
                prefs.edit().putLong("last_check", System.currentTimeMillis()).apply()
                ui {
                    when {
                        candidate != null -> showNewVersion(candidate)
                        manual -> showInfo(R.string.update_no_release, R.string.settings_about_update)
                    }
                }
            } catch (_: Exception) {
                // Network, rate limits and absent releases are non-fatal to browsing.
                if (manual) ui { showInfo(R.string.update_check_failed, R.string.settings_about_update) }
            } finally {
                checking.set(false)
            }
        }
    }

    private fun isDebuggableInstall(): Boolean =
        (activity.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

    private fun fetchCandidate(): Candidate? {
        val conn = openTrustedHttps("https://api.github.com/repos/mmustafatasoy/EymenBrowser/releases/latest")
        val payload = try {
            if (conn.responseCode == 404) return null
            if (conn.responseCode != 200) throw IOException("Release metadata unavailable")
            conn.inputStream.use { stream ->
                val limited = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = stream.read(buffer)
                    if (count < 0) break
                    if (limited.size() + count > MAX_METADATA_BYTES) {
                        throw IOException("Release metadata too large")
                    }
                    limited.write(buffer, 0, count)
                }
                JSONObject(String(limited.toByteArray(), Charsets.UTF_8))
            }
        } finally {
            conn.disconnect()
        }
        if (payload.optBoolean("draft", true) || payload.optBoolean("prerelease", true)) return null
        val tag = payload.optString("tag_name")
        if (!tag.matches(Regex("v[0-9]+\\.[0-9]+\\.[0-9]+"))) return null
        val releaseCode = payload.optLong("version_code", 0L)
        // Version code is assigned in CI as major*10000+minor*100+patch.
        val parts = tag.substring(1).split('.').map { it.toLongOrNull() ?: return null }
        if (parts[0] > 100000 || parts[1] > 99 || parts[2] > 99) return null
        val expectedCode = parts[0] * 10000 + parts[1] * 100 + parts[2]
        if (releaseCode > 0 && releaseCode != expectedCode) return null
        if (expectedCode <= installedVersionCode()) return null

        val assets = payload.optJSONArray("assets") ?: return null
        for (abi in Build.SUPPORTED_ABIS) {
            val requiredName = "EymenBrowser-Android-Phone-$abi.apk"
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                if (asset.optString("name") != requiredName) continue
                val length = asset.optLong("size")
                val digest = asset.optString("digest")
                if (length <= 0 || length > MAX_APK_BYTES ||
                    !digest.matches(Regex("sha256:[a-fA-F0-9]{64}"))
                ) return null
                val assetUrl = asset.optString("browser_download_url")
                if (!isTrustedUrl(assetUrl)) return null
                return Candidate(tag, expectedCode, assetUrl, length, digest.substringAfter(':'))
            }
        }
        return null
    }

    private fun installedVersionCode(): Long {
        @Suppress("DEPRECATION")
        val info = activity.packageManager.getPackageInfo(activity.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else {
            @Suppress("DEPRECATION")
            info.versionCode.toLong()
        }
    }

    private fun showNewVersion(candidate: Candidate) {
        if (disposed) return
        BrowserDialogs.builder(activity)
            .setTitle(activity.getString(R.string.update_available_title, candidate.version))
            .setMessage(activity.getString(R.string.update_available_message,
                candidate.version, candidate.bytes / (1024 * 1024)))
            .setPositiveButton(R.string.update_download_install) { _, _ -> startDownload(candidate) }
            .setNegativeButton(R.string.update_later, null)
            .show()
    }

    private fun startDownload(candidate: Candidate) {
        if (!downloading.compareAndSet(false, true) || disposed) return
        val stop = AtomicBoolean(false)
        cancelled = stop
        val progress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
        }
        val popup = BrowserDialogs.builder(activity)
            .setTitle(activity.getString(R.string.update_downloading, candidate.version))
            .setView(progress)
            .setNegativeButton(android.R.string.cancel) { _, _ -> stop.set(true) }
            .setCancelable(false)
            .create()
        popup.show()
        executor.execute {
            var tmp: File? = null
            try {
                val directory = File(activity.cacheDir, "updates").apply { mkdirs() }
                val output = File(directory, "eymen-update.apk")
                val partial = File(directory, "eymen-update.partial")
                tmp = partial
                output.delete()
                partial.delete()
                val digest = MessageDigest.getInstance("SHA-256")
                val connection = openTrustedHttps(candidate.downloadUrl)
                try {
                    if (connection.responseCode != 200) throw IOException("APK unavailable")
                    var read = 0L
                    FileOutputStream(partial).use { file ->
                        connection.inputStream.use { input ->
                            val buffer = ByteArray(64 * 1024)
                            while (true) {
                                if (stop.get() || disposed) throw IOException("Cancelled")
                                val count = input.read(buffer)
                                if (count == -1) break
                                read += count
                                if (read > candidate.bytes || read > MAX_APK_BYTES)
                                    throw IOException("Unexpected APK length")
                                digest.update(buffer, 0, count)
                                file.write(buffer, 0, count)
                                val percent = (100L * read / candidate.bytes).toInt()
                                ui { progress.progress = percent }
                            }
                            file.fd.sync()
                        }
                    }
                    if (read != candidate.bytes ||
                        !digest.digest().toHex().equals(candidate.sha256, ignoreCase = true)
                    ) throw IOException("APK checksum failed")
                } finally {
                    connection.disconnect()
                }
                verifyPackageAndSigner(partial, candidate.versionCode)
                if (!partial.renameTo(output)) throw IOException("Cannot finalize APK")
                tmp = null
                ui {
                    popup.dismiss()
                    downloading.set(false)
                    cancelled = null
                    startInstallerOrRequestPermission(output)
                }
            } catch (_: Exception) {
                tmp?.delete()
                ui {
                    popup.dismiss()
                    downloading.set(false)
                    cancelled = null
                    if (!stop.get() && !disposed) showInfo(
                        R.string.update_failed, R.string.settings_about_update
                    )
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    private fun signerCertificates(pkg: PackageInfo): List<ByteArray> {
        return if (Build.VERSION.SDK_INT >= 28) {
            pkg.signingInfo?.apkContentsSigners?.map { it.toByteArray() }.orEmpty()
        } else pkg.signatures?.map { it.toByteArray() }.orEmpty()
    }

    @Suppress("DEPRECATION")
    private fun verifyPackageAndSigner(file: File, expectedVersion: Long) {
        val flags = if (Build.VERSION.SDK_INT >= 28)
            PackageManager.GET_SIGNING_CERTIFICATES else PackageManager.GET_SIGNATURES
        @Suppress("DEPRECATION")
        val apk = activity.packageManager.getPackageArchiveInfo(file.absolutePath, flags)
            ?: throw IOException("APK invalid")
        @Suppress("DEPRECATION")
        val installed = activity.packageManager.getPackageInfo(activity.packageName, flags)
        if (apk.packageName != activity.packageName) throw IOException("Wrong package")
        val code = if (Build.VERSION.SDK_INT >= 28) apk.longVersionCode else {
            @Suppress("DEPRECATION")
            apk.versionCode.toLong()
        }
        if (code != expectedVersion || code <= installedVersionCode())
            throw IOException("Invalid upgrade version")
        val a = signerCertificates(apk).map { MessageDigest.getInstance("SHA-256").digest(it).toHex() }.sorted()
        val b = signerCertificates(installed).map { MessageDigest.getInstance("SHA-256").digest(it).toHex() }.sorted()
        if (a.isEmpty() || a != b) throw IOException("APK signing certificate differs")
    }

    private fun startInstallerOrRequestPermission(file: File) {
        if (disposed || !file.isFile) return
        if (Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            pendingInstall = file
            BrowserDialogs.builder(activity)
                .setTitle(R.string.update_install_permission_title)
                .setMessage(R.string.update_install_permission_message)
                .setPositiveButton(R.string.update_open_permissions) { _, _ ->
                    try {
                        activity.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES)
                            .setData(Uri.parse("package:" + activity.packageName)))
                    } catch (_: Exception) {
                        showInfo(R.string.update_install_permission_message,
                            R.string.update_install_permission_title)
                    }
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> pendingInstall = null }
                .show()
            return
        }
        pendingInstall = null
        try {
            val uri = FileProvider.getUriForFile(
                activity, activity.packageName + ".updateprovider", file
            )
            activity.startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        } catch (_: Exception) {
            showInfo(R.string.update_installer_unavailable, R.string.settings_about_update)
        }
    }

    fun onResumed() {
        val pending = pendingInstall
        if (pending != null) startInstallerOrRequestPermission(pending)
        else checkOnForeground()
    }

    private fun openTrustedHttps(address: String): HttpURLConnection {
        var current = URL(address)
        repeat(6) {
            if (!isTrustedUrl(current.toString())) throw IOException("Untrusted update host")
            val conn = current.openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = false
            conn.connectTimeout = 15000
            conn.readTimeout = 25000
            conn.setRequestProperty("User-Agent", "EymenBrowser-Android-Updater")
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            val status = conn.responseCode
            if (status in 300..399) {
                val location = conn.getHeaderField("Location")
                    ?: throw IOException("Missing redirect")
                val next = URL(current, location)
                conn.disconnect()
                current = next
            } else return conn
        }
        throw IOException("Too many redirects")
    }

    private fun isTrustedUrl(text: String): Boolean {
        return try {
            val u = URL(text)
            u.protocol == "https" && u.userInfo == null &&
                (u.host.equals("github.com", true) ||
                    u.host.equals("api.github.com", true) ||
                    u.host.equals("release-assets.githubusercontent.com", true) ||
                    u.host.equals("objects.githubusercontent.com", true))
        } catch (_: Exception) { false }
    }

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

    private fun ui(body: () -> Unit) {
        activity.runOnUiThread {
            if (!disposed && !activity.isFinishing && !activity.isDestroyed) body()
        }
    }

    private fun toast(id: Int) {
        Toast.makeText(activity, id, Toast.LENGTH_SHORT).show()
    }

    private fun showInfo(message: Int, title: Int) {
        BrowserDialogs.builder(activity).setTitle(title).setMessage(message)
            .setPositiveButton(android.R.string.ok, null).show()
    }

    fun dispose() {
        disposed = true
        cancelled?.set(true)
        executor.shutdownNow()
    }

    private companion object {
        const val MAX_APK_BYTES = 500L * 1024 * 1024
        const val MAX_METADATA_BYTES = 2 * 1024 * 1024
        const val CHECK_INTERVAL_MS = 6L * 60L * 60L * 1000L
    }
}
