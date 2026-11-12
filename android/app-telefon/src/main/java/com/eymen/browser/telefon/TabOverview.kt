package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected as semanticSelected
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
    val selected: Boolean,
    val groupId: Long? = null,
    val thumbnail: ImageBitmap? = null
)

internal data class TabGroupPreview(val id: Long, val title: String)

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
    openDevices: Boolean,
    day: Boolean,
    searchEpoch: Int,
    groups: List<TabGroupPreview>,
    undoAvailable: Boolean,
    undoCount: Int,
    onOpen: (Long) -> Unit,
    onClose: (Long) -> Unit,
    onCloseAll: (Boolean) -> Unit,
    onNew: (Boolean, Long?) -> Unit,
    onNewGroup: () -> Unit,
    onRenameGroup: (Long) -> Unit,
    onRemoveGroup: (Long) -> Unit,
    onAssignGroup: (Long) -> Unit,
    onUndoClose: () -> Unit,
    onMoveTab: (Long, Int, Long?) -> Unit,
    onShareTab: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var section by remember { mutableStateOf(
        if (activePrivate) TabSection.PRIVATE else TabSection.NORMAL
    ) }
    LaunchedEffect(activePrivate, openDevices) {
        section = when {
            openDevices -> TabSection.DEVICES
            activePrivate -> TabSection.PRIVATE
            else -> TabSection.NORMAL
        }
    }
    // Normal groups are never applied to private sessions, nor to devices.
    var selectedGroupId by remember { mutableStateOf<Long?>(null) }
    // A close or privacy-context switch must not reuse an earlier search/IME value.
    var searchQuery by remember(searchEpoch, section) { mutableStateOf("") }
    val searchActive = TabSearchPolicy.query(searchQuery).isNotEmpty()
    LaunchedEffect(groups.map { it.id }) {
        if (selectedGroupId != null && groups.none { it.id == selectedGroupId }) {
            selectedGroupId = null
        }
    }
    val background = if (day) T.gunduz_zemin else T.gece_zemin
    val surface = if (day) T.gunduz_yuzey1 else T.gece_yuzey1
    val muted = if (day) T.gunduz_yaziIkincil else T.gece_yaziIkincil
    val text = if (day) T.gunduz_yazi else T.gece_yazi
    val selected = if (day) T.gunduz_vurgu else T.gece_vurgu
    val selectedText = if (day) T.gunduz_vurguUstu else T.gece_vurguUstu
    val outline = if (day) T.gunduz_cizgi else T.gece_cizgi
    val scopedTabs = tabs.filter { tab ->
        tab.privateMode == (section == TabSection.PRIVATE) &&
            (section != TabSection.NORMAL ||
                selectedGroupId == null || tab.groupId == selectedGroupId)
    }
    val visible = scopedTabs.filter { TabSearchPolicy.matches(it.title, it.url, searchQuery) }
    val activeIndex = visible.indexOfFirst { it.selected }
    val canSwitch = visible.isNotEmpty() && (visible.size > 1 || activeIndex < 0)
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }

    // MainActivity reserves statusBars/displayCutout for the ENTIRE
    // FrameLayout root. Do not add statusBarsPadding() here again: doubling
    // it would push T06 down an extra notch-height on cutout devices.
    Column(
        Modifier.fillMaxSize()
            .background(background)
            .padding(top = 12.dp)
    ) {
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
                            .semantics { semanticSelected = section == value }
                            .clickable(role = Role.Tab) { section = value },
                        contentAlignment = Alignment.Center
                    ) {
                        TabLabel(label, 14, if (section == value) text else muted, section == value)
                    }
                }
            }
            val dismissLabel = stringResource(R.string.back)
            Box(
                Modifier.width(44.dp).height(48.dp)
                    .semantics { contentDescription = dismissLabel }
                    .clickable(role = Role.Button, onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                TabLabel("×", 26, text)
            }
        }
        Spacer(Modifier.height(16.dp))

        if (section != TabSection.DEVICES) {
            Column(Modifier.fillMaxWidth().padding(horizontal = T.telefon_ekran_kenar.dp),
                verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)) {
                // Recreate the editor when switching privacy contexts; never transfer composition text.
                key(searchEpoch, section) {
                    ListSearch(searchQuery, stringResource(R.string.tabs_search_hint),
                        browserScreenPalette(day)) { searchQuery = TabSearchPolicy.boundedInput(it) }
                }
                if (searchActive) ScreenText(stringResource(R.string.tabs_search_results, visible.size),
                    muted, T.telefon_liste_ikincil)
            }
            Spacer(Modifier.height(T.telefon_ekran_kucukBosluk.dp))
        }

        if (section == TabSection.NORMAL) {
            // Real, editable groups of the currently open normal GeckoSessions.
            // The pre-existing "groups coming later" placeholder is removed.
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.height(37.dp)
                        .background(
                            if (selectedGroupId == null) selected else surface,
                            RoundedCornerShape(19.dp)
                        )
                        .clickable { selectedGroupId = null }
                        .padding(horizontal = 15.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TabLabel(stringResource(R.string.tab_all), 13,
                        if (selectedGroupId == null) selectedText else text,
                        selectedGroupId == null)
                }
                groups.forEach { group ->
                    val checked = selectedGroupId == group.id
                    Box(
                        Modifier.height(37.dp)
                            .background(
                                if (checked) selected else surface,
                                RoundedCornerShape(19.dp)
                            )
                            .clickable { selectedGroupId = group.id }
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        TabLabel(group.title.take(32), 13,
                            if (checked) selectedText else text, checked)
                    }
                }
                Box(
                    Modifier.height(37.dp)
                        .background(surface, RoundedCornerShape(19.dp))
                        .border(1.dp, outline, RoundedCornerShape(19.dp))
                        .clickable(onClick = onNewGroup)
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    TabLabel("+ " + stringResource(R.string.tab_group_new),
                        13, text, true)
                }
            }
            if (selectedGroup != null) {
                Row(
                    Modifier.fillMaxWidth().padding(
                        start = 18.dp, end = 18.dp, top = 7.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TabLabel(
                        stringResource(R.string.tab_group_rename), 12, muted,
                        modifier = Modifier.clickable {
                            onRenameGroup(selectedGroup.id)
                        }
                    )
                    TabLabel(
                        stringResource(R.string.tab_group_remove), 12, muted,
                        modifier = Modifier.clickable {
                            onRemoveGroup(selectedGroup.id)
                        }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        if (section != TabSection.DEVICES) {
            // Quick switching changes the actual active GeckoSession.
            Row(
                Modifier.fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TabLabel(
                    stringResource(R.string.tab_quick_switch) +
                        " ${if (activeIndex < 0) 0 else activeIndex + 1}/${visible.size}",
                    12, muted
                )
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    TabLabel(
                        "‹ " + stringResource(R.string.tab_prev),
                        13, if (canSwitch) text else muted,
                        modifier = Modifier.clickable(enabled = canSwitch) {
                            if (activeIndex < 0) onOpen(visible.last().id)
                            else onOpen(visible[(activeIndex - 1 + visible.size) % visible.size].id)
                        }
                    )
                    TabLabel(
                        stringResource(R.string.tab_next) + " ›",
                        13, if (canSwitch) text else muted,
                        modifier = Modifier.clickable(enabled = canSwitch) {
                            onOpen(visible[(activeIndex + 1) % visible.size].id)
                        }
                    )
                }
            }
            if (section == TabSection.NORMAL && undoAvailable) {
                Box(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                        .heightIn(min = 38.dp)
                        .background(surface, RoundedCornerShape(12.dp))
                        .clickable(onClick = onUndoClose)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    TabLabel(
                        "↶ " + stringResource(R.string.tab_closed_undo) +
                            " (" + undoCount + ")",
                        13, selected, true
                    )
                }
            }
        }

        if (section == TabSection.DEVICES) {
            val palette = browserScreenPalette(day)
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
                .padding(T.telefon_ekran_kenar.dp),
                verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
                ScreenText(stringResource(R.string.menu_devices_share_note), muted, maxLines = 6)
                ScreenText(stringResource(R.string.device_private_note), muted, maxLines = 3)
                val shareable = tabs.filter { !it.privateMode &&
                    com.eymen.browser.motor.GeckoMotorDenemesi.verifiedHttpsUrl(it.url) == it.url && it.url.isNotBlank() }
                if (shareable.isEmpty()) ScreenText(stringResource(R.string.device_empty), text)
                shareable.forEach { tab ->
                    ScreenButton(tab.title, palette, { onShareTab(tab.id) })
                }
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
                        if (searchActive) stringResource(R.string.tabs_search_empty)
                        else if (section == TabSection.PRIVATE)
                            stringResource(R.string.private_tabs_empty)
                        else stringResource(R.string.normal_tabs_empty),
                        16, text, true
                    )
                    Spacer(Modifier.height(8.dp))
                    TabLabel(stringResource(if (searchActive) R.string.tabs_search_empty_hint
                        else R.string.tab_add_hint), 14, muted)
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
                                    Row(
                                        Modifier.fillMaxWidth().heightIn(min = 30.dp)
                                            .padding(horizontal = 4.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val groupLabel = groups.firstOrNull {
                                            it.id == tab.groupId
                                        }?.title
                                        Box(
                                            Modifier.weight(1f)
                                                .clickable(enabled = !tab.privateMode) {
                                                    onAssignGroup(tab.id)
                                                },
                                            contentAlignment = Alignment.CenterStart
                                        ) {
                                            TabLabel(
                                                if (tab.privateMode)
                                                    stringResource(R.string.private_tab)
                                                else "▦ " + (groupLabel ?:
                                                    stringResource(R.string.tab_group_assign)),
                                                10, selected, true
                                            )
                                        }
                                        Box(
                                            Modifier.size(24.dp).clickable {
                                                onMoveTab(tab.id, -1,
                                                    if (section == TabSection.NORMAL)
                                                        selectedGroupId else null)
                                            }, contentAlignment = Alignment.Center
                                        ) {
                                            TabLabel("‹", 18, muted)
                                        }
                                        Box(
                                            Modifier.size(24.dp).clickable {
                                                onMoveTab(tab.id, +1,
                                                    if (section == TabSection.NORMAL)
                                                        selectedGroupId else null)
                                            }, contentAlignment = Alignment.Center
                                        ) {
                                            TabLabel("›", 18, muted)
                                        }
                                    }
                                    Box(
                                        Modifier.fillMaxSize()
                                            .background(
                                                if (day) T.gunduz_yuzey2 else T.gece_yuzey2,
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        if (!tab.privateMode && tab.thumbnail != null) {
                                            // Genuine Gecko pixels, held in memory only.
                                            // Never capture or persist a private thumbnail.
                                            Image(
                                                bitmap = tab.thumbnail,
                                                contentDescription = tab.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            // Neutral placeholders until a page is rendered.
                                            Column(
                                                Modifier.fillMaxSize().padding(10.dp),
                                                verticalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Box(
                                                    Modifier.fillMaxWidth().height(34.dp)
                                                        .background(
                                                            if (day) T.gunduz_yuzey3
                                                            else T.gece_yuzey3,
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
            Modifier.fillMaxWidth().background(surface)
                // Root window padding handles actual system navigation and
                // keyboard insets on every device; no double padding here.
                .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                Modifier.height(48.dp).clickable(
                    enabled = section != TabSection.DEVICES && selectedGroupId == null && !searchActive
                ) {
                    if (section != TabSection.DEVICES && selectedGroupId == null)
                        onCloseAll(section == TabSection.PRIVATE)
                }.padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                TabLabel(
                    stringResource(R.string.close_all_tabs), 15,
                    if (section == TabSection.DEVICES || selectedGroupId != null || searchActive) muted else text
                )
            }
            Box(
                Modifier.height(48.dp)
                    .background(selected, RoundedCornerShape(24.dp))
                    .clickable(enabled = section != TabSection.DEVICES) {
                        onNew(
                            section == TabSection.PRIVATE,
                            if (section == TabSection.NORMAL) selectedGroupId else null
                        )
                    }
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
