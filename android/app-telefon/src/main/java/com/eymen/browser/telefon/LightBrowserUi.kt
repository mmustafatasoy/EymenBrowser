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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
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


/** Real controls laid out to match the approved 5 November phone mockup. */
@Composable
internal fun LightBrowserHome(
    menuOpen: Boolean,
    recentUrls: List<String>,
    onSearch: (String) -> Unit,
    onOpen: (String) -> Unit,
    onVoiceSearch: () -> Unit
) {
    var input by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 650.dp
        // Capture the BoxWithConstraints maxWidth before entering nested
        // Column scopes; otherwise Kotlin's DSL receiver hides maxWidth.
        val brandShiftDp = if (menuOpen) maxWidth * -0.15f else 0.dp
        val topSpace = if (compact) 76.dp else 101.dp
        val blockGap = if (compact) 14.dp else 22.dp
        val brandSize = if (compact) 78.dp else 94.dp
        val tileHeight = if (compact) 68.dp else 77.dp
        val cardHeight = if (compact) 93.dp else 101.dp

        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(
                    Color(0xFFF9FBFF), Color(0xFFEEF5FF),
                    Color(0xFFF7FAFF), Color(0xFFF6FAFF)
                ))
            )
        ) {
            // Gentle upper/lower blue waves from the reference; decorative only.
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val upper = Path().apply {
                    moveTo(0f, h * .19f)
                    cubicTo(w*.14f,h*.25f,w*.22f,h*.36f,w*.42f,h*.32f)
                    cubicTo(w*.57f,h*.29f,w*.63f,h*.22f,w,h*.20f)
                    lineTo(w, h*.31f)
                    cubicTo(w*.65f,h*.33f,w*.52f,h*.41f,w*.35f,h*.39f)
                    cubicTo(w*.18f,h*.38f,w*.09f,h*.29f,0f,h*.28f)
                    close()
                }
                drawPath(upper, Brush.horizontalGradient(
                    listOf(Color(0x552B8BFA),Color(0x185FA8FF),Color(0x00116BFC))
                ))
                val lower = Path().apply {
                    moveTo(0f,h*.93f)
                    cubicTo(w*.22f,h*.88f,w*.47f,h*1.02f,w*.72f,h*.965f)
                    cubicTo(w*.86f,h*.94f,w*.91f,h*.92f,w,h*.925f)
                    lineTo(w,h);lineTo(0f,h);close()
                }
                drawPath(lower, Brush.verticalGradient(
                    listOf(Color(0xA5CCE2FF),Color(0xFFE2EFFF))
                ))
                val lower2 = Path().apply {
                    moveTo(0f,h*.965f)
                    cubicTo(w*.23f,h*.91f,w*.48f,h*1.035f,w*.73f,h*.975f)
                    cubicTo(w*.86f,h*.958f,w*.94f,h*.952f,w,h*.96f)
                    lineTo(w,h);lineTo(0f,h);close()
                }
                drawPath(lower2, Color(0x77C8E3FF))
            }
            Column(
                Modifier.fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 10.dp, end = 10.dp,
                        top = topSpace, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(blockGap)
            ) {
                Column(
                    // In the approved menu-open reference, the logo/title
                    // occupy the left ~70% of the screen, leaving clear
                    // visual space for the top-right popup.
                    Modifier.offset(x = brandShiftDp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AtasoySiteLogo(
                        Modifier.size(brandSize)
                            .shadow(9.dp, RoundedCornerShape(22.dp))
                    )
                    // The approved wordmark uses deep navy for Eymen and
                    // brighter electric blue for Browser (both italic).
                    BasicText(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = Color(0xFF102858))) {
                                append("Eymen ")
                            }
                            withStyle(SpanStyle(color = Color(0xFF1568F5))) {
                                append("Browser")
                            }
                        },
                        style = TextStyle(
                            fontSize = if (compact) 25.sp else 28.sp,
                            fontWeight = FontWeight.Bold,
                            fontStyle = FontStyle.Italic
                        ),
                        maxLines = 1
                    )
                    // A shallow curved electric-blue underline, not a
                    // rectangular strip (reference T03 home screen).
                    Canvas(Modifier.width(116.dp).height(8.dp)) {
                        val arc = Path().apply {
                            moveTo(0f, size.height * 0.83f)
                            quadraticBezierTo(
                                size.width * 0.5f, -size.height * 0.12f,
                                size.width, size.height * 0.83f
                            )
                        }
                        drawPath(
                            path = arc,
                            brush = Brush.horizontalGradient(
                                listOf(
                                    Color(0x00247EFF), Color(0xFF167DFF),
                                    Color(0xFF4D9BFF), Color(0x00247EFF)
                                )
                            ),
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }

                // Search and microphone in a single airy pill.
                Row(
                    // Keep the voice button visible to the LEFT of the popup.
                    Modifier
                        .fillMaxWidth(if (menuOpen) 0.59f else 1f)
                        .align(Alignment.Start)
                        .height(48.dp)
                        .shadow(4.dp, RoundedCornerShape(26.dp))
                        .border(1.dp, outline, RoundedCornerShape(26.dp))
                        .background(Color(0xFFFDFEFF), RoundedCornerShape(26.dp))
                        .padding(start = 15.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GoogleLetter(Modifier.size(29.dp))
                    Spacer(Modifier.width(10.dp))
                    BasicTextField(
                        value = input,
                        onValueChange = { input = it },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = TextStyle(fontSize = 15.sp, color = ink),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            onSearch(input); keyboard?.hide()
                        }),
                        decorationBox = { field ->
                            Box {
                                if (input.isBlank()) {
                                    LightText("Google'da arayın...", 14, secondary)
                                }
                                field()
                            }
                        }
                    )
                    Box(
                        Modifier.size(37.dp)
                            .semantics {
                                contentDescription = "Google sesli arama"; role = Role.Button
                            }
                            .clickable(onClick = onVoiceSearch),
                        contentAlignment = Alignment.Center
                    ) { MicrophoneIcon(Modifier.size(23.dp)) }
                }

                // Six tappable, brand-specific vector marks, not placeholder letters.
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    siteShortcuts.forEach { shortcut ->
                        Column(
                            Modifier.weight(1f).height(tileHeight)
                                .shadow(2.dp, RoundedCornerShape(16.dp))
                                .border(1.dp, Color(0xFFE0EAF9),
                                    RoundedCornerShape(16.dp))
                                .background(Color(0xFDFEFFFF),
                                    RoundedCornerShape(16.dp))
                                .clickable { onOpen(shortcut.url) }
                                .padding(top = 8.dp, bottom = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            BrandShortcutIcon(shortcut.label)
                            LightText(shortcut.label, if (compact) 9 else 10, ink)
                        }
                    }
                }

                // A single rounded category selector strip as in the mockup.
                Row(
                    Modifier.fillMaxWidth().height(36.dp)
                        .shadow(2.dp, RoundedCornerShape(24.dp))
                        .border(1.dp, outline, RoundedCornerShape(24.dp))
                        .background(Color(0xFFFDFEFF), RoundedCornerShape(24.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    browseChips.forEachIndexed { index, pair ->
                        Row(
                            Modifier.weight(if (index == 0) 1.13f else 1f)
                                .fillMaxHeight()
                                .background(
                                    if (index == 0) Color(0xFF1785F7)
                                    else Color.Transparent,
                                    RoundedCornerShape(20.dp)
                                )
                                .clickable {
                                    if (pair.second.isNotBlank()) onOpen(pair.second)
                                },
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val symbols = listOf("⌂", "♨", "♧", "▣", "⊞")
                            LightText(symbols[index], 14,
                                if (index == 0) Color.White else ink)
                            Spacer(Modifier.width(3.dp))
                            LightText(pair.first, if (compact) 9 else 10,
                                if (index == 0) Color.White else ink,
                                bold = index == 0)
                        }
                    }
                }

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categories.forEach { category ->
                        Column(
                            Modifier.weight(1f).height(cardHeight)
                                .shadow(2.dp, RoundedCornerShape(20.dp))
                                .border(2.dp, Color.White, RoundedCornerShape(20.dp))
                                .background(category.tile, RoundedCornerShape(20.dp))
                                .clickable { onOpen(category.url) }
                                .padding(horizontal = 2.dp, vertical = 10.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceAround
                        ) {
                            CategoryArtwork(category.symbol, category.symbolColor)
                            BasicText(
                                category.title,
                                style = TextStyle(
                                    color = ink,
                                    fontSize = if (compact) 10.sp else 11.sp,
                                    lineHeight = 13.sp,
                                    textAlign = TextAlign.Center
                                )
                            )
                        }
                    }
                }
                if (recentUrls.isNotEmpty()) {
                    Column(Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        LightText("Son ziyaret edilenler", 14, ink, bold = true)
                        recentUrls.take(2).forEach { url ->
                            Row(
                                Modifier.fillMaxWidth().heightIn(min = 42.dp)
                                    .background(Color.White, RoundedCornerShape(13.dp))
                                    .clickable { onOpen(url) }
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LightText(url.removePrefix("https://"),
                                    12, secondary, Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}



/** Device-independent menu symbols drawn as paths, not vendor font glyphs. */
@Composable
private fun ReferenceMenuIcon(symbol: String, enabled: Boolean) {
    val tint = if (enabled) ink else Color(0xFF9BA9BC)
    Canvas(Modifier.size(19.dp)) {
        val unit = size.minDimension / 24f
        val p = { x: Float, y: Float -> Offset(x * unit, y * unit) }
        val stroke = Stroke(
            width = 2.0f * unit,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        when (symbol) {
            "←" -> {
                drawLine(tint, p(20f, 12f), p(4f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(4f, 12f), p(10f, 6f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(4f, 12f), p(10f, 18f), stroke.width, StrokeCap.Round)
            }
            "→" -> {
                drawLine(tint, p(4f, 12f), p(20f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(20f, 12f), p(14f, 6f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(20f, 12f), p(14f, 18f), stroke.width, StrokeCap.Round)
            }
            "⟳" -> {
                drawArc(
                    tint, -40f, 305f, false,
                    p(4.5f, 4.5f), Size(15f * unit, 15f * unit),
                    style = stroke
                )
                drawLine(tint, p(20f, 7f), p(20f, 12f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(20f, 12f), p(15f, 11f), stroke.width, StrokeCap.Round)
            }
            "♧" -> {
                drawLine(tint, p(7f, 12f), p(17f, 6f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(7f, 12f), p(17f, 18f), stroke.width, StrokeCap.Round)
                drawCircle(Color.White, 2.5f * unit, p(5.5f, 12f))
                drawCircle(Color.White, 2.5f * unit, p(18f, 5f))
                drawCircle(Color.White, 2.5f * unit, p(18f, 19f))
                drawCircle(tint, 2.5f * unit, p(5.5f, 12f), style = stroke)
                drawCircle(tint, 2.5f * unit, p(18f, 5f), style = stroke)
                drawCircle(tint, 2.5f * unit, p(18f, 19f), style = stroke)
            }
            "☆" -> {
                val star = Path().apply {
                    moveTo(12f * unit, 2f * unit)
                    lineTo(15.2f * unit, 9f * unit)
                    lineTo(22f * unit, 9.7f * unit)
                    lineTo(16.8f * unit, 14.5f * unit)
                    lineTo(18.1f * unit, 22f * unit)
                    lineTo(12f * unit, 18.2f * unit)
                    lineTo(5.9f * unit, 22f * unit)
                    lineTo(7.2f * unit, 14.5f * unit)
                    lineTo(2f * unit, 9.7f * unit)
                    lineTo(8.8f * unit, 9f * unit)
                    close()
                }
                drawPath(star, tint, style = stroke)
            }
            "↓" -> {
                drawLine(tint, p(12f, 3f), p(12f, 16f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(7f, 11f), p(12f, 16f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(17f, 11f), p(12f, 16f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(4f, 19f), p(4f, 22f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(4f, 22f), p(20f, 22f), stroke.width, StrokeCap.Round)
                drawLine(tint, p(20f, 22f), p(20f, 19f), stroke.width, StrokeCap.Round)
            }
            "◎" -> {
                drawCircle(tint, 5f * unit, p(6.8f, 12f), style = stroke)
                drawCircle(tint, 5f * unit, p(17.2f, 12f), style = stroke)
                drawLine(tint, p(11.8f, 12f), p(12.2f, 12f), stroke.width)
                drawLine(tint, p(2f, 12f), p(1f, 9.5f), stroke.width)
                drawLine(tint, p(22f, 12f), p(23f, 9.5f), stroke.width)
                drawLine(tint, p(7.5f, 3f), p(16.5f, 3f), stroke.width)
            }
            "⚙" -> {
                drawCircle(tint, 7.5f * unit, p(12f, 12f), style = stroke)
                drawCircle(tint, 2.9f * unit, p(12f, 12f), style = stroke)
                val spokes = listOf(
                    floatArrayOf(12f, 1f, 12f, 4f),
                    floatArrayOf(12f, 20f, 12f, 23f),
                    floatArrayOf(1f, 12f, 4f, 12f),
                    floatArrayOf(20f, 12f, 23f, 12f),
                    floatArrayOf(4f, 4f, 6f, 6f),
                    floatArrayOf(18f, 18f, 20f, 20f),
                    floatArrayOf(4f, 20f, 6f, 18f),
                    floatArrayOf(18f, 6f, 20f, 4f)
                )
                spokes.forEach {
                    drawLine(tint, p(it[0], it[1]), p(it[2], it[3]),
                        stroke.width, StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun LightMenuRow(
    title: String, symbol: String, enabled: Boolean = true,
    highlighted: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth()
            .padding(horizontal = 5.dp, vertical = 0.dp)
            // 27dp is the compact visual reference. heightIn allows text
            // accessibility scaling without clipping on larger font settings.
            .heightIn(min = 27.dp)
            .background(
                if (highlighted) Color(0xFFE0EEFE) else Color.Transparent,
                RoundedCornerShape(12.dp)
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ReferenceMenuIcon(symbol, enabled)
        Spacer(Modifier.width(9.dp))
        LightText(title, 12, if (enabled) ink else Color(0xFF9BA9BC))
    }
}

/** Compact 05 Nov approved menu, located under the upper-right dots. */
@Composable
internal fun LightBrowserMenu(
    hasPage: Boolean,
    onDismiss: () -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onReload: () -> Unit,
    onAction: (MenuAction) -> Unit
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().clickable(onClick = onDismiss))
        Column(
            Modifier.align(Alignment.TopEnd)
                .padding(top = 57.dp, end = 6.dp)
                .width(148.dp)
                .heightIn(max = 440.dp)
                .shadow(10.dp, RoundedCornerShape(20.dp))
                .border(1.dp, Color(0xFFBED7F9), RoundedCornerShape(20.dp))
                .background(Color(0xFAFFFFFF), RoundedCornerShape(20.dp))
                .verticalScroll(rememberScrollState())
                .padding(vertical = 5.dp)
        ) {
            LightMenuRow("Geri", "←", highlighted = true) {
                onDismiss(); onBack()
            }
            LightMenuRow("İleri", "→", highlighted = true) {
                onDismiss(); onForward()
            }
            LightMenuRow("Yenile", "⟳", enabled = hasPage) {
                onDismiss(); onReload()
            }
            Box(Modifier.fillMaxWidth().padding(
                horizontal = 9.dp, vertical = 3.dp
            ).height(1.dp).background(outline))
            LightMenuRow("Paylaş", "♧", enabled = hasPage) {
                onAction(MenuAction.SHARE)
            }
            LightMenuRow("Yer İmleri", "☆") { onAction(MenuAction.BOOKMARKS) }
            LightMenuRow("İndirilenler", "↓") { onAction(MenuAction.DOWNLOADS) }
            Box(Modifier.fillMaxWidth().padding(
                horizontal = 9.dp, vertical = 3.dp
            ).height(1.dp).background(outline))
            LightMenuRow("Gizli Sekme", "◎") { onAction(MenuAction.PRIVATE) }
            LightMenuRow("Ayarlar", "⚙") { onAction(MenuAction.SETTINGS) }
        }
    }
}
