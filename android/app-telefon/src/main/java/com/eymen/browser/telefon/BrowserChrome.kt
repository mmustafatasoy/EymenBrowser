package com.eymen.browser.telefon

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

/** T03/T04/T11 phone chrome. No mocked counters, synced tabs, or filter protection. */
private data class Palette(
    val base: Color, val panel: Color, val surface: Color, val border: Color,
    val text: Color, val muted: Color, val disabled: Color, val accent: Color,
    val accentText: Color, val protection: Color
)
private fun colors(day: Boolean) = if (day) Palette(
    T.gunduz_zemin, T.gunduz_yuzey1, T.gunduz_yuzey2, T.gunduz_cizgi,
    T.gunduz_yazi, T.gunduz_yaziIkincil, T.gunduz_yaziPasif,
    T.gunduz_vurgu, T.gunduz_vurguUstu, T.gunduz_koruma
) else Palette(
    T.gece_zemin, T.gece_yuzey1, T.gece_yuzey2, T.gece_cizgi,
    T.gece_yazi, T.gece_yaziIkincil, T.gece_yaziPasif,
    T.gece_vurgu, T.gece_vurguUstu, T.gece_koruma
)

/** Draws the exact icon path generated from design/tokens.json. */
@Composable
internal fun EymenVector(
    pathData: String, modifier: Modifier = Modifier,
    color: Color, iconSize: Dp = 24.dp
) {
    val path = remember(pathData) { PathParser().parsePathString(pathData).toPath() }
    Canvas(modifier.size(iconSize)) {
        val scale = size.minDimension / 24f
        withTransform({ scale(scale, scale, pivot = Offset.Zero) }) {
            drawPath(
                path, color,
                style = Stroke(width = 1.8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }
    }
}

@Composable
private fun ChromeText(
    text: String, size: Int, color: Color,
    modifier: Modifier = Modifier,
    weight: FontWeight = FontWeight.Normal,
    align: TextAlign = TextAlign.Start,
    maxLines: Int = 1
) {
    BasicText(
        text = text,
        modifier = modifier,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            fontSize = size.sp, lineHeight = (size * 1.3f).sp,
            fontWeight = weight, color = color, textAlign = align
        )
    )
}

private data class Shortcut(val name: Int, val letter: String, val url: String)

// The approved design defines labels, not destinations. These are explicit defaults,
// not user history or "recommendations".
private val shortcuts = listOf(
    Shortcut(R.string.shortcut_news, "H", "https://news.google.com/"),
    Shortcut(R.string.shortcut_mail, "P", "https://mail.google.com/"),
    Shortcut(R.string.shortcut_video, "V", "https://www.youtube.com/"),
    Shortcut(R.string.shortcut_music, "M", "https://music.youtube.com/"),
    Shortcut(R.string.shortcut_maps, "H", "https://www.google.com/maps/"),
    Shortcut(R.string.shortcut_bank, "B", "https://www.ziraatbank.com.tr/"),
    Shortcut(R.string.shortcut_dictionary, "S", "https://sozluk.gov.tr/"),
    Shortcut(R.string.shortcut_forum, "F", "https://www.reddit.com/")
)

@Composable
internal fun BrowserHome(
    day: Boolean,
    recentUrls: List<String>,
    addressBottom: Boolean,
    onOpen: (String) -> Unit,
    onDevices: () -> Unit,
    onShield: () -> Unit
) {
    val p = colors(day)
    Column(
        Modifier.fillMaxSize().background(p.base)
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp,
                top = if (addressBottom) 32.dp else 162.dp,
                bottom = if (addressBottom) 148.dp else 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                BrandMark(30, day)
                Spacer(Modifier.width(10.dp))
                ChromeText(
                    stringResource(R.string.onboarding_brand), 18, p.text,
                    weight = FontWeight.SemiBold
                )
            }
            Box(
                Modifier.size(48.dp).clickable { onDevices() },
                contentAlignment = Alignment.Center
            ) {
                EymenVector(EymenPaths.cihazlar, color = p.text)
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            shortcuts.chunked(4).forEach { group ->
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    group.forEach { s ->
                        Column(
                            Modifier.weight(1f).clickable { onOpen(s.url) },
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.size(64.dp)
                                    .background(
                                        if (day) p.panel else p.surface,
                                        RoundedCornerShape(18.dp)
                                    )
                                    .then(
                                        if (day) Modifier.border(
                                            1.dp, T.gunduz_yuzey3, RoundedCornerShape(18.dp)
                                        ) else Modifier
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                ChromeText(
                                    s.letter, 22, p.text, weight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(8.dp))
                            ChromeText(stringResource(s.name), 13, p.muted)
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ChromeText(
                stringResource(R.string.continue_browsing), 16, p.text,
                weight = FontWeight.SemiBold
            )
            if (recentUrls.isEmpty()) {
                ChromeText(
                    stringResource(R.string.no_recent_pages), 13, p.muted,
                    modifier = Modifier.padding(vertical = 14.dp)
                )
            } else {
                recentUrls.take(2).forEach { url ->
                    Row(
                        Modifier.fillMaxWidth().heightIn(min = 60.dp)
                            .clickable { onOpen(url) },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(44.dp).background(
                                p.panel, RoundedCornerShape(12.dp)
                            ), contentAlignment = Alignment.Center
                        ) {
                            EymenVector(
                                EymenPaths.gecmis, color = p.muted,
                                iconSize = 22.dp
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            ChromeText(
                                url.removePrefix("https://").substringBefore('/'),
                                15, p.text, weight = FontWeight.Medium
                            )
                            ChromeText(url, 13, p.muted)
                        }
                    }
                }
            }
        }

        Row(
            Modifier.fillMaxWidth()
                .background(p.panel, RoundedCornerShape(T.telefon_kose_kart.dp))
                .then(
                    if (day) Modifier.border(
                        1.dp, T.gunduz_yuzey3, RoundedCornerShape(T.telefon_kose_kart.dp)
                    ) else Modifier
                )
                .clickable(onClick = onShield)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EymenVector(
                EymenPaths.kalkan, color = p.protection, iconSize = 26.dp
            )
            Spacer(Modifier.width(12.dp))
            Column {
                ChromeText(
                    stringResource(R.string.shield_not_active), 15, p.text,
                    weight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(2.dp))
                ChromeText(stringResource(R.string.shield_planned), 13, p.muted)
            }
        }
    }
}

@Composable
private fun ToolbarButton(
    label: String, icon: String, color: Color,
    onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Box(
        modifier.height(48.dp).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        EymenVector(
            icon, modifier = Modifier, color = color, iconSize = 24.dp
        )
    }
}

@Composable
internal fun BrowserToolbar(
    currentUrl: String,
    home: Boolean,
    day: Boolean,
    addressBottom: Boolean,
    onGo: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onReload: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    onShield: () -> Unit
) {
    val p = colors(day)
    var input by remember(currentUrl, home) {
        mutableStateOf(if (home) "" else currentUrl)
    }
    val keyboard = LocalSoftwareKeyboardController.current
    val addressBox: @Composable () -> Unit = {
        Row(
            Modifier.fillMaxWidth().height(T.telefon_yukseklik_adresCubugu.dp)
                .background(
                    p.surface, RoundedCornerShape(T.telefon_kose_adresCubugu.dp)
                )
                .padding(start = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(32.dp).clickable { if (!home) onShield() },
                contentAlignment = Alignment.Center
            ) {
                EymenVector(
                    if (home) EymenPaths.ara else EymenPaths.kalkan,
                    color = if (home) p.muted else p.protection,
                    iconSize = 20.dp
                )
            }
            Spacer(Modifier.width(4.dp))
            BasicTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f).padding(vertical = 10.dp),
                textStyle = TextStyle(color = p.text, fontSize = 16.sp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    onGo(input)
                    keyboard?.hide()
                }),
                decorationBox = { field ->
                    Box {
                        if (input.isBlank()) {
                            ChromeText(
                                stringResource(R.string.address_hint), 16, p.muted
                            )
                        }
                        field()
                    }
                }
            )
            Box(
                Modifier.size(44.dp).clickable {
                    if (home) {
                        onGo(input)
                        keyboard?.hide()
                    } else onReload()
                }, contentAlignment = Alignment.Center
            ) {
                EymenVector(
                    if (home) EymenPaths.ara else EymenPaths.yenile,
                    color = p.muted, iconSize = 20.dp
                )
            }
        }
    }
    val nav: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth()) {
            ToolbarButton(
                stringResource(R.string.back),
                EymenPaths.geri, p.muted, onBack, Modifier.weight(1f)
            )
            ToolbarButton(
                stringResource(R.string.forward),
                EymenPaths.ileri, p.muted, onForward, Modifier.weight(1f)
            )
            ToolbarButton(
                stringResource(R.string.home),
                EymenPaths.ev, if (home) p.accent else p.text,
                onHome, Modifier.weight(1f)
            )
            Box(
                Modifier.weight(1f).height(48.dp).clickable(onClick = onTabs),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier.size(24.dp).border(
                        1.8.dp, p.text, RoundedCornerShape(7.dp)
                    ), contentAlignment = Alignment.Center
                ) {
                    ChromeText("1", 12, p.text, weight = FontWeight.SemiBold)
                }
            }
            ToolbarButton(
                stringResource(R.string.menu),
                EymenPaths.menu, p.text, onMenu, Modifier.weight(1f)
            )
        }
    }
    Column(
        Modifier.fillMaxWidth()
            .background(p.panel)
            .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 18.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (addressBottom) {
            addressBox()
            nav()
        } else {
            nav()
            addressBox()
        }
    }
}
