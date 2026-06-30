package com.gua.browser.settings

import android.content.Context
import android.net.Uri
import com.gua.browser.GuaApp
import com.gua.browser.bookmark.Bookmark
import com.gua.browser.ui.BrowserState
import com.gua.browser.ui.SearchEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter

/**
 * 数据导入导出管理器
 */
object DataManager {

    /**
     * 导出所有数据为 JSON 字符串
     */
    suspend fun exportToJson(state: BrowserState): String = withContext(Dispatchers.IO) {
        val app = GuaApp.instance
        val bookmarks = app.bookmarkManager.getAll()
        val history = app.historyManager.getRecent()

        val root = JSONObject().apply {
            put("version", 1)
            put("exported_at", System.currentTimeMillis())

            // 书签
            val bkArr = JSONArray()
            bookmarks.forEach { b ->
                bkArr.put(JSONObject().apply {
                    put("title", b.title)
                    put("url", b.url)
                    put("position", b.position)
                })
            }
            put("bookmarks", bkArr)

            // 布局设置
            put("settings", JSONObject().apply {
                put("toolbarPosition", if (state.toolbarPosition == BrowserState.ToolbarPos.TOP) "top" else "bottom")
                put("showUrlBar", state.showUrlBar)
                put("showBackBtn", state.showBackBtn)
                put("showForwardBtn", state.showForwardBtn)
                put("showHomeBtn", state.showHomeBtn)
                put("showTabsBtn", state.showTabsBtn)
                put("showMenuBtn", state.showMenuBtn)
                put("nightMode", state.isNightMode)
                put("adblockEnabled", state.isAdblockEnabled)
                put("desktopMode", state.isDesktopMode)
            })

            // 搜索引擎
            val seArr = JSONArray()
            state.searchEngines.forEach { se ->
                seArr.put(JSONObject().apply {
                    put("name", se.name)
                    put("url", se.urlTemplate)
                    put("short", se.shortName)
                })
            }
            put("searchEngines", seArr)
            put("activeSearchEngineIndex", state.activeSearchEngineIndex)
        }

        root.toString(2)
    }

    /**
     * 从 JSON 导入数据
     */
    suspend fun importFromJson(json: String, state: BrowserState): String? = withContext(Dispatchers.IO) {
        try {
            val root = JSONObject(json)
            val app = GuaApp.instance

            // 导入书签
            if (root.has("bookmarks")) {
                val bkArr = root.getJSONArray("bookmarks")
                for (i in 0 until bkArr.length()) {
                    val obj = bkArr.getJSONObject(i)
                    val url = obj.getString("url")
                    if (!app.bookmarkManager.exists(url)) {
                        app.bookmarkManager.add(Bookmark(
                            title = obj.getString("title"),
                            url = url,
                            position = obj.optInt("position", 0)
                        ))
                    }
                }
            }

            // 导入设置
            if (root.has("settings")) {
                val s = root.getJSONObject("settings")
                state.toolbarPosition = if (s.optString("toolbarPosition") == "top")
                    BrowserState.ToolbarPos.TOP else BrowserState.ToolbarPos.BOTTOM
                state.showUrlBar = s.optBoolean("showUrlBar", true)
                state.showBackBtn = s.optBoolean("showBackBtn", true)
                state.showForwardBtn = s.optBoolean("showForwardBtn", true)
                state.showHomeBtn = s.optBoolean("showHomeBtn", true)
                state.showTabsBtn = s.optBoolean("showTabsBtn", true)
                state.showMenuBtn = s.optBoolean("showMenuBtn", true)
                state.isNightMode = s.optBoolean("nightMode", false)
                state.isAdblockEnabled = s.optBoolean("adblockEnabled", true)
                state.isDesktopMode = s.optBoolean("desktopMode", false)
            }

            // 导入搜索引擎
            if (root.has("searchEngines")) {
                val seArr = root.getJSONArray("searchEngines")
                val engines = mutableListOf<SearchEngine>()
                for (i in 0 until seArr.length()) {
                    val obj = seArr.getJSONObject(i)
                    engines.add(SearchEngine(
                        name = obj.getString("name"),
                        urlTemplate = obj.getString("url"),
                        shortName = obj.getString("short")
                    ))
                }
                if (engines.isNotEmpty()) {
                    state.searchEngines = engines
                    state.activeSearchEngineIndex = root.optInt("activeSearchEngineIndex", 0)
                }
            }

            null
        } catch (e: Exception) {
            e.message ?: "导入失败"
        }
    }

    /**
     * 读取 Uri 内容
     */
    fun readUriContent(context: Context, uri: Uri): String? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BufferedReader(InputStreamReader(stream)).readText()
            }
        } catch (_: Exception) { null }
    }

    /**
     * 写入 Uri
     */
    fun writeUriContent(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { stream ->
                OutputStreamWriter(stream).use { it.write(content) }
            }
            true
        } catch (_: Exception) { false }
    }
}
