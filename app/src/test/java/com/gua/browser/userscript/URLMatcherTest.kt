package com.gua.browser.userscript

import org.junit.Assert.*
import org.junit.Test

/**
 * URLMatcher 单元测试
 *
 * 验证通配符模式匹配的正确性，包括：
 * - * 通配符（不匹配 /）
 * - ** 超级通配符（匹配 /）
 * - 域名匹配
 * - 精确匹配
 * - 缓存机制
 */
class URLMatcherTest {

    @Test
    fun `exact url match`() {
        assertTrue(URLMatcher.matches(
            "https://www.google.com/",
            "https://www.google.com/"
        ))
    }

    @Test
    fun `wildcard matches any domain`() {
        assertTrue(URLMatcher.matches(
            "https://example.com/page",
            "*://example.com/*"
        ))
    }

    @Test
    fun `wildcard matches subdomain`() {
        assertTrue(URLMatcher.matches(
            "https://sub.example.com/page",
            "*://*.example.com/*"
        ))
    }

    @Test
    fun `double wildcard matches path with slashes`() {
        assertTrue(URLMatcher.matches(
            "https://example.com/a/b/c",
            "*://example.com/**"
        ))
    }

    @Test
    fun `single wildcard does not match slashes`() {
        assertFalse(URLMatcher.matches(
            "https://example.com/a/b",
            "*://example.com/*"
        ))
    }

    @Test
    fun `exclude pattern`() {
        assertFalse(URLMatcher.shouldInject(
            "https://example.com/excluded",
            UserScript(
                matches = listOf("*://example.com/*"),
                excludes = listOf("*://example.com/excluded"),
                enabled = true
            )
        ))
    }

    @Test
    fun `no match patterns means match all`() {
        assertTrue(URLMatcher.shouldInject(
            "https://any-site.com/page",
            UserScript(enabled = true)
        ))
    }

    @Test
    fun `disabled script should not inject`() {
        assertFalse(URLMatcher.shouldInject(
            "https://example.com/",
            UserScript(
                matches = listOf("*://example.com/*"),
                enabled = false
            )
        ))
    }

    @Test
    fun `include pattern takes priority`() {
        assertTrue(URLMatcher.shouldInject(
            "https://example.com/",
            UserScript(
                includes = listOf("*://example.com/*"),
                enabled = true
            )
        ))
    }

    @Test
    fun `cache returns same result for repeated pattern`() {
        val result1 = URLMatcher.matches("https://example.com/", "*://example.com/*")
        val result2 = URLMatcher.matches("https://example.com/", "*://example.com/*")
        assertEquals(result1, result2)
    }

    @Test
    fun `empty pattern returns false`() {
        assertFalse(URLMatcher.matches("https://example.com/", ""))
    }

    @Test
    fun `question mark matches single character`() {
        assertTrue(URLMatcher.matches("https://example.com/", "https://example.c?m/"))
    }

    @Test
    fun `special regex chars are escaped`() {
        assertTrue(URLMatcher.matches("https://example.com/page+1", "https://example.com/page+1"))
    }
}
