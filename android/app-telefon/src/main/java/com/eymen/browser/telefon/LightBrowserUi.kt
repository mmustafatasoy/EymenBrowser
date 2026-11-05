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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
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

/** Draw the original blue A site identity without replacing it with a screenshot. */
@Composable
internal fun AtasoySiteLogo(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val u = size.minDimension / 36f
        drawRoundRect(
            brush = Brush.linearGradient(
                listOf(Color(0xFF1983FF), Color(0xFF005BFF), Color(0xFF0751ED))
            ),
            cornerRadius = CornerRadius(9f * u)
        )
        val pt = { x: Float, y: Float -> Offset(x * u, y * u) }
        drawLine(Color.White, pt(10f, 27f), pt(18f, 8f),
            strokeWidth = 3.15f * u, cap = StrokeCap.Round)
        drawLine(Color.White, pt(18f, 8f), pt(26f, 27f),
            strokeWidth = 3.15f * u, cap = StrokeCap.Round)
        drawLine(Color.White, pt(12.9f, 20.3f), pt(23.1f, 20.3f),
            strokeWidth = 3.15f * u, cap = StrokeCap.Round)
    }
}

/** Multi-colour Google mark rendered as vectors, not the old single blue G. */
@Composable
private fun GoogleLetter(modifier: Modifier = Modifier) {
    Canvas(modifier.size(27.dp)) {
        val u = size.minDimension / 40f
        val tl = Offset(5f * u, 5f * u)
        val sz = Size(30f * u, 30f * u)
        val stroke = Stroke(width = 6.4f * u, cap = StrokeCap.Butt)
        drawArc(Color(0xFF4285F4), -39f, 80f, false, tl, sz, style = stroke)
        drawArc(Color(0xFF34A853), 41f, 85f, false, tl, sz, style = stroke)
        drawArc(Color(0xFFFBBC05), 126f, 73f, false, tl, sz, style = stroke)
        drawArc(Color(0xFFEA4335), 199f, 115f, false, tl, sz, style = stroke)
        drawLine(Color(0xFF4285F4), Offset(21f*u,20f*u),
            Offset(36f*u,20f*u), 6f*u, cap = StrokeCap.Square)
    }
}

@Composable
private fun MicrophoneIcon(modifier: Modifier = Modifier, color: Color = ink) {
    Canvas(modifier.size(24.dp)) {
        val u = size.minDimension / 28f
        drawRoundRect(color, Offset(11f*u, 3f*u), Size(6f*u, 14f*u),
            CornerRadius(3f*u))
        drawArc(color, 0f, 180f, false, Offset(6f*u,5f*u),
            Size(16f*u,17f*u), style = Stroke(2.7f*u, cap = StrokeCap.Round))
        drawLine(color, Offset(14f*u,22f*u), Offset(14f*u,26f*u),
            2.6f*u, cap = StrokeCap.Round)
        drawLine(color, Offset(9f*u,26f*u), Offset(19f*u,26f*u),
            2.6f*u, cap = StrokeCap.Round)
    }
}

@Composable
private fun LightIcon(
    label: String, icon: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, color: Color = ink
) {
    Box(
        modifier.size(40.dp)
            .semantics { contentDescription = label; role = Role.Button }
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        EymenVector(icon, color = color, iconSize = 24.dp)
    }
}

@Composable
private fun BrandShortcutIcon(label: String) {
    when (label) {
        "Google" -> GoogleLetter(Modifier.size(33.dp))
        "Facebook" -> Box(
            Modifier.size(33.dp).background(
                Color(0xFF1681F9), RoundedCornerShape(9.dp)
            ), contentAlignment = Alignment.Center
        ) { LightText("f", 30, Color.White, bold = true) }
        "X" -> LightText("𝕏", 32, Color.Black)
        else -> Canvas(Modifier.size(34.dp)) {
            val u = size.minDimension / 40f
            when (label) {
                "YouTube" -> {
                    drawRoundRect(Color(0xFFFF1025), Offset(1f*u,6f*u),
                        Size(38f*u,28f*u), CornerRadius(8f*u))
                    val play = Path().apply {
                        moveTo(16f*u,12f*u); lineTo(29f*u,20f*u)
                        lineTo(16f*u,28f*u); close()
                    }
                    drawPath(play, Color.White)
                }
                "Instagram" -> {
                    drawRoundRect(
                        Brush.linearGradient(listOf(
                            Color(0xFF6428FF), Color(0xFFDC1B83),
                            Color(0xFFFD8A1A), Color(0xFFFFCC34)
                        )),
                        Offset(2f*u,2f*u), Size(36f*u,36f*u), CornerRadius(10f*u)
                    )
                    drawRoundRect(Color.White, Offset(10f*u,10f*u),
                        Size(20f*u,20f*u), CornerRadius(6f*u),
                        style = Stroke(2.5f*u))
                    drawCircle(Color.White, 5.1f*u, Offset(20f*u,20f*u),
                        style = Stroke(2.4f*u))
                    drawCircle(Color.White, 1.8f*u, Offset(28f*u,12f*u))
                }
                "WhatsApp" -> {
                    drawRoundRect(Color(0xFF23CE64), Offset(1f*u,1f*u),
                        Size(38f*u,38f*u), CornerRadius(11f*u))
                    drawCircle(Color.White, 13.1f*u, Offset(20f*u,19f*u),
                        style = Stroke(2.35f*u))
                    val tail = Path().apply {
                        moveTo(9f*u,28f*u); lineTo(7f*u,35f*u)
                        lineTo(15f*u,31f*u); close()
                    }
                    drawPath(tail, Color.White)
                    drawLine(Color.White, Offset(14f*u,13f*u),
                        Offset(17f*u,20f*u), 3f*u, cap=StrokeCap.Round)
                    drawLine(Color.White, Offset(17f*u,20f*u),
                        Offset(26f*u,25f*u), 3f*u, cap=StrokeCap.Round)
                }
            }
        }
    }
}

