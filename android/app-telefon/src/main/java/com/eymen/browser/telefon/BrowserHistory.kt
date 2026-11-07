package com.eymen.browser.telefon

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors

/** App-local SQLite. No private visits, external service, page script or telemetry. */
internal class BrowserHistory private constructor(context: Context) {
    private val helper = object : SQLiteOpenHelper(context.applicationContext, "browser-history.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE visits (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT NOT NULL, title TEXT NOT NULL, visited_at INTEGER NOT NULL, day_key TEXT NOT NULL, UNIQUE(url,day_key))")
            db.execSQL("CREATE INDEX visit_time ON visits(visited_at DESC)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    var entries by mutableStateOf<List<HistoryEntry>>(emptyList()); private set
    var loading by mutableStateOf(false); private set
    var failed by mutableStateOf(false); private set

    private fun task(work: (SQLiteDatabase) -> Unit = {}) {
        loading = true
        executor.execute {
            try {
                val db = helper.writableDatabase
                work(db)
                db.delete("visits", "visited_at < ?", arrayOf(HistoryPolicy.cutoff(System.currentTimeMillis()).toString()))
                db.execSQL("DELETE FROM visits WHERE id NOT IN (SELECT id FROM visits ORDER BY visited_at DESC,id DESC LIMIT ${HistoryPolicy.MAX_ENTRIES})")
                val snapshot = mutableListOf<HistoryEntry>()
                db.query("visits", arrayOf("id", "url", "title", "visited_at"), null, null, null, null, "visited_at DESC,id DESC").use { c ->
                    while (c.moveToNext()) snapshot.add(HistoryEntry(c.getLong(0),c.getString(1),c.getString(2),c.getLong(3)))
                }
                main.post { entries = snapshot; loading = false; failed = false }
            } catch (_: Exception) { main.post { loading = false; failed = true } }
        }
    }
    fun refresh() = task()
    fun record(rawUrl: String, rawTitle: String, privateMode: Boolean, visitedAt: Long = System.currentTimeMillis()) {
        val url = HistoryPolicy.safeUrl(rawUrl, privateMode) ?: return
        val title = HistoryPolicy.title(rawTitle, url)
        val day = HistoryPolicy.dayKey(visitedAt)
        task { db ->
            val values = ContentValues().apply { put("url",url);put("title",title);put("visited_at",visitedAt);put("day_key",day) }
            // Keep the same row ID when a page is reloaded on the same day.
            if (db.update("visits", values, "url=? AND day_key=?", arrayOf(url,day)) == 0)
                db.insertOrThrow("visits", null, values)
        }
    }
    fun updateTitle(rawUrl: String, rawTitle: String, visitedAt: Long) {
        val url = HistoryPolicy.safeUrl(rawUrl, false) ?: return
        val title = HistoryPolicy.title(rawTitle,url)
        task { db -> db.update("visits", ContentValues().apply { put("title",title) },
            "url=? AND day_key=?", arrayOf(url,HistoryPolicy.dayKey(visitedAt))); Unit }
    }
    fun delete(id: Long) = task { db -> db.delete("visits", "id=?", arrayOf(id.toString())); Unit }
    fun clear() = task { db -> db.delete("visits",null,null); Unit }
    companion object {
        @Volatile private var instance: BrowserHistory? = null
        fun get(context: Context): BrowserHistory = instance ?: synchronized(this) {
            instance ?: BrowserHistory(context).also { instance = it }
        }
    }
}
