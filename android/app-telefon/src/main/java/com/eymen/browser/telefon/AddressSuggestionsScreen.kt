package com.eymen.browser.telefon

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T

/** Separate native sibling of the fixed-height toolbar; every row has real touch bounds. */
@Composable
internal fun AddressSuggestionsScreen(items:List<AddressSuggestion>,day:Boolean,onOpen:(String)->Unit) {
    val p=browserListPalette(day)
    Column(Modifier.fillMaxWidth().heightIn(max=T.telefon_liste_satir.dp*3).background(p.background)
        .verticalScroll(rememberScrollState())) {
        items.forEach{item ->
            val label=stringResource(R.string.address_suggestion_action,item.title)
            SettingsActionRow(item.title,p,{onOpen(if(item.search)item.title else item.url)},modifier=Modifier.semantics{contentDescription=label},note=if(item.search)"Google" else item.url,icon=EymenPaths.ara)
        }
    }
}
