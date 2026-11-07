package com.eymen.browser.telefon

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T
import org.json.JSONArray

internal data class OpenSourceComponent(val id:String,val title:Int,val description:Int,val version:String,val license:String,val asset:String)
private fun componentTitle(id:String)=when(id){
    "gecko"->R.string.license_gecko;"androidx"->R.string.license_androidx;"kotlin"->R.string.license_kotlin
    "wireguard"->R.string.license_wireguard;"zxing"->R.string.license_zxing;else->R.string.license_desugar
}
private fun componentDescription(id:String)=when(id){
    "gecko"->R.string.license_gecko_note;"androidx"->R.string.license_androidx_note;"kotlin"->R.string.license_kotlin_note
    "wireguard"->R.string.license_wireguard_note;"zxing"->R.string.license_zxing_note;else->R.string.license_desugar_note
}

@Composable
internal fun OpenSourceLicensesScreen(day:Boolean,onBack:()->Unit){
    val p=browserListPalette(day);val context=LocalContext.current
    val focus=LocalFocusManager.current;val keyboard=LocalSoftwareKeyboardController.current
    val components=remember { try {
        val array=JSONArray(context.assets.open("licenses/index.json").bufferedReader().use{it.readText()})
        (0 until array.length()).map { index->val row=array.getJSONObject(index);val id=row.getString("id")
            require(id in setOf("gecko","androidx","kotlin","wireguard","zxing","desugar"))
            val path=row.getString("asset");require(Regex("licenses/[a-z0-9_.-]+\\.txt").matches(path))
            OpenSourceComponent(id,componentTitle(id),componentDescription(id),row.getString("version"),row.getString("license"),path)
        }
    }catch(_:Exception){emptyList()} }
    var query by remember {mutableStateOf("")}
    var selected by remember {mutableStateOf<OpenSourceComponent?>(null)}
    val back:()->Unit={focus.clearFocus();keyboard?.hide();if(selected!=null)selected=null else onBack()}
    BackHandler(enabled=selected!=null,onBack=back)
    Column(Modifier.fillMaxSize().background(p.background)){
        ListPageHeader(selected?.let{stringResource(it.title)}?:stringResource(R.string.about_licenses),p,back)
        if(selected==null){
            Column(Modifier.padding(horizontal=T.telefon_ekran_kenar.dp),verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)){
                ScreenText(stringResource(R.string.license_intro),p.muted,T.telefon_liste_ikincil,maxLines=6)
                ListSearch(query,stringResource(R.string.license_search),p){query=it}
            }
            val rows=components.filter { query.isBlank() || stringResource(it.title).contains(query,true) || it.license.contains(query,true) }
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(T.telefon_ekran_kenar.dp),verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_bosluk.dp)){
                items(rows,key={it.id}){entry->SettingsActionRow(stringResource(entry.title),p,{focus.clearFocus();keyboard?.hide();selected=entry},
                    note=stringResource(entry.description)+"\n"+entry.license,icon=EymenPaths.belge)}
                if(rows.isEmpty())item{ScreenText(stringResource(if(components.isEmpty())R.string.license_unavailable else R.string.license_empty),p.muted,maxLines=5)}
            }
        }else{
            val entry=selected!!
            val paragraphs=remember(entry){try{context.assets.open(entry.asset).bufferedReader().use{it.readText()}
                .split(Regex("\\n\\s*\\n")).flatMap{it.chunked(6000)}}catch(_:Exception){emptyList()}}
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(T.telefon_ekran_kenar.dp),verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_kenar.dp)){
                item{ScreenText(entry.license,p.text,T.telefon_liste_yazi,strong=true)
                    if(entry.version.isNotBlank())ScreenText(stringResource(R.string.license_version,entry.version),p.muted,T.telefon_liste_ikincil)}
                items(paragraphs){paragraph->SelectionContainer{BasicText(paragraph,style=TextStyle(color=p.text,
                    fontSize=T.telefon_dialog_dugme.sp,lineHeight=T.telefon_liste_baslik.sp))}}
                if(paragraphs.isEmpty())item{ScreenText(stringResource(R.string.license_unavailable),p.muted,maxLines=5)}
            }
        }
    }
}

@Composable
internal fun FeedbackScreen(day:Boolean,onBack:()->Unit,onSupport:()->Unit,onEmail:()->Unit,onWebsite:()->Unit){
    val p=browserListPalette(day)
    Column(Modifier.fillMaxSize().background(p.background)){
        ListPageHeader(stringResource(R.string.feedback_title),p,onBack)
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(T.telefon_ekran_kenar.dp),
            verticalArrangement=Arrangement.spacedBy(T.telefon_liste_bolumBosluk.dp)){
            ScreenText(stringResource(R.string.feedback_description),p.text,T.telefon_liste_yazi,maxLines=8)
            SettingsRows(p){
                SettingsActionRow(stringResource(R.string.feedback_support),p,onSupport,note=stringResource(R.string.about_website_address),icon=EymenPaths.bilgi)
                SettingsActionRow(stringResource(R.string.feedback_email),p,onEmail,note=stringResource(R.string.feedback_email_note),icon=EymenPaths.paylas)
                SettingsActionRow(stringResource(R.string.about_website),p,onWebsite,note=stringResource(R.string.about_website_address),icon=EymenPaths.site)
            }
        }
    }
}
