package com.gua.browser.bookmark

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 历史记录数据类
 */
data class HistoryItem(
    val id: Long = 0,
    val title: String = "",
    val url: String = "",
    val visitCount: Int = 1,
    val lastVisited: Long = System.currentTimeMillis()
)

/**
 * 浏览历史管理器
 */
class HistoryManager(context: Context) {

    private val dbHelper = HistoryDbHelper(context)

    /**
     * 记录访问
     */
    suspend fun recordVisit(title: String, url: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase

        // 检查是否已存在同 URL 记录
        val cursor = db.query(
            "history", arrayOf("id", "visit_count"),
            "url = ?", arrayOf(url), null, null, null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val id = it.getLong(0)
                val count = it.getInt(1)
                val values = ContentValues().apply {
                    put("visit_count", count + 1)
                    put("last_visited", System.currentTimeMillis())
                    put("title", title)
                }
                db.update("history", values, "id = ?", arrayOf(id.toString()))
            } else {
                val values = ContentValues().apply {
                    put("title", title)
                    put("url", url)
                    put("visit_count", 1)
                    put("last_visited", System.currentTimeMillis())
                }
                db.insertOrThrow("history", null, values)
            }
        }
    }

    /**
     * 获取最近访问历史
     */
    suspend fun getRecent(limit: Int = 100): List<HistoryItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val cursor = db.query(
            "history", null, null, null, null, null,
            "last_visited DESC", limit.toString()
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryItem(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        url = it.getString(it.getColumnIndexOrThrow("url")),
                        visitCount = it.getInt(it.getColumnIndexOrThrow("visit_count")),
                        lastVisited = it.getLong(it.getColumnIndexOrThrow("last_visited"))
                    )
                )
            }
        }
        list
    }

    /**
     * 搜索历史
     */
    suspend fun search(keyword: String): List<HistoryItem> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val like = "%$keyword%"
        val cursor = db.query(
            "history", null,
            "title LIKE ? OR url LIKE ?",
            arrayOf(like, like), null, null,
            "last_visited DESC", "50"
        )
        val list = mutableListOf<HistoryItem>()
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    HistoryItem(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        url = it.getString(it.getColumnIndexOrThrow("url")),
                        visitCount = it.getInt(it.getColumnIndexOrThrow("visit_count")),
                        lastVisited = it.getLong(it.getColumnIndexOrThrow("last_visited"))
                    )
                )
            }
        }
        list
    }

    /**
     * 清除历史
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("history", null, null)
    }

    /**
     * 删除单条记录
     */
    suspend fun delete(id: Long) = withContext(Dispatchers.IO) {
        dbHelper.writableDatabase.delete("history", "id = ?", arrayOf(id.toString()))
    }

    private class HistoryDbHelper(context: Context) :
        SQLiteOpenHelper(context, "history.db", null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("""
                CREATE TABLE history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL DEFAULT '',
                    url TEXT NOT NULL,
                    visit_count INTEGER DEFAULT 1,
                    last_visited INTEGER NOT NULL
                )
            """.trimIndent())
            db.execSQL("CREATE INDEX idx_history_url ON history(url)")
            db.execSQL("CREATE INDEX idx_history_last_visited ON history(last_visited DESC)")
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    }
}
