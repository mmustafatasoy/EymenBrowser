package com.eymen.browser.telefon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.eymen.browser.tema.EymenTokens

/** Aşama 0: yalnızca boş, tokens.json tabanlı Compose gövdesi. */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(Modifier.fillMaxSize().background(EymenTokens.gece_zemin))
        }
    }
}
