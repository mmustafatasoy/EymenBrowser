package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal enum class SettingsAction {
    SHIELD, VPN, PASSWORDS, SEARCH_ENGINE, APPEARANCE, EXTENSIONS,
    SITE_PERMISSIONS, DOWNLOADS, LANGUAGE, CHILD_MODE, ABOUT
}
internal enum class BrowserSettingsPage { MAIN, SEARCH, APPEARANCE, ABOUT }
private data class SettingRow(val title: Int, val icon: String, val action: SettingsAction,
    val section: Int, val implemented: Boolean = false)
private val settingsRows = listOf(
    SettingRow(R.string.settings_shield, EymenPaths.kalkan, SettingsAction.SHIELD, R.string.settings_group_privacy, true),
    SettingRow(R.string.vpn_title, EymenPaths.kalkan, SettingsAction.VPN, R.string.settings_group_privacy, true),
    SettingRow(R.string.settings_passwords, EymenPaths.kilit, SettingsAction.PASSWORDS, R.string.settings_group_privacy),
    SettingRow(R.string.settings_site_permissions, EymenPaths.site, SettingsAction.SITE_PERMISSIONS, R.string.settings_group_privacy),
    SettingRow(R.string.menu_settings_search, EymenPaths.ara, SettingsAction.SEARCH_ENGINE, R.string.settings_group_browsing, true),
    SettingRow(R.string.settings_appearance, EymenPaths.ay, SettingsAction.APPEARANCE, R.string.settings_group_browsing, true),
    SettingRow(R.string.settings_language, EymenPaths.cevir, SettingsAction.LANGUAGE, R.string.settings_group_browsing),
    SettingRow(R.string.downloads, EymenPaths.indir, SettingsAction.DOWNLOADS, R.string.settings_group_tools, true),
    SettingRow(R.string.menu_extensions, EymenPaths.eklenti, SettingsAction.EXTENSIONS, R.string.settings_group_tools),
    SettingRow(R.string.settings_child_mode, EymenPaths.cocuk, SettingsAction.CHILD_MODE, R.string.settings_group_tools),
    SettingRow(R.string.settings_about_update, EymenPaths.bilgi, SettingsAction.ABOUT, R.string.settings_group_app, true)
)

@Composable
private fun ChoiceRow(label: String, chosen: Boolean, p: BrowserScreenPalette, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = T.telefon_dokunma_standart.dp)
        .semantics { selected = chosen }.clickable(role = Role.RadioButton, onClick = onClick)
        .padding(T.telefon_ekran_kenar.dp), verticalAlignment = Alignment.CenterVertically) {
        ScreenText(label, p.text, modifier = Modifier.weight(1f), strong = chosen)
        Box(Modifier.size(T.telefon_ekran_simge.dp)
            .border(T.telefon_ekran_cizgi.dp, if (chosen) p.accent else p.line, CircleShape)
            .padding(T.telefon_ekran_cizgi.dp * 4)
            .background(if (chosen) p.accent else p.surface, CircleShape))
    }
}

