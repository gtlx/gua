package com.gua.browser.adblock

/**
 * 广告过滤规则
 *
 * 支持 uBlock Origin 规则语法子集：
 * - ||example.com^         域名匹配
 * - |http://example.com/    精确匹配开头
 * - example.com/ads/        部分匹配
 * - /regex/                 正则匹配
 * - @@                     白名单
 */
data class AdBlockRule(
    val pattern: String,
    val type: RuleType,
    val isException: Boolean = false
) {
    enum class RuleType {
        DOMAIN,      // ||example.com^
        EXACT,       // |http://example.com/...
        PREFIX,      // 普通前缀匹配
        REGEX,       // /regex/
        URL_PATTERN  // 含通配符的 URL *
    }
}
