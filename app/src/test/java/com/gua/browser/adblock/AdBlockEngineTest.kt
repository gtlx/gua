package com.gua.browser.adblock

import org.junit.Assert.*
import org.junit.Test

/**
 * AdBlockEngine 规则解析与匹配单元测试
 */
class AdBlockEngineTest {

    @Test
    fun `domain rule matches containing url`() {
        val engine = AdBlockEngine(null)
        assertTrue(engine.ruleMatches("https://www.doubleclick.net/page", "doubleclick.net"))
    }

    @Test
    fun `prefix rule matches url containing pattern`() {
        val engine = AdBlockEngine(null)
        assertTrue(engine.ruleMatches("https://example.com/ad/banner.jpg", "/ad/"))
    }

    @Test
    fun `whitelist exception overrides block`() {
        val engine = AdBlockEngine(null)
        assertTrue(engine.isExceptionRule("@@||example.com^"))
    }
}
