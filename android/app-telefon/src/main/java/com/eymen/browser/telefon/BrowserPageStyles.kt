package com.eymen.browser.telefon

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension

/** No site data or URLs are stored or sent to the style extension. */
internal class BrowserPageStyles(context:Context,private val live:(GeckoSession)->Boolean) {
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("eymen_browser_setup",0)
    private var extension:WebExtension?=null
    var enabled by mutableStateOf(prefs.getBoolean("dark_pages",false));private set
    var ready by mutableStateOf(false);private set

    fun initialize(sessions:()->List<GeckoSession>) {
        applyRuntime()
        try {
            GeckoMotorDenemesi.getRuntime(app).webExtensionController.ensureBuiltIn(
                "resource://android/assets/eymen-page-tools/","eymen-page-tools@atasoyyazilim.com"
            ).accept({value ->
                extension=value;ready=value!=null
                if(value!=null)sessions().forEach(::attach)
            },{_ -> ready=false})
        }catch(_:RuntimeException){ready=false}
    }
    fun refresh():Boolean {
        val wanted=prefs.getBoolean("dark_pages",false);val changed=wanted!=enabled
        enabled=wanted;applyRuntime();return changed
    }
    fun updateEnabled(value:Boolean):Boolean {
        if(!ready)return false
        enabled=value;prefs.edit().putBoolean("dark_pages",value).apply();applyRuntime();return true
    }
    private fun applyRuntime() {
        GeckoMotorDenemesi.getRuntime(app).settings.setPreferredColorScheme(
            if(enabled)GeckoRuntimeSettings.COLOR_SCHEME_LIGHT else GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM)
    }
    fun attach(session:GeckoSession) {
        val addon=extension ?: return
        session.webExtensionController.setMessageDelegate(addon,object:WebExtension.MessageDelegate {
            override fun onMessage(nativeApp:String,message:Any,sender:WebExtension.MessageSender):GeckoResult<Any> {
                val verified=GeckoMotorDenemesi.verifiedHttpsUrl(sender.url)
                val allowed=nativeApp=="eymen_page_tools" &&
                    (message as? JSONObject)?.optString("type")=="dark-page-policy-v1" &&
                    sender.environmentType==WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT &&
                    sender.isTopLevel && sender.session===session && sender.webExtension.id==addon.id &&
                    live(session) && verified!=null && verified==sender.url
                return GeckoResult.fromValue(JSONObject().put("enabled",allowed && enabled).toString() as Any)
            }
        },"eymen_page_tools")
    }
}
