package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenTokens as T
import org.mozilla.geckoview.GeckoSession

@Composable
internal fun SettingsPage(title: Int, dayMode: Boolean, onBack: () -> Unit,
    content: @Composable ColumnScope.(BrowserScreenPalette) -> Unit) {
    val p = browserScreenPalette(dayMode)
    BackHandler { onBack() }
    Column(Modifier.fillMaxSize().screenBackground(p).padding(T.telefon_ekran_kenar.dp),
        verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
        ScreenHeader(stringResource(title), stringResource(R.string.settings_personal_subtitle), p, onBack)
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) { content(p) }
    }
}

@Composable
internal fun SettingsToggle(label: String, note: String, selected: Boolean,
    p: BrowserScreenPalette, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
        .toggleable(selected, role = Role.Switch, onValueChange = onChange)
        .padding(T.telefon_ekran_kenar.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            ScreenText(label, p.text, strong = true)
            ScreenText(note, p.muted, T.telefon_yazi_ikincil, maxLines = 5)
        }
        Spacer(Modifier.width(T.telefon_ekran_kucukBosluk.dp))
        ScreenText(stringResource(if (selected) R.string.settings_enabled else R.string.settings_disabled),
            if (selected) p.accent else p.muted, strong = true)
    }
}

@Composable
internal fun PrivacySettingsScreen(services: BrowserSettingsServices, dayMode: Boolean,
    onBack: () -> Unit, onClear: (Boolean) -> Unit) = SettingsPage(R.string.settings_shield, dayMode, onBack) { p ->
    listOf(
        Triple(PrivacyOption.COOKIE_REJECT, R.string.shield_cookie_dialog, R.string.shield_cookie_dialog_desc),
        Triple(PrivacyOption.FINGERPRINT, R.string.shield_fingerprint, R.string.shield_fingerprint_desc),
        Triple(PrivacyOption.HTTPS, R.string.shield_https, R.string.shield_https_desc),
        Triple(PrivacyOption.AUTOPLAY, R.string.shield_autoplay, R.string.shield_autoplay_desc)
    ).forEach { (option, title, desc) ->
        val chosen = when (option) {
            PrivacyOption.COOKIE_REJECT -> services.cookieReject
            PrivacyOption.FINGERPRINT -> services.fingerprint
            PrivacyOption.HTTPS -> services.httpsOnly
            else -> services.autoplay
        }
        SettingsToggle(stringResource(title), stringResource(desc), chosen, p) { services.set(option, it) }
    }
    ScreenText(stringResource(R.string.cookie_policy_title), p.text, strong = true)
    listOf("partition" to R.string.cookie_policy_partition, "first" to R.string.cookie_policy_first,
        "all" to R.string.cookie_policy_all, "none" to R.string.cookie_policy_none).forEach { (mode, title) ->
        ScreenButton(stringResource(title), p,
            { services.setCookies(mode) }, primary = services.cookieMode == mode)
    }
    ScreenText(stringResource(R.string.cookie_policy_note), p.muted, T.telefon_yazi_ikincil, maxLines = 5)
    ScreenButton(stringResource(R.string.cookie_clear), p, { onClear(false) }, enabled = !services.clearing)
    ScreenButton(stringResource(R.string.cache_clear), p, { onClear(true) }, enabled = !services.clearing)
    if (services.message.isNotEmpty()) ScreenText(services.message, p.muted, maxLines = 4)
}

@Composable
internal fun PermissionsSettingsScreen(services: BrowserSettingsServices, dayMode: Boolean,
    onBack: () -> Unit, currentUrl: String, privateMode: Boolean, onAndroidSettings: () -> Unit) {
    LaunchedEffect(Unit) { services.refreshPermissions() }
    SettingsPage(R.string.settings_site_permissions, dayMode, onBack) { p ->
        ScreenText(stringResource(R.string.permission_settings_note), p.muted, maxLines = 5)
        BrowserSettingsServices.origin(currentUrl)?.let { origin ->
            SettingsToggle(stringResource(R.string.popup_allow), origin,
                services.popupAllowed(currentUrl, privateMode), p) { services.setPopups(currentUrl, privateMode, it) }
        }
        ScreenButton(stringResource(R.string.permission_android), p, onAndroidSettings)
        ScreenButton(stringResource(R.string.permission_refresh), p, services::refreshPermissions)
        if (services.permissions.isEmpty()) ScreenText(stringResource(
            if (services.busy) R.string.settings_loading else R.string.permission_none), p.muted)
        services.permissions.forEach { perm ->
            val type = when (perm.permission) {
                GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> R.string.permission_location
                GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS -> R.string.permission_cookies
                GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> R.string.permission_storage
                GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> R.string.permission_drm
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE,
                GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE -> R.string.shield_autoplay
                else -> R.string.permission_site
            }
            val value = when (perm.value) {
                GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW -> R.string.permission_allow
                GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY -> R.string.permission_deny
                else -> R.string.permission_ask
            }
            Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
                .clickable(role = Role.Button) { services.changePermission(perm) }.padding(T.telefon_ekran_kenar.dp)) {
                ScreenText(BrowserSettingsServices.origin(perm.uri).orEmpty(), p.text, strong = true)
                ScreenText(stringResource(type) + " · " + stringResource(value), p.muted)
            }
        }
        if (services.message.isNotEmpty()) ScreenText(services.message, p.muted)
    }
}