/** Branded settings with actual persisted choices; unavailable features stay labelled. */
@Composable
internal fun SettingsScreen(
    globalShield: Boolean, searchEngine: String, dayMode: Boolean, addressBottom: Boolean,
    visible: Boolean, initialPage: BrowserSettingsPage,
    onBack: () -> Unit, onAction: (SettingsAction) -> Unit,
    onSearchEngine: (String) -> Unit, onDayMode: (Boolean) -> Unit, onAddressBottom: (Boolean) -> Unit
) {
    val p = browserScreenPalette(dayMode)
    var query by remember(visible) { mutableStateOf("") }
    var page by remember(visible, initialPage) { mutableStateOf(initialPage) }
    val focus = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val back: () -> Unit = {
        focus.clearFocus(); keyboard?.hide()
        if (page == BrowserSettingsPage.MAIN) onBack() else page = BrowserSettingsPage.MAIN
    }
    BackHandler(enabled = visible && page != BrowserSettingsPage.MAIN) { back() }
    val title = stringResource(when (page) {
        BrowserSettingsPage.MAIN -> R.string.settings
        BrowserSettingsPage.SEARCH -> R.string.menu_settings_search
        BrowserSettingsPage.APPEARANCE -> R.string.settings_appearance
        BrowserSettingsPage.ABOUT -> R.string.settings_about_update
    })
    val filtered = settingsRows.filter { row ->
        query.isBlank() || stringResource(row.title).contains(query.trim(), ignoreCase = true)
    }
    Column(Modifier.fillMaxSize().screenBackground(p).padding(T.telefon_ekran_kenar.dp),
        verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
        ScreenHeader(title, stringResource(R.string.settings_personal_subtitle), p, back)
        if (page == BrowserSettingsPage.MAIN) {
            ScreenSearch(query, stringResource(R.string.settings_search_hint), p) { query = it }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_kenar.dp)) {
            when (page) {
                BrowserSettingsPage.MAIN -> {
                    if (query.isBlank()) {
                        Row(Modifier.fillMaxWidth().background(p.accent, RoundedCornerShape(T.telefon_kose_panel.dp))
                            .padding(T.telefon_ekran_kenar.dp), verticalAlignment = Alignment.CenterVertically) {
                            EymenVector(EymenPaths.ayarlar, color = p.onAccent, iconSize = T.telefon_dokunma_standart.dp)
                            Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
                            Column(Modifier.weight(1f)) {
                                ScreenText(stringResource(R.string.browser_display_name), p.onAccent,
                                    T.telefon_yazi_bolumBasligi, strong = true)
                                ScreenText(stringResource(R.string.settings_personal_hint), p.onAccent, T.telefon_yazi_ikincil)
                            }
                        }
                    }
                    filtered.groupBy { it.section }.forEach { (section, rows) ->
                        Column(verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
                            ScreenText(stringResource(section), p.muted, T.telefon_yazi_ikincil, strong = true)
                            Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))) {
                                rows.forEach { row ->
                                    val note = when (row.action) {
                                        SettingsAction.VPN -> stringResource(R.string.settings_vpn_note)
                                        SettingsAction.SHIELD -> stringResource(if (globalShield) R.string.settings_enabled else R.string.settings_disabled)
                                        SettingsAction.SEARCH_ENGINE -> searchEngine
                                        SettingsAction.APPEARANCE -> stringResource(if (dayMode) R.string.theme_day else R.string.theme_night)
                                        SettingsAction.ABOUT -> stringResource(R.string.settings_updates_note)
                                        else -> if (row.implemented) "" else stringResource(R.string.settings_coming_soon)
                                    }
                                    Row(Modifier.fillMaxWidth().heightIn(min = T.telefon_dokunma_standart.dp)
                                        .clickable(enabled = row.implemented, role = Role.Button) {
                                            focus.clearFocus(); keyboard?.hide()
                                            when (row.action) {
                                                SettingsAction.SEARCH_ENGINE -> page = BrowserSettingsPage.SEARCH
                                                SettingsAction.APPEARANCE -> page = BrowserSettingsPage.APPEARANCE
                                                SettingsAction.ABOUT -> page = BrowserSettingsPage.ABOUT
                                                else -> onAction(row.action)
                                            }
                                        }.padding(T.telefon_ekran_bosluk.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Box(Modifier.size(T.telefon_ekran_rozet.dp)
                                            .background(p.soft, RoundedCornerShape(T.telefon_kose_kucuk.dp)),
                                            contentAlignment = Alignment.Center) {
                                            EymenVector(row.icon, color = if (row.implemented) p.accent else p.disabled,
                                                iconSize = T.telefon_ekran_simge.dp)
                                        }
                                        Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
                                        Column(Modifier.weight(1f)) {
                                            ScreenText(stringResource(row.title), if (row.implemented) p.text else p.disabled, strong = true)
                                            if (note.isNotEmpty()) ScreenText(note, p.muted, T.telefon_yazi_ikincil)
                                        }
                                        if (row.implemented) EymenVector(EymenPaths.ileri, color = p.muted,
                                            iconSize = T.telefon_ekran_simge.dp)
                                    }
                                }
                            }
                        }
                    }
                    if (filtered.isEmpty()) ScreenText(stringResource(R.string.settings_no_results), p.muted)
                }
                BrowserSettingsPage.SEARCH -> {
                    ScreenText(stringResource(R.string.settings_search_explanation), p.muted)
                    Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))) {
                        listOf("Google", "DuckDuckGo", "Bing", "Yandex").forEach { engine ->
                            ChoiceRow(engine, searchEngine == engine, p) { onSearchEngine(engine) }
                        }
                    }
                }
                BrowserSettingsPage.APPEARANCE -> {
                    ScreenText(stringResource(R.string.settings_change_theme), p.muted, strong = true)
                    Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))) {
                        ChoiceRow(stringResource(R.string.theme_day), dayMode, p) { onDayMode(true) }
                        ChoiceRow(stringResource(R.string.theme_night), !dayMode, p) { onDayMode(false) }
                    }
                    if (!dayMode) {
                        ScreenText(stringResource(R.string.settings_change_address), p.muted, strong = true)
                        Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))) {
                            ChoiceRow(stringResource(R.string.onboarding_top), !addressBottom, p) { onAddressBottom(false) }
                            ChoiceRow(stringResource(R.string.onboarding_bottom), addressBottom, p) { onAddressBottom(true) }
                        }
                    }
                }
                BrowserSettingsPage.ABOUT -> {
                    val context = LocalContext.current
                    val version = remember { try {
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
                    } catch (_: Exception) { "" } }
                    Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))
                        .padding(T.telefon_ekran_kenar.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
                        AtasoySiteLogo(Modifier.size(T.telefon_dokunma_standart.dp * 2))
                        ScreenText(stringResource(R.string.browser_display_name), p.text, T.telefon_yazi_sayfaBasligi, strong = true)
                        ScreenText(stringResource(R.string.settings_app_version, version), p.muted)
                    }
                    ScreenButton(stringResource(R.string.update_manual_check), p, { onAction(SettingsAction.ABOUT) }, primary = true)
                }
            }
        }
    }
}
