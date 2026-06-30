package com.gua.browser.userscript

import android.content.Context
import android.util.Log
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import java.io.File

/**
 * 油猴脚本注入器
 *
 * 利用 GeckoView 的 WebExtension API 将用户脚本注册为
 * 原生 content_scripts。利用 Gecko 的扩展机制实现真正的
 * 隔离世界、document_start 注入、CSP 穿透。
 *
 * 核心流程：
 *   1. 为用户脚本生成临时 WebExtension 目录
 *   2. 包含 manifest.json 声明 content_scripts
 *   3. 包含 wrapper.js 注入 GM_API 桥接
 *   4. 通过 GeckoRuntime.registerWebExtension 注册
 *   5. 后续通过 browser.runtime 消息通信
 */
class ScriptInjector(private val context: Context) {

    companion object {
        private const val TAG = "ScriptInjector"
        private const val EXTENSION_DIR = "gua_userscripts"
    }

    private val registeredExtensions = mutableMapOf<Long, WebExtension>()
    private var runtime: GeckoRuntime? = null

    /**
     * 设置 WebExtension 控制器（从 GeckoRuntime 获取）
     */
    fun setRuntime(runtime: GeckoRuntime) {
        this.runtime = runtime
    }

    fun getRuntime(): GeckoRuntime? = runtime

    /**
     * 从 ScriptRepository 中安装或更新所有已启用的脚本
     */
    fun installAll(repository: ScriptRepository) {
        repository.scripts.value.forEach { script ->
            if (script.enabled) {
                installScript(script)
            }
        }
    }

