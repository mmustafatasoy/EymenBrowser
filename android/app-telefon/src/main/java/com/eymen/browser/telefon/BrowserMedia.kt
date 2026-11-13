package com.eymen.browser.telefon

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaMetadata
import android.media.session.MediaSession as AndroidMediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.IBinder
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.MediaSession

/** Feature 29. Only real Gecko media events can start the media foreground service. */
internal class BrowserMedia private constructor(private val app: Context) : MediaSession.Delegate {
    var owner: GeckoSession?=null; private set
    var transport: MediaSession?=null; private set
    var playing=false; private set
    var title=""; private set
    var duration=0.0; private set
    var position=0.0; private set
    var changed: (() -> Unit)?=null
    private val titles=java.util.IdentityHashMap<GeckoSession,String>()
    private val audio=app.getSystemService(AudioManager::class.java)
    private val focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
        .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MOVIE).build())
        .setOnAudioFocusChangeListener({change -> if(change<0)pause()},Handler(Looper.getMainLooper())).build()
    fun attach(session: GeckoSession) {session.mediaSessionDelegate=this}
    fun isPlaying(session: GeckoSession?)=playing && owner===session
    private fun mediaTitle(session: GeckoSession)=if(session.settings.usePrivateMode)app.getString(R.string.private_tab)
        else titles[session].orEmpty().ifBlank {app.getString(R.string.browser_display_name)}
    override fun onActivated(session: GeckoSession, mediaSession: MediaSession) {
        if(owner==null){owner=session;transport=mediaSession;title=mediaTitle(session)}
    }
    override fun onMetadata(session: GeckoSession, mediaSession: MediaSession, meta: MediaSession.Metadata) {
        // Never put a private page title/URL in Android's media history or lock screen.
        if(!session.settings.usePrivateMode)titles[session]=meta.title.orEmpty().filterNot {it.isISOControl()}.take(160)
        if(owner===session){title=mediaTitle(session);changed?.invoke()}
    }
    override fun onPlay(session: GeckoSession, mediaSession: MediaSession) {
        if(owner!==session){transport?.pause();duration=0.0;position=0.0}
        owner=session;transport=mediaSession;title=mediaTitle(session)
        if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){playing=false;mediaSession.pause();changed?.invoke();return}
        playing=true
        try {
            app.startForegroundService(Intent(app,BrowserMediaService::class.java).setAction(BrowserMediaService.UPDATE))
            changed?.invoke()
        } catch(_:RuntimeException) {pause()}
    }
    override fun onPause(session: GeckoSession, mediaSession: MediaSession) {
        if(owner===session){playing=false;audio.abandonAudioFocusRequest(focus);changed?.invoke()}
    }
    override fun onPositionState(session: GeckoSession, mediaSession: MediaSession, state: MediaSession.PositionState) {
        if(owner===session){duration=state.duration.takeIf {it.isFinite() && it>0}?:0.0
            position=state.position.takeIf {it.isFinite() && it>=0}?:0.0;changed?.invoke()}
    }
    override fun onStop(session: GeckoSession, mediaSession: MediaSession) {release(session)}
    override fun onDeactivated(session: GeckoSession, mediaSession: MediaSession) {release(session)}
    fun pause(){transport?.pause()}
    fun play(){transport?.play()}
    fun stop(){val old=owner;transport?.pause();transport?.stop();if(old!=null)release(old)}
    fun release(session: GeckoSession) {
        titles.remove(session)
        if(owner===session){playing=false;owner=null;transport=null;title="";duration=0.0;position=0.0
            audio.abandonAudioFocusRequest(focus);changed?.invoke()}
    }
    companion object {
        @Volatile private var instance: BrowserMedia?=null
        fun get(context: Context): BrowserMedia=instance?:synchronized(this){instance?:BrowserMedia(context.applicationContext).also{instance=it}}
    }
}

