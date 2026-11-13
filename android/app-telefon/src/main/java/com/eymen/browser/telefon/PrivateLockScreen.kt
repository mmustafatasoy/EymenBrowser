package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenTokens as T

internal fun privateScreenPalette()=BrowserScreenPalette(T.gizli_zemin,T.gizli_yuzey1,T.gizli_yuzey2,T.gizli_yuzey2,
    T.gizli_yazi,T.gizli_yaziIkincil,T.gizli_yaziPasif,T.gizli_vurgu,T.gizli_vurguUstu,T.gizli_vurgu)

@Composable
internal fun PrivateLockScreen(onUnlock:()->Unit,onNormal:()->Unit,normalAllowed:Boolean) {
    val p=privateScreenPalette()
    Column(Modifier.fillMaxSize().background(p.background).padding(T.telefon_ekran_kenar.dp),
        verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)) {
        Spacer(Modifier.weight(1f))
        ScreenText(stringResource(R.string.private_locked_title),p.text,T.telefon_liste_baslik,strong=true,maxLines=3)
        ScreenText(stringResource(R.string.private_unlock_note),p.muted,maxLines=5)
        ScreenButton(stringResource(R.string.private_unlock),p,onUnlock,primary=true)
        if(normalAllowed)ScreenButton(stringResource(R.string.private_return_normal),p,onNormal)
        Spacer(Modifier.weight(1f))
    }
}
