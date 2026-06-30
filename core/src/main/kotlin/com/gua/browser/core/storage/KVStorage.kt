package com.gua.browser.core.storage

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

private val Context.dataStore by preferencesDataStore(name = "gua_browser_prefs")

/**
 * 轻量级 KV 存储，基于 DataStore
 * 用于 GM_setValue / GM_getValue 的存储后端
 *
 * 异步方法（put/delete）使用外部传入的 CoroutineScope，
 * 避免每次调用创建新 Scope 导致泄漏。
 */
class KVStorage(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    fun put(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs[prefKey] = value
            }
        }
    }

    suspend fun putSync(key: String, value: String) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { prefs ->
            prefs[prefKey] = value
        }
    }

    fun get(key: String): Flow<String?> {
        val prefKey = stringPreferencesKey(key)
        return context.dataStore.data.map { prefs ->
            prefs[prefKey]
        }
    }

    suspend fun getSync(key: String): String? {
        val prefKey = stringPreferencesKey(key)
        return context.dataStore.data.first()[prefKey]
    }

    fun delete(key: String) {
        val prefKey = stringPreferencesKey(key)
        scope.launch {
            context.dataStore.edit { prefs ->
                prefs.remove(prefKey)
            }
        }
    }

    suspend fun deleteSync(key: String) {
        val prefKey = stringPreferencesKey(key)
        context.dataStore.edit { prefs ->
            prefs.remove(prefKey)
        }
    }

    suspend fun keys(): Set<String> {
        return context.dataStore.data.first().asMap().keys.map { it.name }.toSet()
    }
}
