package com.eymen.browser.telefon

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * On-device PDF renderer. No PDF bytes or page text are uploaded anywhere.
 * A single worker serializes PdfRenderer access and bounds bitmap memory.
 */
class BrowserPdfViewerActivity : Activity() {
    companion object { const val TITLE = "eymen_pdf_title" }
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    @Volatile private var disposed = false
    @Volatile private var generation = 0
    private var descriptor: ParcelFileDescriptor? = null
    private var pdf: PdfRenderer? = null
    private var pageCount = 0
    private var pageIndex = 0
    private var zoom = 1f
    private var displayed: Bitmap? = null
    private lateinit var picture: ImageView
    private lateinit var status: TextView
    private lateinit var previous: Button
    private lateinit var next: Button
    private lateinit var scroll: ScrollView

    private fun label(tr:String,en:String) =
        if(resources.configuration.locales[0].language.equals("tr",true))tr else en

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        val uri=intent?.data
        if(uri==null || uri.scheme!="content" ||
            !intent.type.orEmpty().substringBefore(';').equals("application/pdf",true)) {
            finish();return
        }
        val surface=if((resources.configuration.uiMode and 0x30)==0x20)Color.rgb(30,31,33) else Color.WHITE
        val foreground=if(surface==Color.WHITE)Color.BLACK else Color.WHITE
        val root=LinearLayout(this).apply { orientation=LinearLayout.VERTICAL;setBackgroundColor(surface) }
        root.setOnApplyWindowInsetsListener {_,insets ->
            @Suppress("DEPRECATION")
            val top=if(Build.VERSION.SDK_INT>=30)insets.getInsets(WindowInsets.Type.statusBars() or WindowInsets.Type.displayCutout()).top else insets.systemWindowInsetTop
            @Suppress("DEPRECATION")
            val bottom=if(Build.VERSION.SDK_INT>=30)insets.getInsets(WindowInsets.Type.navigationBars()).bottom else insets.systemWindowInsetBottom
            root.setPadding(0,top,0,bottom)
            insets
        }
        val header=LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL}
        val close=button("‹") {finish()}
        header.addView(close)
        val name=TextView(this).apply {
            text=intent.getStringExtra(TITLE)?.take(120) ?: "PDF"
            setTextColor(foreground);textSize=16f;maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END
        }
        header.addView(name,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        root.addView(header)
        scroll=ScrollView(this).apply {setFillViewport(false);setBackgroundColor(Color.rgb(238,239,241))}
        picture=ImageView(this).apply {
            adjustViewBounds=true;scaleType=ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.WHITE)
            contentDescription=label("PDF sayfası","PDF page")
        }
        scroll.addView(picture,ScrollView.LayoutParams(ScrollView.LayoutParams.MATCH_PARENT,ScrollView.LayoutParams.WRAP_CONTENT))
        root.addView(scroll,LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f))
        val controls=LinearLayout(this).apply {gravity=Gravity.CENTER_VERTICAL;orientation=LinearLayout.HORIZONTAL}
        previous=button("‹") { if(pageIndex>0){pageIndex--;renderPage()} }
        next=button("›") { if(pageIndex+1<pageCount){pageIndex++;renderPage()} }
        status=TextView(this).apply {
            setTextColor(foreground);textSize=14f;gravity=Gravity.CENTER
            text=label("PDF açılıyor…","Opening PDF…")
        }
        controls.addView(previous)
        controls.addView(status,LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f))
        controls.addView(next)
        controls.addView(button("−") {zoom=(zoom/1.25f).coerceAtLeast(0.65f);renderPage()})
        controls.addView(button("+") {zoom=(zoom*1.25f).coerceAtMost(2.5f);renderPage()})
        root.addView(controls)
        setContentView(root)
        previous.isEnabled=false;next.isEnabled=false

        worker.execute {
            var opened:ParcelFileDescriptor?=null
            var document:PdfRenderer?=null
            try {
                opened=contentResolver.openFileDescriptor(uri,"r") ?: error("PDF has no readable descriptor")
                document=PdfRenderer(opened)
                if(document.pageCount==0)error("PDF contains no pages")
                if(disposed) {document.close();opened.close();return@execute}
                descriptor=opened;pdf=document;pageCount=document.pageCount
                main.post {if(!disposed){updateButtons();renderPage()} }
            } catch (_:Exception) {
                try{document?.close()}catch(_:Exception){}
                try{opened?.close()}catch(_:Exception){}
                main.post {if(!disposed)status.text=label("PDF okunamadı; dosya bozuk veya şifreli olabilir.","Unable to read PDF; it may be corrupt or password-protected.")}
            }
        }
    }
    private fun button(value:String,onClick:()->Unit):Button =
        Button(this).apply {text=value;textSize=17f;minWidth=44;setOnClickListener{onClick()}}

    private fun updateButtons(){
        previous.isEnabled=pageIndex>0
        next.isEnabled=pageIndex+1<pageCount
        status.text=label("Sayfa ","Page ")+(pageIndex+1)+" / "+pageCount
    }

    private fun renderPage(){
        if(disposed||pageCount==0)return
        val request=++generation
        val index=pageIndex
        val scale=zoom
        updateButtons()
        worker.execute {
            if(disposed||generation!=request)return@execute
            var bitmap:Bitmap?=null
            try {
                val doc=pdf ?: return@execute
                doc.openPage(index).use {page ->
                    val nativeWidth=page.width.coerceAtLeast(1)
                    val nativeHeight=page.height.coerceAtLeast(1)
                    val screen=resources.displayMetrics.widthPixels.coerceIn(320,1600)
                    val desired=screen.toFloat()*scale/nativeWidth
                    val memoryCap=sqrt(5_000_000.0/(nativeWidth.toDouble()*nativeHeight.toDouble())).toFloat()
                    val ratio=minOf(desired,memoryCap,2200f/nativeWidth,3000f/nativeHeight).coerceAtLeast(0.05f)
                    val width=(nativeWidth*ratio).roundToInt().coerceAtLeast(1)
                    val height=(nativeHeight*ratio).roundToInt().coerceAtLeast(1)
                    bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888).also {it.eraseColor(Color.WHITE)}
                    page.render(bitmap!!,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                }
                val completed=bitmap
                main.post {
                    if(disposed||generation!=request){completed?.recycle();return@post}
                    picture.setImageDrawable(null)
                    displayed?.recycle()
                    displayed=completed
                    picture.setImageBitmap(completed)
                    scroll.scrollTo(0,0)
                    updateButtons()
                }
            }catch (_:Exception) {
                bitmap?.recycle()
                main.post {if(!disposed&&generation==request)status.text=label("Bu sayfa gösterilemedi.","Page could not be displayed.")}
            }catch (_:OutOfMemoryError) {
                bitmap?.recycle()
                main.post {if(!disposed&&generation==request)status.text=label("Sayfa için bellek yetersiz.","Not enough memory for this page.")}
            }
        }
    }
    override fun onDestroy(){
        disposed=true;generation++
        if(::picture.isInitialized)picture.setImageDrawable(null)
        displayed?.recycle();displayed=null
        worker.execute {
            try{pdf?.close()}catch(_:Exception){}
            try{descriptor?.close()}catch(_:Exception){}
            pdf=null;descriptor=null
        }
        worker.shutdown()
        super.onDestroy()
    }
}
