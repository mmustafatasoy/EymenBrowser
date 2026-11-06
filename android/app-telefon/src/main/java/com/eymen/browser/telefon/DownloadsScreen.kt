package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T
import java.text.DateFormat
import java.util.Date

private enum class DownloadType { ALL, VIDEO, DOCUMENT, OTHER }
private fun fileGroup(file: SavedDownload) = when {
    file.mimeType.startsWith("video/") -> DownloadType.VIDEO
    file.mimeType == "application/pdf" || file.mimeType.startsWith("text/") ||
        file.mimeType.contains("document") -> DownloadType.DOCUMENT
    else -> DownloadType.OTHER
}
private fun bytesLabel(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes.toDouble() / 1024 / 1024 / 1024)
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes.toDouble() / 1024 / 1024)
    bytes >= 1024 -> "%.1f KB".format(bytes.toDouble() / 1024)
    else -> "$bytes B"
}

/** Personal download library. Every count, byte value and item comes from real state. */
@Composable
internal fun DownloadsScreen(
    files: List<SavedDownload>, active: ActiveDownload?, queued: List<QueuedDownload>,
    dayMode: Boolean, visible: Boolean,
    onBack: () -> Unit, onOpen: (SavedDownload) -> Unit,
    onCancelActive: () -> Unit, onCancelQueued: (Long) -> Unit, onClear: () -> Unit
) {
    val p = browserScreenPalette(dayMode)
    var selected by remember(visible) { mutableStateOf(DownloadType.ALL) }
    var query by remember(visible) { mutableStateOf("") }
    var confirmClear by remember(visible) { mutableStateOf(false) }
    val filters = listOf(
        DownloadType.ALL to stringResource(R.string.download_filter_all),
        DownloadType.VIDEO to stringResource(R.string.download_filter_video),
        DownloadType.DOCUMENT to stringResource(R.string.download_filter_document),
        DownloadType.OTHER to stringResource(R.string.download_filter_other)
    )
    val visibleFiles = files.filter {
        (selected == DownloadType.ALL || fileGroup(it) == selected) &&
            (query.isBlank() || it.name.contains(query.trim(), ignoreCase = true))
    }
    Column(Modifier.fillMaxSize().screenBackground(p).padding(T.telefon_ekran_kenar.dp),
        verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
        ScreenHeader(stringResource(R.string.downloads), stringResource(R.string.download_library_subtitle), p, onBack)
        ScreenSearch(query, stringResource(R.string.download_search_hint), p) { query = it }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
            filters.forEach { (type, label) ->
                val chosen = selected == type
                Box(Modifier.heightIn(min = T.telefon_dokunma_enAz.dp)
                    .background(if (chosen) p.accent else p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))
                    .semantics { this.selected = chosen }
                    .clickable(role = Role.Tab) { selected = type }
                    .padding(horizontal = T.telefon_ekran_kenar.dp, vertical = T.telefon_ekran_kucukBosluk.dp),
                    contentAlignment = Alignment.Center) {
                    ScreenText(label, if (chosen) p.onAccent else p.muted, strong = chosen)
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
                listOf(
                    (if (active == null) 0 else 1) to stringResource(R.string.download_summary_active),
                    queued.size to stringResource(R.string.download_summary_queue),
                    files.size to stringResource(R.string.download_summary_saved)
                ).forEach { (count, label) ->
                    Column(Modifier.weight(1f).background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
                        .padding(T.telefon_ekran_bosluk.dp)) {
                        ScreenText(count.toString(), p.accent, T.telefon_yazi_sayfaBasligi, strong = true)
                        ScreenText(label, p.muted, T.telefon_yazi_ikincil)
                    }
                }
            }
            if (active != null) {
                val percent = DownloadProgressMath.percent(active.bytesWritten, active.totalBytes)
                Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))
                    .border(T.telefon_ekran_cizgi.dp, p.line, RoundedCornerShape(T.telefon_kose_panel.dp))
                    .padding(T.telefon_ekran_kenar.dp),
                    verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        EymenVector(EymenPaths.indir, color = p.accent, iconSize = T.telefon_ekran_simge.dp)
                        Spacer(Modifier.width(T.telefon_ekran_kucukBosluk.dp))
                        ScreenText(stringResource(R.string.download_active), p.accent, strong = true)
                    }
                    ScreenText(active.name, p.text, T.telefon_yazi_bolumBasligi, strong = true)
                    ScreenText(bytesLabel(active.bytesWritten) + if (active.totalBytes != null)
                        " / " + bytesLabel(active.totalBytes) + " · " + (percent?.toString() ?: "—") + "%"
                        else " · " + stringResource(R.string.download_size_unknown), p.muted, T.telefon_yazi_ikincil)
                    if (percent != null) {
                        Box(Modifier.fillMaxWidth().height(T.telefon_ekran_ilerleme.dp)
                            .background(p.soft, RoundedCornerShape(T.telefon_kose_kucuk.dp))) {
                            Box(Modifier.fillMaxWidth(percent / 100f).fillMaxHeight()
                                .background(p.accent, RoundedCornerShape(T.telefon_kose_kucuk.dp)))
                        }
                    }
                    ScreenButton(stringResource(if (active.cancelling) R.string.download_cancelling
                        else R.string.download_cancel_active), p, onCancelActive, enabled = !active.cancelling)
                }
            }
            queued.forEach { item ->
                Row(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
                    .padding(T.telefon_ekran_bosluk.dp), verticalAlignment = Alignment.CenterVertically) {
                    EymenVector(EymenPaths.gecmis, color = p.muted, iconSize = T.telefon_ekran_simge.dp)
                    Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
                    Column(Modifier.weight(1f)) {
                        ScreenText(item.name, p.text, strong = true)
                        ScreenText(stringResource(R.string.download_queued), p.muted, T.telefon_yazi_ikincil)
                    }
                    ScreenIconAction(stringResource(R.string.download_cancel_named, item.name),
                        EymenPaths.temizle, p, { onCancelQueued(item.id) })
                }
            }
            if (visibleFiles.isEmpty()) {
                Column(Modifier.fillMaxWidth().padding(vertical = T.telefon_ekran_kenar.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
                    Box(Modifier.size(T.telefon_dokunma_standart.dp * 2)
                        .background(p.soft, RoundedCornerShape(T.telefon_kose_panel.dp)),
                        contentAlignment = Alignment.Center) {
                        EymenVector(EymenPaths.klasor, color = p.accent, iconSize = T.telefon_dokunma_standart.dp)
                    }
                    ScreenText(stringResource(if (query.isNotBlank()) R.string.download_no_matches
                        else R.string.download_none), p.text, T.telefon_yazi_bolumBasligi, strong = true)
                    ScreenText(stringResource(R.string.download_library_empty_hint), p.muted)
                }
            }
            visibleFiles.forEach { file ->
                Row(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
                    .clickable(role = Role.Button) { onOpen(file) }
                    .padding(T.telefon_ekran_bosluk.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(T.telefon_dokunma_standart.dp)
                        .background(p.soft, RoundedCornerShape(T.telefon_kose_kucuk.dp)),
                        contentAlignment = Alignment.Center) {
                        EymenVector(if (fileGroup(file) == DownloadType.VIDEO) EymenPaths.video else EymenPaths.belge,
                            color = p.accent, iconSize = T.telefon_ekran_simge.dp)
                    }
                    Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
                    Column(Modifier.weight(1f)) {
                        ScreenText(file.name, p.text, strong = true)
                        val date = if (file.savedAt > 0) DateFormat.getDateTimeInstance(
                            DateFormat.SHORT, DateFormat.SHORT).format(Date(file.savedAt)) else ""
                        ScreenText(bytesLabel(file.bytes) + " · " + date, p.muted, T.telefon_yazi_ikincil)
                    }
                    EymenVector(EymenPaths.ileri, color = p.muted, iconSize = T.telefon_ekran_simge.dp)
                }
            }
            if (files.isNotEmpty()) {
                ScreenText(stringResource(R.string.download_recorded_size, bytesLabel(files.sumOf { it.bytes })),
                    p.muted, T.telefon_yazi_ikincil)
                ScreenButton(stringResource(R.string.download_clear_records), p, { confirmClear = true })
            }
        }
    }
    if (visible && confirmClear) Dialog(onDismissRequest = { confirmClear = false }) {
        Column(Modifier.fillMaxWidth().background(p.surface, RoundedCornerShape(T.telefon_kose_panel.dp))
            .padding(T.telefon_ekran_kenar.dp),
            verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_kenar.dp)) {
            ScreenText(stringResource(R.string.download_clear_records), p.text, T.telefon_yazi_bolumBasligi, strong = true)
            ScreenText(stringResource(R.string.download_clear_confirm), p.muted)
            ScreenButton(stringResource(R.string.download_clear_records), p,
                { confirmClear = false; onClear() }, primary = true)
            ScreenButton(stringResource(R.string.download_confirm_deny), p, { confirmClear = false })
        }
    }
}
