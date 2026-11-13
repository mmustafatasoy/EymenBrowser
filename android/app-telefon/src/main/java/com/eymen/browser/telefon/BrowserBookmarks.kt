package com.eymen.browser.telefon

import android.content.ContentValues
import android.content.Context
import android.content.SharedPreferences
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.concurrent.Executors

/** App-local, transactionally updated library. The old URL set is retained for update compatibility. */
internal class BrowserBookmarks private constructor(context: Context, private val legacy: SharedPreferences) {
    private val helper = object : SQLiteOpenHelper(context.applicationContext, "browser-bookmarks.db", null, 1) {
        override fun onConfigure(db: SQLiteDatabase) { db.setForeignKeyConstraintsEnabled(true) }
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE folders (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL UNIQUE COLLATE NOCASE)")
            db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT NOT NULL UNIQUE, title TEXT NOT NULL, folder_id INTEGER REFERENCES folders(id) ON DELETE SET NULL)")
            db.execSQL("CREATE TABLE metadata (key TEXT PRIMARY KEY)")
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
    }
    private val worker = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    var entries by mutableStateOf<List<BookmarkEntry>>(emptyList()); private set
    var folders by mutableStateOf<List<BookmarkFolder>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<Int?>(null); private set
    private var pending = 0
    init { refresh() }
    private fun count(db: SQLiteDatabase, table: String): Int =
        db.rawQuery("SELECT COUNT(*) FROM $table", null).use { it.moveToFirst(); it.getInt(0) }
    private class Rejected(val messageId: Int) : Exception()
    private fun task(work: (SQLiteDatabase) -> Unit = {}) {
        pending++; loading = true; error = null
        worker.execute {
            try {
                val db = helper.writableDatabase
                db.beginTransaction()
                val snapshot: List<BookmarkEntry>
                val folderSnapshot: List<BookmarkFolder>
                try {
                    val migrated = db.rawQuery("SELECT key FROM metadata WHERE key='legacy-v1'",null).use { it.moveToFirst() }
                    if (!migrated) {
                        // Never consume/delete legacy records. A failed transaction can be retried safely.
                        for (raw in legacy.getStringSet("urls",emptySet()).orEmpty()) {
                            val url = BookmarkPolicy.url(raw) ?: continue
                            val values = ContentValues().apply { put("url",url);put("title",BookmarkPolicy.title("",url)) }
                            db.insertWithOnConflict("bookmarks",null,values,SQLiteDatabase.CONFLICT_IGNORE)
                        }
                        db.execSQL("INSERT INTO metadata(key) VALUES('legacy-v1')")
                    }
                    work(db)
                    snapshot = db.query("bookmarks",arrayOf("id","url","title","folder_id"),null,null,null,null,"id DESC").use { c ->
                        buildList { while(c.moveToNext()) add(BookmarkEntry(c.getLong(0),c.getString(1),c.getString(2),if(c.isNull(3))null else c.getLong(3))) }
                    }
                    folderSnapshot = db.query("folders",arrayOf("id","name"),null,null,null,null,"id ASC").use { c ->
                        buildList { while(c.moveToNext()) add(BookmarkFolder(c.getLong(0),c.getString(1))) }
                    }
                    db.setTransactionSuccessful()
                } finally { db.endTransaction() }
                // Preserve the legacy URL contract used by previous production versions.
                legacy.edit().putStringSet("urls",snapshot.map { it.url }.toSet()).apply()
                main.post { entries=snapshot;folders=folderSnapshot;error=null;pending--;loading=pending>0 }
            } catch (failure: Exception) {
                main.post { error=(failure as? Rejected)?.messageId ?: R.string.bookmark_operation_failed;pending--;loading=pending>0 }
            }
        }
    }
    fun refresh() = task()
    fun clearError() { error=null }
    fun add(url: String, title: String) {
        val safe=BookmarkPolicy.url(url) ?: return
        task { db ->
            if(count(db,"bookmarks")>=BookmarkPolicy.MAX_ENTRIES) throw Rejected(R.string.bookmark_limit)
            db.insertOrThrow("bookmarks",null,ContentValues().apply { put("url",safe);put("title",BookmarkPolicy.title(title,safe)) })
        }
    }
    fun delete(id: Long) = task { db -> db.delete("bookmarks","id=?",arrayOf(id.toString()));Unit }
    fun edit(id: Long, title: String, rawUrl: String) {
        val safe=BookmarkPolicy.url(rawUrl) ?: run { error=R.string.invalid_address;return }
        task { db ->
            val duplicate=db.rawQuery("SELECT id FROM bookmarks WHERE url=? AND id<>?",arrayOf(safe,id.toString())).use { it.moveToFirst() }
            if(duplicate) throw Rejected(R.string.bookmark_duplicate)
            db.update("bookmarks",ContentValues().apply {put("url",safe);put("title",BookmarkPolicy.title(title,safe))},"id=?",arrayOf(id.toString()));Unit
        }
    }
    fun move(id: Long, folderId: Long?) = task { db ->
        db.update("bookmarks",ContentValues().apply { if(folderId==null)putNull("folder_id") else put("folder_id",folderId) },"id=?",arrayOf(id.toString()));Unit
    }
    fun saveFolder(id: Long?, rawName: String) {
        val name=BookmarkPolicy.name(rawName)
        if(name.isEmpty()) {error=R.string.bookmark_folder_name_required;return}
        task { db ->
            if(id==null && count(db,"folders")>=BookmarkPolicy.MAX_FOLDERS) throw Rejected(R.string.bookmark_limit)
            val duplicate=db.rawQuery("SELECT id FROM folders WHERE name=? COLLATE NOCASE AND id<>?",arrayOf(name,(id?:-1L).toString())).use { it.moveToFirst() }
            if(duplicate) throw Rejected(R.string.bookmark_folder_exists)
            val values=ContentValues().apply {put("name",name)}
            if(id==null)db.insertOrThrow("folders",null,values) else db.update("folders",values,"id=?",arrayOf(id.toString()))
            Unit
        }
    }
    /** Foreign key ON DELETE SET NULL preserves every bookmark in the deleted folder. */
    fun deleteFolder(id: Long) = task { db -> db.delete("folders","id=?",arrayOf(id.toString()));Unit }
    companion object {
        @Volatile private var instance: BrowserBookmarks? = null
        fun get(context: Context, legacy: SharedPreferences): BrowserBookmarks = instance ?: synchronized(this) {
            instance ?: BrowserBookmarks(context,legacy).also {instance=it}
        }
    }
}
