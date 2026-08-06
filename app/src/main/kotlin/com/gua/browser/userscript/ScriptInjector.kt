package com.gua.browser.userscript

import android.content.Context
import android.util.Log
import com.gua.browser.core.network.HttpClient
import com.gua.browser.userscript.gmapi.GMApiBridge
import kotlinx.coroutines.runBlocking
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController
import org.json.JSONObject
import java.io.File

class ScriptInjector(private val context: Context) {

    companion object {
        private const val TAG = "ScriptInjector"
        private const val EXTENSION_DIR = "gua_userscripts"
    }

    private val registeredExtensions = mutableMapOf<Long, WebExtension>()
    private var runtime: GeckoRuntime? = null

    fun setRuntime(runtime: GeckoRuntime) {
        this.runtime = runtime
    }

    fun getRuntime(): GeckoRuntime? = runtime

    fun installAll(repository: ScriptRepository, apiBridge: GMApiBridge) {
        repository.scripts.value.forEach { script ->
            if (script.enabled) installScript(script, apiBridge)
        }
    }

    fun installScript(script: UserScript, apiBridge: GMApiBridge) {
        uninstallScript(script.id)
        try {
            val extDir = createExtensionDir(script)
            val uri = extDir.toURI().toString()
            val controller = runtime?.webExtensionController
            controller?.install(uri)?.accept(object : org.mozilla.geckoview.GeckoResult.Consumer<WebExtension> {
                override fun accept(ext: WebExtension?) {
                    if (ext != null) {
                        registeredExtensions[script.id] = ext
                        ext.setMessageDelegate(NativeMessageDelegate(apiBridge), "gua_browser")
                        Log.d(TAG, "已安装: ${script.name} v${script.version}")
                    }
                }
            })
        } catch (e: Exception) {
            Log.e(TAG, "安装失败: ${script.name}", e)
        }
    }

    fun uninstallScript(scriptId: Long) {
        val ext = registeredExtensions.remove(scriptId) ?: return
        try { runtime?.webExtensionController?.uninstall(ext) } catch (_: Exception) {}
        cleanupExtensionDir(scriptId)
    }

    private fun cleanupExtensionDir(scriptId: Long) {
        val dir = File(context.cacheDir, "$EXTENSION_DIR/$scriptId")
        if (dir.exists()) dir.deleteRecursively()
    }

    fun reloadAll(repository: ScriptRepository, apiBridge: GMApiBridge) {
        registeredExtensions.keys.toList().forEach { uninstallScript(it) }
        installAll(repository, apiBridge)
    }

