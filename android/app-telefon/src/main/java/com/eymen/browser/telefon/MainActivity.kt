package com.eymen.browser.telefon

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView

/** Aşama 1: yalnızca GeckoView motor denemesi, tasarım arayüzü değildir. */
class MainActivity : ComponentActivity() {
    private lateinit var view: GeckoView
    private var session: GeckoSession? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        view = GeckoView(this)
        setContentView(view)
        session = GeckoMotorDenemesi.attach(this, view, intent.getStringExtra("test_url"))
    }

    override fun onDestroy() {
        GeckoMotorDenemesi.detach(view, session)
        session = null
        super.onDestroy()
    }
}
