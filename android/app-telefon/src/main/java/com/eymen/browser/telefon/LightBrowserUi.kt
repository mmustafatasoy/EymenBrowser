package com.eymen.browser.telefon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

// Approved 05 Nov light design. Drawn with actual Compose controls, not a
// screenshot wallpaper: URL entry, menu, page history and shortcuts work.
private val ink = Color(0xFF1B2945)
private val secondary = Color(0xFF5B6B85)
private val blue = Color(0xFF0B63ED)
private val outline = Color(0xFFD4E3F8)
private val surface = Color.White

@Composable
private fun LightText(
    content: String, size: Int, color: Color = ink, modifier: Modifier = Modifier,
    bold: Boolean = false, italic: Boolean = false, maxLines: Int = 1
) {
    BasicText(
        text = content,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = color, fontSize = size.sp,
            fontWeight = if (bold) FontWeight.SemiBold else FontWeight.Normal,
            fontStyle = if (italic) FontStyle.Italic else FontStyle.Normal,
            lineHeight = (size * 1.26f).sp
        )
    )
}

/** The exact mark geometry from atasoyyazilim.com/public/favicon.svg. */
@Composable
internal fun AtasoySiteLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val factor = size.minDimension / 36f
        drawRoundRect(
            color = Color(0xFF2F5BFF),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(9f * factor)
        )
        val cap = 2.6f * factor
        val p = { x: Float, y: Float -> Offset(x * factor, y * factor) }
        drawLine(Color.White, p(11f, 26f), p(18f, 9f), cap, cap = StrokeCap.Round)
        drawLine(Color.White, p(18f, 9f), p(25f, 26f), cap, cap = StrokeCap.Round)
        drawLine(Color.White, p(13.8f, 20f), p(22.2f, 20f), cap, cap = StrokeCap.Round)
    }
}

@Composable
private fun GoogleLetter() {
    // Text only; no external Google image or SDK is required.
    LightText("G", 24, Color(0xFF4285F4), bold = true)
}

@Composable
private fun LightIcon(
    label: String, icon: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, color: Color = ink
) {
    Box(
        modifier.size(42.dp)
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        EymenVector(icon, color = color, iconSize = 25.dp)
    }
}

