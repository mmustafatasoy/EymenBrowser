package com.eymen.browser.telefon

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import org.mozilla.geckoview.GeckoResult
import java.util.concurrent.Executors

/** Explicit user export only; no screenshot cache or private browsing archive. */
internal class BrowserPageCapture(private val activity:ComponentActivity) {
    private val writer=Executors.newSingleThreadExecutor()
    var busy=false;private set
    fun close(){writer.shutdown()}

    fun save(uri:Uri, eligible:()->Boolean, capture:()->GeckoResult<Bitmap>, done:(Boolean)->Unit) {
        if(busy || uri.scheme!="content" || !eligible()){done(false);return}
        busy=true
        fun finish(success:Boolean) {activity.runOnUiThread{busy=false;done(success)}}
        try {
            capture().accept({bitmap ->
                if(bitmap==null){finish(false);return@accept}
                if(!eligible()){bitmap.recycle();finish(false);return@accept}
                writer.execute {
                    val saved=try {
                        // Refuse huge output instead of compressing indefinitely.
                        if(bitmap.width.toLong()*bitmap.height>32_000_000L)false
                        else activity.contentResolver.openOutputStream(uri,"wt")?.use {
                            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
                        } ?: false
                    }catch(_:Exception){false}catch(_:OutOfMemoryError){false}finally{bitmap.recycle()}
                    finish(saved)
                }
            },{_ -> finish(false)})
        }catch(_:Exception){finish(false)}catch(_:OutOfMemoryError){finish(false)}
    }
}
