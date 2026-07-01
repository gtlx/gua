package com.gua.browser.adblock

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 广告过滤引擎
 *
 * 基于 uBlock Origin 规则列表，在 URL 请求时判定是否拦截。
 * 拦截点：GeckoSession.navigationDelegate.onLoadRequest
 *
 * 规则来源：EasyList + EasyPrivacy + CJX's Annoyance List
 *
 * 优化：预编译所有正则规则，避免每次 URL 检查时重复编译。
 */
class AdBlockEngine(private val context: Context) {

    /** 规则类型 */
    enum class RuleType { DOMAIN, EXACT, PREFIX, REGEX, URL_PATTERN }

    companion object {
        private const val TAG = "AdBlockEngine"
        private const val RULES_FILE = "easylist_mini.txt"
    }

    /** 预处理后的编译规则 */
    private data class CompiledRule(
        val pattern: String,
        val type: RuleType,
        val isException: Boolean = false,
        val regex: Regex? = null  // 预编译的正则
    )

    private val rules = mutableListOf<CompiledRule>()
    private var isReady = false

    /**
     * 初始化规则引擎，从 assets 加载规则
     */
    suspend fun init() = withContext(Dispatchers.IO) {
        if (isReady) return@withContext
        try {
            loadRulesFromAssets()
            isReady = true
            Log.d(TAG, "广告规则加载完成: ${rules.size} 条")
        } catch (e: Exception) {
            Log.e(TAG, "规则加载失败", e)
        }
    }

    private fun loadRulesFromAssets() {
        try {
            context.assets.open(RULES_FILE).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                    reader.lineSequence().forEach { line ->
                        val trimmed = line.trim()
                        if (trimmed.isNotEmpty() && !trimmed.startsWith("!")) {
                            parseAndAddRule(trimmed)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "未找到规则文件，使用内置默认规则")
            addDefaultRules()
        }
    }

    private fun parseAndAddRule(line: String) {
        var pattern = line
        val isException = line.startsWith("@@")

        if (isException) {
            pattern = pattern.removePrefix("@@").trimStart()
        }

        val (ruleType, precompiledRegex) = when {
            pattern.startsWith("||") && pattern.endsWith("^") -> {
                val domain = pattern.removePrefix("||").removeSuffix("^")
                Pair(RuleType.DOMAIN, null)
            }
            pattern.startsWith("|") && pattern.endsWith("|") -> {
                val exact = pattern.removePrefix("|").removeSuffix("|")
                Pair(RuleType.EXACT, null)
            }
            pattern.startsWith("/") && pattern.endsWith("/") -> {
                val regexStr = pattern.removePrefix("/").removeSuffix("/")
                val regex = try { Regex(regexStr) } catch (_: Exception) { null }
                Pair(RuleType.REGEX, regex)
            }
            pattern.contains("*") -> {
                val regexStr = pattern
                    .replace(".", "\\.")
                    .replace("*", ".*")
                val regex = try { Regex(regexStr) } catch (_: Exception) { null }
                Pair(RuleType.URL_PATTERN, regex)
            }
            else -> {
                Pair(RuleType.PREFIX, null)
            }
        }

        rules.add(CompiledRule(
            pattern = pattern,
            type = ruleType,
            isException = isException,
            regex = precompiledRegex
        ))
    }

    /**
     * 检查 URL 是否应该被拦截
     * @return true = 拦截, false = 放行
     */
    fun shouldBlock(url: String): Boolean {
        if (!isReady || rules.isEmpty()) return false

        var blocked = false

        for (rule in rules) {
            val matches = when (rule.type) {
                RuleType.DOMAIN -> url.contains(rule.pattern)
                RuleType.EXACT -> url == rule.pattern
                RuleType.PREFIX -> url.contains(rule.pattern)
                RuleType.REGEX -> rule.regex?.containsMatchIn(url) ?: false
                RuleType.URL_PATTERN -> rule.regex?.containsMatchIn(url) ?: false
            }

            if (matches) {
                if (rule.isException) {
                    // 白名单优先返回
                    return false
                }
                blocked = true
            }
        }

        return blocked
    }

    /**
     * 检查请求类型为文档（主框架）时是否拦截
     */
    fun shouldBlockMainFrame(url: String): Boolean = false

    private fun addDefaultRules() {
        val defaultRules = listOf(
            "||doubleclick.net^",
            "||googlesyndication.com^",
            "||googleadservices.com^",
            "||google-analytics.com^",
            "||googletagmanager.com^",
            "||adservice.google.com^",
            "||pagead2.googlesyndication.com^",
            "||adzerk.net^",
            "||exoclick.com^",
            "||popads.net^",
            "||propellerads.com^",
            "||pos.baidu.com^",
            "||cpro.baidustatic.com^",
            "||sogou.com/feedback/"
        )
        defaultRules.forEach { parseAndAddRule(it) }
    }

    // ===== 测试辅助方法 =====

    /**
     * 测试用：判断规则是否为白名单例外
     */
    fun isExceptionRule(line: String): Boolean {
        return line.trimStart().startsWith("@@")
    }

    /**
     * 测试用：检查 URL 是否包含指定模式（绕过完整引擎初始化）
     */
    fun ruleMatches(url: String, pattern: String): Boolean {
        return url.contains(pattern)
    }
}
