package com.eymen.browser.telefon

import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.activity.OnBackPressedCallback
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.eymen.browser.tema.EymenPaths
import com.eymen.browser.tema.EymenTokens as T
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.net.URI
import kotlin.math.min

/** The preview and outgoing payload always refer to the same user-selected page. No remote preview request. */
internal data class SharedPage(val title: String,val url: String,val privateMode: Boolean) {
    val host: String get() = URI(url).host.orEmpty()
}

internal class BrowserShareController(private val activity: ComponentActivity,
    private val day: () -> Boolean,private val notice: (Int) -> Unit) {
    private var dialog: Dialog? = null
    private var composition: ComposeView? = null
    var page: SharedPage? = null; private set
    var qrBitmap by mutableStateOf<Bitmap?>(null); private set
    private var qrVisible by mutableStateOf(false)
    private var qrLoading by mutableStateOf(false)
    private var qrFailed by mutableStateOf(false)

    fun show(selected: SharedPage) {
        dismiss(); page=selected;qrVisible=false;qrFailed=false;qrLoading=false
        val popup=ComponentDialog(activity,if(day()) android.R.style.Theme_Material_Light_Dialog_NoActionBar
            else android.R.style.Theme_Material_Dialog_NoActionBar)
        val content=ComposeView(activity)
        composition=content;dialog=popup
        content.setContent {
            SharePagePanel(selected,day(),qrVisible,qrBitmap,qrLoading,qrFailed,
                onClose=::dismiss,onBack={qrVisible=false},onCopy={copy(selected)},
                onQr={qrVisible=true;createQr(selected,popup)},onShare={shareText(selected)},onQrShare={shareQr(selected)})
        }
        popup.onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if(qrVisible)qrVisible=false else dismiss() }
        })
        popup.setContentView(content)
        popup.setOnDismissListener {
            content.disposeComposition()
            if(dialog===popup){dialog=null;composition=null;page=null;qrBitmap?.recycle();qrBitmap=null;qrLoading=false}
        }
        popup.window?.apply {
            setBackgroundDrawable(ColorDrawable(android.graphics.Color.TRANSPARENT))
            setGravity(Gravity.BOTTOM);setDimAmount(T.telefon_dialog_perde.toFloat())
        }
        popup.show()
        popup.window?.setLayout(min(activity.resources.displayMetrics.widthPixels,
            (T.telefon_paylas_enFazla*activity.resources.displayMetrics.density).toInt()),ViewGroup.LayoutParams.WRAP_CONTENT)
    }
    fun dismiss(){dialog?.dismiss()}
    private fun copy(selected: SharedPage) {
        val clip=ClipData.newPlainText(selected.title,selected.url)
        if(android.os.Build.VERSION.SDK_INT>=33 && selected.privateMode)
            clip.description.extras=android.os.PersistableBundle().apply {putBoolean("android.content.extra.IS_SENSITIVE",true)}
        activity.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
        dismiss();if(android.os.Build.VERSION.SDK_INT<33)notice(R.string.share_link_copied)
    }
    fun textIntent(selected: SharedPage): Intent = Intent(Intent.ACTION_SEND).apply {
        type="text/plain";putExtra(Intent.EXTRA_TEXT,selected.url)
        putExtra(Intent.EXTRA_TITLE,selected.title);putExtra(Intent.EXTRA_SUBJECT,selected.title)
    }
    private fun shareText(selected: SharedPage){launch(textIntent(selected))}
    private fun createQr(selected: SharedPage,popup: Dialog) {
        if(qrBitmap!=null || qrLoading)return
        qrLoading=true;qrFailed=false
        Thread {
            val result=try {
                // Never shorten a URL to force it into a different QR payload.
                require(selected.url.toByteArray(Charsets.UTF_8).size<=2048)
                val matrix=QRCodeWriter().encode(selected.url,BarcodeFormat.QR_CODE,T.telefon_paylas_qrBitmap,T.telefon_paylas_qrBitmap,
                    mapOf(EncodeHintType.CHARACTER_SET to "UTF-8",EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M,EncodeHintType.MARGIN to 4))
                val pixels=IntArray(matrix.width*matrix.height){index->if(matrix[index%matrix.width,index/matrix.width])T.qr_kod.toArgb() else T.qr_zemin.toArgb()}
                Bitmap.createBitmap(pixels,matrix.width,matrix.height,Bitmap.Config.ARGB_8888)
            }catch(_:Exception){null}catch(_:OutOfMemoryError){null}
            activity.runOnUiThread {
                if(dialog!==popup || !popup.isShowing || activity.isDestroyed){result?.recycle();return@runOnUiThread}
                qrLoading=false;qrBitmap=result;qrFailed=result==null
            }
        }.start()
    }
    fun qrIntent(selected: SharedPage): Intent {
        val bitmap=qrBitmap ?: error("QR not ready")
        val directory=File(activity.cacheDir,"shares").apply{check(isDirectory||mkdirs())}
        directory.listFiles()?.filter{System.currentTimeMillis()-it.lastModified()>86_400_000L}?.forEach{it.delete()}
        val file=File.createTempFile("qr-",".png",directory)
        file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.PNG,100,it)) }
        val uri=FileProvider.getUriForFile(activity,"${activity.packageName}.updateprovider",file)
        return Intent(Intent.ACTION_SEND).apply {
            type="image/png";putExtra(Intent.EXTRA_STREAM,uri);putExtra(Intent.EXTRA_TITLE,selected.title)
            clipData=ClipData.newUri(activity.contentResolver,selected.title,uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    private fun shareQr(selected: SharedPage){try{launch(qrIntent(selected))}catch(_:Exception){notice(R.string.share_qr_failed)}}
    private fun launch(intent: Intent){dismiss();try{activity.startActivity(Intent.createChooser(intent,activity.getString(R.string.menu_share)))}catch(_:Exception){notice(R.string.menu_share_error)}}
}

@Composable
private fun SharePagePanel(page: SharedPage,day: Boolean,qrVisible: Boolean,bitmap: Bitmap?,loading: Boolean,failed: Boolean,
    onClose:()->Unit,onBack:()->Unit,onCopy:()->Unit,onQr:()->Unit,onShare:()->Unit,onQrShare:()->Unit){
    val p=browserListPalette(day)
    val maxHeight=(LocalConfiguration.current.screenHeightDp*T.telefon_paylas_yukseklikYuzde/100).dp
    Column(Modifier.fillMaxWidth().heightIn(max=maxHeight).background(p.background,RoundedCornerShape(
        topStart=T.telefon_dialog_kose.dp,topEnd=T.telefon_dialog_kose.dp)).navigationBarsPadding()
        .verticalScroll(rememberScrollState()).padding(T.telefon_dialog_kenar.dp),
        verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_kenar.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){
            if(qrVisible)ListIconAction(stringResource(R.string.back),EymenPaths.geri,p,onBack)
            ScreenText(stringResource(if(qrVisible)R.string.share_qr else R.string.share_page_title),p.text,T.telefon_liste_baslik,Modifier.weight(1f),strong=true)
            ListIconAction(stringResource(R.string.share_close),EymenPaths.temizle,p,onClose)
        }
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(T.telefon_ekran_kenar.dp)){
            Box(Modifier.size(T.telefon_paylas_simgeAlan.dp).background(p.surface,RoundedCornerShape(T.telefon_kose_kart.dp)),contentAlignment=Alignment.Center){
                EymenVector(if(page.privateMode)EymenPaths.gizli else EymenPaths.site,color=p.muted,iconSize=T.telefon_liste_menuSimge.dp)
            }
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)){
                ScreenText(page.title,p.text,T.telefon_liste_yazi,strong=true)
                ScreenText(page.host,p.muted,T.telefon_liste_ikincil)
                if(page.privateMode)ScreenText(stringResource(R.string.share_private_page),p.muted,T.telefon_liste_ikincil)
            }
        }
        SelectionContainer {ScreenText(page.url,p.muted,T.telefon_liste_ikincil,Modifier.fillMaxWidth(),maxLines=4)}
        if(qrVisible){
            if(bitmap!=null)Image(bitmap.asImageBitmap(),stringResource(R.string.share_qr_accessibility),Modifier.align(Alignment.CenterHorizontally).size(T.telefon_paylas_qr.dp))
            if(loading)ScreenText(stringResource(R.string.share_qr_loading),p.muted,maxLines=4)
            if(failed)ScreenText(stringResource(R.string.share_qr_failed),p.muted,maxLines=8)
            if(bitmap!=null){ScreenText(stringResource(R.string.share_qr_note),p.muted,maxLines=4)
                ScreenButton(stringResource(R.string.share_qr_image),p,onQrShare,primary=true)}
            ScreenButton(stringResource(R.string.share_copy_link),p,onCopy)
        }else Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)){
            ShareAction(stringResource(R.string.share_copy_link),EymenPaths.kopyala,p,onCopy,Modifier.weight(1f))
            ShareAction(stringResource(R.string.share_qr),EymenPaths.qr,p,onQr,Modifier.weight(1f))
            ShareAction(stringResource(R.string.share_other_apps),EymenPaths.paylas,p,onShare,Modifier.weight(1f))
        }
    }
}
@Composable
private fun ShareAction(label:String,path:String,p:BrowserScreenPalette,onClick:()->Unit,modifier:Modifier){
    Column(modifier.heightIn(min=T.telefon_paylas_eylemAlan.dp).semantics{contentDescription=label}
        .clickable(role=Role.Button,onClick=onClick).padding(vertical=T.telefon_ekran_kucukBosluk.dp),
        horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(T.telefon_ekran_kucukBosluk.dp)){
        Box(Modifier.size(T.telefon_paylas_simgeAlan.dp).background(p.surface,RoundedCornerShape(T.telefon_paylas_simgeAlan.dp)),contentAlignment=Alignment.Center){EymenVector(path,color=p.text,iconSize=T.telefon_liste_menuSimge.dp)}
        ScreenText(label,p.text,T.telefon_liste_ikincil,maxLines=2)
    }
}
