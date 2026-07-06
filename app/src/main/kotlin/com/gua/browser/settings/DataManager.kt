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
import java.io.File
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 数据导入导出管理器
 *
 * 导出格式为 ZIP 压缩包，包含：
 *   bookmarks.json — 书签数据
 *   settings.json  — 设置与搜索引擎
 */
object DataManager {

    private const val BOOKMARKS_FILE = "bookmarks.json"
    private const val SETTINGS_FILE = "settings.json"

    /**
     * 导出为 ZIP 字节数组
     */
    suspend fun exportToZip(state: BrowserState): ByteArray = withContext(Dispatchers.IO) {
        val app = GuaApp.instance
        val bookmarks = app.bookmarkManager.getAll()

        val output = java.io.ByteArrayOutputStream()
        ZipOutputStream(output).use { zos ->
            // 书签
            val bkJson = buildBookmarksJson(bookmarks)
            zos.putNextEntry(ZipEntry(BOOKMARKS_FILE))
            zos.write(bkJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 设置
            val stJson = buildSettingsJson(state)
            zos.putNextEntry(ZipEntry(SETTINGS_FILE))
            zos.write(stJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        output.toByteArray()
    }

    /**
     * 从 ZIP 导入数据
     */
    suspend fun importFromZip(data: ByteArray, state: BrowserState): String? = withContext(Dispatchers.IO) {
        val app = GuaApp.instance
        try {
            ZipInputStream(data.inputStream()).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    val content = zis.readBytes().toString(Charsets.UTF_8)
                    when (entry.name) {
                        BOOKMARKS_FILE -> importBookmarks(content, app)
                        SETTINGS_FILE -> importSettings(content, state)
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            null
        } catch (e: Exception) {
            e.message ?: "导入失败"
        }
    }

    private fun buildBookmarksJson(bookmarks: List<Bookmark>): String {
        val arr = JSONArray()
        bookmarks.forEach { b ->
            arr.put(JSONObject().apply {
                put("title", b.title)
                put("url", b.url)
                put("position", b.position)
            })
        }
        return JSONObject().apply {
            put("version", 1)
            put("count", arr.length())
            put("bookmarks", arr)
        }.toString(2)
    }

    private fun buildSettingsJson(state: BrowserState): String {
        return JSONObject().apply {
            put("version", 1)
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
        }.toString(2)
    }

    private suspend fun importBookmarks(json: String, app: GuaApp) {
        val root = JSONObject(json)
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

    private fun importSettings(json: String, state: BrowserState) {
        val s = JSONObject(json)
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

        if (s.has("searchEngines")) {
            val seArr = s.getJSONArray("searchEngines")
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
                state.activeSearchEngineIndex = s.optInt("activeSearchEngineIndex", 0)
            }
        }
    }

    // ===== Uri 读写工具 =====

    fun readUriBytes(context: Context, uri: Uri): ByteArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (_: Exception) { null }
    }

    fun writeUriBytes(context: Context, uri: Uri, data: ByteArray): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(data) }
            true
        } catch (_: Exception) { false }
    }
}