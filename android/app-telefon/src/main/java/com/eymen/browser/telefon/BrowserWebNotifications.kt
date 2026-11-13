package com.eymen.browser.telefon

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.eymen.browser.motor.GeckoMotorDenemesi
import org.mozilla.geckoview.WebNotification
import org.mozilla.geckoview.WebNotificationDelegate
import java.util.UUID

/** Real Gecko Notifications API delivery. No remote WebPush subscription service. */
internal class BrowserWebNotifications private constructor(context:Context):WebNotificationDelegate {
    private val app=context.applicationContext
    private val main=Handler(Looper.getMainLooper())
    private val manager=app.getSystemService(NotificationManager::class.java)
    private data class Entry(val id:Int,val token:String,val origin:String,val web:WebNotification)
    private val entries=linkedMapOf<String,Entry>()
    private var nextId=210000
    init {
        manager.createNotificationChannel(NotificationChannel(CHANNEL,app.getString(R.string.web_notifications_channel),NotificationManager.IMPORTANCE_DEFAULT))
        // Objects from a dead engine process cannot receive page callbacks.
        manager.activeNotifications.filter{it.notification.channelId==CHANNEL}.forEach{manager.cancel(it.id)}
        GeckoMotorDenemesi.getRuntime(app).setWebNotificationDelegate(this)
    }
    private fun childEnabled()=app.getSharedPreferences("eymen_child_mode",0).getBoolean("enabled",false)
    private fun permitted()=manager.areNotificationsEnabled() && manager.getNotificationChannel(CHANNEL)?.importance!=NotificationManager.IMPORTANCE_NONE &&
        (Build.VERSION.SDK_INT<33 || app.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)==PackageManager.PERMISSION_GRANTED)
    private fun key(web:WebNotification)=web.origin+"\u0000"+web.tag
    override fun onShowNotification(notification:WebNotification) {main.post {
        val origin=BrowserSettingsServices.origin(notification.origin)
        if(origin==null || notification.privateBrowsing || childEnabled() || !permitted() || notification.tag.length>2048){notification.dismiss();return@post}
        val key=key(notification)
        if(key !in entries && entries.size>=32)remove(entries.keys.first())
        val previous=entries[key]
        val entry=Entry(previous?.id ?: ++nextId,UUID.randomUUID().toString(),origin,notification)
        val open=Intent(app,MainActivity::class.java).setAction(OPEN).setData(Uri.parse(origin))
            .putExtra(TOKEN,entry.token).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val dismiss=Intent(app,BrowserWebNotificationDismissReceiver::class.java).setAction(CLOSE).putExtra(TOKEN,entry.token)
        val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        try {
            val native=NotificationCompat.Builder(app,CHANNEL).setSmallIcon(R.drawable.ic_eymen_browser)
                .setContentTitle(notification.title.orEmpty().take(200)).setContentText(notification.text.orEmpty().take(1000))
                .setSubText(origin).setVisibility(Notification.VISIBILITY_PRIVATE).setAutoCancel(true)
                .setOnlyAlertOnce(previous!=null).setSilent(notification.silent)
                .setContentIntent(PendingIntent.getActivity(app,entry.id,open,flags))
                .setDeleteIntent(PendingIntent.getBroadcast(app,entry.id,dismiss,flags)).build()
            entries[key]=entry;manager.notify(entry.id,native);notification.show()
        }catch(_:Exception){entries.remove(key);manager.cancel(entry.id);notification.dismiss()}
    }}
    override fun onCloseNotification(notification:WebNotification) {main.post {
        val entry=entries.remove(key(notification));if(entry!=null)manager.cancel(entry.id)
        notification.dismiss()
    }}
    private fun remove(key:String) {val entry=entries.remove(key) ?: return;manager.cancel(entry.id);entry.web.dismiss()}
    fun closeToken(token:String?) {
        if(token==null)return
        entries.entries.firstOrNull{it.value.token==token}?.key?.let(::remove)
    }
    fun clearOrigin(origin:String?) {entries.filter{it.value.origin==origin}.keys.toList().forEach(::remove)}
    fun clearAll(){entries.keys.toList().forEach(::remove)}
    /** Only the live opaque token can fire a Gecko click; fallback is a normal HTTPS origin. */
    fun open(intent:Intent):String? {
        if(intent.action!=OPEN || childEnabled())return null
        val origin=BrowserSettingsServices.origin(intent.dataString) ?: return null
        val match=entries.entries.firstOrNull{it.value.token==intent.getStringExtra(TOKEN) && it.value.origin==origin}
        if(match!=null){val entry=entries.remove(match.key)!!;manager.cancel(entry.id);entry.web.click();entry.web.dismiss()}
        return origin
    }
    companion object {
        const val CHANNEL="web_sites"
        const val OPEN="com.eymen.browser.telefon.OPEN_WEB_NOTIFICATION"
        private const val CLOSE="com.eymen.browser.telefon.CLOSE_WEB_NOTIFICATION"
        const val TOKEN="web_notification_token"
        @Volatile private var instance:BrowserWebNotifications?=null
        fun dismissExisting(token:String?){instance?.closeToken(token)}
        fun get(context:Context):BrowserWebNotifications=instance ?: synchronized(this){instance ?: BrowserWebNotifications(context).also{instance=it}}
    }
}

/** Explicit immutable PendingIntent target; never exported to other apps. */
class BrowserWebNotificationDismissReceiver:BroadcastReceiver() {
    override fun onReceive(context:Context,intent:Intent){BrowserWebNotifications.dismissExisting(intent.getStringExtra(BrowserWebNotifications.TOKEN))}
}
