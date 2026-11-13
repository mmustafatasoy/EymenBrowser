package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.eymen.browser.tema.EymenTokens as T
import com.eymen.browser.tema.EymenPaths

internal data class ReadingEntry(val id:Long,val url:String,val title:String,val read:Boolean)

@Composable
internal fun ReadingListScreen(store:BrowserBookmarks,visible:Boolean,day:Boolean,onBack:()->Unit,onOpen:(ReadingEntry)->Unit) {
    val p=browserListPalette(day)
    var query by remember(visible){mutableStateOf("")}
    var filter by remember(visible){mutableStateOf<Boolean?>(null)}
    BackHandler(visible,onBack)
    val entries=store.reading.filter{(filter==null || it.read==filter) && TabSearchPolicy.matches(it.title,it.url,query)}
    Column(Modifier.fillMaxSize().background(p.background)) {
        ListPageHeader(stringResource(R.string.reading_list),p,onBack)
        Box(Modifier.fillMaxWidth().padding(T.telefon_ekran_kenar.dp)) {
            ListSearch(query,stringResource(R.string.reading_search),p){query=TabSearchPolicy.boundedInput(it)}
        }
        Row(Modifier.fillMaxWidth().padding(horizontal=T.telefon_ekran_kucukBosluk.dp),horizontalArrangement=Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
            for((label,value) in listOf(R.string.tab_all to null,R.string.reading_unread to false,R.string.reading_read to true))
                ScreenButton(stringResource(label),p,{filter=value},modifier=Modifier.weight(1f),primary=filter==value)
        }
        store.error?.let{ScreenText(stringResource(it),p.muted,modifier=Modifier.padding(T.telefon_ekran_kenar.dp),maxLines=4)}
        if(entries.isEmpty())ScreenText(stringResource(if(query.isNotBlank())R.string.history_no_results else R.string.reading_empty),p.muted,
            modifier=Modifier.padding(T.telefon_ekran_kenar.dp),maxLines=4)
        LazyColumn(Modifier.weight(1f).fillMaxWidth()) {
            items(entries,key={it.id}) {item ->
                Row(Modifier.fillMaxWidth().heightIn(min=T.telefon_liste_satir.dp),verticalAlignment=Alignment.CenterVertically) {
                    val openLabel=stringResource(R.string.reading_open,item.title)
                    Column(Modifier.weight(1f).semantics{contentDescription=openLabel}.clickable(role=Role.Button){onOpen(item)}.padding(T.telefon_ekran_kenar.dp)) {
                        ScreenText(item.title,p.text,T.telefon_liste_yazi,maxLines=2)
                        ScreenText(item.url,p.muted,T.telefon_liste_ikincil,maxLines=2)
                        ScreenText(stringResource(if(item.read)R.string.reading_read else R.string.reading_unread),p.muted,T.telefon_liste_ikincil)
                    }
                    ListIconAction(stringResource(if(item.read)R.string.reading_mark_unread else R.string.reading_mark_read,item.title),EymenPaths.okuma,p,{store.markReading(item.id,!item.read)})
                    ListIconAction(stringResource(R.string.reading_remove,item.title),EymenPaths.cop,p,{store.deleteReading(item.id)})
                }
            }
        }
    }
}
