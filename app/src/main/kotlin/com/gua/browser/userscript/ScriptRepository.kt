package com.gua.browser.userscript

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * 用户脚本存储层
 *
 * 管理用户安装的所有脚本：
 * - 增删改查
 * - 启用/禁用状态
 * - 持久化存储（SharedPreferences JSON）
 */
class ScriptRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("user_scripts", Context.MODE_PRIVATE)

    private val _scripts = MutableStateFlow<List<UserScript>>(emptyList())
    val scripts: StateFlow<List<UserScript>> = _scripts.asStateFlow()

    init {
        loadScripts()
    }

    // ===== 增删改查 =====

    /**
     * 安装新脚本
     * 如果同名脚本已存在，则覆盖
     */
    fun install(code: String): UserScript {
        val parsed = ScriptParser.parse(code)
        val existingIndex = _scripts.value.indexOfFirst {
            it.name == parsed.name && it.namespace == parsed.namespace
        }

        val script = if (existingIndex >= 0) {
            // 覆盖已有脚本
            parsed.copy(id = _scripts.value[existingIndex].id, enabled = true)
        } else {
            parsed.copy(id = System.currentTimeMillis())
        }

        val list = _scripts.value.toMutableList()
        if (existingIndex >= 0) {
            list[existingIndex] = script
        } else {
            list.add(script)
        }

        _scripts.value = list
        saveScripts(list)
        return script
    }

    /**
     * 删除脚本
     */
    fun delete(scriptId: Long) {
        val list = _scripts.value.filter { it.id != scriptId }
        _scripts.value = list
        saveScripts(list)
    }

    /**
     * 更新脚本（修改启用/禁用状态等）
     */
    fun update(script: UserScript) {
        val list = _scripts.value.toMutableList()
        val index = list.indexOfFirst { it.id == script.id }
        if (index >= 0) {
            list[index] = script
            _scripts.value = list
            saveScripts(list)
        }
    }

    /**
     * 切换脚本启用状态
     */
    fun toggleEnabled(scriptId: Long) {
        val list = _scripts.value.toMutableList()
        val index = list.indexOfFirst { it.id == scriptId }
        if (index >= 0) {
            list[index] = list[index].copy(enabled = !list[index].enabled)
            _scripts.value = list
            saveScripts(list)
        }
    }

    /**
     * 根据 URL 获取匹配的已启用脚本
     */
    fun getMatchingScripts(url: String): List<UserScript> {
        return _scripts.value.filter { script ->
            script.enabled && URLMatcher.shouldInject(url, script)
        }
    }

    fun getScriptById(id: Long): UserScript? {
        return _scripts.value.find { it.id == id }
    }

    // ===== 持久化 =====

    private fun loadScripts() {
        val json = prefs.getString("scripts_json", null) ?: return
        val list = mutableListOf<UserScript>()
        try {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val obj = arr.getJSONObject(i)
                list.add(jsonToScript(obj))
            }
        } catch (_: Exception) {}
        _scripts.value = list
    }

    private fun saveScripts(scripts: List<UserScript>) {
        val arr = JSONArray()
        scripts.forEach { script ->
            arr.put(scriptToJson(script))
        }
        prefs.edit().putString("scripts_json", arr.toString()).apply()
    }

    private fun scriptToJson(script: UserScript): JSONObject {
        return JSONObject().apply {
            put("id", script.id)
            put("name", script.name)
            put("namespace", script.namespace ?: "")
            put("version", script.version)
            put("description", script.description ?: "")
            put("author", script.author ?: "")
            put("match", JSONArray(script.matches))
            put("include", JSONArray(script.includes))
            put("exclude", JSONArray(script.excludes))
            put("runAt", script.runAt.value)
            put("grant", JSONArray(script.grants))
            put("enabled", script.enabled)
            put("code", script.code)
        }
    }

    private fun jsonToScript(obj: JSONObject): UserScript {
        return UserScript(
            id = obj.optLong("id", 0),
            name = obj.optString("name", ""),
            namespace = obj.optString("namespace").ifBlank { null },
            version = obj.optString("version", "1.0"),
            description = obj.optString("description").ifBlank { null },
            author = obj.optString("author").ifBlank { null },
            matches = jsonArrayToList(obj.optJSONArray("match")),
            includes = jsonArrayToList(obj.optJSONArray("include")),
            excludes = jsonArrayToList(obj.optJSONArray("exclude")),
            runAt = RunAt.fromValue(obj.optString("runAt", "document-idle")),
            grants = jsonArrayToList(obj.optJSONArray("grant")),
            enabled = obj.optBoolean("enabled", true),
            code = obj.optString("code", "")
        )
    }

    private fun jsonArrayToList(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        return (0 until arr.length()).map { arr.optString(it, "") }
            .filter { it.isNotBlank() }
    }
}
