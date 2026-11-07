package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal enum class MenuAction {
    BOOKMARKS, HISTORY, DOWNLOADS, PRIVATE, TRANSLATE, FIND,
    READER, DESKTOP, PDF, SHARE, SEND_TO_DEVICE, QR,
    EXTENSIONS, OPEN_EXTERNAL, OPEN_BROWSER, THEME, SETTINGS, TOOLS,
    NEW_TAB, TAB_GROUP, RECENT_TABS, CLEAR_DATA, SITE_CONTROLS, HELP
}
private data class MenuRow(val title: Int,val icon: String,val action: MenuAction,val needsPage: Boolean = false)
private val groups = listOf(
    listOf(MenuRow(R.string.menu_new_tab,EymenPaths.eklenti,MenuAction.NEW_TAB),
        MenuRow(R.string.private_tab,EymenPaths.gizli,MenuAction.PRIVATE),
        MenuRow(R.string.menu_group_tab,EymenPaths.klasor,MenuAction.TAB_GROUP)),
    listOf(MenuRow(R.string.history,EymenPaths.gecmis,MenuAction.HISTORY),
        MenuRow(R.string.menu_clear_data,EymenPaths.cop,MenuAction.CLEAR_DATA),
        MenuRow(R.string.menu_site_controls,EymenPaths.ayarlar,MenuAction.SITE_CONTROLS,true)),
    listOf(MenuRow(R.string.downloads,EymenPaths.indir,MenuAction.DOWNLOADS),
        MenuRow(R.string.bookmarks,EymenPaths.yerImi,MenuAction.BOOKMARKS),
        MenuRow(R.string.library_recent_tabs,EymenPaths.cihazlar,MenuAction.RECENT_TABS)),
    listOf(MenuRow(R.string.menu_share,EymenPaths.paylas,MenuAction.SHARE,true),
        MenuRow(R.string.menu_find,EymenPaths.ara,MenuAction.FIND,true),
        MenuRow(R.string.menu_translate,EymenPaths.cevir,MenuAction.TRANSLATE,true),
        MenuRow(R.string.menu_reader,EymenPaths.okuma,MenuAction.READER,true),
        MenuRow(R.string.menu_save_pdf,EymenPaths.belge,MenuAction.PDF,true),
        MenuRow(R.string.menu_desktop,EymenPaths.masaustu,MenuAction.DESKTOP,true)),
    listOf(MenuRow(R.string.menu_extensions,EymenPaths.eklenti,MenuAction.EXTENSIONS),
        MenuRow(R.string.menu_scan_qr,EymenPaths.qr,MenuAction.QR),
        MenuRow(R.string.menu_tools,EymenPaths.ayarlar,MenuAction.TOOLS),
        MenuRow(R.string.menu_open_official_app,EymenPaths.cihazlar,MenuAction.OPEN_EXTERNAL,true),
        MenuRow(R.string.menu_open_other_browser,EymenPaths.site,MenuAction.OPEN_BROWSER,true)),
    listOf(MenuRow(R.string.settings,EymenPaths.ayarlar,MenuAction.SETTINGS),
        MenuRow(R.string.settings_help,EymenPaths.bilgi,MenuAction.HELP))
)

