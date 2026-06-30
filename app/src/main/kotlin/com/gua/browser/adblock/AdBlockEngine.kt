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
 */
class AdBlockEngine(private val context: Context) {

    companion object {
        private const val TAG = "AdBlockEngine"
        private const val RULES_FILE = "easylist_mini.txt"
    }

    private val rules = mutableListOf<AdBlockRule>()
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
            // assets 中可能没有规则文件，使用内置默认规则
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

        val rule = when {
            pattern.startsWith("||") && pattern.endsWith("^") -> {
                val domain = pattern.removePrefix("||").removeSuffix("^")
                AdBlockRule(domain, AdBlockRule.RuleType.DOMAIN, isException)
            }
            pattern.startsWith("|") && pattern.endsWith("|") -> {
                val exact = pattern.removePrefix("|").removeSuffix("|")
                AdBlockRule(exact, AdBlockRule.RuleType.EXACT, isException)
            }
            pattern.startsWith("/") && pattern.endsWith("/") -> {
                val regex = pattern.removePrefix("/").removeSuffix("/")
                AdBlockRule(regex, AdBlockRule.RuleType.REGEX, isException)
            }
            pattern.contains("*") -> {
                AdBlockRule(pattern, AdBlockRule.RuleType.URL_PATTERN, isException)
            }
            else -> {
                AdBlockRule(pattern, AdBlockRule.RuleType.PREFIX, isException)
            }
        }

        rules.add(rule)
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
                AdBlockRule.RuleType.DOMAIN -> url.contains(rule.pattern)
                AdBlockRule.RuleType.EXACT -> url == rule.pattern
                AdBlockRule.RuleType.PREFIX -> url.contains(rule.pattern)
                AdBlockRule.RuleType.REGEX -> {
                    try { Regex(rule.pattern).containsMatchIn(url) }
                    catch (_: Exception) { false }
                }
                AdBlockRule.RuleType.URL_PATTERN -> {
                    val regex = rule.pattern
                        .replace(".", "\\.")
                        .replace("*", ".*")
                    try { Regex(regex).containsMatchIn(url) }
                    catch (_: Exception) { false }
                }
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
    fun shouldBlockMainFrame(url: String): Boolean {
        // 文档请求一般不过滤（避免页面打不开）
        return false
    }

    private fun addDefaultRules() {
        // 内置最常用规则
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

            // 国内常见广告域名
            "||pos.baidu.com^",
            "||cpro.baidustatic.com^",
            "||sogou.com/feedback/",
        )

        defaultRules.forEach { parseAndAddRule(it) }
    }
}
