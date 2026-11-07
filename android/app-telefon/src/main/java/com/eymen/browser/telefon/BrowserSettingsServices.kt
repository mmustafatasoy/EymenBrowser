package com.eymen.browser.telefon

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoResult
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

internal enum class FunctionalSettingsPage { PRIVACY, PERMISSIONS, PASSWORDS, EXTENSIONS, LANGUAGE, CHILD, TOOLS, ACCESSIBILITY, DATA, SECURITY }
internal enum class PrivacyOption { COOKIE_REJECT, FINGERPRINT, HTTPS, AUTOPLAY, AUTOFILL }

/** Preferences are applied to the real Gecko runtime; site permissions stay in Gecko. */
internal class BrowserSettingsServices(
    private val activity: ComponentActivity,
    private val active: (GeckoSession) -> Boolean,
    private val reload: () -> Unit
) : GeckoSession.PermissionDelegate {
    private val prefs = activity.getSharedPreferences("eymen_browser_setup", 0)
    private val runtime get() = GeckoMotorDenemesi.getRuntime(activity)
    var cookieReject by mutableStateOf(prefs.getBoolean("cookie_reject", false)); private set
    var fingerprint by mutableStateOf(prefs.getBoolean("fingerprint", false)); private set
    var httpsOnly by mutableStateOf(prefs.getBoolean("https_only", true)); private set
    var autoplay by mutableStateOf(prefs.getBoolean("autoplay", false)); private set
    var autofill by mutableStateOf(prefs.getBoolean("autofill", true)); private set
    var cookieMode by mutableStateOf(prefs.getString("cookie_mode", "partition") ?: "partition"); private set
    var language by mutableStateOf(prefs.getString("app_language", "system") ?: "system"); private set
    var textPercent by mutableIntStateOf(prefs.getInt("text_percent", 100).coerceIn(50, 300)); private set
    val permissions = mutableStateListOf<GeckoSession.PermissionDelegate.ContentPermission>()
    var busy by mutableStateOf(false); private set
    var clearing by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    private var currentDialog: AlertDialog? = null
    private var pendingAndroid: ((Boolean) -> Unit)? = null
    private val privatePopups = mutableMapOf<String, Boolean>()
    private var popupRevision by mutableIntStateOf(0)
    private val permissionLauncher = activity.registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        val done = pendingAndroid; pendingAndroid = null
        done?.invoke(result.isNotEmpty() && result.values.all { it })
    }

    fun apply() {
        val behavior = when (cookieMode) {
            "all" -> ContentBlocking.CookieBehavior.ACCEPT_ALL
            "first" -> ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY
            "none" -> ContentBlocking.CookieBehavior.ACCEPT_NONE
            else -> ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY_AND_ISOLATE_OTHERS
        }
        runtime.settings.contentBlocking.setCookieBehavior(behavior)
            .setCookieBehaviorPrivateMode(behavior)
        runtime.settings.setFingerprintingProtection(fingerprint)
            .setFingerprintingProtectionPrivateBrowsing(fingerprint)
            .setAllowInsecureConnections(if (httpsOnly) GeckoRuntimeSettings.HTTPS_ONLY else GeckoRuntimeSettings.ALLOW_ALL)
        // Android's user-selected password service owns its own vault/biometric UI.
        // No credentials are copied into app preferences or a sync service.
        runtime.settings.setLoginAutofillEnabled(false)
        runtime.settings.setFontSizeFactor(textPercent / 100f)
    }

    fun setTextSize(percent: Int) {
        textPercent = percent.coerceIn(50, 300)
        prefs.edit().putInt("text_percent", textPercent).apply()
        runtime.settings.setFontSizeFactor(textPercent / 100f)
    }

    fun set(option: PrivacyOption, enabled: Boolean) {
        val key = when (option) {
            PrivacyOption.COOKIE_REJECT -> { cookieReject = enabled; "cookie_reject" }
            PrivacyOption.FINGERPRINT -> { fingerprint = enabled; "fingerprint" }
            PrivacyOption.HTTPS -> { httpsOnly = enabled; "https_only" }
            PrivacyOption.AUTOPLAY -> { autoplay = enabled; "autoplay" }
            PrivacyOption.AUTOFILL -> { autofill = enabled; "autofill" }
        }
        prefs.edit().putBoolean(key, enabled).apply()
        apply()
        if (option == PrivacyOption.AUTOPLAY) {
            runtime.storageController.allPermissions.accept({ items ->
                items?.filter { it.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE }
                    ?.forEach { runtime.storageController.setPermission(it, if (enabled) allow else deny) }
            }, { _ -> Unit })
        }
        reload()
    }

    fun setCookies(mode: String) {
        if (mode !in setOf("partition", "first", "all", "none")) return
        cookieMode = mode; prefs.edit().putString("cookie_mode", mode).apply(); apply(); reload()
    }

    fun popupAllowed(url: String, privateMode: Boolean): Boolean {
        popupRevision // Observe changes in the permission screen.
        val site = origin(url) ?: return false
        return if (privateMode) privatePopups[site] == true else prefs.getBoolean("popup_$site", false)
    }
    fun setPopups(url: String, privateMode: Boolean, enabled: Boolean) {
        val site = origin(url) ?: return
        if (privateMode) privatePopups[site] = enabled else prefs.edit().putBoolean("popup_$site", enabled).apply()
        popupRevision++
    }
    fun clearPrivatePermissions() { privatePopups.clear(); popupRevision++ }

    fun updateLanguage(code: String) {
        if (code !in setOf("tr", "en", "system")) return
        language = code; prefs.edit().putString("app_language", code).commit()
        runtime.settings.setLocales(if (code == "system") null else arrayOf(code))
        if (Build.VERSION.SDK_INT >= 33) {
            activity.getSystemService(android.app.LocaleManager::class.java).applicationLocales =
                android.os.LocaleList.forLanguageTags(if (code == "system") "" else code)
        } else activity.recreate()
    }

    fun openPasswordService() {
        try { activity.startActivity(Intent("android.settings.AUTOFILL_SETTINGS")) }
        catch (_: Exception) {
            try { activity.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) }
            catch (_: Exception) { message = activity.getString(R.string.settings_system_unavailable) }
        }
    }

    fun refreshPermissions() {
        busy = true
        runtime.storageController.allPermissions.accept({ items ->
            permissions.clear(); permissions.addAll(items.orEmpty().filter { !it.privateMode })
            busy = false
        }, { _ -> busy = false; message = activity.getString(R.string.settings_operation_failed) })
    }
    fun beginClear() { clearing = true; message = "" }
    fun finishClear(messageId: Int) { clearing = false; message = activity.getString(messageId) }

    fun changePermission(perm: GeckoSession.PermissionDelegate.ContentPermission) {
        val values = intArrayOf(allow, deny, GeckoSession.PermissionDelegate.ContentPermission.VALUE_PROMPT)
        currentDialog = AlertDialog.Builder(activity)
            .setTitle(origin(perm.uri))
            .setItems(arrayOf(activity.getString(R.string.permission_allow),
                activity.getString(R.string.permission_deny), activity.getString(R.string.permission_ask))) { _, index ->
                runtime.storageController.setPermission(perm, values[index]); refreshPermissions()
            }.show()
    }

    private fun eligible(session: GeckoSession, uri: String?) =
        active(session) && !activity.isFinishing && !activity.isDestroyed && origin(uri) != null

    override fun onAndroidPermissionsRequest(session: GeckoSession, requested: Array<String>?, callback: GeckoSession.PermissionDelegate.Callback) {
        val allowed = setOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        val wanted = requested?.filter { it in allowed }.orEmpty()
        if (!active(session) || wanted.isEmpty() || wanted.size != requested?.size || pendingAndroid != null) {
            callback.reject(); return
        }
        if (wanted.all { activity.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }) {
            callback.grant(); return
        }
        pendingAndroid = { ok -> if (ok && active(session)) callback.grant() else callback.reject() }
        try { permissionLauncher.launch(wanted.toTypedArray()) }
        catch (_: Exception) { pendingAndroid?.invoke(false); pendingAndroid = null }
    }

    override fun onContentPermissionRequest(session: GeckoSession,
        perm: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int> {
        if (!eligible(session, perm.uri)) return GeckoResult.fromValue(deny)
        if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE)
            return GeckoResult.fromValue(if (autoplay) allow else deny)
        if (perm.permission == GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE)
            return GeckoResult.fromValue(allow)
        val title = when (perm.permission) {
            GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> R.string.permission_location
            GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS -> R.string.permission_cookies
            GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> R.string.permission_storage
            else -> return GeckoResult.fromValue(deny)
        }
        val result = GeckoResult<Int>()
        confirm(origin(perm.uri).orEmpty(), activity.getString(title)) { ok ->
            result.complete(if (ok && active(session)) allow else deny)
        }
        perm.notifyShown()
        return result
    }

    override fun onMediaPermissionRequest(session: GeckoSession, uri: String,
        video: Array<GeckoSession.PermissionDelegate.MediaSource>?,
        audio: Array<GeckoSession.PermissionDelegate.MediaSource>?,
        callback: GeckoSession.PermissionDelegate.MediaCallback) {
        if (!eligible(session, uri) ||
            (!video.isNullOrEmpty() && activity.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) ||
            (!audio.isNullOrEmpty() && activity.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED)) {
            callback.reject(); return
        }
        confirm(origin(uri).orEmpty(), activity.getString(R.string.permission_camera_microphone)) { ok ->
            if (ok && active(session)) callback.grant(video?.firstOrNull(), audio?.firstOrNull()) else callback.reject()
        }
    }

    private fun confirm(title: String, body: String, done: (Boolean) -> Unit) {
        currentDialog?.dismiss()
        val finished = AtomicBoolean(false)
        fun finish(ok: Boolean) { if (finished.compareAndSet(false, true)) done(ok) }
        if (activity.isFinishing || activity.isDestroyed) { finish(false); return }
        currentDialog = AlertDialog.Builder(activity).setTitle(title).setMessage(body)
            .setNegativeButton(R.string.permission_deny) { _, _ -> finish(false) }
            .setPositiveButton(R.string.permission_allow) { _, _ -> finish(true) }
            .setOnDismissListener { finish(false) }.show()
    }

    fun stop() { currentDialog?.dismiss(); currentDialog = null }

    companion object {
        private val allow = GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
        private val deny = GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
        fun origin(uri: String?): String? = try {
            val parsed = URI(uri ?: "")
            if (parsed.scheme == "https" && !parsed.host.isNullOrBlank() && parsed.userInfo == null)
                "https://" + parsed.host.lowercase(java.util.Locale.ROOT) +
                    if (parsed.port != -1 && parsed.port != 443) ":${parsed.port}" else ""
            else null
        } catch (_: Exception) { null }
    }
}
