package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal enum class MenuAction {
    BOOKMARKS, HISTORY, DOWNLOADS, PRIVATE, TRANSLATE, FIND,
    READER, DESKTOP, PDF, SHARE, SEND_TO_DEVICE, QR,
    EXTENSIONS, OPEN_EXTERNAL, OPEN_BROWSER, THEME, SETTINGS, TOOLS
}

private data class MenuTile(
    val title: Int,
    val icon: String,
    val action: MenuAction,
    val supported: Boolean = true
)

private val tiles = listOf(
    MenuTile(R.string.bookmarks, EymenPaths.yerImi, MenuAction.BOOKMARKS),
    MenuTile(R.string.history, EymenPaths.gecmis, MenuAction.HISTORY),
    MenuTile(R.string.downloads, EymenPaths.indir, MenuAction.DOWNLOADS),
    MenuTile(R.string.private_tab, EymenPaths.gizli, MenuAction.PRIVATE),
    MenuTile(R.string.menu_translate, EymenPaths.cevir, MenuAction.TRANSLATE),
    MenuTile(R.string.menu_find, EymenPaths.ara, MenuAction.FIND),
    MenuTile(R.string.menu_reader, EymenPaths.okuma, MenuAction.READER),
    MenuTile(R.string.menu_desktop, EymenPaths.masaustu, MenuAction.DESKTOP),
    MenuTile(R.string.menu_save_pdf, EymenPaths.belge, MenuAction.PDF),
    MenuTile(R.string.menu_share, EymenPaths.paylas, MenuAction.SHARE),
    MenuTile(R.string.menu_send_device, EymenPaths.cihazlar, MenuAction.SEND_TO_DEVICE),
    MenuTile(R.string.menu_scan_qr, EymenPaths.qr, MenuAction.QR)
)

/**
 * T07 approved 390x844 design: 4x3 actions, zoom row, three bottom settings.
 * Disabled controls are deliberately noninteractive until backed by a real engine.
 * Draws no fabricated extensions, download counts or sync status.
 */
@Composable
internal fun BrowserMenu(
    hasPage: Boolean,
    isDesktop: Boolean,
    dayMode: Boolean,
    textPercent: Int,
    onTextSize: (Int) -> Unit,
    onDismiss: () -> Unit,
    onAction: (MenuAction) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0x993E4350))) {
        val disabledText = stringResource(R.string.menu_requires_page)
        val dismissText = stringResource(R.string.menu_dismiss)
        Box(
            Modifier.fillMaxSize().clickable(onClick = onDismiss)
                .semantics { contentDescription = dismissText }
        )
        Column(
            Modifier.align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(628.dp)
                .background(
                    T.gece_yuzey1, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                )
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                Modifier.width(40.dp).height(4.dp).align(Alignment.CenterHorizontally)
                    .background(T.gece_cizgi, RoundedCornerShape(2.dp))
            )
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                tiles.chunked(4).forEach { row ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        row.forEach { tile ->
                            val itemTitle = stringResource(tile.title)
                            val allowed = tile.supported && (
                                tile.action !in listOf(
                                    MenuAction.FIND, MenuAction.DESKTOP, MenuAction.PDF, MenuAction.SHARE,
                                    MenuAction.TRANSLATE, MenuAction.READER, MenuAction.SEND_TO_DEVICE
                                ) || hasPage
                            )
                            Column(
                                Modifier.weight(1f)
                                    .heightIn(min = 84.dp)
                                    .clickable(enabled = allowed) {
                                        onAction(tile.action)
                                    }
                                    .semantics {
                                        contentDescription = if (allowed) itemTitle
                                            else itemTitle + " · " + disabledText
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    Modifier.size(52.dp).background(
                                        if (isDesktop && tile.action == MenuAction.DESKTOP)
                                            T.gece_yuzey3 else T.gece_yuzey2,
                                        RoundedCornerShape(16.dp)
                                    ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    EymenVector(
                                        tile.icon,
                                        color = if (allowed) T.gece_yazi else T.gece_yaziPasif
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                BasicText(
                                    stringResource(tile.title),
                                    style = TextStyle(
                                        color = if (allowed) T.gece_yazi else T.gece_yaziPasif,
                                        fontSize = 12.5.sp, lineHeight = 15.sp,
                                        textAlign = TextAlign.Center
                                    ),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Actual Gecko text scaling; pinch zoom remains the engine's gesture.
            Row(
                Modifier.fillMaxWidth().height(56.dp)
                    .background(T.gece_yuzey2, RoundedCornerShape(16.dp))
                    .padding(start = 16.dp, end = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MenuLabel(
                    stringResource(R.string.menu_text_size), 15, T.gece_yazi,
                    modifier = Modifier.weight(1f), medium = true
                )
                MenuLabel("$textPercent%", 12, T.gece_yaziIkincil,
                    modifier = Modifier.clickable { onTextSize(100) })
                Spacer(Modifier.width(10.dp))
                MenuZoomButton("−", textPercent > 50) { onTextSize(textPercent - 10) }
                Spacer(Modifier.width(6.dp))
                MenuZoomButton("+", textPercent < 300) { onTextSize(textPercent + 10) }
            }

            Column {
                MenuBottomRow(
                    R.string.menu_extensions, EymenPaths.eklenti
                ) { onAction(MenuAction.EXTENSIONS) }
                MenuBottomRow(
                    R.string.menu_open_official_app, EymenPaths.cihazlar,
                    stringResource(R.string.menu_open_official_app_hint),
                    enabled = hasPage
                ) { onAction(MenuAction.OPEN_EXTERNAL) }
                MenuBottomRow(
                    R.string.menu_open_other_browser, EymenPaths.cevir,
                    stringResource(R.string.menu_open_other_browser_hint),
                    enabled = hasPage
                ) { onAction(MenuAction.OPEN_BROWSER) }
                MenuBottomRow(
                    R.string.menu_theme, EymenPaths.ay,
                    if (dayMode) stringResource(R.string.theme_day)
                    else stringResource(R.string.theme_night)
                ) { onAction(MenuAction.THEME) }
                MenuBottomRow(R.string.settings, EymenPaths.ayarlar) {
                    onAction(MenuAction.SETTINGS)
                }
            }
        }
    }
}

@Composable
private fun MenuLabel(
    value: String,
    size: Int,
    color: Color,
    modifier: Modifier = Modifier,
    medium: Boolean = false
) {
    BasicText(
        value, modifier = modifier, maxLines = 2,
        style = TextStyle(
            color = color, fontSize = size.sp,
            fontWeight = if (medium) FontWeight.Medium else FontWeight.Normal,
            lineHeight = (size * 1.2).sp
        )
    )
}

@Composable
private fun MenuZoomButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp)
            .background(T.gece_yuzey3, RoundedCornerShape(12.dp))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        MenuLabel(label, 22, if (enabled) T.gece_yazi else T.gece_yaziPasif)
    }
}

@Composable
private fun MenuBottomRow(
    title: Int,
    icon: String,
    note: String = "",
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp)
            .clickable(enabled = enabled, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        EymenVector(icon, color = T.gece_yaziIkincil, iconSize = 22.dp)
        Spacer(Modifier.width(14.dp))
        MenuLabel(
            stringResource(title), 15,
            if (enabled) T.gece_yazi else T.gece_yaziPasif,
            modifier = Modifier.weight(1f), medium = true
        )
        MenuLabel(note, 13, T.gece_yaziIkincil)
    }
}
