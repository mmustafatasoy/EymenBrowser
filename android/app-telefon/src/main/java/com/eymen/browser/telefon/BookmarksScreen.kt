package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

@Composable
internal fun BookmarksScreen(store: BrowserBookmarks, visible: Boolean, dayMode: Boolean,
    onBack: () -> Unit, onOpen: (String) -> Unit, onNewFolder: () -> Unit,
    onFolder: (BookmarkFolder) -> Unit, onItem: (BookmarkEntry) -> Unit) {
    val p=browserListPalette(dayMode)
    var query by remember(visible) {mutableStateOf("")}
    var selected by remember(visible) {mutableStateOf<Long?>(null)}
    LaunchedEffect(store.folders,selected) {if(selected!=null && store.folders.none {it.id==selected})selected=null}
    BackHandler(visible) {if(selected!=null)selected=null else onBack()}
    val items=store.entries.filter { (selected==null || it.folderId==selected) && TabSearchPolicy.matches(it.title,it.url,query) }
    Column(Modifier.fillMaxSize().background(p.background)) {
        ListPageHeader(stringResource(R.string.bookmarks),p,onBack) {
            ListIconAction(stringResource(R.string.bookmark_new_folder),EymenPaths.klasor,p,onNewFolder)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal=T.telefon_ekran_kenar.dp,vertical=T.telefon_ekran_kucukBosluk.dp)) {
            ListSearch(query,stringResource(R.string.library_search_bookmarks),p) {query=TabSearchPolicy.boundedInput(it)}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(T.telefon_ekran_kucukBosluk.dp),
            horizontalArrangement=Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
            val allLabel=stringResource(R.string.bookmark_show_all)
            ScreenButton(stringResource(R.string.tab_all),p,{selected=null},modifier=Modifier.semantics{contentDescription=allLabel},primary=selected==null)
            store.folders.forEach {folder ->
                val openLabel=stringResource(R.string.bookmark_open_folder,folder.name)
                ScreenButton(folder.name,p,{selected=folder.id},modifier=Modifier.semantics{contentDescription=openLabel},primary=selected==folder.id)
            }
        }
        val folder=store.folders.firstOrNull {it.id==selected}
        if(folder!=null) SettingsActionRow(stringResource(R.string.bookmark_manage_folder,folder.name),p,{onFolder(folder)},icon=EymenPaths.ayarlar)
        store.error?.let { ScreenText(stringResource(it),p.muted,modifier=Modifier.padding(T.telefon_ekran_kenar.dp),maxLines=4) }
        if(items.isEmpty()) ScreenText(stringResource(if(store.loading)R.string.history_loading else if(query.isNotBlank())R.string.history_no_results else R.string.library_bookmarks_empty),
            p.muted,modifier=Modifier.padding(T.telefon_ekran_kenar.dp),maxLines=4)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding=PaddingValues(bottom=T.telefon_ekran_kenar.dp)) {
            items(items,key={it.id}) {item ->
                Row(Modifier.fillMaxWidth().heightIn(min=T.telefon_liste_satir.dp),verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).clickable(role=Role.Button){onOpen(item.url)}.padding(T.telefon_ekran_kenar.dp)) {
                        ScreenText(item.title,p.text,T.telefon_liste_yazi,maxLines=2)
                        ScreenText(item.url,p.muted,T.telefon_liste_ikincil,maxLines=2)
                        store.folders.firstOrNull {it.id==item.folderId}?.let {ScreenText(it.name,p.muted,T.telefon_liste_ikincil,maxLines=1)}
                    }
                    ListIconAction(stringResource(R.string.bookmark_item_options,item.title),EymenPaths.ayarlar,p,{onItem(item)})
                }
            }
        }
    }
}
