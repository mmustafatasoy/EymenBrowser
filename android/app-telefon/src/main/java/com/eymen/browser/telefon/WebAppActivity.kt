package com.eymen.browser.telefon

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.graphics.toArgb
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/** A registered web app has its own Android document task and scoped Gecko page. */
class WebAppActivity:ComponentActivity() {
    private lateinit var session:GeckoSession
    private lateinit var view:GeckoView
    private lateinit var root:FrameLayout
    private lateinit var services:BrowserSettingsServices
    private lateinit var downloads:AndroidDownloadDenemesi
    private lateinit var spec:WebAppSpec
    private val media by lazy{BrowserMedia.get(applicationContext)}
    private val styles by lazy{BrowserPageStyles(this){it===session && !isFinishing && !isDestroyed}}
    private val prefs by lazy{getSharedPreferences("eymen_browser_setup",MODE_PRIVATE)}
    private var canGoBack=false
    private var fullscreen=false
    private var drmDialog:android.app.AlertDialog?=null
    private fun childEnabled()=getSharedPreferences("eymen_child_mode",MODE_PRIVATE).getBoolean("enabled",false)
    override fun attachBaseContext(base:Context) {
        val language=base.getSharedPreferences("eymen_browser_setup",MODE_PRIVATE).getString("app_language","system")
        if(Build.VERSION.SDK_INT<33 && language in setOf("tr","en")) {
            val config=Configuration(base.resources.configuration);config.setLocale(Locale.forLanguageTag(language!!))
            super.attachBaseContext(base.createConfigurationContext(config))
        }else super.attachBaseContext(base)
    }
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val data=intent.data
        val registered=if(data?.scheme=="eymen-web-app")BrowserWebApps(this).find(data.host) else null
        if(registered==null || childEnabled()){finish();return}
        spec=registered;title=spec.name
        setTaskDescription(android.app.ActivityManager.TaskDescription(spec.name))
        WindowCompat.setDecorFitsSystemWindows(window,false)
        root=FrameLayout(this);view=GeckoView(this)
        root.addView(view,FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,FrameLayout.LayoutParams.MATCH_PARENT));setContentView(root);applyWindowTheme()
        ViewCompat.setOnApplyWindowInsetsListener(root){target,insets ->
            val safe=insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val keyboard=insets.getInsets(WindowInsetsCompat.Type.ime())
            target.setPadding(safe.left,safe.top,safe.right,maxOf(safe.bottom,keyboard.bottom));insets
        }
        services=BrowserSettingsServices(this,{it===session && resumed()},{if(::session.isInitialized)session.reload()})
        services.apply();BrowserWebNotifications.get(this)
        GeckoMotorDenemesi.configureTrackerProtection(this,prefs.getBoolean("shield_strict_mode",false))
        downloads=AndroidDownloadDenemesi(this)
        view.setActivityContextDelegate(object:GeckoView.ActivityContextDelegate {
            override fun getActivityContext():Context?=if(resumed())this@WebAppActivity else null
        })
        session=GeckoMotorDenemesi.attach(this,view,null,
            onFullScreen={fullscreen=it;WindowCompat.getInsetsController(window,root).let{bars ->
                if(it)bars.hide(WindowInsetsCompat.Type.systemBars()) else bars.show(WindowInsetsCompat.Type.systemBars())}},
            askDrmPermission=::askDrm,onDownload=downloads::handleResponse,initialLoad=false,sitePermissions=services,
            trackingProtection=protectionFor(spec.startUrl),
            allowNavigation={url ->
                if(childEnabled())false
                else if(WebAppPolicy.withinScope(spec.scope,url))true
                else {openBrowser(url);false}
            },
            onLocation={url -> if(url!=null && ::session.isInitialized)session.settings.setUseTrackingProtection(protectionFor(url))},
            onTitle={value -> if(!value.isNullOrBlank())title=value.take(120)},onCanGoBack={canGoBack=it},
            allowPopups={true},onUserOpenedWindow={url ->openBrowser(url);null},onClose={finish()})
        media.attach(session);view.setAutofillEnabled(prefs.getBoolean("autofill",true))
        styles.initialize{listOf(session)};styles.attach(session)
        installCosmeticPolicy()
        session.loadUri(spec.startUrl)
        onBackPressedDispatcher.addCallback(this,object:OnBackPressedCallback(true){
            override fun handleOnBackPressed(){when{fullscreen->session.exitFullScreen();canGoBack->session.goBack();else->finish()}}
        })
    }
    private fun installCosmeticPolicy() {
        GeckoMotorDenemesi.ensureCosmeticExtension(this){extension ->runOnUiThread {
            if(extension!=null && session.isOpen && !isFinishing && !isDestroyed)
                session.webExtensionController.setMessageDelegate(extension,object:org.mozilla.geckoview.WebExtension.MessageDelegate {
                    override fun onMessage(nativeApp:String,message:Any,sender:org.mozilla.geckoview.WebExtension.MessageSender):org.mozilla.geckoview.GeckoResult<Any> {
                        val safe=GeckoMotorDenemesi.verifiedHttpsUrl(sender.url)
                        val allowed=nativeApp=="eymen_cosmetic" && (message as? org.json.JSONObject)?.optString("type")=="cosmetic-policy-v1" &&
                            sender.environmentType==org.mozilla.geckoview.WebExtension.MessageSender.ENV_TYPE_CONTENT_SCRIPT &&
                            sender.isTopLevel && sender.session===session && sender.webExtension.id==extension.id &&
                            session.isOpen && !childEnabled() && safe!=null && safe==sender.url && protectionFor(safe)
                        return org.mozilla.geckoview.GeckoResult.fromValue(org.json.JSONObject()
                            .put("enabled",allowed && prefs.getBoolean("cosmetic_filter_wanted",false))
                            .put("rejectCookies",allowed && prefs.getBoolean("cookie_reject",false)).toString() as Any)
                    }
                },"eymen_cosmetic")
        }}
    }
    private fun protectionFor(url:String):Boolean {
        val global=prefs.getBoolean("shield_wanted",true)
        val host=try{java.net.URI(url).host?.lowercase(Locale.ROOT)}catch(_:Exception){null} ?: return global
        return getSharedPreferences("eymen_browser_shield_sites",MODE_PRIVATE).getBoolean("host_$host",global)
    }
    private fun resumed()=!isFinishing && !isDestroyed && !childEnabled() && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)
    private fun openBrowser(url:String) {
        val safe=GeckoMotorDenemesi.verifiedWebUrl(url) ?: return
        if(childEnabled())return
        startActivity(Intent(this,MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(safe)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    private fun askDrm(host:String,answer:(Boolean)->Unit) {
        if(!resumed()){answer(false);return}
        val done=AtomicBoolean(false)
        fun finish(allowed:Boolean){if(done.compareAndSet(false,true))answer(allowed && resumed())}
        drmDialog?.dismiss()
        drmDialog=BrowserDialogs.builder(this).setTitle(R.string.drm_permission_title)
            .setMessage(getString(R.string.drm_permission_message,host))
            .setPositiveButton(R.string.drm_permission_allow){_,_->finish(true)}
            .setNegativeButton(R.string.drm_permission_deny){_,_->finish(false)}.setOnDismissListener{finish(false)}.show()
    }
    private fun applyWindowTheme() {
        val day=if(prefs.getString("theme_mode","")=="system")resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK!=Configuration.UI_MODE_NIGHT_YES else prefs.getBoolean("day_theme",true)
        root.setBackgroundColor(browserListPalette(day).background.toArgb())
        WindowCompat.getInsetsController(window,root).let{it.isAppearanceLightStatusBars=day;it.isAppearanceLightNavigationBars=day}
    }
    override fun onResume(){super.onResume();if(childEnabled()){finish();return}
        if(::downloads.isInitialized)downloads.observe()
        if(::session.isInitialized){if(styles.refresh())session.reload();view.setAutofillEnabled(prefs.getBoolean("autofill",true))}
    }
    override fun onPause(){drmDialog?.dismiss();if(::services.isInitialized)services.stop();super.onPause()}
    override fun onConfigurationChanged(newConfig:Configuration){super.onConfigurationChanged(newConfig);GeckoMotorDenemesi.onConfigurationChanged(newConfig);if(::root.isInitialized)applyWindowTheme()}
    override fun onDestroy(){drmDialog?.dismiss();if(::downloads.isInitialized)downloads.dispose()
        if(::view.isInitialized)view.releaseSession()
        if(::session.isInitialized){media.release(session);if(session.isOpen)session.close()}
        super.onDestroy()
    }
}
