package com.eymen.browser.telefon

import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.net.Uri
import org.json.JSONObject
import java.util.UUID

/** User-approved manifest entries only. The launcher owns the final pin confirmation. */
internal class BrowserWebApps(context:Context) {
    private val app=context.applicationContext
    private val prefs=app.getSharedPreferences("eymen_web_apps",0)
    private val shortcuts=app.getSystemService(ShortcutManager::class.java)
    private val idPattern=Regex("[a-f0-9-]{36}")
    fun discover(document:String,manifest:JSONObject):WebAppSpec? {
        if(manifest.optString("display") !in setOf("standalone","minimal-ui","fullscreen"))return null
        return WebAppPolicy.candidate(document,manifest.optString("start_url"),manifest.optString("scope"),
            manifest.optString("short_name").ifBlank{manifest.optString("name")})
    }
    private fun records():MutableMap<String,WebAppSpec> {
        val result=linkedMapOf<String,WebAppSpec>()
        try {
            val raw=prefs.getString("apps","{}").orEmpty();if(raw.length>131072)return result
            val json=JSONObject(raw)
            json.keys().asSequence().take(20).forEach{id ->
                val item=json.optJSONObject(id)
                if(idPattern.matches(id) && item!=null)WebAppPolicy.candidate(item.optString("start"),item.optString("start"),item.optString("scope"),item.optString("name"))?.let{result[id]=it}
            }
        }catch(_:Exception){}
        return result
    }
    private fun save(items:Map<String,WebAppSpec>):Boolean {
        val json=JSONObject();items.forEach{(id,spec)->json.put(id,JSONObject().put("name",spec.name).put("start",spec.startUrl).put("scope",spec.scope))}
        return prefs.edit().putString("apps",json.toString()).commit()
    }
    fun find(id:String?):WebAppSpec?=id?.takeIf{idPattern.matches(it)}?.let{records()[it]}
    fun launchIntent(id:String)=Intent(app,WebAppActivity::class.java).setAction(Intent.ACTION_VIEW)
        .setData(Uri.parse("eymen-web-app://$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT)
    fun request(spec:WebAppSpec):Boolean {
        if(!shortcuts.isRequestPinShortcutSupported)return false
        val before=records();val pinned=shortcuts.pinnedShortcuts.map{it.id}.toSet()
        val entries=before.filterKeys{PREFIX+it in pinned}.toMutableMap()
        val id=entries.entries.firstOrNull{it.value.startUrl==spec.startUrl && it.value.scope==spec.scope}?.key ?: UUID.randomUUID().toString()
        if(id !in entries && entries.size>=20)return false
        entries[id]=spec;if(!save(entries))return false
        return try {
            val info=ShortcutInfo.Builder(app,PREFIX+id).setShortLabel(spec.name)
                .setLongLabel(spec.name+" · "+Uri.parse(spec.startUrl).host)
                .setIcon(Icon.createWithResource(app,R.drawable.ic_eymen_browser)).setIntent(launchIntent(id)).build()
            shortcuts.requestPinShortcut(info,null).also{if(!it)save(before)}
        }catch(_:Exception){save(before);false}
    }
    companion object {const val PREFIX="eymen-webapp-"}
}
