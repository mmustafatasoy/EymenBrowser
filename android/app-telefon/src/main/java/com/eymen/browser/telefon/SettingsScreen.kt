package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal enum class SettingsAction {
    SHIELD, VPN, PASSWORDS, SEARCH_ENGINE, APPEARANCE, EXTENSIONS,
    SITE_PERMISSIONS, DOWNLOADS, LANGUAGE, CHILD_MODE, ABOUT, TOOLS, LICENSES,
    ADDRESS, DEFAULT_BROWSER, SECURITY_CHECK, HOME, NOTIFICATIONS, ACCESSIBILITY, TABS, CLEAR_DATA,
    WEBSITE, SUPPORT, FEEDBACK_EMAIL, PRIVACY_POLICY
}
internal enum class BrowserSettingsPage { MAIN, SEARCH, APPEARANCE, ADDRESS, HOME, ABOUT, LICENSES, FEEDBACK }
private data class SettingRow(val title: Int,val action: SettingsAction,val section: Int)
private val settingsRows=listOf(
    SettingRow(R.string.menu_settings_search,SettingsAction.SEARCH_ENGINE,R.string.settings_basic),
    SettingRow(R.string.settings_address,SettingsAction.ADDRESS,R.string.settings_basic),
    SettingRow(R.string.settings_shield,SettingsAction.SHIELD,R.string.settings_basic),
    SettingRow(R.string.settings_security_check,SettingsAction.SECURITY_CHECK,R.string.settings_basic),
    SettingRow(R.string.settings_default_browser,SettingsAction.DEFAULT_BROWSER,R.string.settings_basic),
    SettingRow(R.string.settings_passwords,SettingsAction.PASSWORDS,R.string.settings_basic),
    SettingRow(R.string.settings_tabs_groups,SettingsAction.TABS,R.string.settings_advanced),
    SettingRow(R.string.settings_home,SettingsAction.HOME,R.string.settings_advanced),
    SettingRow(R.string.settings_notifications,SettingsAction.NOTIFICATIONS,R.string.settings_advanced),
    SettingRow(R.string.settings_appearance,SettingsAction.APPEARANCE,R.string.settings_advanced),
    SettingRow(R.string.settings_accessibility,SettingsAction.ACCESSIBILITY,R.string.settings_advanced),
    SettingRow(R.string.settings_site_permissions,SettingsAction.SITE_PERMISSIONS,R.string.settings_advanced),
    SettingRow(R.string.settings_language,SettingsAction.LANGUAGE,R.string.settings_advanced),
    SettingRow(R.string.downloads,SettingsAction.DOWNLOADS,R.string.settings_advanced),
    SettingRow(R.string.settings_about_update,SettingsAction.ABOUT,R.string.settings_advanced),
    SettingRow(R.string.vpn_title,SettingsAction.VPN,R.string.settings_group_tools),
    SettingRow(R.string.menu_extensions,SettingsAction.EXTENSIONS,R.string.settings_group_tools),
    SettingRow(R.string.settings_child_mode,SettingsAction.CHILD_MODE,R.string.settings_group_tools),
    SettingRow(R.string.menu_tools,SettingsAction.TOOLS,R.string.settings_group_tools)
)

