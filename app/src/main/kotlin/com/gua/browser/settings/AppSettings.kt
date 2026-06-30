package com.gua.browser.settings

import android.content.Context
import com.gua.browser.core.prefs.PrefManager

/**
 * 应用设置
 *
 * 在 PrefManager 基础上封装业务层设置项。
 */
class AppSettings(context: Context) {

    private val prefs = PrefManager(context)

    val nightMode = prefs.nightMode
    suspend fun setNightMode(enabled: Boolean) = prefs.setNightMode(enabled)
    suspend fun isNightMode() = prefs.isNightMode()

    val adblockEnabled = prefs.adblockEnabled
    suspend fun setAdblockEnabled(enabled: Boolean) = prefs.setAdblockEnabled(enabled)

    val desktopMode = prefs.desktopMode
    suspend fun setDesktopMode(enabled: Boolean) = prefs.setDesktopMode(enabled)

    val searchEngine = prefs.searchEngine
    suspend fun setSearchEngine(url: String) = prefs.setSearchEngine(url)

    val homePage = prefs.homePage
    suspend fun setHomePage(url: String) = prefs.setHomePage(url)

    val textSize = prefs.textSize
    suspend fun setTextSize(size: Int) = prefs.setTextSize(size)

    val saveHistory = prefs.saveHistory
    suspend fun setSaveHistory(enabled: Boolean) = prefs.setSaveHistory(enabled)

    val javascriptEnabled = prefs.javascriptEnabled
    suspend fun setJavascriptEnabled(enabled: Boolean) = prefs.setJavascriptEnabled(enabled)

    // ===== 搜索引擎预设 =====
    object SearchEngines {
        val BAIDU = "https://www.baidu.com/s?wd=%s"
        val BING = "https://www.bing.com/search?q=%s"
        val GOOGLE = "https://www.google.com/search?q=%s"
        val SOGOU = "https://www.sogou.com/web?query=%s"
        val DUCKDUCKGO = "https://duckduckgo.com/?q=%s"
    }
}
