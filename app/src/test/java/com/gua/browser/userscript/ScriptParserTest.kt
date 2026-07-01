package com.gua.browser.userscript

import org.junit.Assert.*
import org.junit.Test

/**
 * ScriptParser 单元测试
 */
class ScriptParserTest {

    private val sampleScript = """
        // ==UserScript==
        // @name         Example Script
        // @namespace    http://example.com/
        // @version      1.0.0
        // @description  A test script
        // @author       testuser
        // @match        *://*.example.com/*
        // @match        https://www.google.com/*
        // @exclude      https://www.google.com/search
        // @grant        GM_setValue
        // @grant        GM_getValue
        // @grant        GM_xmlhttpRequest
        // @run-at       document-end
        // ==/UserScript==
        (function() {
            console.log('hello');
        })();
    """.trimIndent()

    @Test
    fun `parse script header correctly`() {
        val script = ScriptParser.parse(sampleScript)

        assertEquals("Example Script", script.name)
        assertEquals("http://example.com/", script.namespace)
        assertEquals("1.0.0", script.version)
        assertEquals("A test script", script.description)
        assertEquals("testuser", script.author)
        assertEquals(RunAt.DOCUMENT_END, script.runAt)
    }

    @Test
    fun `parse match patterns`() {
        val script = ScriptParser.parse(sampleScript)

        assertEquals(2, script.matches.size)
        assertTrue(script.matches.contains("*://*.example.com/*"))
        assertTrue(script.matches.contains("https://www.google.com/*"))
    }

    @Test
    fun `parse exclude patterns`() {
        val script = ScriptParser.parse(sampleScript)

        assertEquals(1, script.excludes.size)
        assertEquals("https://www.google.com/search", script.excludes.first())
    }

    @Test
    fun `parse grant list`() {
        val script = ScriptParser.parse(sampleScript)

        assertEquals(3, script.grants.size)
        assertTrue(script.grants.contains("GM_setValue"))
        assertTrue(script.grants.contains("GM_getValue"))
        assertTrue(script.grants.contains("GM_xmlhttpRequest"))
    }

    @Test
    fun `parse script without header returns default values`() {
        val code = "console.log('no header');"
        val script = ScriptParser.parse(code)

        assertEquals("未命名脚本", script.name)
        assertEquals(emptyList<String>(), script.matches)
        assertEquals(RunAt.DOCUMENT_IDLE, script.runAt)
    }

    @Test
    fun `extract body removes header`() {
        val body = ScriptParser.extractBody(sampleScript)

        assertFalse(body.contains("==UserScript=="))
        assertTrue(body.contains("console.log('hello')"))
    }

    @Test
    fun `parse resource directive`() {
        val code = """
            // ==UserScript==
            // @resource icon https://example.com/icon.png
            // @resource style https://example.com/style.css
            // ==/UserScript==
            console.log('test');
        """.trimIndent()

        val script = ScriptParser.parse(code)
        assertEquals(2, script.resources.size)
        assertEquals("https://example.com/icon.png", script.resources["icon"])
        assertEquals("https://example.com/style.css", script.resources["style"])
    }

    @Test
    fun `default run-at is document-idle`() {
        val code = """
            // ==UserScript==
            // @name Test
            // ==/UserScript==
            console.log('test');
        """.trimIndent()

        val script = ScriptParser.parse(code)
        assertEquals(RunAt.DOCUMENT_IDLE, script.runAt)
    }
}
