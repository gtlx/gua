package com.gua.browser.ui

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.stateStore by preferencesDataStore(name = "browser_state")

class BrowserStateSaver(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        val KEY_NIGHT_MODE = intPreferencesKey("night_mode")
        val KEY_ADBLOCK = intPreferencesKey("adblock")
        val KEY_DESKTOP = intPreferencesKey("desktop")
        val KEY_SEARCH_ENGINES = stringPreferencesKey("search_engines")
        val KEY_ACTIVE_SEARCH = intPreferencesKey("active_search")
        val KEY_TOOLBAR_POS = intPreferencesKey("toolbar_pos")
        val KEY_SHOW_URLBAR = intPreferencesKey("show_urlbar")
        val KEY_SHOW_BACK = intPreferencesKey("show_back")
        val KEY_SHOW_FORWARD = intPreferencesKey("show_forward")
        val KEY_SHOW_HOME = intPreferencesKey("show_home")
        val KEY_SHOW_TABS = intPreferencesKey("show_tabs")
        val KEY_SHOW_MENU = intPreferencesKey("show_menu")
    }

    suspend fun load(state: BrowserState) {
        val prefs = context.stateStore.data.first()

        state.isNightMode = prefs[KEY_NIGHT_MODE] == 1
        state.isAdblockEnabled = prefs[KEY_ADBLOCK] != 0
        state.isDesktopMode = prefs[KEY_DESKTOP] == 1
        state.toolbarPosition = if (prefs[KEY_TOOLBAR_POS] == 1)
            BrowserState.ToolbarPos.TOP else BrowserState.ToolbarPos.BOTTOM
        state.showUrlBar = prefs[KEY_SHOW_URLBAR] != 0
        state.showBackBtn = prefs[KEY_SHOW_BACK] != 0
        state.showForwardBtn = prefs[KEY_SHOW_FORWARD] != 0
        state.showHomeBtn = prefs[KEY_SHOW_HOME] != 0
        state.showTabsBtn = prefs[KEY_SHOW_TABS] != 0
        state.showMenuBtn = prefs[KEY_SHOW_MENU] != 0

        val enginesJson = prefs[KEY_SEARCH_ENGINES]
        if (enginesJson != null) {
            try {
                val arr = JSONArray(enginesJson)
                val engines = mutableListOf<SearchEngine>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    engines.add(SearchEngine(
                        name = obj.getString("name"),
                        urlTemplate = obj.getString("url"),
                        shortName = obj.getString("short")
                    ))
                }
                if (engines.isNotEmpty()) state.searchEngines = engines
            } catch (_: Exception) {}
        }
        state.activeSearchEngineIndex = prefs[KEY_ACTIVE_SEARCH] ?: 0
    }

    fun save(state: BrowserState) {
        scope.launch {
            context.stateStore.edit { prefs ->
                prefs[KEY_NIGHT_MODE] = if (state.isNightMode) 1 else 0
                prefs[KEY_ADBLOCK] = if (state.isAdblockEnabled) 1 else 0
                prefs[KEY_DESKTOP] = if (state.isDesktopMode) 1 else 0
                prefs[KEY_TOOLBAR_POS] = if (state.toolbarPosition == BrowserState.ToolbarPos.TOP) 1 else 0
                prefs[KEY_SHOW_URLBAR] = if (state.showUrlBar) 1 else 0
                prefs[KEY_SHOW_BACK] = if (state.showBackBtn) 1 else 0
                prefs[KEY_SHOW_FORWARD] = if (state.showForwardBtn) 1 else 0
                prefs[KEY_SHOW_HOME] = if (state.showHomeBtn) 1 else 0
                prefs[KEY_SHOW_TABS] = if (state.showTabsBtn) 1 else 0
                prefs[KEY_SHOW_MENU] = if (state.showMenuBtn) 1 else 0
                prefs[KEY_ACTIVE_SEARCH] = state.activeSearchEngineIndex

                val arr = JSONArray()
                state.searchEngines.forEach { engine ->
                    arr.put(JSONObject().apply {
                        put("name", engine.name)
                        put("url", engine.urlTemplate)
                        put("short", engine.shortName)
                    })
                }
                prefs[KEY_SEARCH_ENGINES] = arr.toString()
            }
        }
    }
}