    /**
     * 安装单个用户脚本为 WebExtension
     */
    fun installScript(script: UserScript) {
        // 如果已经注册，先卸载
        uninstallScript(script.id)

        try {
            val extDir = createExtensionDir(script)
            val uri = extDir.toURI().toString()
            val controller = runtime?.webExtensionController
            if (controller != null) {
                controller.install(uri).accept(
                    object : org.mozilla.geckoview.WebExtension.InstallCallback {
                        override fun onSuccess(ext: WebExtension) {
                            registeredExtensions[script.id] = ext
                            Log.d(TAG, "已安装: ${script.name} v${script.version}")
                        }
                        override fun onError(e: Throwable) {
                            Log.e(TAG, "安装失败: ${script.name}", e)
                        }
                    }
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "安装脚本失败: ${script.name}", e)
        }
    }

    /**
     * 卸载脚本
     */
    fun uninstallScript(scriptId: Long) {
        registeredExtensions.remove(scriptId)?.let { ext ->
            // GeckoView 没有直接的 unregister API，但可以通过
            // WebExtensionController 管理
            // GeckoView unregister is not directly supported
        }
    }

    /**
     * 重新加载所有脚本
     */
    fun reloadAll(repository: ScriptRepository) {
        registeredExtensions.clear()
        installAll(repository)
    }

    /**
     * 为用户脚本创建临时 WebExtension 目录
     *
     * 生成的 manifest.json 示例：
     * {
     *   "manifest_version": 2,
     *   "name": "示例脚本",
     *   "version": "1.0.0",
     *   "content_scripts": [{
     *     "matches": ["*://star.example.com/"],
     *     "js": ["gm-wrapper.js"],
     *     "run_at": "document_end"
     *   }]
     * }
     */
    private fun createExtensionDir(script: UserScript): File {
        val baseDir = File(context.cacheDir, EXTENSION_DIR)
        val scriptDir = File(baseDir, script.id.toString())

        // 清理旧的
        if (scriptDir.exists()) {
            scriptDir.deleteRecursively()
        }

        scriptDir.mkdirs()

        // 1. 创建 manifest.json
        val matchesJson = buildJsonArray(script.matches + script.includes)
        val excludesJson = buildJsonArray(script.excludes)
        val runAt = when (script.runAt) {
            RunAt.DOCUMENT_START -> "document_start"
            RunAt.DOCUMENT_END -> "document_end"
            RunAt.DOCUMENT_IDLE -> "document_idle"
        }

        val manifest = buildString {
            appendLine("{")
            appendLine("  \"manifest_version\": 2,")
            appendLine("  \"name\": \"${escapeJson(script.name)}\",")
            appendLine("  \"version\": \"${escapeJson(script.version)}\",")
            appendLine("  \"description\": \"${escapeJson(script.description ?: "")}\",")
            appendLine("  \"content_scripts\": [{")
            appendLine("    \"matches\": ${matchesJson},")
            if (script.excludes.isNotEmpty()) {
                appendLine("    \"exclude_matches\": ${excludesJson},")
            }
            appendLine("    \"js\": [\"gm-wrapper.js\"],")
            appendLine("    \"run_at\": \"${runAt}\"")
            appendLine("  }],")
            appendLine("  \"permissions\": [")
            appendLine("    \"<all_urls>\"")
            if (script.grants.any { it.contains("GM_xmlhttpRequest", true) }) {
                appendLine("    ,\"webRequest\"")
                appendLine("    ,\"webRequestBlocking\"")
            }
            appendLine("  ]")
            appendLine("}")
        }

        File(scriptDir, "manifest.json").writeText(manifest)

        // 2. 创建 gm-wrapper.js
        val wrapperJs = generateWrapperJs(script)
        File(scriptDir, "gm-wrapper.js").writeText(wrapperJs)

        return scriptDir
    }

    /**
     * 生成 GM API 包装器 + 用户脚本主体
     *
     * 通过 browser.runtime.sendMessage 与原生侧通信，
     * 实现 GM_* 功能。
     */
    private fun generateWrapperJs(script: UserScript): String {
        return buildString {
            appendLine("(function() {")
            appendLine("  'use strict;'")
            appendLine()
            appendLine("  // ===== GM_API 桥接 ===== ")
            appendLine("  const browser = window.browser || window.chrome;")
            appendLine()

            // 根据 @grant 生成对应 API
            val grants = script.grants

            // unsafeWindow
            if (grants.any { it.equals("unsafeWindow", true) }) {
                appendLine("  var unsafeWindow = window;")
                appendLine()
            }

            // GM_getValue / GM_setValue
            if (grants.any { it.equals("GM_getValue", true) }) {
                appendLine(gmGetValueImpl())
            }
            if (grants.any { it.equals("GM_setValue", true) }) {
                appendLine(gmSetValueImpl())
            }
            if (grants.any { it.equals("GM_deleteValue", true) }) {
                appendLine(gmDeleteValueImpl())
            }
            if (grants.any { it.equals("GM_listValues", true) }) {
                appendLine(gmListValuesImpl())
            }

            // GM_xmlhttpRequest
            if (grants.any { it.equals("GM_xmlhttpRequest", true) }) {
                appendLine(gmXmlHttpRequestImpl())
            }

            // GM_addStyle
            if (grants.any { it.equals("GM_addStyle", true) }) {
                appendLine(gmAddStyleImpl())
            }

            // GM_addElement
            if (grants.any { it.equals("GM_addElement", true) }) {
                appendLine(gmAddElementImpl())
            }

            // GM_notification
            if (grants.any { it.equals("GM_notification", true) }) {
                appendLine(gmNotificationImpl())
            }

            // GM_setClipboard
            if (grants.any { it.equals("GM_setClipboard", true) }) {
                appendLine(gmSetClipboardImpl())
            }

            // GM_openInTab
            if (grants.any { it.equals("GM_openInTab", true) }) {
                appendLine(gmOpenInTabImpl())
            }

            // GM_registerMenuCommand
            if (grants.any { it.equals("GM_registerMenuCommand", true) }) {
                appendLine(gmRegisterMenuCommandImpl())
            }

            // GM_log
            if (grants.any { it.equals("GM_log", true) }) {
                appendLine(gmLogImpl())
            }

            // GM_getResourceText / GM_getResourceURL
            if (grants.any { it.equals("GM_getResourceText", true) }) {
                appendLine(gmGetResourceTextImpl(script.resources))
            }
            if (grants.any { it.equals("GM_getResourceURL", true) }) {
                appendLine(gmGetResourceURLImpl(script.resources))
            }

            // GM_info
            appendLine(gmInfoImpl(script))

            appendLine()
            appendLine("  // ===== 用户脚本主体 ===== ")
            appendLine(script.code)
            appendLine()
            appendLine("})();")
        }
    }

    // ===== GM_API 实现 =====

    private fun gmGetValueImpl() = """
        function GM_getValue(key, defaultValue) {
            var raw = localStorage.getItem('__gm_' + key);
            if (raw === null) return defaultValue;
            try { return JSON.parse(raw); } catch(e) { return raw; }
        }
    """.trimIndent()

    private fun gmSetValueImpl() = """
        function GM_setValue(key, value) {
            localStorage.setItem('__gm_' + key, JSON.stringify(value));
        }
    """.trimIndent()

    private fun gmDeleteValueImpl() = """
        function GM_deleteValue(key) {
            localStorage.removeItem('__gm_' + key);
        }
    """.trimIndent()

    private fun gmListValuesImpl() = """
        function GM_listValues() {
            var result = [];
            for (var i = 0; i < localStorage.length; i++) {
                var key = localStorage.key(i);
                if (key.startsWith('__gm_')) {
                    result.push(key.substring(5));
                }
            }
            return result;
        }
    """.trimIndent()

    private fun gmXmlHttpRequestImpl() = """
        function GM_xmlhttpRequest(details) {
            var xhr = new XMLHttpRequest();
            xhr.open(details.method || 'GET', details.url, true);
            if (details.headers) {
                Object.keys(details.headers).forEach(function(k) {
                    xhr.setRequestHeader(k, details.headers[k]);
                });
            }
            xhr.responseType = details.responseType || 'text';
            xhr.onload = function() {
                var resp = {
                    readyState: 4,
                    responseHeaders: xhr.getAllResponseHeaders(),
                    responseText: xhr.responseText,
                    response: xhr.response,
                    status: xhr.status,
                    statusText: xhr.statusText,
                    finalUrl: xhr.responseURL
                };
                if (details.onload) details.onload(resp);
            };
            xhr.onerror = function() {
                if (details.onerror) details.onerror({ readyState: 4 });
            };
            xhr.onprogress = function(e) {
                if (details.onprogress) details.onprogress(e);
            };
            xhr.ontimeout = function() {
                if (details.ontimeout) details.ontimeout();
            };
            xhr.timeout = details.timeout || 0;
            if (details.data) xhr.send(details.data);
            else xhr.send();
            return {
                abort: function() { xhr.abort(); }
            };
        }
    """.trimIndent()

    private fun gmAddStyleImpl() = """
        function GM_addStyle(css) {
            var style = document.createElement('style');
            style.type = 'text/css';
            style.textContent = css;
            document.head.appendChild(style);
            return style;
        }
    """.trimIndent()

    private fun gmAddElementImpl() = """
        function GM_addElement(tag, attrs) {
            var el = document.createElement(tag);
            if (attrs) {
                Object.keys(attrs).forEach(function(k) {
                    el.setAttribute(k, attrs[k]);
                });
            }
            document.body.appendChild(el);
            return el;
        }
    """.trimIndent()

    private fun gmNotificationImpl() = """
        function GM_notification(details) {
            if (typeof details === 'string') {
                details = { text: details };
            }
            if ('Notification' in window) {
                try {
                    new Notification(details.title || '', {
                        body: details.text || details.body || '',
                        icon: details.image || details.icon || ''
                    });
                } catch(e) {
                    console.log('GM_notification:', details.text);
                }
            } else {
                console.log('GM_notification:', details.text);
            }
        }
    """.trimIndent()

    private fun gmSetClipboardImpl() = """
        function GM_setClipboard(text) {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text);
            } else {
                var ta = document.createElement('textarea');
                ta.value = text;
                document.body.appendChild(ta);
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
            }
        }
    """.trimIndent()

    private fun gmOpenInTabImpl() = """
        function GM_openInTab(url, options) {
            var win = window.open(url, '_blank');
            return win ? { close: function() { win.close(); }, onclose: null } : null;
        }
    """.trimIndent()

    private fun gmRegisterMenuCommandImpl() = """
        var __gm_menu_commands = {};
        function GM_registerMenuCommand(name, callback) {
            __gm_menu_commands[name] = callback;
            console.log('[GM] Registered menu: ' + name);
        }
    """.trimIndent()

    private fun gmLogImpl() = """
        function GM_log() {
            console.log.apply(console, ['[GM]'].concat(Array.prototype.slice.call(arguments)));
        }
    """.trimIndent()

    private fun gmGetResourceTextImpl(resources: Map<String, String>) = """
        var __gm_resources = ${buildResourceMapJs(resources)};
        function GM_getResourceText(name) {
            return __gm_resources[name] || null;
        }
    """.trimIndent()

    private fun gmGetResourceURLImpl(resources: Map<String, String>) = """
        var __gm_resources = ${buildResourceMapJs(resources)};
        function GM_getResourceURL(name) {
            return __gm_resources[name] || null;
        }
    """.trimIndent()

    private fun gmInfoImpl(script: UserScript) = """
        var GM_info = {
            script: {
                name: '${escapeJson(script.name)}',
                namespace: '${escapeJson(script.namespace ?: "")}',
                version: '${escapeJson(script.version)}',
                description: '${escapeJson(script.description ?: "")}',
                author: '${escapeJson(script.author ?: "")}'
            },
            scriptHandler: 'GuaBrowser',
            version: '0.1.0'
        };
    """.trimIndent()

    // ===== 辅助函数 =====

    private fun buildJsonArray(list: List<String>): String {
        return list.joinToString(",", "[", "]") { "\"${escapeJson(it)}\"" }
    }

    private fun buildResourceMapJs(resources: Map<String, String>): String {
        return resources.entries.joinToString(",", "{", "}") {
            "'${escapeJson(it.key)}': '${escapeJson(it.value)}'"
        }
    }

    private fun escapeJson(str: String): String {
        return str
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    fun destroy() {
        registeredExtensions.clear()
        val baseDir = File(context.cacheDir, EXTENSION_DIR)
        if (baseDir.exists()) {
            baseDir.deleteRecursively()
        }
    }
}
