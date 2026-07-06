package com.gua.browser.ui

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

private val Context.stateStore by preferencesDataStore(name = "browser_state")

class BrowserStateSaver(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val KEY_NIGHT_MODE = booleanPreferencesKey("night_mode")
        val KEY_ADBLOCK = booleanPreferencesKey("adblock")
        val KEY_DESKTOP = booleanPreferencesKey("desktop")
        val KEY_SEARCH_ENGINES = stringPreferencesKey("search_engines")
        val KEY_ACTIVE_SEARCH = intPreferencesKey("active_search")
        val KEY_TOOLBAR_POS = booleanPreferencesKey("toolbar_pos")
        val KEY_SHOW_URLBAR = booleanPreferencesKey("show_urlbar")
        val KEY_SHOW_BACK = booleanPreferencesKey("show_back")
        val KEY_SHOW_FORWARD = booleanPreferencesKey("show_forward")
        val KEY_SHOW_HOME = booleanPreferencesKey("show_home")
        val KEY_SHOW_TABS = booleanPreferencesKey("show_tabs")
        val KEY_SHOW_MENU = booleanPreferencesKey("show_menu")
        val KEY_INCOGNITO = booleanPreferencesKey("incognito")
        val KEY_CUSTOM_AD_RULES = stringPreferencesKey("custom_ad_rules")
    }

    private var saveJob: Job? = null

    suspend fun load(state: BrowserState) {
        val prefs = context.stateStore.data.first()

        state.isNightMode = prefs[KEY_NIGHT_MODE] ?: false
        state.isAdblockEnabled = prefs[KEY_ADBLOCK] ?: true
        state.isDesktopMode = prefs[KEY_DESKTOP] ?: false
        state.isIncognito = prefs[KEY_INCOGNITO] ?: false
        state.toolbarPosition = if (prefs[KEY_TOOLBAR_POS] == true)
            BrowserState.ToolbarPos.TOP else BrowserState.ToolbarPos.BOTTOM
        state.showUrlBar = prefs[KEY_SHOW_URLBAR] ?: true
        state.showBackBtn = prefs[KEY_SHOW_BACK] ?: false
        state.showForwardBtn = prefs[KEY_SHOW_FORWARD] ?: false
        state.showHomeBtn = prefs[KEY_SHOW_HOME] ?: true
        state.showTabsBtn = prefs[KEY_SHOW_TABS] ?: true
        state.showMenuBtn = prefs[KEY_SHOW_MENU] ?: true

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

        val rulesJson = prefs[KEY_CUSTOM_AD_RULES]
        if (rulesJson != null) {
            try {
                val arr = JSONArray(rulesJson)
                val rules = mutableListOf<BrowserState.AdRule>()
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    rules.add(BrowserState.AdRule(
                        pattern = obj.getString("pattern"),
                        enabled = obj.optBoolean("enabled", true)
                    ))
                }
                state.customAdRules = rules
            } catch (_: Exception) {}
        }
    }

    fun save(state: BrowserState) {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            context.stateStore.edit { prefs ->
                prefs[KEY_NIGHT_MODE] = state.isNightMode
                prefs[KEY_ADBLOCK] = state.isAdblockEnabled
                prefs[KEY_DESKTOP] = state.isDesktopMode
                prefs[KEY_INCOGNITO] = state.isIncognito
                prefs[KEY_TOOLBAR_POS] = state.toolbarPosition == BrowserState.ToolbarPos.TOP
                prefs[KEY_SHOW_URLBAR] = state.showUrlBar
                prefs[KEY_SHOW_BACK] = state.showBackBtn
                prefs[KEY_SHOW_FORWARD] = state.showForwardBtn
                prefs[KEY_SHOW_HOME] = state.showHomeBtn
                prefs[KEY_SHOW_TABS] = state.showTabsBtn
                prefs[KEY_SHOW_MENU] = state.showMenuBtn
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

                val rulesArr = JSONArray()
                state.customAdRules.forEach { rule ->
                    rulesArr.put(JSONObject().apply {
                        put("pattern", rule.pattern)
                        put("enabled", rule.enabled)
                    })
                }
                prefs[KEY_CUSTOM_AD_RULES] = rulesArr.toString()
            }
        }
    }

    fun destroy() {
        saveJob?.cancel()
        scope.cancel()
    }
}