/** A real scrollable overflow menu; each row names an implemented action. */
@Composable
internal fun BrowserMenu(hasPage: Boolean,isDesktop: Boolean,dayMode: Boolean,textPercent: Int,
    onTextSize: (Int) -> Unit,onDismiss: () -> Unit,onAction: (MenuAction) -> Unit,
    onBack: () -> Unit,onForward: () -> Unit,onReload: () -> Unit,
    canGoBack: Boolean,canGoForward: Boolean,bookmarked: Boolean,onBookmark: () -> Unit) {
    val p=browserListPalette(dayMode)
    val dismissLabel=stringResource(R.string.menu_dismiss)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().semantics { contentDescription = dismissLabel }
            .clickable(onClick = onDismiss))
        val width=minOf(T.telefon_liste_menuGenislik.dp,maxWidth-T.telefon_ekran_kenar.dp)
        val available=(maxHeight-T.telefon_liste_menuKenar.dp*2).coerceAtLeast(T.telefon_dokunma_standart.dp)
        Column(Modifier.align(Alignment.TopEnd).padding(T.telefon_liste_menuKenar.dp)
            .width(width).heightIn(max=available)
            .shadow(T.telefon_ekran_kucukBosluk.dp,RoundedCornerShape(T.telefon_liste_menuKose.dp))
            .background(p.surface,RoundedCornerShape(T.telefon_liste_menuKose.dp))
            .verticalScroll(rememberScrollState()).padding(vertical=T.telefon_ekran_kucukBosluk.dp)) {
            Row(Modifier.fillMaxWidth().padding(horizontal=T.telefon_ekran_kucukBosluk.dp),
                horizontalArrangement=Arrangement.SpaceBetween) {
                val circle=Modifier.background(p.background,CircleShape)
                ListIconAction(stringResource(R.string.back),EymenPaths.geri,p,{onDismiss();onBack()},canGoBack,circle)
                ListIconAction(stringResource(R.string.forward),EymenPaths.ileri,p,{onDismiss();onForward()},canGoForward,circle)
                ListIconAction(stringResource(if(bookmarked)R.string.menu_bookmark_remove else R.string.menu_bookmark_add),
                    EymenPaths.yerImi,p,{onDismiss();onBookmark()},hasPage,circle)
                ListIconAction(stringResource(R.string.menu_save_pdf),EymenPaths.indir,p,{onAction(MenuAction.PDF)},hasPage,circle)
                ListIconAction(stringResource(R.string.menu_reload),EymenPaths.yenile,p,{onDismiss();onReload()},hasPage,circle)
            }
            groups.forEachIndexed { index,rows ->
                if(index>0) Box(Modifier.fillMaxWidth().padding(horizontal=T.telefon_ekran_kenar.dp,
                    vertical=T.telefon_ekran_kucukBosluk.dp).height(T.telefon_ekran_cizgi.dp).background(p.background))
                if(index==3) {
                    Row(Modifier.fillMaxWidth().heightIn(min=T.telefon_liste_satir.dp).padding(horizontal=T.telefon_ekran_kenar.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        EymenVector(EymenPaths.yakinlastir,color=p.muted,iconSize=T.telefon_liste_menuSimge.dp)
                        Spacer(Modifier.width(T.telefon_ekran_kenar.dp))
                        ScreenText(stringResource(R.string.menu_text_size_value,textPercent),p.text,
                            T.telefon_liste_ikincil,Modifier.weight(1f))
                        MenuZoomButton("−",p,textPercent>50){onTextSize(textPercent-10)}
                        MenuZoomButton("+",p,textPercent<300){onTextSize(textPercent + 10)}
                    }
                }
                rows.forEach { row ->
                    val allowed=!row.needsPage || hasPage
                    Row(Modifier.fillMaxWidth().heightIn(min=T.telefon_liste_satir.dp)
                        .clickable(enabled=allowed,role=Role.Button){onAction(row.action)}
                        .padding(horizontal=T.telefon_ekran_kenar.dp,vertical=T.telefon_ekran_bosluk.dp),
                        verticalAlignment=Alignment.CenterVertically) {
                        EymenVector(row.icon,color=if(allowed)p.muted else p.disabled,iconSize=T.telefon_liste_menuSimge.dp)
                        Spacer(Modifier.width(T.telefon_ekran_kenar.dp))
                        ScreenText(stringResource(row.title),if(allowed)p.text else p.disabled,T.telefon_liste_yazi,Modifier.weight(1f),maxLines=2)
                        if(row.action==MenuAction.DESKTOP && isDesktop)
                            EymenVector(EymenPaths.kalkan,color=p.accent,iconSize=T.telefon_liste_menuSimge.dp)
                    }
                }
            }
        }
    }
}
@Composable
private fun MenuZoomButton(label: String,p: BrowserScreenPalette,enabled: Boolean,onClick: () -> Unit) {
    Box(Modifier.size(T.telefon_dokunma_enAz.dp).semantics { contentDescription=label }
        .clickable(enabled,role=Role.Button,onClick=onClick),contentAlignment=Alignment.Center) {
        ScreenText(label,if(enabled)p.text else p.disabled,T.telefon_liste_baslik)
    }
}
