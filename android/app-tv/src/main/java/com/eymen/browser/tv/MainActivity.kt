package com.eymen.browser.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.eymen.browser.tema.EymenTokens

/** Aşama 0: ekran ve kumanda kodu Aşama 4'te, yalnız boş Compose for TV uygulaması. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(Modifier.fillMaxSize().background(EymenTokens.gece_zemin))
        }
    }
}
