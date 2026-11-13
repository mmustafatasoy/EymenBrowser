package com.eymen.browser.telefon

import android.app.KeyguardManager
import android.app.Activity
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle

/** Feature 45. Android verifies its device credential; no PIN/biometric data enters this app. */
internal class PrivateTabLock(private val activity:ComponentActivity,private val changed:()->Unit) {
    private val prefs=activity.getSharedPreferences("eymen_browser_setup",0)
    private val keyguard=activity.getSystemService(KeyguardManager::class.java)
    var enabled by mutableStateOf(prefs.getBoolean("private_tab_lock",false));private set
    var locked by mutableStateOf(enabled);private set
    var authenticating by mutableStateOf(false);private set
    val available get()=keyguard.isDeviceSecure
    private var accepted:(()->Unit)?=null
    private val launcher=activity.registerForActivityResult(ActivityResultContracts.StartActivityForResult()){result ->
        val action=accepted;accepted=null;authenticating=false
        if(result.resultCode==Activity.RESULT_OK && keyguard.isDeviceSecure){locked=false;action?.invoke()}
        changed()
    }
    @Suppress("DEPRECATION")
    private fun authenticate(action:()->Unit) {
        if(authenticating || !activity.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))return
        if(!available){Toast.makeText(activity,R.string.private_lock_device_required,Toast.LENGTH_LONG).show();return}
        val intent=keyguard.createConfirmDeviceCredentialIntent(activity.getString(R.string.private_unlock_title),activity.getString(R.string.private_unlock_note))
        if(intent==null){Toast.makeText(activity,R.string.private_lock_device_required,Toast.LENGTH_LONG).show();return}
        authenticating=true;accepted=action
        try{launcher.launch(intent)}catch(_:RuntimeException){accepted=null;authenticating=false;changed()}
    }
    fun changeEnabled(value:Boolean) {
        if(enabled==value)return
        authenticate {enabled=value;locked=false;prefs.edit().putBoolean("private_tab_lock",value).apply()}
    }
    fun unlock(action:()->Unit={}) {
        if(!enabled || !locked){action();return}
        authenticate(action)
    }
    fun background() {if(enabled){locked=true;changed()}}
}
