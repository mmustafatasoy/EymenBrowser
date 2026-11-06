package com.eymen.browser.telefon

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock

/** User-initiated in-process transfers. Streams cannot be restored after death. */
internal class EymenDownloadService : Service(), DownloadObserver {
    private val coordinator by lazy { DownloadCoordinator.get(this) }
    private val notifications by lazy { getSystemService(NotificationManager::class.java) }
    private var wakeLock: PowerManager.WakeLock? = null
    private var foregroundStarted = false
    private var lastStartId = 0
    private var lastNotification = 0L

    override fun onCreate() {
        super.onCreate()
        notifications.createNotificationChannel(NotificationChannel(
            CHANNEL, getString(R.string.download_notification_channel), NotificationManager.IMPORTANCE_LOW
        ).apply { lockscreenVisibility = Notification.VISIBILITY_PRIVATE })
        try {
            val notification = notification(coordinator.snapshot())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
            } else startForeground(ID, notification)
            foregroundStarted = true
            wakeLock = getSystemService(PowerManager::class.java)
                .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "EymenBrowser:Downloads").apply {
                    setReferenceCounted(false)
                    acquire(6 * 60 * 60 * 1000L)
                }
            coordinator.observe(this)
        } catch (_: Exception) {
            foregroundStarted = false
            coordinator.serviceStartFailed()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        lastStartId = startId
        if (!foregroundStarted || intent == null) {
            coordinator.cancelAll()
            stopSelf(startId)
        } else if (intent.action == CANCEL_ALL) {
            coordinator.cancelAll()
            stopIfIdle()
        } else {
            coordinator.serviceStarted()
            stopIfIdle()
        }
        return START_NOT_STICKY
    }

    private fun stopIfIdle() {
        // stopSelfResult refuses to stop when a newer start command is pending.
        if (lastStartId != 0 && !coordinator.hasWork() && stopSelfResult(lastStartId)) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    override fun onSnapshot(snapshot: DownloadSnapshot) {
        if (!foregroundStarted) return
        val now = SystemClock.elapsedRealtime()
        if (snapshot.active == null || snapshot.active.cancelling || now - lastNotification >= 1000) {
            lastNotification = now
            try { notifications.notify(ID, notification(snapshot)) } catch (_: SecurityException) {}
        }
        stopIfIdle()
    }

    private fun notification(snapshot: DownloadSnapshot): Notification {
        val immutable = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val open = PendingIntent.getActivity(this, 0,
            Intent(this, MainActivity::class.java).setAction(OPEN_DOWNLOADS), immutable)
        val cancel = PendingIntent.getService(this, 1,
            Intent(this, EymenDownloadService::class.java).setAction(CANCEL_ALL), immutable)
        val active = snapshot.active
        val percent = active?.let { DownloadProgressMath.percent(it.bytesWritten, it.totalBytes) }
        return Notification.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(getString(R.string.download_active))
            .setContentText(active?.name ?: getString(R.string.download_preparing))
            .setSubText(getString(R.string.download_queue_count, snapshot.queued.size))
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(Notification.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle(getString(R.string.app_name)).build())
            .setProgress(100, percent ?: 0, percent == null)
            .addAction(Notification.Action.Builder(null,
                getString(R.string.download_cancel_all), cancel).build())
            .build()
    }

    override fun onTimeout(startId: Int, fgsType: Int) {
        // Android 15+: stop promptly; stream closing and file cleanup are async.
        coordinator.cancelAll()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        coordinator.unobserve(this)
        coordinator.serviceDestroyed()
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
        foregroundStarted = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        super.onDestroy()
    }
    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val OPEN_DOWNLOADS = "com.eymen.browser.telefon.OPEN_DOWNLOADS"
        private const val CHANNEL = "eymen_downloads_v1"
        private const val ID = 3301
        private const val CANCEL_ALL = "com.eymen.browser.telefon.CANCEL_DOWNLOADS"
    }
}
