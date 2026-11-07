package com.eymen.browser.telefon

import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

/** Native density, flat text rows and neutral surfaces from the user's references. */
internal fun browserListPalette(day: Boolean) = if (day) BrowserScreenPalette(
    T.listeGunduz_zemin,T.listeGunduz_yuzey,T.listeGunduz_yuzey,T.listeGunduz_cizgi,
    T.listeGunduz_yazi,T.listeGunduz_ikincil,T.listeGunduz_pasif,
    T.listeGunduz_vurgu,T.listeGunduz_vurguUstu,T.gunduz_koruma
) else BrowserScreenPalette(
    T.listeGece_zemin,T.listeGece_yuzey,T.listeGece_yuzey,T.listeGece_cizgi,
    T.listeGece_yazi,T.listeGece_ikincil,T.listeGece_pasif,
    T.listeGece_vurgu,T.listeGece_vurguUstu,T.gece_koruma
)

@Composable
internal fun ListIconAction(label: String, path: String, p: BrowserScreenPalette,
    onClick: () -> Unit, enabled: Boolean = true, modifier: Modifier = Modifier) {
    Box(modifier.size(T.telefon_dokunma_standart.dp)
        .semantics { contentDescription = label }.clickable(enabled, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center) {
        EymenVector(path, color = if (enabled) p.text else p.disabled, iconSize = T.telefon_liste_menuSimge.dp)
    }
}

@Composable
internal fun ListPageHeader(title: String, p: BrowserScreenPalette, onBack: () -> Unit,
    trailing: @Composable RowScope.() -> Unit = {}) {
    Row(Modifier.fillMaxWidth().heightIn(min = T.telefon_liste_ust.dp),
        verticalAlignment = Alignment.CenterVertically) {
        ListIconAction(stringResource(R.string.back),EymenPaths.geri,p,onBack)
        ScreenText(title,p.text,T.telefon_liste_baslik,Modifier.weight(1f),maxLines = 2)
        trailing()
    }
}

@Composable
internal fun ListSearch(query: String, hint: String, p: BrowserScreenPalette, onChange: (String) -> Unit) {
    Row(Modifier.fillMaxWidth().heightIn(min = T.telefon_liste_ara.dp)
        .background(p.surface,RoundedCornerShape(T.telefon_liste_ara.dp))
        .padding(start = T.telefon_ekran_kenar.dp,end = T.telefon_ekran_kucukBosluk.dp),
        verticalAlignment = Alignment.CenterVertically) {
        EymenVector(EymenPaths.ara,color = p.muted,iconSize = T.telefon_liste_menuSimge.dp)
        Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
        BasicTextField(query,onChange,Modifier.weight(1f).padding(vertical = T.telefon_ekran_bosluk.dp)
            .semantics { contentDescription = hint },singleLine = true,
            textStyle = TextStyle(color = p.text,fontSize = T.telefon_liste_yazi.sp),
            decorationBox = { field -> Box {
                if (query.isEmpty()) ScreenText(hint,p.muted,T.telefon_liste_yazi)
                field()
            } })
        if (query.isNotEmpty()) ListIconAction(stringResource(R.string.search_clear),EymenPaths.temizle,p,{ onChange("") })
    }
}

@Composable
internal fun SettingsRows(p: BrowserScreenPalette, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(T.telefon_liste_grupKose.dp)).background(p.background),
        verticalArrangement = Arrangement.spacedBy(T.telefon_liste_ayirici.dp),content = content)
}

@Composable
internal fun SettingsActionRow(label: String, p: BrowserScreenPalette, onClick: () -> Unit,
    modifier: Modifier = Modifier, note: String = "", enabled: Boolean = true,
    chosen: Boolean? = null, icon: String? = null) {
    Row(modifier.fillMaxWidth().heightIn(min = T.telefon_liste_satir.dp)
        .background(p.surface,RoundedCornerShape(T.telefon_liste_satirKose.dp))
        .semantics { if (chosen != null) selected = chosen }
        .clickable(enabled,role = if (chosen == null) Role.Button else Role.RadioButton,onClick = onClick)
        .padding(horizontal = T.telefon_ekran_kenar.dp,vertical = T.telefon_liste_satirDikey.dp),
        verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            EymenVector(icon,color = if (enabled) p.muted else p.disabled,iconSize = T.telefon_liste_menuSimge.dp)
            Spacer(Modifier.width(T.telefon_ekran_kenar.dp))
        }
        Column(Modifier.weight(1f),verticalArrangement = Arrangement.spacedBy(T.telefon_ekran_cizgi.dp * 4)) {
            ScreenText(label,if (enabled) p.text else p.disabled,T.telefon_liste_yazi,maxLines = 3)
            if (note.isNotEmpty()) ScreenText(note,p.muted,T.telefon_liste_ikincil,maxLines = 4)
        }
        if (chosen != null) {
            Spacer(Modifier.width(T.telefon_ekran_bosluk.dp))
            ListSelectionMark(chosen,p)
        }
    }
}

@Composable
internal fun ListSelectionMark(chosen: Boolean,p: BrowserScreenPalette){
    Canvas(Modifier.size(T.telefon_liste_menuSimge.dp)){
        val width=T.telefon_ekran_cizgi.dp.toPx()*2
        drawCircle(if(chosen)p.accent else p.muted,radius=size.minDimension/2-width,style=Stroke(width))
        if(chosen)drawCircle(p.accent,radius=size.minDimension/5)
    }
}
@Composable
internal fun ListToggleMark(chosen: Boolean,p: BrowserScreenPalette){
    Canvas(Modifier.size(T.telefon_ekran_rozet.dp,T.telefon_liste_menuSimge.dp)){
        drawRoundRect(if(chosen)p.accent else p.disabled,cornerRadius=CornerRadius(size.height/2))
        drawCircle(p.background,radius=size.height/2-T.telefon_ekran_cizgi.dp.toPx()*2,
            center=Offset(if(chosen)size.width-size.height/2 else size.height/2,size.height/2))
    }
}
