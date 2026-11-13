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
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
    onMenu: () -> Unit,
    dayMode: Boolean = true,
    homeButtonEnabled: Boolean = true,
    privateMode: Boolean = false,
    addressSuggestions: List<AddressSuggestion> = emptyList(),
    onEditingChanged: (Boolean) -> Unit = {}
) {
    val palette=browserListPalette(dayMode)
    val privateLabel=stringResource(R.string.private_tab)
    val toolbarInk=if(privateMode)T.gizli_yazi else if(dayMode)ink else palette.text
    val editor = rememberAddressEditor(if (atHome) "" else currentUrl)
    val input = editor.value
    val focus = LocalFocusManager.current
    val requester = remember { FocusRequester() }
    val addressLabel = stringResource(R.string.address_edit_label)
    val clearLabel = stringResource(R.string.address_clear)
    val voiceLabel = stringResource(R.string.voice_search)
    val newTabLabel = stringResource(R.string.new_tab)
    val tabsLabel = stringResource(R.string.tabs)
    val menuLabel = stringResource(R.string.browser_menu)
    val keyboard = LocalSoftwareKeyboardController.current
    val draft=input.text.trim()
    val addressAction=if(draft.contains("://") || ('.' in draft && draft.none { it.isWhitespace() })) ImeAction.Go else ImeAction.Search
    val submitAddress={
        val entered=editor.value.text
        focus.clearFocus()
        onGo(entered)
        keyboard?.hide()
    }
    Column(Modifier.fillMaxWidth()) {
    Row(
        Modifier.fillMaxWidth()
            .background(if(privateMode)T.gizli_yuzey1 else if(dayMode)Color(0xFFF3F7FD) else palette.background)
            .padding(start = 3.dp, end = 6.dp, top = 7.dp, bottom = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if(homeButtonEnabled)LightIcon(stringResource(R.string.home), EymenPaths.ev, onHome, Modifier.size(36.dp),color=toolbarInk)
        Row(
            Modifier.weight(1f).height(47.dp)
                .shadow(2.dp, RoundedCornerShape(26.dp))
                .border(1.dp, if(privateMode)T.gizli_yuzey2 else if(dayMode)outline else palette.surface, RoundedCornerShape(26.dp))
                .background(if(privateMode)T.gizli_yuzey2 else if(dayMode)Color(0xFFEAF1FB) else palette.surface, RoundedCornerShape(26.dp))
                .padding(start = 9.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(privateMode)Box(Modifier.size(23.dp).semantics{contentDescription=privateLabel}) {
                EymenVector(EymenPaths.gizli,color=toolbarInk,iconSize=23.dp)
            } else GoogleLetter(Modifier.size(23.dp))
            Spacer(Modifier.width(6.dp))
            BasicTextField(
                value = input,
                onValueChange = editor::update,
                modifier = Modifier.weight(1f).fillMaxHeight().focusRequester(requester)
                    .pointerInput(editor) {
                        // Observe the entire physical tap without consuming it.
                        // A normal finger tap lasts longer than one render frame.
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed=false,pass=PointerEventPass.Initial)
                            editor.pointerDown()
                            try {
                                var pressed=true
                                while(pressed)pressed=awaitPointerEvent(PointerEventPass.Final).changes.any { it.pressed }
                            } finally { editor.pointerUp() }
                        }
                    }
                    .onFocusChanged { editor.focusChanged(it.isFocused);onEditingChanged(it.isFocused) }
                    .semantics { contentDescription = addressLabel },
                singleLine = true,
                textStyle = TextStyle(color = toolbarInk, fontSize = T.telefon_liste_yazi.sp),
                cursorBrush = SolidColor(toolbarInk),
                keyboardOptions = KeyboardOptions(imeAction = addressAction),
                keyboardActions = KeyboardActions(onGo = {submitAddress()},onSearch = {submitAddress()},onDone = {submitAddress()}),
                decorationBox = { field ->
                    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.CenterStart) {
                        if (input.text.isBlank()) LightText(stringResource(R.string.address_hint), T.telefon_liste_yazi, if(privateMode)T.gizli_yaziIkincil else if(dayMode)secondary else palette.muted)
                        field()
                    }
                }
            )
            if (editor.focused && input.text.isNotEmpty()) {
                Box(Modifier.size(31.dp).semantics { contentDescription = clearLabel; role = Role.Button }
                    .clickable { editor.clear(); requester.requestFocus(); keyboard?.show() },
                    contentAlignment = Alignment.Center) {
                    EymenVector(EymenPaths.temizle, color = toolbarInk, iconSize = 21.dp)
                }
            } else {
            Box(
                Modifier.size(31.dp)
                    .semantics { contentDescription = voiceLabel; role = Role.Button }
                    .clickable(onClick = onVoiceSearch),
                contentAlignment = Alignment.Center
            ) { if(dayMode)MicrophoneIcon(Modifier.size(21.dp))
                else EymenVector(EymenPaths.mikrofon,color=toolbarInk,iconSize=21.dp) }
            }
        }
        Box(
            Modifier.size(35.dp)
                .semantics { contentDescription = newTabLabel; role = Role.Button }
                .clickable(onClick = onNewTab),
            contentAlignment = Alignment.Center
        ) { LightText("+", 30, toolbarInk) }
        Box(
            Modifier.size(36.dp)
                .semantics { contentDescription = tabsLabel; role = Role.Button }
                .clickable(onClick = onTabs),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier.size(25.dp).border(1.8.dp, toolbarInk, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) { LightText(tabCount.toString(), 12, toolbarInk, bold = true) }
        }
        Box(
            Modifier.size(35.dp).background(if(privateMode)T.gizli_yuzey2 else if(dayMode)Color(0xFFB1D6FF) else palette.surface, RoundedCornerShape(13.dp))
                .semantics { contentDescription = menuLabel; role = Role.Button }
                .clickable(onClick = onMenu),
            contentAlignment = Alignment.Center
        ) { LightText("⋮", 26, toolbarInk) }
    }
    val suggested=if(editor.focused)AddressSuggestionPolicy.find(draft,addressSuggestions,privateMode) else emptyList()
    if(suggested.isNotEmpty()) {
        Column(Modifier.fillMaxWidth().background(palette.background)
            .heightIn(max=T.telefon_liste_satir.dp*3).verticalScroll(rememberScrollState())) {
            suggested.forEach {item ->
                val suggestionLabel=stringResource(R.string.address_suggestion_action,item.title)
                SettingsActionRow(item.title,palette,{
                    focus.clearFocus();keyboard?.hide();onGo(item.url)
                },modifier=Modifier.semantics {contentDescription=suggestionLabel},note=item.url,icon=EymenPaths.ara)
            }
        }
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
    val title: Int, val symbol: String, val tile: Color, val symbolColor: Color, val url: String
)
private val categories = listOf(
    CategoryLink(R.string.home_android, "Android", Color(0xFFF1E8FF), Color(0xFF9347E9),
        "https://play.google.com/store/apps"),
    CategoryLink(R.string.home_tv, "TV", Color(0xFFFFEAF0), Color(0xFFF3474D),
        "https://www.google.com/search?q=akilli+tv+uygulamalari"),
    CategoryLink(R.string.home_car, "Araç", Color(0xFFE1F0FF), Color(0xFF1675DE),
        "https://www.google.com/search?q=android+auto+uygulamalari"),
    CategoryLink(R.string.home_custom, "⬡", Color(0xFFDFF7F1), Color(0xFF11B98B),
        "https://www.google.com/search?q=yazilim+uygulamalari")
)
private val browseChips = listOf(
    R.string.home_recommended to "",
    R.string.home_news to "https://news.google.com/",
    R.string.home_games to "https://play.google.com/store/games",
    R.string.home_live to "https://www.google.com/search?q=canli+tv",
    R.string.home_apps to "https://play.google.com/store/apps"
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
    val voiceLabel = stringResource(R.string.voice_search)
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compact = maxHeight < 650.dp
        // Capture the BoxWithConstraints maxWidth before entering nested
        // Column scopes; otherwise Kotlin's DSL receiver hides maxWidth.
        val brandShiftDp = 0.dp
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
                        .fillMaxWidth()
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
                                    LightText(stringResource(R.string.address_edit_label), 14, secondary)
                                }
                                field()
                            }
                        }
                    )
                    Box(
                        Modifier.size(37.dp)
                            .semantics {
                                contentDescription = voiceLabel; role = Role.Button
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
                            LightText(stringResource(pair.first), if (compact) 9 else 10,
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
                                stringResource(category.title),
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
                        LightText(stringResource(R.string.home_recent), 14, ink, bold = true)
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
