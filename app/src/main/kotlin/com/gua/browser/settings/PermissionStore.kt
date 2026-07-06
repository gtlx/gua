package com.gua.browser.settings

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/**
 * 网站权限持久化存储
 *
 * 记录用户对每个网站每种权限的选择（allow/deny）。
 * 格式: permissions.json 存为 JSON 字符串
 *   {
 *     "entries": [
 *       {"origin": "https://example.com", "type": 0, "allowed": true},
 *       ...
 *     ]
 *   }
 */
class PermissionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("permissions", Context.MODE_PRIVATE)

    data class PermissionEntry(
        val origin: String,
        val type: Int,
        val allowed: Boolean
    ) {
        val typeName: String get() = when (type) {
            0 -> "地理位置"
            1 -> "桌面通知"
            2 -> "麦克风"
            3 -> "摄像头"
            else -> "未知权限"
        }
    }

    /**
     * 获取保存的权限选择，null 表示未保存
     */
    fun get(origin: String, type: Int): Boolean? {
        return getAll().find { it.origin == origin && it.type == type }?.allowed
    }

    /**
     * 保存权限选择
     */
    fun set(origin: String, type: Int, allowed: Boolean) {
        val entries = getAll().toMutableList()
        entries.removeAll { it.origin == origin && it.type == type }
        entries.add(PermissionEntry(origin, type, allowed))
        saveAll(entries)
    }

    /**
     * 删除某条记录
     */
    fun remove(origin: String, type: Int) {
        val entries = getAll().toMutableList()
        entries.removeAll { it.origin == origin && it.type == type }
        saveAll(entries)
    }

    /**
     * 清空所有记录
     */
    fun clear() {
        prefs.edit().remove("permissions").apply()
    }

    /**
     * 获取所有记录
     */
    fun getAll(): List<PermissionEntry> {
        val json = prefs.getString("permissions", null) ?: return emptyList()
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                PermissionEntry(
                    origin = obj.getString("origin"),
                    type = obj.getInt("type"),
                    allowed = obj.getBoolean("allowed")
                )
            }
        } catch (_: Exception) { emptyList() }
    }

    /**
     * 获取按来源分组的权限
     */
    fun getGroupedByOrigin(): Map<String, List<PermissionEntry>> {
        return getAll().groupBy { it.origin }
    }

    private fun saveAll(entries: List<PermissionEntry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("origin", e.origin)
                put("type", e.type)
                put("allowed", e.allowed)
            })
        }
        prefs.edit().putString("permissions", arr.toString()).apply()
    }
}