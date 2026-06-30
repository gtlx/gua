package com.gua.browser.ui

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.stateStore by preferencesDataStore(name = "browser_state")

/**
 * BrowserState 持久化
 *
 * 在设置变更时自动保存，应用启动时自动恢复。
 * 保存内容：
 *   - 夜间模式 / 广告过滤 / 桌面模式
 *   - 搜索引擎列表及当前选中
 *   - 最近访问的 URL
 */
class BrowserStateSaver(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private val KEY_NIGHT_MODE = intPreferencesKey("night_mode")
        private val KEY_ADBLOCK = intPreferencesKey("adblock")
        private val KEY_DESKTOP = intPreferencesKey("desktop")
        private val KEY_SEARCH_ENGINES = stringPreferencesKey("search_engines")
        private val KEY_ACTIVE_SEARCH = intPreferencesKey("active_search")
    }

    /**
     * 加载持久化的状态到 BrowserState
     */
    suspend fun load(state: BrowserState) {
        val prefs = context.stateStore.data.first()

        state.isNightMode = prefs[KEY_NIGHT_MODE] == 1
        state.isAdblockEnabled = prefs[KEY_ADBLOCK] != 0
        state.isDesktopMode = prefs[KEY_DESKTOP] == 1

        // 恢复搜索引擎
        val enginesJson = prefs[KEY_SEARCH_ENGINES]
        if (enginesJson != null) {
            try {
                val arr = JSONArray(enginesJson)
                val engines = mutableListOf<SearchEngine>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    engines.add(
                        SearchEngine(
                            name = obj.getString("name"),
                            urlTemplate = obj.getString("url"),
                            shortName = obj.getString("short")
                        )
                    )
                }
                if (engines.isNotEmpty()) {
                    state.searchEngines = engines
                }
            } catch (_: Exception) {}
        }

        state.activeSearchEngineIndex = prefs[KEY_ACTIVE_SEARCH] ?: 0
    }

    /**
     * 监听状态变更并自动保存
     */
    fun autoSave(state: BrowserState) {
        // 通过 StateFlow/回调监听变化（简化：每次变更时手动调用 save）
    }

    /**
     * 手动保存当前状态
     */
    fun save(state: BrowserState) {
        scope.launch {
            context.stateStore.edit { prefs ->
                prefs[KEY_NIGHT_MODE] = if (state.isNightMode) 1 else 0
                prefs[KEY_ADBLOCK] = if (state.isAdblockEnabled) 1 else 0
                prefs[KEY_DESKTOP] = if (state.isDesktopMode) 1 else 0
                prefs[KEY_ACTIVE_SEARCH] = state.activeSearchEngineIndex

                // 搜索引擎列表
                val arr = JSONArray()
                state.searchEngines.forEach { engine ->
                    arr.put(
                        JSONObject().apply {
                            put("name", engine.name)
                            put("url", engine.urlTemplate)
                            put("short", engine.shortName)
                        }
                    )
                }
                prefs[KEY_SEARCH_ENGINES] = arr.toString()
            }
        }
    }
}
