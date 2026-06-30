package com.gua.browser.userscript

/**
 * 注入时机
 */
enum class RunAt(val value: String) {
    DOCUMENT_START("document-start"),
    DOCUMENT_END("document-end"),
    DOCUMENT_IDLE("document-idle");

    companion object {
        fun fromValue(value: String): RunAt = entries.find {
            it.value == value
        } ?: DOCUMENT_IDLE
    }
}

/**
 * 用户脚本数据模型
 */
data class UserScript(
    val id: Long = 0,
    val name: String = "",
    val namespace: String? = null,
    val version: String = "1.0",
    val description: String? = null,
    val author: String? = null,
    val homepage: String? = null,
    val icon: String? = null,
    val downloadURL: String? = null,
    val updateURL: String? = null,
    val supportURL: String? = null,
    val license: String? = null,

    /** 匹配规则 */
    val matches: List<String> = emptyList(),
    val includes: List<String> = emptyList(),
    val excludes: List<String> = emptyList(),
    val runAt: RunAt = RunAt.DOCUMENT_IDLE,

    /** 授权 API 列表 */
    val grants: List<String> = emptyList(),

    /** 资源定义 */
    val resources: Map<String, String> = emptyMap(),

    /** 脚本是否启用 */
    val enabled: Boolean = true,

    /** 原始 JS 代码 */
    val code: String = "",
)
