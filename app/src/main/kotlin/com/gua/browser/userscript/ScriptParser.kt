package com.gua.browser.userscript

/**
 * 油猴脚本元信息解析器
 *
 * 解析脚本头部注释中的 @name、@match、@grant 等元数据。
 * 遵循 Greasemonkey / Tampermonkey 规范。
 *
 * 输入示例：
 * ```
 * // ==UserScript==
 * // @name         示例脚本
 * // @namespace    http://example.com
 * // @version      1.0
 * // @description  这是一个示例
 * // @author       user
 * // @match        *://star.example.com/
 * // @match        https://www.google.com/
 * // @grant        GM_setValue
 * // @grant        GM_getValue
 * // @grant        GM_xmlhttpRequest
 * // @run-at       document-end
 * // ==/UserScript==
 * ...
 * ```
 */
object ScriptParser {

    private val HEADER_START = Regex("// ==UserScript==")
    private val HEADER_END = Regex("// ==/UserScript==")
    private val DIRECTIVE = Regex("// @(\\w+)\\s+(.*)")

    /**
     * 从脚本代码中解析 UserScript 对象
     */
    fun parse(rawCode: String): UserScript {
        val header = extractHeader(rawCode) ?: return UserScript(code = rawCode)

        val directives = parseDirectives(header)

        return UserScript(
            name = directives.getFirst("name") ?: "未命名脚本",
            namespace = directives.getFirst("namespace"),
            version = directives.getFirst("version") ?: "1.0",
            description = directives.getFirst("description"),
            author = directives.getFirst("author"),
            homepage = directives.getFirst("homepage") ?: directives.getFirst("source"),
            icon = directives.getFirst("icon"),
            downloadURL = directives.getFirst("downloadURL") ?: directives.getFirst("downloadurl"),
            updateURL = directives.getFirst("updateURL") ?: directives.getFirst("updateurl"),
            supportURL = directives.getFirst("supportURL"),
            license = directives.getFirst("license"),

            matches = directives.getAll("match"),
            includes = directives.getAll("include"),
            excludes = directives.getAll("exclude"),
            runAt = UserScript.RunAt.fromValue(
                directives.getFirst("run-at") ?: "document-idle"
            ),

            grants = directives.getAll("grant"),
            resources = parseResources(directives.getAll("resource")),

            code = rawCode
        )
    }

    /**
     * 从完整脚本代码中提取头部注释块
     */
    private fun extractHeader(code: String): String? {
        val lines = code.lines()
        val startIdx = lines.indexOfFirst { HEADER_START.matches(it.trim()) }
        if (startIdx == -1) return null

        val endIdx = lines.indexOfFirst { HEADER_END.matches(it.trim()) }
        if (endIdx == -1 || endIdx <= startIdx) return null

        return lines.subList(startIdx + 1, endIdx).joinToString("\n")
    }

    /**
     * 解析头部中的指令行
     */
    private fun parseDirectives(header: String): Map<String, List<String>> {
        val result = mutableMapOf<String, MutableList<String>>()
        header.lines().forEach { line ->
            DIRECTIVE.find(line.trim())?.let { match ->
                val key = match.groupValues[1].lowercase()
                val value = match.groupValues[2].trim()
                result.getOrPut(key) { mutableListOf() }.add(value)
            }
        }
        return result
    }

    /**
     * 解析 @resource 指令
     * 格式: // @resource resourceName https://example.com/file.css
     */
    private fun parseResources(resources: List<String>): Map<String, String> {
        val result = mutableMapOf<String, String>()
        resources.forEach { res ->
            val parts = res.split("\\s+".toRegex(), 2)
            if (parts.size == 2) {
                result[parts[0]] = parts[1]
            }
        }
        return result
    }

    /**
     * 提取脚本体的纯代码（除去头部注释）
     */
    fun extractBody(rawCode: String): String {
        val lines = rawCode.lines()
        val endIdx = lines.indexOfFirst { HEADER_END.matches(it.trim()) }
        return if (endIdx != -1) {
            lines.drop(endIdx + 1).joinToString("\n")
        } else {
            rawCode
        }
    }

    /** 辅助：取单个值 */
    private fun Map<String, List<String>>.getFirst(key: String): String? {
        return this[key.lowercase()]?.firstOrNull()
    }

    /** 辅助：取所有值 */
    private fun Map<String, List<String>>.getAll(key: String): List<String> {
        return this[key.lowercase()] ?: emptyList()
    }
}
