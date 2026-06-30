package com.gua.browser.bookmark

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 书签数据类
 */
data class Bookmark(
    val id: Long = 0,
    val title: String = "",
    val url: String = "",
    val icon: String? = null,
    val position: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * 书签管理器
 */
class BookmarkManager(context: Context) {

    private val dbHelper = BookmarkDbHelper(context)

    suspend fun add(bookmark: Bookmark): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put("title", bookmark.title)
            put("url", bookmark.url)
            put("icon", bookmark.icon)
            put("position", bookmark.position)
            put("created_at", bookmark.createdAt)
        }
        db.insertOrThrow("bookmarks", null, values)
    }

    suspend fun getAll(): List<Bookmark> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "bookmarks", null, null, null, null, null,
            "position ASC, created_at DESC"
        )
        val list = mutableListOf<Bookmark>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    Bookmark(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        url = it.getString(it.getColumnIndexOrThrow("url")),
                        icon = it.getString(it.getColumnIndexOrThrow("icon")),
                        position = it.getInt(it.getColumnIndexOrThrow("position")),
                        createdAt = it.getLong(it.getColumnIndexOrThrow("created_at"))
                    )
                )
            }
        }
        list
    }

    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("bookmarks", "id = ?", arrayOf(id.toString()))
    }

    suspend fun exists(url: String): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "bookmarks", arrayOf("id"), "url = ?",
            arrayOf(url), null, null, null
        )
        cursor.use { it.count > 0 }
    }

    suspend fun updatePosition(id: Long, position: Int) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply { put("position", position) }
        dbHelper.writableDatabase.update("bookmarks", values, "id = ?", arrayOf(id.toString()))
    }

    private class BookmarkDbHelper(context: Context) :
        SQLiteOpenHelper(context, "bookmarks.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE bookmarks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    url TEXT NOT NULL UNIQUE,
                    icon TEXT,
                    position INTEGER DEFAULT 0,
                    created_at INTEGER NOT NULL
                )
            """.trimIndent())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
}
