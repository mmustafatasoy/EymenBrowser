package com.eymen.browser.telefon

import android.app.AlertDialog
import androidx.activity.ComponentActivity
import androidx.compose.runtime.*
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.net.URI
import java.util.concurrent.atomic.AtomicBoolean

/** Gecko performs XPI signature verification; native dialogs disclose requested permissions. */
internal class BrowserAddons(private val activity: ComponentActivity) {
    private val controller get() = GeckoMotorDenemesi.getRuntime(activity).webExtensionController
    val items = mutableStateListOf<WebExtension>()
    var busy by mutableStateOf(false); private set
    var message by mutableStateOf(""); private set
    private var dialog: AlertDialog? = null

    init {
        controller.setPromptDelegate(object : WebExtensionController.PromptDelegate {
            override fun onInstallPromptRequest(extension: WebExtension, permissions: Array<String>,
                origins: Array<String>, dataCollectionPermissions: Array<String>): GeckoResult<WebExtension.PermissionPromptResponse> {
                val result = GeckoResult<WebExtension.PermissionPromptResponse>()
                prompt(extension, permissions, origins, dataCollectionPermissions) { ok ->
                    // Never silently enable private browsing or extension telemetry.
                    result.complete(WebExtension.PermissionPromptResponse(ok, false, false))
                }
                return result
            }
            override fun onUpdatePrompt(extension: WebExtension, newPermissions: Array<String>,
                newOrigins: Array<String>, newDataCollectionPermissions: Array<String>): GeckoResult<AllowOrDeny> {
                val result = GeckoResult<AllowOrDeny>()
                prompt(extension, newPermissions, newOrigins, newDataCollectionPermissions) {
                    result.complete(if (it) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
                }
                return result
            }
            override fun onOptionalPrompt(extension: WebExtension, permissions: Array<String>,
                origins: Array<String>, dataCollectionPermissions: Array<String>): GeckoResult<AllowOrDeny> {
                val result = GeckoResult<AllowOrDeny>()
                if (dataCollectionPermissions.isNotEmpty()) return GeckoResult.fromValue(AllowOrDeny.DENY)
                prompt(extension, permissions, origins, dataCollectionPermissions) {
                    result.complete(if (it) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
                }
                return result
            }
        })
    }

    fun refresh() {
        controller.list().accept({ installed ->
            items.clear(); items.addAll(installed.orEmpty().filter { !it.isBuiltIn })
        }, { _ -> message = activity.getString(R.string.settings_operation_failed) })
    }

    fun install(raw: String) {
        if (busy) return
        val uri = validatedInstallUrl(raw)
        if (uri == null) { message = activity.getString(R.string.addon_url_invalid); return }
        busy = true; message = activity.getString(R.string.settings_loading)
        controller.install(uri).accept({ _ ->
            busy = false; message = activity.getString(R.string.addon_installed); refresh()
        }, { _ -> busy = false; message = activity.getString(R.string.addon_failed); refresh() })
    }

    fun toggle(extension: WebExtension) {
        if (busy) return
        busy = true
        val operation = if ((extension.metaData?.enabled == true)) controller.disable(extension, WebExtensionController.EnableSource.USER)
            else controller.enable(extension, WebExtensionController.EnableSource.USER)
        operation.accept({ _ -> busy = false; refresh() }, { _ -> busy = false; message = activity.getString(R.string.addon_failed) })
    }

    fun remove(extension: WebExtension) {
        dialog = BrowserDialogs.builder(activity).setTitle(R.string.addon_remove)
            .setMessage((extension.metaData?.name ?: extension.id)).setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                controller.uninstall(extension).accept({ _ -> refresh() }, { _ -> message = activity.getString(R.string.addon_failed) })
            }.show()
    }

    private fun prompt(extension: WebExtension, permissions: Array<String>, origins: Array<String>,
        data: Array<String>, done: (Boolean) -> Unit) {
        val finished = AtomicBoolean(false)
        fun finish(ok: Boolean) { if (finished.compareAndSet(false, true)) done(ok) }
        if (activity.isFinishing || activity.isDestroyed) { finish(false); return }
        dialog?.dismiss()
        val detail = (permissions.toList() + origins.toList() + data.toList()).take(50).joinToString("\n")
        dialog = BrowserDialogs.builder(activity).setTitle((extension.metaData?.name ?: extension.id))
            .setMessage(activity.getString(R.string.addon_permissions) + "\n\n" + detail + "\n\n" + activity.getString(R.string.addon_private_note))
            .setNegativeButton(android.R.string.cancel) { _, _ -> finish(false) }
            .setPositiveButton(R.string.addon_install) { _, _ -> finish(true) }
            .setOnDismissListener { finish(false) }.show()
    }

    fun stop() { dialog?.dismiss(); dialog = null }

    companion object {
        fun validatedInstallUrl(input: String): String? = try {
            val uri = URI(input.trim())
            if (uri.scheme == "https" && uri.userInfo == null && uri.port in setOf(-1, 443) &&
                uri.host in setOf("addons.mozilla.org", "addons.mozilla.net") &&
                uri.path.endsWith(".xpi") && uri.fragment == null) uri.toASCIIString() else null
        } catch (_: Exception) { null }
    }
}