    private fun createExtensionDir(script: UserScript): File {
        val scriptId = script.id
        val baseDir = File(context.cacheDir, EXTENSION_DIR)
        val scriptDir = File(baseDir, scriptId.toString())
        if (scriptDir.exists()) scriptDir.deleteRecursively()
        scriptDir.mkdirs()

        // 下载 @require 依赖
        val requireFiles = mutableListOf<String>()
        runBlocking {
            script.requires.forEach { url ->
                try {
                    val resp = HttpClient.execute(HttpClient.Request(url = url, timeout = 10000))
                    if (resp.statusCode == 200) {
                        val fileName = "require_${url.hashCode().toUInt()}.js"
                        File(scriptDir, fileName).writeText(resp.body)
                        requireFiles.add(fileName)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "下载 @require 失败: $url", e)
                }
            }
        }

        // 下载 @resource
        runBlocking {
            script.resources.forEach { (name, url) ->
                try {
                    val resp = HttpClient.execute(HttpClient.Request(url = url, timeout = 10000))
                    if (resp.statusCode == 200) {
                        File(scriptDir, "res_$name").writeText(resp.body)
                    }
                } catch (_: Exception) {}
            }
        }

        // manifest.json
        val matchesJson = buildJsonArray(script.matches + script.includes)
        val excludesJson = buildJsonArray(script.excludes)
        val runAt = when (script.runAt) {
            RunAt.DOCUMENT_START -> "document_start"
            RunAt.DOCUMENT_END -> "document_end"
            RunAt.DOCUMENT_IDLE -> "document_idle"
        }
        val jsFiles = buildString {
            requireFiles.forEach { appendLine("""    ,"$it"""") }
            appendLine("""    ,"gm-wrapper.js"""")
        }

        val manifest = buildString {
            appendLine("{")
            appendLine("  \"manifest_version\": 2,")
            appendLine("  \"name\": \"${escapeJson(script.name)}\",")
            appendLine("  \"version\": \"${escapeJson(script.version)}\",")
            appendLine("  \"description\": \"${escapeJson(script.description ?: "")}\",")
            appendLine("  \"applications\": {\"gecko\": {\"id\": \"userscript_${scriptId}@gua\"}},")
            appendLine("  \"content_scripts\": [{")
            appendLine("    \"matches\": ${matchesJson},")
            if (script.excludes.isNotEmpty()) {
                appendLine("    \"exclude_matches\": ${excludesJson},")
            }
            appendLine("    \"all_frames\": ${!script.noframes},")
            appendLine("    \"js\": [${jsFiles.trim().removePrefix(",")}],")
            appendLine("    \"run_at\": \"${runAt}\"")
            appendLine("  }],")
            appendLine("  \"permissions\": [")
            appendLine("    \"<all_urls>\",")
            appendLine("    \"nativeMessaging\"")
            appendLine("  ]")
            appendLine("}")
        }
        File(scriptDir, "manifest.json").writeText(manifest)

        val wrapperJs = generateWrapperJs(script)
        File(scriptDir, "gm-wrapper.js").writeText(wrapperJs)

        return scriptDir
    }

    private fun generateWrapperJs(script: UserScript): String {
        val grants = script.grants
        return buildString {
            appendLine("(function() {")
            appendLine("  'use strict;'")
            appendLine()
            appendLine("  var __gm_pending = {};")
            appendLine("  var __gm_native = typeof browser !== 'undefined' && browser.runtime && browser.runtime.sendNativeMessage;")
            appendLine()
            appendLine("  function __gm_sendNative(api, args) {")
            appendLine("    var msgId = 'cb_' + Date.now() + '_' + Math.random();")
            appendLine("    return new Promise(function(resolve) {")
            appendLine("      __gm_pending[msgId] = resolve;")
            appendLine("      try {")
            appendLine("        browser.runtime.sendNativeMessage('gua_browser', {")
            appendLine("          type: 'gm_api', id: msgId, api: api, args: args")
            appendLine("        }).then(function(resp) {")
            appendLine("          var cb = __gm_pending[resp.id || msgId];")
            appendLine("          if (cb) { cb(resp); delete __gm_pending[resp.id || msgId]; }")
            appendLine("        });")
            appendLine("      } catch(e) {")
            appendLine("        var cb = __gm_pending[msgId];")
            appendLine("        if (cb) { cb({error: e.message}); delete __gm_pending[msgId]; }")
            appendLine("      }")
            appendLine("    });")
            appendLine("  }")
            appendLine()

            // unsafeWindow
            if (grants.any { it.equals("unsafeWindow", true) }) {
                appendLine("  var unsafeWindow = window.wrappedJSObject || window;")
                appendLine()
            }

            // GM_getValue (localStorage sync + native global)
            if (grants.any { it.equals("GM_getValue", true) }) {
                appendLine("  function GM_getValue(key, defaultVal) {")
                appendLine("    var raw = localStorage.getItem('__gm_' + key);")
                appendLine("    if (raw !== null) { try { return JSON.parse(raw); } catch(e) { return raw; } }")
                appendLine("    return defaultVal;")
                appendLine("  }")
                appendLine()
            }

            // GM_setValue
            if (grants.any { it.equals("GM_setValue", true) }) {
                appendLine("  function GM_setValue(key, value) {")
                appendLine("    localStorage.setItem('__gm_' + key, JSON.stringify(value));")
                appendLine("    if (__gm_native) __gm_sendNative('GM_setValue', {key: key, value: JSON.stringify(value)});")
                appendLine("  }")
                appendLine()
            }

            // GM_deleteValue
            if (grants.any { it.equals("GM_deleteValue", true) }) {
                appendLine("  function GM_deleteValue(key) {")
                appendLine("    localStorage.removeItem('__gm_' + key);")
                appendLine("    if (__gm_native) __gm_sendNative('GM_deleteValue', {key: key});")
                appendLine("  }")
                appendLine()
            }

            // GM_listValues
            if (grants.any { it.equals("GM_listValues", true) }) {
                appendLine("  function GM_listValues() {")
                appendLine("    var result = [];")
                appendLine("    for (var i = 0; i < localStorage.length; i++) {")
                appendLine("      var key = localStorage.key(i);")
                appendLine("      if (key.startsWith('__gm_')) result.push(key.substring(5));")
                appendLine("    }")
                appendLine("    return result;")
                appendLine("  }")
                appendLine()
            }

            // GM_xmlhttpRequest — always via native
            if (grants.any { it.equals("GM_xmlhttpRequest", true) }) {
                appendLine("  function GM_xmlhttpRequest(details) {")
                appendLine("    var clean = {};")
                appendLine("    var cbs = {};")
                appendLine("    ['onload','onerror','onprogress','ontimeout','onreadystatechange'].forEach(function(k) {")
                appendLine("      if (typeof details[k] === 'function') { cbs[k] = details[k]; }")
                appendLine("    });")
                appendLine("    Object.keys(details).forEach(function(k) {")
                appendLine("      if (typeof details[k] !== 'function') clean[k] = details[k];")
                appendLine("    });")
                appendLine("    __gm_sendNative('GM_xmlhttpRequest', clean).then(function(resp) {")
                appendLine("      if (resp.error) {")
                appendLine("        if (cbs.onerror) cbs.onerror(resp);")
                appendLine("      } else {")
                appendLine("        if (cbs.onload) cbs.onload(resp);")
                appendLine("      }")
                appendLine("    });")
                appendLine("    return { abort: function() {} };")
                appendLine("  }")
                appendLine()
            }

            // GM_addStyle
            if (grants.any { it.equals("GM_addStyle", true) }) {
                appendLine("  function GM_addStyle(css) {")
                appendLine("    var s = document.createElement('style');")
                appendLine("    s.type = 'text/css'; s.textContent = css;")
                appendLine("    document.head.appendChild(s); return s;")
                appendLine("  }")
                appendLine()
            }

            // GM_addElement
            if (grants.any { it.equals("GM_addElement", true) }) {
                appendLine("  function GM_addElement(tag, attrs) {")
                appendLine("    var el = document.createElement(tag);")
                appendLine("    if (attrs) Object.keys(attrs).forEach(function(k) { el.setAttribute(k, attrs[k]); });")
                appendLine("    document.body.appendChild(el); return el;")
                appendLine("  }")
                appendLine()
            }

            // GM_notification — via native
            if (grants.any { it.equals("GM_notification", true) }) {
                appendLine("  function GM_notification(details) {")
                appendLine("    if (typeof details === 'string') details = {text: details};")
                appendLine("    if (__gm_native) __gm_sendNative('GM_notification', details);")
                appendLine("  }")
                appendLine()
            }

            // GM_setClipboard — via native
            if (grants.any { it.equals("GM_setClipboard", true) }) {
                appendLine("  function GM_setClipboard(text) {")
                appendLine("    if (__gm_native) __gm_sendNative('GM_setClipboard', {text: text});")
                appendLine("  }")
                appendLine()
            }

            // GM_openInTab — via native
            if (grants.any { it.equals("GM_openInTab", true) }) {
                appendLine("  function GM_openInTab(url) {")
                appendLine("    if (__gm_native) __gm_sendNative('GM_openInTab', {url: url});")
                appendLine("  }")
                appendLine()
            }

            // GM_registerMenuCommand
            if (grants.any { it.equals("GM_registerMenuCommand", true) }) {
                appendLine("  function GM_registerMenuCommand(name, callback) {")
                appendLine("    console.log('[GM] Registered menu: ' + name);")
                appendLine("  }")
                appendLine()
            }

            // GM_log
            if (grants.any { it.equals("GM_log", true) }) {
                appendLine("  function GM_log() {")
                appendLine("    console.log.apply(console, ['[GM]'].concat(Array.prototype.slice.call(arguments)));")
                appendLine("  }")
                appendLine()
            }

            // GM_getResourceText — return pre-fetched content
            if (grants.any { it.equals("GM_getResourceText", true) }) {
                val resText = script.resources.entries.joinToString(",") { (name, _) ->
                    val content = try { File(context.cacheDir, "$EXTENSION_DIR/${script.id}/res_$name").readText() } catch (_: Exception) { "" }
                    "'${escapeJson(name)}': '${escapeJson(content)}'"
                }
                appendLine("  var __gm_resources_text = {$resText};")
                appendLine("  function GM_getResourceText(name) { return __gm_resources_text[name] || null; }")
                appendLine()
            }

            // GM_getResourceURL — return data: URI
            if (grants.any { it.equals("GM_getResourceURL", true) }) {
                appendLine("  function GM_getResourceURL(name) { return __gm_resources_text[name] || null; }")
                appendLine()
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

    private fun gmInfoImpl(script: UserScript) = """
        var GM_info = {
            script: {
                name: '${escapeJson(script.name)}',
                namespace: '${escapeJson(script.namespace ?: "")}',
                version: '${escapeJson(script.version)}',
                description: '${escapeJson(script.description ?: "")}',
                author: '${escapeJson(script.author ?: "")}',
                matches: ${buildJsonArray(script.matches + script.includes)},
                grants: ${buildJsonArray(script.grants)}
            },
            scriptHandler: 'GuaBrowser',
            version: '0.1.0'
        };
    """.trimIndent()

    private fun buildJsonArray(list: List<String>): String {
        return list.joinToString(",", "[", "]") { "\"${escapeJson(it)}\"" }
    }

    private fun escapeJson(str: String): String {
        return str.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    // ===== 原生消息代理 =====
    private class NativeMessageDelegate(private val apiBridge: GMApiBridge) : WebExtension.MessageDelegate {
        override fun onMessage(nativeApp: String, message: Any, sender: WebExtension.MessageSender): GeckoResult<Any>? {
            if (nativeApp != "gua_browser") return null
            return apiBridge.handleMessage(message)
        }
    }

    // ===== 夜间模式扩展 =====
    private val NIGHT_MODE_EXT_ID = -1L
    private val NIGHT_MODE_DIR = "gua_nightmode"

    fun installNightMode(enabled: Boolean) {
        if (enabled) {
            uninstallNightMode()
            try {
                val extDir = createNightModeExtensionDir()
                val uri = extDir.toURI().toString()
                runtime?.webExtensionController?.install(uri)?.accept { ext ->
                    if (ext != null) registeredExtensions[NIGHT_MODE_EXT_ID] = ext
                }
            } catch (e: Exception) {
                Log.e(TAG, "夜间模式安装失败", e)
            }
        } else {
            uninstallNightMode()
        }
    }

    private fun uninstallNightMode() {
        registeredExtensions.remove(NIGHT_MODE_EXT_ID)?.let { ext ->
            try { runtime?.webExtensionController?.uninstall(ext) } catch (_: Exception) {}
        }
        val dir = File(context.cacheDir, NIGHT_MODE_DIR)
        if (dir.exists()) dir.deleteRecursively()
    }

    private fun createNightModeExtensionDir(): File {
        val dir = File(context.cacheDir, NIGHT_MODE_DIR)
        if (dir.exists()) dir.deleteRecursively()
        dir.mkdirs()

        val manifest = buildString {
            appendLine("{")
            appendLine("  \"manifest_version\": 2,")
            appendLine("  \"name\": \"GuaBrowser Night Mode\",")
            appendLine("  \"version\": \"1.0\",")
            appendLine("  \"applications\": {\"gecko\": {\"id\": \"nightmode@gua\"}},")
            appendLine("  \"content_scripts\": [{")
            appendLine("    \"matches\": [\"<all_urls>\"],")
            appendLine("    \"js\": [\"night-mode.js\"],")
            appendLine("    \"run_at\": \"document_start\"")
            appendLine("  }]")
            appendLine("}")
        }
        File(dir, "manifest.json").writeText(manifest)

        val js = """(function() {
  'use strict';
  var s = document.createElement('style');
  s.id = '__gua_night_mode';
  s.textContent = [
    'html { -webkit-filter: invert(1) hue-rotate(180deg); filter: invert(1) hue-rotate(180deg); }',
    'img, video, canvas, iframe, [style*="background-image"] { -webkit-filter: invert(1) hue-rotate(180deg); filter: invert(1) hue-rotate(180deg); }'
  ].join(' ');
  document.documentElement.appendChild(s);
})();"""
        File(dir, "night-mode.js").writeText(js)

        return dir
    }

    fun destroy() {
        registeredExtensions.keys.toList().forEach { uninstallScript(it) }
        uninstallNightMode()
        val baseDir = File(context.cacheDir, EXTENSION_DIR)
        if (baseDir.exists()) baseDir.deleteRecursively()
    }
}
