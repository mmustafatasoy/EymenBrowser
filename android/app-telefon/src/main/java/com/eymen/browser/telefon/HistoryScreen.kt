package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T
import java.net.URI
import java.text.DateFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

internal enum class BrowserLibrary { HISTORY, BOOKMARKS, RECENT_TABS }

@Composable
internal fun HistoryScreen(visible: Boolean, mode: BrowserLibrary, entries: List<HistoryEntry>,
    loading: Boolean, failed: Boolean, dayMode: Boolean, onBack: () -> Unit,
    onOpen: (String) -> Unit, onDelete: (HistoryEntry) -> Unit, onClear: () -> Unit) {
    val p = browserListPalette(dayMode)
    var query by remember(visible,mode) { mutableStateOf("") }
    BackHandler(enabled = visible,onBack = onBack)
    val title = stringResource(when (mode) {
        BrowserLibrary.HISTORY -> R.string.history
        BrowserLibrary.BOOKMARKS -> R.string.bookmarks
        BrowserLibrary.RECENT_TABS -> R.string.library_recent_tabs
    })
    val hint = stringResource(when (mode) {
        BrowserLibrary.HISTORY -> R.string.history_search_hint
        BrowserLibrary.BOOKMARKS -> R.string.library_search_bookmarks
        BrowserLibrary.RECENT_TABS -> R.string.library_search_tabs
    })
    val filtered = remember(entries,query) { entries.filter {
        query.isBlank() || it.title.contains(query,true) || it.url.contains(query,true)
    } }
    Column(Modifier.fillMaxSize().background(p.background)) {
        ListPageHeader(title,p,onBack) {
            if (mode == BrowserLibrary.HISTORY && entries.isNotEmpty())
                ListIconAction(stringResource(R.string.history_clear),EymenPaths.cop,p,onClear)
        }
        Box(Modifier.fillMaxWidth().padding(horizontal = T.telefon_ekran_kenar.dp,
            vertical = T.telefon_ekran_kucukBosluk.dp)) { ListSearch(query,hint,p) { query = it } }
        if (failed) ScreenText(stringResource(R.string.history_error),p.muted,
            modifier = Modifier.padding(T.telefon_ekran_kenar.dp))
        if (filtered.isEmpty()) {
            val text = if (loading && entries.isEmpty()) R.string.history_loading
                else if (query.isNotBlank()) R.string.history_no_results
                else when (mode) {
                    BrowserLibrary.HISTORY -> R.string.history_empty
                    BrowserLibrary.BOOKMARKS -> R.string.library_bookmarks_empty
                    BrowserLibrary.RECENT_TABS -> R.string.library_recent_empty
                }
            ScreenText(stringResource(text),p.muted,modifier = Modifier.padding(T.telefon_ekran_kenar.dp),maxLines = 4)
        }
        LazyColumn(Modifier.weight(1f).fillMaxWidth(),contentPadding = PaddingValues(bottom = T.telefon_ekran_kenar.dp)) {
            if (mode == BrowserLibrary.HISTORY) {
                val zone = ZoneId.systemDefault()
                filtered.groupBy { Instant.ofEpochMilli(it.visitedAt).atZone(zone).toLocalDate() }.forEach { (date, visits) ->
                    item(key = "day-$date") {
                        val label = when (date) {
                            LocalDate.now(zone) -> stringResource(R.string.history_today)
                            LocalDate.now(zone).minusDays(1) -> stringResource(R.string.history_yesterday)
                            else -> DateFormat.getDateInstance(DateFormat.FULL).format(Date(visits.first().visitedAt))
                        }
                        ScreenText(label,p.muted,T.telefon_liste_ikincil,
                            Modifier.padding(horizontal = T.telefon_ekran_kenar.dp,vertical = T.telefon_ekran_bosluk.dp))
                    }
                    items(visits,key = { "visit-${it.id}" }) { visit -> HistoryRow(visit,p,true,onOpen,onDelete) }
                }
            } else items(filtered,key = { "item-${it.id}" }) { visit -> HistoryRow(visit,p,false,onOpen,onDelete) }
        }
    }
}

@Composable
private fun HistoryRow(entry: HistoryEntry,p: BrowserScreenPalette,showTime: Boolean,
    onOpen: (String) -> Unit,onDelete: (HistoryEntry) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = T.telefon_liste_satir.dp),verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f).clickable(role = Role.Button) { onOpen(entry.url) }
            .padding(start = T.telefon_ekran_kenar.dp,top = T.telefon_ekran_bosluk.dp,bottom = T.telefon_ekran_bosluk.dp),
            verticalAlignment = Alignment.CenterVertically) {
            val host = remember(entry.url) { try { URI(entry.url).host.orEmpty().removePrefix("www.") } catch (_: Exception) { "" } }
            Box(Modifier.size(T.telefon_liste_simge.dp).background(p.surface,CircleShape),contentAlignment = Alignment.Center) {
                ScreenText(host.take(1).uppercase(),p.muted,T.telefon_liste_yazi)
            }
            Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
            Column(Modifier.weight(1f)) {
                ScreenText(entry.title,p.text,T.telefon_liste_yazi,maxLines = 1)
                ScreenText(entry.url.removePrefix("https://").removePrefix("http://"),p.muted,T.telefon_liste_ikincil,maxLines = 1)
                if (showTime) ScreenText(DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(entry.visitedAt)),p.muted,T.telefon_yazi_enAz,maxLines = 1)
            }
        }
        ListIconAction(stringResource(R.string.history_delete_item,entry.url),EymenPaths.temizle,p,{ onDelete(entry) })
    }
}