/** Home + one editable URL/search pill + new tab + tab counter + 3-dot menu. */
@Composable
internal fun LightBrowserToolbar(
    currentUrl: String,
    atHome: Boolean,
    tabCount: Int,
    onGo: (String) -> Unit,
    onHome: () -> Unit,
    onNewTab: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit
) {
    var input by remember(currentUrl, atHome) {
        mutableStateOf(if (atHome) "" else currentUrl)
    }
    val keyboard = LocalSoftwareKeyboardController.current
    Row(
        Modifier.fillMaxWidth()
            .background(Color(0xFFF5F8FE))
            .padding(start = 4.dp, end = 4.dp, top = 9.dp, bottom = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        LightIcon("Ana sayfa", EymenPaths.ev, onHome, Modifier.size(40.dp))
        Row(
            Modifier.weight(1f).height(50.dp)
                .border(1.dp, outline, RoundedCornerShape(26.dp))
                .background(Color(0xFFECF3FD), RoundedCornerShape(26.dp))
                .padding(start = 12.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoogleLetter()
            Spacer(Modifier.width(8.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = ink, fontSize = 14.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onGo(input)
                    keyboard?.hide()
                }),
                decorationBox = { field ->
                    Box {
                        if (input.isBlank()) {
                            LightText("Arama veya URL girin", 13, secondary)
                        }
                        field()
                    }
                }
            )
        }
        Box(
            Modifier.size(40.dp)
                .semantics { contentDescription = "Yeni sekme"; role = Role.Button }
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center
        ) {
            LightText("+", 31, ink)
        }
        Box(
            Modifier.size(40.dp)
                .semantics { contentDescription = "Sekmeler"; role = Role.Button }
                .clickable(onClick = onTabs),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(25.dp)
                    .border(1.9.dp, ink, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                LightText(tabCount.toString(), 12, ink, bold = true)
            }
        }
        Box(
            Modifier.size(40.dp)
                .background(Color(0xFFDDEBFF), RoundedCornerShape(15.dp))
                .semantics { contentDescription = "Tarayıcı menüsü"; role = Role.Button }
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) {
            LightText("⋮", 26, ink)
        }
    }
}

private data class LightShortcut(
    val label: String, val symbol: String, val color: Color, val url: String
)
private val siteShortcuts = listOf(
    LightShortcut("YouTube", "▶", Color(0xFFEE1D27), "https://www.youtube.com/"),
    LightShortcut("Google", "G", Color(0xFF4285F4), "https://www.google.com/"),
    LightShortcut("Facebook", "f", Color(0xFF1877F2), "https://www.facebook.com/"),
    LightShortcut("X", "X", Color(0xFF131922), "https://x.com/"),
    LightShortcut("Instagram", "◎", Color(0xFFC13584), "https://www.instagram.com/"),
    LightShortcut("WhatsApp", "☎", Color(0xFF16BD5B), "https://web.whatsapp.com/")
)
private data class CategoryLink(
    val title: String, val symbol: String, val tile: Color, val symbolColor: Color, val url: String
)
private val categories = listOf(
    CategoryLink("Android\nUygulamaları", "♟", Color(0xFFF1E8FF), Color(0xFF9347E9),
        "https://play.google.com/store/apps"),
    CategoryLink("TV\nUygulamaları", "▣", Color(0xFFFFEAF0), Color(0xFFF3474D),
        "https://www.google.com/search?q=akilli+tv+uygulamalari"),
    CategoryLink("Araç\nUygulamaları", "▰", Color(0xFFE1F0FF), Color(0xFF1675DE),
        "https://www.google.com/search?q=android+auto+uygulamalari"),
    CategoryLink("Özel\nYazılımlar", "⬡", Color(0xFFDFF7F1), Color(0xFF11B98B),
        "https://www.google.com/search?q=yazilim+uygulamalari")
)
private val browseChips = listOf(
    "Önerilen" to "",
    "Haberler" to "https://news.google.com/",
    "Oyunlar" to "https://play.google.com/store/games",
    "TV / Canlı" to "https://www.google.com/search?q=canli+tv",
    "Uygulamalar" to "https://play.google.com/store/apps"
)

@Composable
internal fun LightBrowserHome(
    recentUrls: List<String>,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit
) {
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    Box(
        Modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF6F9FF), Color(0xFFEEF5FF), Color(0xFFF9FBFF))
                )
            )
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(start = 12.dp, end = 12.dp, top = 108.dp, bottom = 30.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Column(
                Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AtasoySiteLogo(Modifier.size(112.dp).shadow(9.dp, RoundedCornerShape(26.dp)))
                Spacer(Modifier.height(16.dp))
                LightText("Eymen Browser", 30, Color(0xFF174CA7), bold = true, italic = true)
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier.width(124.dp).height(3.dp)
                        .background(Color(0xFF4896FD), RoundedCornerShape(2.dp))
                )
            }
            Row(
                Modifier.fillMaxWidth().height(58.dp)
                    .shadow(4.dp, RoundedCornerShape(29.dp))
                    .border(1.dp, outline, RoundedCornerShape(29.dp))
                    .background(surface, RoundedCornerShape(29.dp))
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GoogleLetter()
                Spacer(Modifier.width(12.dp))
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 16.sp, color = ink),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        onSearch(input)
                        keyboard?.hide()
                    }),
                    decorationBox = { field ->
                        Box {
                            if (input.isBlank()) LightText("Google'da arayın...", 16, secondary)
                            field()
                        }
                    }
                )
                LightIcon("Ara", EymenPaths.ara, onClick = {
                    onSearch(input)
                    keyboard?.hide()
                }, Modifier.size(40.dp))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                siteShortcuts.forEach { shortcut ->
                    Column(
                        Modifier.weight(1f).heightIn(min = 91.dp)
                            .border(1.dp, outline, RoundedCornerShape(18.dp))
                            .background(surface, RoundedCornerShape(18.dp))
                            .clickable { onOpen(shortcut.url) }
                            .padding(top = 10.dp, bottom = 8.dp, start = 2.dp, end = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            Modifier.size(37.dp)
                                .background(shortcut.color, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            LightText(shortcut.symbol, 23, Color.White, bold = true)
                        }
                        Spacer(Modifier.height(7.dp))
                        LightText(shortcut.label, 10, ink)
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                browseChips.forEachIndexed { index, pair ->
                    Box(
                        Modifier.height(41.dp)
                            .background(
                                if (index == 0) blue else Color.White,
                                RoundedCornerShape(22.dp)
                            )
                            .border(
                                1.dp,
                                if (index == 0) blue else outline,
                                RoundedCornerShape(22.dp)
                            )
                            .clickable {
                                if (pair.second.isNotEmpty()) onOpen(pair.second)
                            }
                            .padding(horizontal = 13.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        LightText(
                            pair.first, 13,
                            if (index == 0) Color.White else ink,
                            bold = index == 0
                        )
                    }
                }
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                categories.forEach { category ->
                    Column(
                        Modifier.weight(1f).height(123.dp)
                            .border(1.dp, outline, RoundedCornerShape(20.dp))
                            .background(category.tile, RoundedCornerShape(20.dp))
                            .clickable { onOpen(category.url) }
                            .padding(horizontal = 3.dp, vertical = 13.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        LightText(category.symbol, 30, category.symbolColor, bold = true)
                        Spacer(Modifier.height(9.dp))
                        BasicText(
                            category.title,
                            style = TextStyle(
                                color = ink, fontSize = 11.5.sp,
                                lineHeight = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        )
                    }
                }
            }
            // Real browsing history only, no fabricated sponsor/promo panel.
            if (recentUrls.isNotEmpty()) {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    LightText("Son ziyaret edilenler", 16, ink, bold = true)
                    recentUrls.take(2).forEach { url ->
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 45.dp)
                                .background(Color.White, RoundedCornerShape(13.dp))
                                .clickable { onOpen(url) }
                                .padding(horizontal = 12.dp, vertical = 9.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LightText(
                                url.removePrefix("https://"), 13, secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LightMenuRow(
    title: String, symbol: String, enabled: Boolean = true,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth().height(50.dp)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LightText(symbol, 22, if (enabled) ink else Color(0xFFA6B0C0), bold = true)
        Spacer(Modifier.width(18.dp))
        LightText(title, 16, if (enabled) ink else Color(0xFFA6B0C0))
    }
}

/** A real top-right three-dot popup, with browser Back/Forward moved here. */
@Composable
internal fun LightBrowserMenu(
    hasPage: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onAction: (MenuAction) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Color(0x250F172A))) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        Column(
            Modifier.align(Alignment.TopEnd)
                .padding(top = 68.dp, end = 8.dp)
                .width(250.dp)
                .heightIn(max = 520.dp)
                .shadow(9.dp, RoundedCornerShape(23.dp))
                .border(1.dp, outline, RoundedCornerShape(23.dp))
                .background(Color.White, RoundedCornerShape(23.dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 8.dp)
        ) {
            LightMenuRow("Geri", "←") { onDismiss(); onBack() }
            LightMenuRow("İleri", "→") { onDismiss(); onForward() }
            LightMenuRow("Yenile", "⟳", enabled = hasPage) { onDismiss(); onReload() }
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                    .height(1.dp).background(outline)
            )
            LightMenuRow("Paylaş", "◇", enabled = hasPage) {
                onAction(MenuAction.SHARE)
            }
            LightMenuRow("Yer İmleri", "☆") { onAction(MenuAction.BOOKMARKS) }
            LightMenuRow("İndirilenler", "↓") { onAction(MenuAction.DOWNLOADS) }
            Box(
                Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                    .height(1.dp).background(outline)
            )
            LightMenuRow("Gizli Sekme", "◉") { onAction(MenuAction.PRIVATE) }
            LightMenuRow("Görünüm", "◑") { onAction(MenuAction.THEME) }
            LightMenuRow("Ayarlar", "⚙") { onAction(MenuAction.SETTINGS) }
        }
    }
}
