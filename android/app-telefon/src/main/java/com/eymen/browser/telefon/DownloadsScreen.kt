package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T
import java.text.DateFormat
import java.util.Date

private enum class DownloadType { ALL, VIDEO, DOCUMENT, OTHER }
private fun fileGroup(file: SavedDownload) = when {
    file.mimeType.startsWith("video/") -> DownloadType.VIDEO
    file.mimeType == "application/pdf" ||
        file.mimeType.startsWith("text/") ||
        file.mimeType.contains("document") -> DownloadType.DOCUMENT
    else -> DownloadType.OTHER
}

private fun bytesLabel(bytes: Long): String = when {
    bytes >= 1024L * 1024L * 1024L -> "%.1f GB".format(bytes.toDouble() / 1024 / 1024 / 1024)
    bytes >= 1024L * 1024L -> "%.1f MB".format(bytes.toDouble() / 1024 / 1024)
    bytes >= 1024 -> "%.1f KB".format(bytes.toDouble() / 1024)
    else -> "$bytes B"
}

@Composable
private fun DownloadText(
    value: String, size: Int, color: Color, modifier: Modifier = Modifier,
    strong: Boolean = false
) {
    BasicText(
        value, modifier = modifier, maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = size.sp, lineHeight = (size * 1.25f).sp, color = color,
            fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal
        )
    )
}

/** T08: only actual locally recorded completed downloads; no mocked progress. */
@Composable
internal fun DownloadsScreen(
    files: List<SavedDownload>,
    active: ActiveDownload?,
    queued: List<QueuedDownload>,
    onBack: () -> Unit,
    onOpen: (SavedDownload) -> Unit,
    onCancelActive: () -> Unit,
    onCancelQueued: (Long) -> Unit,
    onClear: () -> Unit
) {
    var selected by remember { mutableStateOf(DownloadType.ALL) }
    val filters = listOf(
        DownloadType.ALL to stringResource(R.string.download_filter_all),
        DownloadType.VIDEO to stringResource(R.string.download_filter_video),
        DownloadType.DOCUMENT to stringResource(R.string.download_filter_document),
        DownloadType.OTHER to stringResource(R.string.download_filter_other)
    )
    val visible = files.filter { selected == DownloadType.ALL || fileGroup(it) == selected }
    Column(
        Modifier.fillMaxSize().background(T.gece_zemin)
            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(48.dp).clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                EymenVector(EymenPaths.geri, color = T.gece_yazi)
            }
            DownloadText(stringResource(R.string.downloads), 24, T.gece_yazi, strong = true)
        }
        if (active != null) {
            val percent = DownloadProgressMath.percent(active.bytesWritten, active.totalBytes)
            Column(
                Modifier.fillMaxWidth()
                    .background(T.gece_yuzey1, RoundedCornerShape(16.dp))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                DownloadText(
                    stringResource(R.string.download_active), 13,
                    T.gece_yaziIkincil
                )
                DownloadText(active.name, 16, T.gece_yazi, strong = true)
                DownloadText(
                    bytesLabel(active.bytesWritten) + if (active.totalBytes != null)
                        " / " + bytesLabel(active.totalBytes) +
                            " (" + (percent?.toString() ?: "—") + "%)"
                    else " · " + stringResource(R.string.download_size_unknown),
                    13, T.gece_yaziIkincil
                )
                if (percent != null) {
                    Box(
                        Modifier.fillMaxWidth().height(7.dp)
                            .background(T.gece_yuzey3, RoundedCornerShape(6.dp))
                    ) {
                        Box(
                            Modifier.fillMaxWidth(percent / 100f).fillMaxHeight()
                                .background(T.gece_vurgu, RoundedCornerShape(6.dp))
                        )
                    }
                }
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 44.dp)
                        .border(1.dp, T.gece_cizgi, RoundedCornerShape(12.dp))
                        .clickable(enabled = !active.cancelling) { onCancelActive() },
                    contentAlignment = Alignment.Center
                ) {
                    DownloadText(
                        stringResource(
                            if (active.cancelling) R.string.download_cancelling
                            else R.string.download_cancel_active
                        ), 14,
                        if (active.cancelling) T.gece_yaziPasif else T.gece_yazi,
                        strong = true
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            filters.forEach { (type, label) ->
                Box(
                    Modifier.height(44.dp)
                        .background(
                            if (selected == type) T.gece_vurgu else Color.Transparent,
                            RoundedCornerShape(22.dp)
                        )
                        .then(
                            if (selected != type) Modifier.border(
                                1.dp, T.gece_cizgi, RoundedCornerShape(22.dp)
                            ) else Modifier
                        )
                        .clickable { selected = type }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    DownloadText(
                        label, 13,
                        if (selected == type) T.gece_vurguUstu else T.gece_yazi,
                        strong = selected == type
                    )
                }
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            queued.forEach { item ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(T.gece_yuzey1, RoundedCornerShape(16.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        DownloadText(item.name, 15, T.gece_yazi, strong = true)
                        DownloadText(stringResource(R.string.download_queued), 13, T.gece_yaziIkincil)
                    }
                    Box(
                        Modifier.heightIn(min = 44.dp)
                            .clickable { onCancelQueued(item.id) }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        DownloadText(stringResource(R.string.download_confirm_deny), 14, T.gece_yazi)
                    }
                }
            }
            if (visible.isEmpty()) {
                Spacer(Modifier.height(36.dp))
                DownloadText(
                    stringResource(R.string.download_none), 15, T.gece_yaziIkincil
                )
            }
            visible.forEach { file ->
                Row(
                    Modifier.fillMaxWidth()
                        .background(T.gece_yuzey1, RoundedCornerShape(16.dp))
                        .clickable { onOpen(file) }
                        .padding(start = 12.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(44.dp)
                            .background(T.gece_yuzey2, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        EymenVector(
                            if (fileGroup(file) == DownloadType.VIDEO)
                                EymenPaths.video else EymenPaths.belge,
                            color = T.gece_yaziIkincil, iconSize = 22.dp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        DownloadText(file.name, 15, T.gece_yazi, strong = true)
                        val date = if (file.savedAt > 0) {
                            DateFormat.getDateTimeInstance(
                                DateFormat.SHORT, DateFormat.SHORT
                            ).format(Date(file.savedAt))
                        } else ""
                        DownloadText(
                            bytesLabel(file.bytes) + " · " + date, 13,
                            T.gece_yaziIkincil
                        )
                    }
                    Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                        EymenVector(EymenPaths.ileri, color = T.gece_yazi, iconSize = 22.dp)
                    }
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DownloadText(
                    stringResource(R.string.download_folder), 13,
                    T.gece_yaziIkincil
                )
                DownloadText(
                    stringResource(
                        R.string.download_recorded_size,
                        bytesLabel(files.sumOf { it.bytes })
                    ), 13, T.gece_yaziIkincil
                )
            }
            Box(
                Modifier.fillMaxWidth().height(48.dp)
                    .border(1.dp, T.gece_cizgi, RoundedCornerShape(24.dp))
                    .clickable(enabled = files.isNotEmpty(), onClick = onClear),
                contentAlignment = Alignment.Center
            ) {
                DownloadText(
                    stringResource(R.string.download_clear_records), 15,
                    if (files.isEmpty()) T.gece_yaziPasif else T.gece_yazi,
                    strong = true
                )
            }
        }
    }
}
