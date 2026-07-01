package com.gua.browser.userscript

/**
 * URL 匹配引擎
 *
 * 将 @match、@include、@exclude 中的通配符模式编译为正则表达式。
 * 支持 Tampermonkey 完整的匹配语法：
 *   - *        通配符（匹配任意字符，除 /）
 *   - **       超级通配符（匹配任意字符，包括 /）
 *   - ?        匹配单个字符（除 /）
 *   - *.ext    匹配任意域名 + 特定扩展名
 *   - 任意协议://任意域名/任意路径
 *   - https://www.google.com/  === 精确匹配
 *   - *://star.example.com/baz/  带子域名
 */
object URLMatcher {

    /**
     * 检查 URL 是否匹配任意一个模式
     */
    fun matchesAny(url: String, patterns: List<String>): Boolean {
        return patterns.any { pattern -> matches(url, pattern) }
    }

    /**
     * 检查 URL 是否匹配任意一个排除模式
     */
    fun isExcluded(url: String, excludePatterns: List<String>): Boolean {
        return matchesAny(url, excludePatterns)
    }

    // 编译后的正则缓存
    private val patternCache = mutableMapOf<String, Regex>()
    private const val CACHE_MAX_SIZE = 200

    /**
     * 核心匹配逻辑
     */
    fun matches(url: String, pattern: String): Boolean {
        val regex = compile(pattern) ?: return false
        return regex.containsMatchIn(url)
    }

    /**
     * 将 Tampermonkey 通配符模式编译为正则
     * 结果缓存至 patternCache，避免重复编译
     */
    fun compile(pattern: String): Regex? {
        if (pattern.isBlank()) return null

        val sb = StringBuilder()

        // 如果模式不包含协议，自动补 *
        val p = if (!pattern.contains("://") && !pattern.startsWith("/")) {
            val star = '*'
            ("*:/" + '/' + star + star).replace("*", "__WILD__") + pattern
        } else pattern

        var i = 0
        while (i < p.length) {
            val c = p[i]
            when (c) {
                '*' -> {
                    // 检查是否为 **
                    if (i + 1 < p.length && p[i + 1] == '*') {
                        sb.append(".*")  // ** 匹配任意字符（包括 /）
                        i += 2
                    } else {
                        sb.append("[^/]*")  // * 匹配任意字符（不包括 /）
                        i += 1
                    }
                }
                '?' -> {
                    sb.append("[^/]")  // ? 匹配单个字符（不包括 /）
                    i += 1
                }
                '.' -> {
                    sb.append("\\.")
                    i += 1
                }
                '|', '(', ')', '[', ']', '{', '}', '^', '$', '+', '\\' -> {
                    sb.append("\\")
                    sb.append(c)
                    i += 1
                }
                else -> {
                    sb.append(c)
                    i += 1
                }
            }
        }

        return try {
            val regexString = sb.toString()
            // 从缓存读取，避免重复编译
            patternCache[regexString]?.let { return it }
            // 控制缓存大小
            if (patternCache.size >= CACHE_MAX_SIZE) {
                patternCache.clear()
            }
            val regex = Regex(regexString, RegexOption.IGNORE_CASE)
            patternCache[regexString] = regex
            regex
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 检查 @match 是否对给定 URL 生效
     */
    fun shouldInject(url: String, script: UserScript): Boolean {
        if (!script.enabled) return false

        // 1. 检查 exclude 优先
        if (matchesAny(url, script.excludes)) return false

        // 2. 检查 include / match
        val hasInclude = script.includes.isNotEmpty()
        val hasMatch = script.matches.isNotEmpty()

        if (!hasInclude && !hasMatch) {
            // 没有匹配规则 = 所有页面都注入
            return true
        }

        if (hasInclude && matchesAny(url, script.includes)) return true
        if (hasMatch && matchesAny(url, script.matches)) return true

        return false
    }
}