/** Approved single top row: home, Google/address, working microphone, +, tabs, menu. */
@Composable
internal fun LightBrowserToolbar(
    currentUrl: String,
    atHome: Boolean,
    tabCount: Int,
    onGo: (String) -> Unit,
    onHome: () -> Unit,
    onVoiceSearch: () -> Unit,
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
            .background(Color(0xFFF3F7FD))
            .padding(start = 3.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        LightIcon("Ana sayfa", EymenPaths.ev, onHome, Modifier.size(36.dp))
        Row(
            Modifier.weight(1f).height(47.dp)
                .shadow(2.dp, RoundedCornerShape(26.dp))
                .border(1.dp, outline, RoundedCornerShape(26.dp))
                .background(Color(0xFFEAF1FB), RoundedCornerShape(26.dp))
                .padding(start = 9.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GoogleLetter(Modifier.size(23.dp))
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = TextStyle(color = ink, fontSize = 13.sp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onGo(input)
                    keyboard?.hide()
                }),
                decorationBox = { field ->
                    Box {
                        if (input.isBlank()) LightText("Arama yapın veya URL girin", 12, secondary)
                        field()
                    }
                }
            )
            Box(
                Modifier.size(31.dp)
                    .semantics { contentDescription = "Sesli arama"; role = Role.Button }
                    .clickable(onClick = onVoiceSearch),
                contentAlignment = Alignment.Center
            ) { MicrophoneIcon(Modifier.size(21.dp)) }
        }
        Box(
            Modifier.size(35.dp)
                .semantics { contentDescription = "Yeni sekme"; role = Role.Button }
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center
        ) { LightText("+", 30, ink) }
        Box(
            Modifier.size(36.dp)
                .semantics { contentDescription = "Sekmeler"; role = Role.Button }
                .clickable(onClick = onTabs),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(25.dp).border(1.8.dp, ink, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) { LightText(tabCount.toString(), 12, ink, bold = true) }
        }
        Box(
            Modifier.size(35.dp).background(Color(0xFFB1D6FF), RoundedCornerShape(13.dp))
                .semantics { contentDescription = "Tarayıcı menüsü"; role = Role.Button }
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) { LightText("⋮", 26, ink) }
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
@Composable
private fun CategoryArtwork(kind: String, color: Color) {
    when (kind) {
        "Android" -> Canvas(Modifier.size(33.dp)) {
            val u = size.minDimension / 40f
            drawArc(color, 180f, 180f, true, Offset(9f*u, 5f*u), Size(22f*u, 24f*u))
            drawRoundRect(color, Offset(9f*u, 18f*u), Size(22f*u, 15f*u),
                CornerRadius(2.5f*u))
            drawLine(color, Offset(12f*u, 10f*u), Offset(8f*u, 4f*u), 2f*u)
            drawLine(color, Offset(28f*u, 10f*u), Offset(32f*u, 4f*u), 2f*u)
            drawLine(color, Offset(5f*u, 21f*u), Offset(5f*u, 30f*u),
                4f*u, cap = StrokeCap.Round)
            drawLine(color, Offset(35f*u, 21f*u), Offset(35f*u, 30f*u),
                4f*u, cap = StrokeCap.Round)
            drawLine(color, Offset(14f*u, 32f*u), Offset(14f*u, 37f*u),
                4f*u, cap = StrokeCap.Round)
            drawLine(color, Offset(26f*u, 32f*u), Offset(26f*u, 37f*u),
                4f*u, cap = StrokeCap.Round)
            drawCircle(Color.White, 1.4f*u, Offset(15f*u, 16f*u))
            drawCircle(Color.White, 1.4f*u, Offset(25f*u, 16f*u))
        }
        "TV" -> EymenVector(EymenPaths.masaustu, color = color, iconSize = 33.dp)
        "Araç" -> Canvas(Modifier.size(33.dp)) {
            val u = size.minDimension / 40f
            drawRoundRect(color, Offset(4f*u, 17f*u), Size(32f*u, 13f*u),
                CornerRadius(4f*u))
            drawLine(color, Offset(11f*u, 17f*u), Offset(15f*u, 9f*u), 3f*u)
            drawLine(color, Offset(15f*u, 9f*u), Offset(27f*u, 9f*u), 3f*u)
            drawLine(color, Offset(27f*u, 9f*u), Offset(31f*u, 17f*u), 3f*u)
            drawCircle(color, 3.8f*u, Offset(12f*u, 31f*u))
            drawCircle(color, 3.8f*u, Offset(28f*u, 31f*u))
            drawLine(Color.White, Offset(8f*u, 23f*u), Offset(12f*u, 23f*u), 2f*u)
            drawLine(Color.White, Offset(28f*u, 23f*u), Offset(32f*u, 23f*u), 2f*u)
        }
        else -> LightText("⬡", 31, color, bold = true)
    }
}

private data class CategoryLink(
    val title: String, val symbol: String, val tile: Color, val symbolColor: Color, val url: String
)
private val categories = listOf(
    CategoryLink("Android\nUygulamaları", "Android", Color(0xFFF1E8FF), Color(0xFF9347E9),
        "https://play.google.com/store/apps"),
    CategoryLink("TV\nUygulamaları", "TV", Color(0xFFFFEAF0), Color(0xFFF3474D),
        "https://www.google.com/search?q=akilli+tv+uygulamalari"),
    CategoryLink("Araç\nUygulamaları", "Araç", Color(0xFFE1F0FF), Color(0xFF1675DE),
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
                        CategoryArtwork(category.symbol, category.symbolColor)
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