@Composable
internal fun SettingsScreen(globalShield: Boolean,searchEngine: String,dayMode: Boolean,addressBottom: Boolean,
    visible: Boolean,initialPage: BrowserSettingsPage,defaultBrowser: Boolean,notificationsEnabled: Boolean,
    homeButtonEnabled: Boolean,onHomeButton: (Boolean)->Unit,
    onBack: ()->Unit,onAction: (SettingsAction)->Unit,onSearchEngine: (String)->Unit,
    onDayMode: (Boolean)->Unit,onAddressBottom: (Boolean)->Unit) {
    val p=browserListPalette(dayMode)
    var query by remember(visible){mutableStateOf("")}
    val pages=remember(visible,initialPage){mutableStateListOf(initialPage)}
    val page=pages.last()
    val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
    val back:()->Unit={focus.clearFocus();keyboard?.hide();if(pages.size==1)onBack() else pages.removeAt(pages.lastIndex)}
    BackHandler(enabled=visible && pages.size>1,onBack=back)
    if(page==BrowserSettingsPage.LICENSES){OpenSourceLicensesScreen(dayMode,back);return}
    if(page==BrowserSettingsPage.FEEDBACK){FeedbackScreen(dayMode,back,{onAction(SettingsAction.SUPPORT)},
        {onAction(SettingsAction.FEEDBACK_EMAIL)},{onAction(SettingsAction.WEBSITE)});return}
    val title=stringResource(when(page){
        BrowserSettingsPage.MAIN->R.string.settings
        BrowserSettingsPage.SEARCH->R.string.menu_settings_search
        BrowserSettingsPage.APPEARANCE->R.string.settings_appearance
        BrowserSettingsPage.ADDRESS->R.string.settings_address
        BrowserSettingsPage.HOME->R.string.settings_home
        BrowserSettingsPage.ABOUT->R.string.settings_about_update
        BrowserSettingsPage.LICENSES->R.string.about_licenses
        BrowserSettingsPage.FEEDBACK->R.string.feedback_title
    })
    val filtered=settingsRows.filter { query.isBlank() || stringResource(it.title).contains(query.trim(),true) }
    Column(Modifier.fillMaxSize().background(p.background)) {
        ListPageHeader(title,p,back){
            if(page==BrowserSettingsPage.MAIN)ListIconAction(stringResource(R.string.settings_help),EymenPaths.bilgi,p,{pages.add(BrowserSettingsPage.FEEDBACK)})
        }
        if(page==BrowserSettingsPage.MAIN)Box(Modifier.fillMaxWidth().padding(horizontal=T.telefon_ekran_kenar.dp,
            vertical=T.telefon_ekran_kucukBosluk.dp)){
            ListSearch(query,stringResource(R.string.settings_search_hint),p){query=it}
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
            .padding(horizontal=T.telefon_ekran_kenar.dp,vertical=T.telefon_ekran_bosluk.dp),
            verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
            when(page){
                BrowserSettingsPage.MAIN->{
                    filtered.groupBy { it.section }.forEach { (section,rows)->
                        Column(verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)){
                            ScreenText(stringResource(section),p.muted,T.telefon_liste_ikincil,
                                Modifier.padding(start=T.telefon_ekran_kenar.dp,top=T.telefon_ekran_bosluk.dp,bottom=T.telefon_ekran_kucukBosluk.dp))
                            SettingsRows(p){rows.forEach { row->
                                val note=when(row.action){
                                    SettingsAction.SEARCH_ENGINE->searchEngine
                                    SettingsAction.ADDRESS->stringResource(if(addressBottom)R.string.onboarding_bottom else R.string.onboarding_top)
                                    SettingsAction.DEFAULT_BROWSER->stringResource(if(defaultBrowser)R.string.settings_default_yes else R.string.settings_default_choose)
                                    SettingsAction.HOME->stringResource(if(homeButtonEnabled)R.string.settings_enabled else R.string.settings_disabled)
                                    SettingsAction.NOTIFICATIONS->stringResource(if(notificationsEnabled)R.string.settings_enabled else R.string.settings_disabled)
                                    else->""
                                }
                                SettingsActionRow(stringResource(row.title),p,{
                                    focus.clearFocus();keyboard?.hide()
                                    when(row.action){
                                        SettingsAction.SEARCH_ENGINE->pages.add(BrowserSettingsPage.SEARCH)
                                        SettingsAction.APPEARANCE->pages.add(BrowserSettingsPage.APPEARANCE)
                                        SettingsAction.ADDRESS->pages.add(BrowserSettingsPage.ADDRESS)
                                        SettingsAction.HOME->pages.add(BrowserSettingsPage.HOME)
                                        SettingsAction.ABOUT->pages.add(BrowserSettingsPage.ABOUT)
                                        else->onAction(row.action)
                                    }
                                },note=note)
                            }}
                            Spacer(Modifier.height(T.telefon_ekran_kucukBosluk.dp))
                        }
                    }
                    if(filtered.isEmpty())ScreenText(stringResource(R.string.settings_no_results),p.muted)
                }
                BrowserSettingsPage.SEARCH->SettingsRows(p){
                    listOf("Google","DuckDuckGo","Bing","Yandex").forEach { engine->
                        SettingsActionRow(engine,p,{onSearchEngine(engine)},chosen=searchEngine==engine)
                    }
                }
                BrowserSettingsPage.ADDRESS->SettingsRows(p){
                    SettingsActionRow(stringResource(R.string.onboarding_top),p,{onAddressBottom(false)},chosen=!addressBottom)
                    SettingsActionRow(stringResource(R.string.onboarding_bottom),p,{onAddressBottom(true)},chosen=addressBottom)
                }
                BrowserSettingsPage.APPEARANCE->SettingsRows(p){
                    SettingsActionRow(stringResource(R.string.theme_day),p,{onDayMode(true)},chosen=dayMode)
                    SettingsActionRow(stringResource(R.string.theme_night),p,{onDayMode(false)},chosen=!dayMode)
                }
                BrowserSettingsPage.HOME->{
                    SettingsToggle(stringResource(R.string.settings_home_button),stringResource(R.string.settings_home_note),homeButtonEnabled,p,onHomeButton)
                }
                BrowserSettingsPage.ABOUT->{
                    val context=LocalContext.current
                    val version=remember{try{context.packageManager.getPackageInfo(context.packageName,0).versionName.orEmpty()}catch(_:Exception){""}}
                    Row(Modifier.fillMaxWidth().padding(vertical=T.telefon_ekran_kenar.dp),horizontalArrangement=Arrangement.spacedBy(T.telefon_ekran_kenar.dp)){
                        AtasoySiteLogo(Modifier.size(T.telefon_dokunma_standart.dp))
                        Column{ScreenText(stringResource(R.string.browser_display_name),p.text,T.telefon_liste_baslik)
                            ScreenText(stringResource(R.string.settings_app_version,version),p.muted,T.telefon_liste_ikincil)}
                    }
                    ScreenText(stringResource(R.string.about_publisher),p.text,T.telefon_liste_yazi,strong=true)
                    ScreenText(stringResource(R.string.about_summary),p.muted,T.telefon_liste_ikincil,maxLines=5)
                    SettingsRows(p){
                        SettingsActionRow(stringResource(R.string.update_manual_check),p,{onAction(SettingsAction.ABOUT)})
                        SettingsActionRow(stringResource(R.string.about_website),p,{onAction(SettingsAction.WEBSITE)},note=stringResource(R.string.about_website_address),icon=EymenPaths.site)
                        SettingsActionRow(stringResource(R.string.about_feedback),p,{pages.add(BrowserSettingsPage.FEEDBACK)},icon=EymenPaths.paylas)
                        SettingsActionRow(stringResource(R.string.about_privacy),p,{onAction(SettingsAction.PRIVACY_POLICY)},icon=EymenPaths.kilit)
                        SettingsActionRow(stringResource(R.string.about_licenses),p,{pages.add(BrowserSettingsPage.LICENSES)},icon=EymenPaths.belge)
                    }
                }
                BrowserSettingsPage.LICENSES,BrowserSettingsPage.FEEDBACK->Unit
            }
        }
    }
}
