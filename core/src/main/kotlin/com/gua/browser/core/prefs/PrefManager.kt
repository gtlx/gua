package com.gua.browser.core.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.prefStore by preferencesDataStore(name = "app_settings")

/**
 * 应用设置管理器
 * 存储用户偏好设置（夜间模式、广告过滤开关等）
 */
class PrefManager(private val context: Context) {

    // ===== 按键定义 =====
    object Keys {
        val NIGHT_MODE = booleanPreferencesKey("night_mode")
        val ADBLOCK_ENABLED = booleanPreferencesKey("adblock_enabled")
        val DESKTOP_MODE = booleanPreferencesKey("desktop_mode")
        val SEARCH_ENGINE = stringPreferencesKey("search_engine")
        val HOME_PAGE = stringPreferencesKey("home_page")
        val DEFAULT_ZOOM = floatPreferencesKey("default_zoom")
        val TEXT_SIZE = intPreferencesKey("text_size")
        val SAVE_HISTORY = booleanPreferencesKey("save_history")
        val SAVE_PASSWORDS = booleanPreferencesKey("save_passwords")
        val BLOCK_IMAGES = booleanPreferencesKey("block_images")
        val JAVASCRIPT_ENABLED = booleanPreferencesKey("javascript_enabled")
        val RECENT_URL = stringPreferencesKey("recent_url_")
    }

    // ===== 通用存取 =====
    private suspend fun <T> get(key: Preferences.Key<T>): T? {
        return context.prefStore.data.first()[key]
    }

    private suspend fun <T> set(key: Preferences.Key<T>, value: T) {
        context.prefStore.edit { prefs ->
            prefs[key] = value
        }
    }

    private fun <T> observe(key: Preferences.Key<T>, default: T): Flow<T> {
        return context.prefStore.data.map { prefs ->
            prefs[key] ?: default
        }
    }

    // ===== 具体设置项 =====
    val nightMode: Flow<Boolean> = observe(Keys.NIGHT_MODE, false)
    suspend fun setNightMode(enabled: Boolean) = set(Keys.NIGHT_MODE, enabled)
    suspend fun isNightMode(): Boolean = get(Keys.NIGHT_MODE) ?: false

    val adblockEnabled: Flow<Boolean> = observe(Keys.ADBLOCK_ENABLED, true)
    suspend fun setAdblockEnabled(enabled: Boolean) = set(Keys.ADBLOCK_ENABLED, enabled)
    suspend fun isAdblockEnabled(): Boolean = get(Keys.ADBLOCK_ENABLED) ?: true

    val desktopMode: Flow<Boolean> = observe(Keys.DESKTOP_MODE, false)
    suspend fun setDesktopMode(enabled: Boolean) = set(Keys.DESKTOP_MODE, enabled)
    suspend fun isDesktopMode(): Boolean = get(Keys.DESKTOP_MODE) ?: false

    val searchEngine: Flow<String> = observe(Keys.SEARCH_ENGINE, "https://www.baidu.com/s?wd=%s")
    suspend fun setSearchEngine(url: String) = set(Keys.SEARCH_ENGINE, url)

    val homePage: Flow<String> = observe(Keys.HOME_PAGE, "about:start")
    suspend fun setHomePage(url: String) = set(Keys.HOME_PAGE, url)

    val textSize: Flow<Int> = observe(Keys.TEXT_SIZE, 100)
    suspend fun setTextSize(size: Int) = set(Keys.TEXT_SIZE, size)

    val saveHistory: Flow<Boolean> = observe(Keys.SAVE_HISTORY, true)
    suspend fun setSaveHistory(enabled: Boolean) = set(Keys.SAVE_HISTORY, enabled)

    val javascriptEnabled: Flow<Boolean> = observe(Keys.JAVASCRIPT_ENABLED, true)
    suspend fun setJavascriptEnabled(enabled: Boolean) = set(Keys.JAVASCRIPT_ENABLED, enabled)

    // ===== 最近网址（快速恢复） =====
    suspend fun saveRecentUrl(tabId: Int, url: String) {
        set(stringPreferencesKey(Keys.RECENT_URL.name + tabId), url)
    }

    suspend fun getRecentUrl(tabId: Int): String? {
        return get(stringPreferencesKey(Keys.RECENT_URL.name + tabId))
    }
}
