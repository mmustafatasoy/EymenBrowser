package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

internal data class BrowserScreenPalette(
    val background: Color, val surface: Color, val soft: Color,
    val line: Color, val text: Color, val muted: Color, val disabled: Color,
    val accent: Color, val onAccent: Color, val protection: Color
)
internal fun browserScreenPalette(day: Boolean) = if (day) BrowserScreenPalette(
    T.gunduz_zemin, T.gunduz_yuzey1, T.gunduz_yuzey2, T.gunduz_cizgi,
    T.gunduz_yazi, T.gunduz_yaziIkincil, T.gunduz_yaziPasif,
    T.gunduz_vurgu, T.gunduz_vurguUstu, T.gunduz_koruma
) else BrowserScreenPalette(
    T.gece_zemin, T.gece_yuzey1, T.gece_yuzey2, T.gece_cizgi,
    T.gece_yazi, T.gece_yaziIkincil, T.gece_yaziPasif,
    T.gece_vurgu, T.gece_vurguUstu, T.gece_koruma
)

@Composable
internal fun ScreenText(
    text: String, color: Color, size: Int = T.telefon_yazi_govde,
    modifier: Modifier = Modifier, strong: Boolean = false, maxLines: Int = 2
) = BasicText(text, modifier, maxLines = maxLines, overflow = TextOverflow.Ellipsis,
    style = TextStyle(color = color, fontSize = size.sp, lineHeight = (size * 1.35f).sp,
        fontWeight = if (strong) FontWeight.SemiBold else FontWeight.Normal))

@Composable
internal fun ScreenHeader(title: String, subtitle: String, p: BrowserScreenPalette, onBack: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        ScreenIconAction(stringResource(R.string.back), EymenPaths.geri, p, onBack)
        Spacer(Modifier.width(T.telefon_ekran_kucukBosluk.dp))
        Column(Modifier.weight(1f)) {
            ScreenText(title, p.text, T.telefon_yazi_sayfaBasligi, strong = true)
            ScreenText(subtitle, p.muted, T.telefon_yazi_ikincil)
        }
        AtasoySiteLogo(Modifier.size(T.telefon_ekran_rozet.dp))
    }
}

@Composable
internal fun ScreenIconAction(label: String, path: String, p: BrowserScreenPalette,
    onClick: () -> Unit, enabled: Boolean = true) {
    Box(Modifier.size(T.telefon_dokunma_standart.dp)
        .background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
        .semantics { contentDescription = label }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        EymenVector(path, color = if (enabled) p.text else p.disabled,
            iconSize = T.telefon_ekran_simge.dp)
    }
}

@Composable
internal fun ScreenSearch(query: String, hint: String, p: BrowserScreenPalette,
    onQuery: (String) -> Unit) {
    val clearLabel = stringResource(R.string.search_clear)
    Row(Modifier.fillMaxWidth().height(T.telefon_dokunma_standart.dp)
        .background(p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
        .border(T.telefon_ekran_cizgi.dp, p.line, RoundedCornerShape(T.telefon_kose_kart.dp))
        .padding(horizontal = T.telefon_ekran_kenar.dp),
        verticalAlignment = Alignment.CenterVertically) {
        EymenVector(EymenPaths.ara, color = p.muted, iconSize = T.telefon_ekran_simge.dp)
        Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
        BasicTextField(query, onValueChange = onQuery,
            Modifier.weight(1f).semantics { contentDescription = hint }, singleLine = true,
            textStyle = TextStyle(color = p.text, fontSize = T.telefon_yazi_govde.sp),
            decorationBox = { field -> Box {
                if (query.isEmpty()) ScreenText(hint, p.muted)
                field()
            } })
        if (query.isNotEmpty()) {
            Box(Modifier.size(T.telefon_dokunma_enAz.dp)
                .semantics { contentDescription = clearLabel }
                .clickable(role = Role.Button) { onQuery("") }, contentAlignment = Alignment.Center) {
                EymenVector(EymenPaths.temizle, color = p.muted, iconSize = T.telefon_ekran_simge.dp)
            }
        }
    }
}

@Composable
internal fun ScreenButton(label: String, p: BrowserScreenPalette, onClick: () -> Unit,
    modifier: Modifier = Modifier, primary: Boolean = false, enabled: Boolean = true) {
    Box(modifier.fillMaxWidth().heightIn(min = T.telefon_dokunma_standart.dp)
        .background(if (primary) p.accent else p.surface, RoundedCornerShape(T.telefon_kose_kart.dp))
        .border(T.telefon_ekran_cizgi.dp, if (primary) p.accent else p.line,
            RoundedCornerShape(T.telefon_kose_kart.dp))
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .padding(T.telefon_ekran_bosluk.dp), contentAlignment = Alignment.Center) {
        ScreenText(label, if (!enabled) p.disabled else if (primary) p.onAccent else p.text, strong = true)
    }
}

internal fun Modifier.screenBackground(p: BrowserScreenPalette) =
    background(Brush.verticalGradient(listOf(p.background, p.soft, p.background)))
