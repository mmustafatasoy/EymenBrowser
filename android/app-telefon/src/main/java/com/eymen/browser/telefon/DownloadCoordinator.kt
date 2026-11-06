package com.eymen.browser.telefon

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.DocumentsContract
import android.provider.MediaStore
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.CancellationException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

internal data class QueuedDownload(val id: Long, val name: String)
internal data class DownloadSnapshot(
    val active: ActiveDownload?, val queued: List<QueuedDownload>
)
internal interface DownloadObserver {
    fun onSnapshot(snapshot: DownloadSnapshot)
    fun onCompleted(file: SavedDownload) {}
    fun onMessage(message: Int) {}
}

/** Owns original Gecko streams and application Context only, never an Activity. */
internal class DownloadCoordinator private constructor(private val context: Context) {
    private class Request(
        val name: String, val mime: String, val body: InputStream,
        val expectedBytes: Long?, val destination: Uri?,
        val cancelled: AtomicBoolean = AtomicBoolean(false)
    )
    private val main = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor()
    private val cleanup = Executors.newCachedThreadPool()
    private val scheduler = DownloadScheduler<Request>()
    private val registry = DownloadRegistry(context)
    private val observers = mutableSetOf<DownloadObserver>()
    private var progress: ActiveDownload? = null
    private var serviceReady = false

    private fun mainThread() = check(Looper.myLooper() == Looper.getMainLooper())
    fun observe(observer: DownloadObserver) {
        mainThread()
        observers.add(observer)
        observer.onSnapshot(snapshot())
    }
    fun unobserve(observer: DownloadObserver) { mainThread(); observers.remove(observer) }
    fun snapshot() = DownloadSnapshot(
        progress, scheduler.pending().map { QueuedDownload(it.id, it.value.name) }
    )
    fun hasWork() = scheduler.hasWork()
    private fun publish() {
        val current = snapshot()
        observers.toList().forEach { it.onSnapshot(current) }
    }
    private fun message(id: Int) = observers.toList().forEach { it.onMessage(id) }

    fun enqueue(name: String, mime: String, body: InputStream, expected: Long?, uri: Uri?) {
        mainThread()
        val request = Request(name, mime, body, expected, uri)
        val entry = scheduler.offer(request)
        if (entry == null) {
            discard(request)
            message(R.string.download_queue_full)
            return
        }
        try {
            // Called only after a visible Activity's explicit download consent.
            context.startForegroundService(Intent(context, EymenDownloadService::class.java))
        } catch (_: Exception) {
            scheduler.removeWaiting(entry.id)?.let { discard(it.value) }
            publish()
            message(R.string.download_background_failed)
            return
        }
        publish()
        // No stream is consumed until Service.startForeground has succeeded.
        if (serviceReady) pump()
    }

    fun serviceStarted() { mainThread(); serviceReady = true; pump() }
    fun serviceStartFailed() {
        mainThread()
        serviceReady = false
        message(R.string.download_background_failed)
        cancelAll()
    }
    fun serviceDestroyed() {
        mainThread()
        serviceReady = false
        cancelAll()
    }
    fun cancelActive() { scheduler.active()?.let { cancel(it.id) } }
    fun cancel(id: Long) {
        mainThread()
        val running = scheduler.active()
        if (running?.id == id) {
            if (running.value.cancelled.compareAndSet(false, true)) {
                progress = progress?.copy(cancelling = true)
                // Closing a remote stream must never block Android's UI thread.
                cleanup.execute { running.value.body.closeSafely() }
            }
        } else {
            scheduler.removeWaiting(id)?.let { discard(it.value) }
        }
        publish()
    }
    fun cancelAll() {
        mainThread()
        scheduler.drainWaiting().forEach { discard(it.value) }
        cancelActive()
        publish()
    }
    private fun discard(request: Request) {
        request.cancelled.set(true)
        cleanup.execute {
            request.body.closeSafely()
            // SAF has already created this new document before queue admission.
            request.destination?.let {
                if (!deleteDocument(it)) main.post { message(R.string.download_incomplete_saf) }
            }
        }
    }
    private fun deleteDocument(uri: Uri): Boolean = try {
        DocumentsContract.deleteDocument(context.contentResolver, uri)
    } catch (_: Exception) { false }

    private fun pump() {
        if (!serviceReady) return
        val entry = scheduler.startNext() ?: run { publish(); return }
        val request = entry.value
        progress = ActiveDownload(request.name, 0, request.expectedBytes)
        publish()
        worker.execute { transfer(entry.id, request) }
    }

    private fun transfer(id: Long, request: Request) {
        val resolver = context.contentResolver
        var createdDownload: Uri? = null
        var completed: SavedDownload? = null
        var resultMessage = R.string.download_failed
        var lastUpdate = 0L
        try {
            if (request.cancelled.get()) throw CancellationException()
            val destination = request.destination ?: run {
                check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, request.name)
                    put(MediaStore.MediaColumns.MIME_TYPE, request.mime)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                (resolver.insert(MediaStore.Downloads.getContentUri(
                    MediaStore.VOLUME_EXTERNAL_PRIMARY
                ), values) ?: throw IOException("Could not create Downloads entry"))
                    .also { createdDownload = it }
            }
            val bytes = request.body.use { source ->
                (resolver.openOutputStream(destination, "w")
                    ?: throw IOException("Could not open destination")).use { output ->
                    DownloadStreamCopy.copy(source, output, request.cancelled::get) { written ->
                        val now = SystemClock.elapsedRealtime()
                        if (now - lastUpdate >= 150L) {
                            lastUpdate = now
                            main.post {
                                if (scheduler.active()?.id == id && !request.cancelled.get()) {
                                    progress = ActiveDownload(request.name, written, request.expectedBytes)
                                    publish()
                                }
                            }
                        }
                    }
                }
            }
            if (bytes <= 0L) throw IOException("Empty response")
            if (request.cancelled.get()) throw CancellationException()
            createdDownload?.let {
                val values = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                check(resolver.update(it, values, null, null) > 0)
            }
            // Commit on the main thread below, serialized with user cancellation.
            completed = SavedDownload(destination.toString(), request.name, request.mime,
                bytes = bytes, savedAt = System.currentTimeMillis())
        } catch (error: Exception) {
            resultMessage = if (request.cancelled.get() || error is CancellationException)
                R.string.download_cancelled else R.string.download_failed
        } finally {
            request.body.closeSafely()
        }
        val candidate = completed
        main.post {
            if (candidate != null && !request.cancelled.get()) {
                try { registry.record(candidate) } catch (_: Exception) { /* file is valid */ }
                observers.toList().forEach { it.onCompleted(candidate) }
            } else {
                val failure = if (request.cancelled.get()) R.string.download_cancelled else resultMessage
                cleanup.execute {
                    createdDownload?.let { try { resolver.delete(it, null, null) } catch (_: Exception) {} }
                    val safRemains = request.destination?.let { !deleteDocument(it) } ?: false
                    main.post { message(if (safRemains) R.string.download_incomplete_saf else failure) }
                }
            }
            if (scheduler.finish(id)) {
                progress = null
                pump()
                publish()
            }
        }
    }

    companion object {
        @Volatile private var instance: DownloadCoordinator? = null
        fun get(context: Context): DownloadCoordinator = instance ?: synchronized(this) {
            instance ?: DownloadCoordinator(context.applicationContext).also { instance = it }
        }
        private fun InputStream.closeSafely() { try { close() } catch (_: Exception) {} }
    }
}