/** Native Android media controls, audio focus and foreground lifecycle; never exported. */
class BrowserMediaService : Service() {
    private lateinit var controller: BrowserMedia
    private lateinit var session: AndroidMediaSession
    private var foreground=false
    private val main=Handler(Looper.getMainLooper())
    private val idle=Runnable {if(!controller.playing)stopSelf()}
    private val noisy=object:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){controller.pause()}}
    @Suppress("DEPRECATION", "UnspecifiedRegisterReceiverFlag")
    override fun onCreate() {
        super.onCreate();controller=BrowserMedia.get(this)
        getSystemService(NotificationManager::class.java).createNotificationChannel(NotificationChannel(CHANNEL,getString(R.string.media_channel),NotificationManager.IMPORTANCE_LOW))
        session=AndroidMediaSession(this,"EymenBrowserMedia")
        session.setCallback(object:AndroidMediaSession.Callback(){
            override fun onPlay(){if(controller.owner!=null && !foreground)promote();controller.play()}
            override fun onPause(){controller.pause()}
            override fun onStop(){controller.stop()}
            override fun onSeekTo(pos:Long){if(controller.duration>0)controller.transport?.seekTo((pos/1000.0).coerceIn(0.0,controller.duration),false)}
        })
        val filter=IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY)
        if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,filter,RECEIVER_NOT_EXPORTED) else registerReceiver(noisy,filter)
        controller.changed=::refresh
    }
    private fun command(action:String):PendingIntent {
        val intent=Intent(this,BrowserMediaService::class.java).setAction(action)
        val flags=PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if(action==PLAY)PendingIntent.getForegroundService(this,action.hashCode(),intent,flags)
            else PendingIntent.getService(this,action.hashCode(),intent,flags)
    }
    private fun notification():Notification {
        val open=PendingIntent.getActivity(this,0,Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val privateMode=controller.owner?.settings?.usePrivateMode==true
        return Notification.Builder(this,CHANNEL).setSmallIcon(R.drawable.ic_eymen_browser)
            .setContentTitle(controller.title.ifBlank{getString(R.string.browser_display_name)})
            .setContentText(getString(if(controller.playing)R.string.media_playing else R.string.media_paused))
            .setContentIntent(open).setDeleteIntent(command(STOP)).setOngoing(controller.playing)
            .setVisibility(if(privateMode)Notification.VISIBILITY_SECRET else Notification.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true).setCategory(Notification.CATEGORY_TRANSPORT)
            .addAction(Notification.Action.Builder(if(controller.playing)android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                getString(if(controller.playing)R.string.media_pause else R.string.media_play),command(if(controller.playing)PAUSE else PLAY)).build())
            .addAction(Notification.Action.Builder(android.R.drawable.ic_menu_close_clear_cancel,getString(R.string.media_stop),command(STOP)).build())
            .setStyle(Notification.MediaStyle().setMediaSession(session.sessionToken).setShowActionsInCompactView(0)).build()
    }
    private fun promote(){startForeground(NOTIFICATION,notification());foreground=true}
    private fun refresh() {
        main.removeCallbacks(idle)
        if(controller.owner==null){stopForeground(STOP_FOREGROUND_REMOVE);foreground=false;session.isActive=false;stopSelf();return}
        session.isActive=true
        session.setMetadata(MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,controller.title)
            .putLong(MediaMetadata.METADATA_KEY_DURATION,(controller.duration*1000).toLong()).build())
        var actions=PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE or PlaybackState.ACTION_STOP
        if(controller.duration>0)actions=actions or PlaybackState.ACTION_SEEK_TO
        session.setPlaybackState(PlaybackState.Builder().setActions(actions)
            .setState(if(controller.playing)PlaybackState.STATE_PLAYING else PlaybackState.STATE_PAUSED,(controller.position*1000).toLong(),if(controller.playing)1f else 0f).build())
        if(controller.playing){if(!foreground)promote() else getSystemService(NotificationManager::class.java).notify(NOTIFICATION,notification())}
        else {if(foreground){stopForeground(STOP_FOREGROUND_DETACH);foreground=false}
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION,notification());main.postDelayed(idle,60000)}
    }
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int {
        // A user resume action can start this service from its notification.
        // Satisfy the foreground deadline before Gecko emits its async play event.
        if(intent?.action==PLAY && controller.owner!=null && !foreground)promote()
        when(intent?.action){PLAY->controller.play();PAUSE->controller.pause();STOP->controller.stop()}
        refresh();return START_NOT_STICKY
    }
    override fun onTaskRemoved(rootIntent:Intent?){controller.stop();stopSelf()}
    override fun onDestroy(){main.removeCallbacksAndMessages(null);controller.changed=null;controller.stop();unregisterReceiver(noisy)
        session.isActive=false;session.release();stopForeground(STOP_FOREGROUND_REMOVE)
        getSystemService(NotificationManager::class.java).cancel(NOTIFICATION);super.onDestroy()}
    override fun onBind(intent:Intent?):IBinder?=null
    companion object {
        private const val CHANNEL="browser-media"
        const val NOTIFICATION=3029
        const val UPDATE="com.eymen.browser.telefon.MEDIA_UPDATE"
        const val PLAY="com.eymen.browser.telefon.MEDIA_PLAY"
        const val PAUSE="com.eymen.browser.telefon.MEDIA_PAUSE"
        const val STOP="com.eymen.browser.telefon.MEDIA_STOP"
    }
}
