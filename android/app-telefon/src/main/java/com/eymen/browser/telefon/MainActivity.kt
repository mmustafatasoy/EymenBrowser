package com.eymen.browser.telefon

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/**
 * Aşama 1: yalnızca GeckoView motor testi. Son kullanıcı arayüzü değildir.
 *
 * HTTPS bağlantısını başka bir uygulamada "Paylaş > EymenBrowser" seçerek
 * denemek mümkündür; tarayıcı ekranları Kapı 1 onayı sonrasına aittir.
 */
class MainActivity : ComponentActivity() {
    private lateinit var view: GeckoView
    private var session: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = GeckoView(this)
        setContentView(view)
        session = GeckoMotorDenemesi.attach(
            this, view, GeckoMotorDenemesi.urlFromIntent(intent)
        )
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        GeckoMotorDenemesi.urlFromIntent(intent)?.let { safeUrl ->
            session?.loadUri(safeUrl)
        }
    }

    override fun onDestroy() {
        GeckoMotorDenemesi.detach(view, session)
        session = null
        super.onDestroy()
    }
}