@Composable
internal fun PasswordSettingsScreen(services: BrowserSettingsServices, dayMode: Boolean, onBack: () -> Unit) =
    SettingsPage(R.string.settings_passwords, dayMode, onBack) { p ->
        SettingsToggle(stringResource(R.string.autofill_title), stringResource(R.string.autofill_note),
            services.autofill, p) { services.set(PrivacyOption.AUTOFILL, it) }
        ScreenText(stringResource(R.string.autofill_privacy_note), p.muted, maxLines = 6)
        ScreenButton(stringResource(R.string.autofill_manage), p, services::openPasswordService, primary = true)
        if (services.message.isNotEmpty()) ScreenText(services.message, p.muted)
    }

@Composable
internal fun LanguageSettingsScreen(services: BrowserSettingsServices, dayMode: Boolean,
    onBack: () -> Unit, onTranslate: () -> Unit) = SettingsPage(R.string.settings_language, dayMode, onBack) { p ->
    listOf("system" to R.string.language_system, "tr" to R.string.language_turkish, "en" to R.string.language_english).forEach { (code, label) ->
        ScreenButton(stringResource(label), p, { services.setLanguage(code) }, primary = services.language == code)
    }
    ScreenText(stringResource(R.string.translation_local_note), p.muted, maxLines = 6)
    ScreenButton(stringResource(R.string.menu_translate), p, onTranslate)
}

@Composable
internal fun AddonsSettingsScreen(addons: BrowserAddons, dayMode: Boolean,
    onBack: () -> Unit, onBrowse: () -> Unit, onOptions: (String) -> Unit) {
    var url by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { addons.refresh() }
    SettingsPage(R.string.menu_extensions, dayMode, onBack) { p ->
        ScreenText(stringResource(R.string.addon_note), p.muted, maxLines = 5)
        ScreenButton(stringResource(R.string.addon_browse), p, onBrowse)
        ScreenSearch(url, stringResource(R.string.addon_url_hint), p) { url = it }
        ScreenButton(stringResource(R.string.addon_install), p, { addons.install(url) },
            primary = true, enabled = !addons.busy && url.isNotBlank())
        if (addons.message.isNotBlank()) ScreenText(addons.message, p.muted, maxLines = 4)
        if (addons.items.isEmpty()) ScreenText(stringResource(R.string.addon_none), p.muted)
        addons.items.forEach { item ->
            SettingsToggle(item.metaData?.name ?: item.id, item.metaData?.version.orEmpty(), item.metaData?.enabled == true, p) { addons.toggle(item) }
            val options = item.metaData?.optionsPageUrl
            if (!options.isNullOrBlank()) ScreenButton(stringResource(R.string.addon_options), p, { onOptions(options) })
            ScreenButton(stringResource(R.string.addon_remove), p, { addons.remove(item) }, enabled = !addons.busy)
        }
    }
}

@Composable
internal fun ChildSettingsScreen(child: BrowserChildMode, dayMode: Boolean, onBack: () -> Unit) {
    var host by remember { mutableStateOf("") }
    SettingsPage(R.string.settings_child_mode, dayMode, onBack) { p ->
        ScreenText(stringResource(R.string.child_scope_note), p.muted, maxLines = 8)
        ScreenText(stringResource(R.string.child_tabs_note), p.muted, maxLines = 3)
        ScreenButton(stringResource(if (child.enabled) R.string.child_disable else R.string.child_enable), p,
            child::toggle, primary = true, enabled = !child.busy)
        ScreenButton(stringResource(R.string.child_set_pin), p, child::configurePin, enabled = !child.busy)
        ScreenSearch(host, stringResource(R.string.child_host_hint), p) { host = it }
        ScreenButton(stringResource(R.string.child_add_host), p, { child.add(host) }, enabled = !child.busy && host.isNotBlank())
        child.allowed.forEach { name ->
            Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
                .padding(T.telefon_ekran_bosluk.dp)) {
                ScreenText(name, p.text, strong = true)
                ScreenButton(stringResource(R.string.child_remove_host), p, { child.remove(name) }, enabled = !child.busy)
            }
        }
        if (child.message.isNotEmpty()) ScreenText(child.message, p.muted, maxLines = 4)
    }
}

@Composable
internal fun BrowserToolsScreen(services: BrowserSettingsServices, dayMode: Boolean,
    hasPage: Boolean, onBack: () -> Unit, onAction: (MenuAction) -> Unit) =
    SettingsPage(R.string.menu_tools, dayMode, onBack) { p ->
        ScreenText(stringResource(R.string.menu_text_size), p.text, strong = true)
        ScreenText(stringResource(R.string.menu_text_size_value, services.textPercent), p.accent)
        Row(horizontalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
            ScreenButton("−", p, { services.setTextSize(services.textPercent - 10) },
                Modifier.weight(1f), enabled = services.textPercent > 50)
            ScreenButton("100%", p, { services.setTextSize(100) }, Modifier.weight(1f))
            ScreenButton("+", p, { services.setTextSize(services.textPercent + 10) },
                Modifier.weight(1f), enabled = services.textPercent < 300)
        }
        listOf(MenuAction.HISTORY to R.string.history, MenuAction.QR to R.string.menu_scan_qr,
            MenuAction.TRANSLATE to R.string.menu_translate, MenuAction.READER to R.string.menu_reader,
            MenuAction.FIND to R.string.menu_find, MenuAction.PDF to R.string.menu_save_pdf,
            MenuAction.DESKTOP to R.string.menu_desktop, MenuAction.SEND_TO_DEVICE to R.string.menu_send_device,
            MenuAction.OPEN_EXTERNAL to R.string.menu_open_official_app,
            MenuAction.OPEN_BROWSER to R.string.menu_open_other_browser).forEach { (action, label) ->
            ScreenButton(stringResource(label), p, { onAction(action) },
                enabled = hasPage || action in setOf(MenuAction.HISTORY, MenuAction.QR, MenuAction.READER))
        }
        ScreenText(stringResource(R.string.menu_devices_share_note), p.muted, maxLines = 5)
    }
