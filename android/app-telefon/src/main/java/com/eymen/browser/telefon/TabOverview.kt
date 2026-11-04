package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal data class TabPreview(
    val id: Long,
    val title: String,
    val url: String,
    val privateMode: Boolean,
    val selected: Boolean
)

private enum class TabSection { NORMAL, PRIVATE, DEVICES }

@Composable
private fun TabLabel(
    value: String,
    size: Int,
    color: Color,
    bold: Boolean = false,
    modifier: Modifier = Modifier
) {
    BasicText(
        value, modifier, maxLines = 2, overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color, fontSize = size.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            lineHeight = (size * 1.32).sp
        )
    )
}

/**
 * T06 grid and tabs. Cards correspond to real GeckoSessions, not fake
 * browsing history or synthetic screenshots. Thumbnails are neutral placeholders
 * until the screenshot pipeline exists.
 */
@Composable
internal fun TabOverview(
    tabs: List<TabPreview>,
    activePrivate: Boolean,
    day: Boolean,
    onOpen: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onCloseAll: (Boolean) -> Unit,
    onNew: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var section by remember { mutableStateOf(
        if (activePrivate) TabSection.PRIVATE else TabSection.NORMAL
    ) }
    LaunchedEffect(activePrivate) {
        section = if (activePrivate) TabSection.PRIVATE else TabSection.NORMAL
    }
    val background = if (day) T.gunduz_zemin else T.gece_zemin
    val surface = if (day) T.gunduz_yuzey1 else T.gece_yuzey1
    val muted = if (day) T.gunduz_yaziIkincil else T.gece_yaziIkincil
    val text = if (day) T.gunduz_yazi else T.gece_yazi
    val selected = if (day) T.gunduz_vurgu else T.gece_vurgu
    val selectedText = if (day) T.gunduz_vurguUstu else T.gece_vurguUstu
    val outline = if (day) T.gunduz_cizgi else T.gece_cizgi
    val visible = tabs.filter { it.privateMode == (section == TabSection.PRIVATE) }

    Column(Modifier.fillMaxSize().background(background).padding(top = 24.dp)) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                Modifier.weight(1f)
                    .background(surface, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    TabSection.NORMAL to stringResource(R.string.tabs),
                    TabSection.PRIVATE to stringResource(R.string.private_tab),
                    TabSection.DEVICES to stringResource(R.string.devices)
                ).forEach { (value, label) ->
                    Box(
                        Modifier.weight(1f).height(40.dp)
                            .background(
                                if (section == value) {
                                    if (day) T.gunduz_yuzey3 else T.gece_yuzey3
                                } else Color.Transparent,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { section = value },
                        contentAlignment = Alignment.Center
                    ) {
                        TabLabel(label, 14, if (section == value) text else muted, section == value)
                    }
                }
            }
            Box(
                Modifier.width(44.dp).height(48.dp).clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                TabLabel("×", 26, text)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (section == TabSection.NORMAL) {
            Row(
                Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.height(44.dp)
                        .background(selected, RoundedCornerShape(22.dp))
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TabLabel(stringResource(R.string.tab_all), 14, selectedText, true)
                }
                Spacer(Modifier.width(8.dp))
                TabLabel(stringResource(R.string.tab_groups_pending), 13, muted)
            }
            Spacer(Modifier.height(16.dp))
        }

        if (section == TabSection.DEVICES) {
            Box(
                Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TabLabel(
                    stringResource(R.string.devices_not_ready), 15, muted,
                    modifier = Modifier.padding(24.dp)
                )
            }
        } else {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (visible.isEmpty()) {
                    Spacer(Modifier.height(72.dp))
                    TabLabel(
                        if (section == TabSection.PRIVATE)
                            stringResource(R.string.private_tabs_empty)
                        else stringResource(R.string.normal_tabs_empty),
                        16, text, true
                    )
                    Spacer(Modifier.height(8.dp))
                    TabLabel(stringResource(R.string.tab_add_hint), 14, muted)
                } else {
                    visible.chunked(2).forEach { pair ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            pair.forEach { tab ->
                                Column(
                                    Modifier.weight(1f).height(196.dp)
                                        .background(surface, RoundedCornerShape(16.dp))
                                        .then(
                                            if (tab.selected) Modifier.border(
                                                2.dp, selected, RoundedCornerShape(16.dp)
                                            ) else Modifier
                                        )
                                        .clickable { onOpen(tab.id) }
                                        .padding(8.dp)
                                ) {
                                    Row(
                                        Modifier.fillMaxWidth().height(36.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            Modifier.size(20.dp)
                                                .background(
                                                    if (day) T.gunduz_yuzey3 else T.gece_yuzey3,
                                                    RoundedCornerShape(6.dp)
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (tab.privateMode) {
                                                EymenVector(
                                                    EymenPaths.gizli, color = selected,
                                                    iconSize = 16.dp
                                                )
                                            } else {
                                                TabLabel(
                                                    tab.title.firstOrNull()?.uppercase() ?: "E",
                                                    12, text, true
                                                )
                                            }
                                        }
                                        Spacer(Modifier.width(6.dp))
                                        TabLabel(
                                            tab.title, 13, text,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Box(
                                            Modifier.size(32.dp)
                                                .clickable { onClose(tab.id) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            TabLabel("×", 20, muted)
                                        }
                                    }
                                    Column(
                                        Modifier.fillMaxSize()
                                            .background(
                                                if (day) T.gunduz_yuzey2 else T.gece_yuzey2,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            Modifier.fillMaxWidth().height(34.dp)
                                                .background(
                                                    if (day) T.gunduz_yuzey3 else T.gece_yuzey3,
                                                    RoundedCornerShape(4.dp)
                                                )
                                        )
                                        listOf(0.86f, 1f, 0.64f).forEach { width ->
                                            Box(
                                                Modifier.fillMaxWidth(width).height(6.dp)
                                                    .background(
                                                        outline, RoundedCornerShape(3.dp)
                                                    )
                                            )
                                        }
                                        TabLabel(tab.url, 11, muted)
                                    }
                                }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        Row(
            Modifier.fillMaxWidth().background(surface).padding(
                start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.height(48.dp).clickable {
                    if (section != TabSection.DEVICES) onCloseAll(section == TabSection.PRIVATE)
                }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                TabLabel(
                    stringResource(R.string.close_all_tabs), 15,
                    if (section == TabSection.DEVICES) muted else text
                )
            }
            Box(
                Modifier.height(48.dp)
                    .background(selected, RoundedCornerShape(24.dp))
                    .clickable { onNew(section == TabSection.PRIVATE) }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TabLabel("+", 20, selectedText, true)
                    Spacer(Modifier.width(6.dp))
                    TabLabel(
                        if (section == TabSection.PRIVATE)
                            stringResource(R.string.new_private_tab)
                        else stringResource(R.string.new_tab),
                        15, selectedText, true
                    )
                }
            }
        }
    }
}

/** T09 private start: labels follow approved design; no unimplemented lock toggle. */
@Composable
internal fun PrivateHome() {
    Column(
        Modifier.fillMaxSize().background(T.gizli_zemin)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 32.dp, bottom = 154.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Box(
            Modifier.size(64.dp)
                .background(T.gizli_yuzey2, RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            EymenVector(EymenPaths.gizli, color = T.gizli_vurgu, iconSize = 34.dp)
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TabLabel(
                stringResource(R.string.private_tab), 30, T.gizli_yazi, true
            )
            TabLabel(
                stringResource(R.string.private_intro), 15, T.gizli_yaziIkincil
            )
        }
        listOf(
            R.string.private_does_not_keep_title to R.string.private_does_not_keep_desc,
            R.string.private_still_visible_title to R.string.private_still_visible_desc
        ).forEach { (title, desc) ->
            Column(
                Modifier.fillMaxWidth()
                    .background(T.gizli_yuzey1, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TabLabel(stringResource(title), 15, T.gizli_yazi, true)
                BasicText(
                    stringResource(desc),
                    style = TextStyle(
                        color = T.gizli_yaziIkincil, fontSize = 14.sp,
                        lineHeight = 21.sp
                    )
                )
            }
        }
        TabLabel(
            stringResource(R.string.private_lock_unavailable),
            13, T.gizli_yaziIkincil
        )
    }
}
