package com.gua.browser.userscript

import android.content.Context
import android.util.Log
import com.gua.browser.core.network.HttpClient
import com.gua.browser.userscript.gmapi.GMApiBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.WebExtensionController

/**
 * 用户脚本管理器
 *
 * 统一管理脚本的安装、卸载、注入、更新。
 * 串联 ScriptParser → ScriptRepository → ScriptInjector 整个流程。
 */
class ScriptManager(private val context: Context) {

    companion object {
        private const val TAG = "ScriptManager"
    }

    val repository: ScriptRepository = ScriptRepository(context)
    val injector: ScriptInjector = ScriptInjector(context)
    val apiBridge: GMApiBridge = GMApiBridge(context)

    /** 所有已安装的脚本（可观察） */
    val scripts: StateFlow<List<UserScript>> = repository.scripts

    /**
     * 初始化：设置 API 回调
     */
    fun init() {
        // 设置 GM_openInTab 回调
        apiBridge.setOpenInTabCallback { url ->
            Log.d(TAG, "GM_openInTab: $url")
            // 通过 EventBus 或回调通知 UI 层新建标签页
            onOpenInTab?.invoke(url)
        }

        apiBridge.setHttpCallbacks(
            onResponse = { json ->
                Log.d(TAG, "HTTP response: $json")
            },
            onError = { json ->
                Log.e(TAG, "HTTP error: $json")
            }
        )
    }

    /**
     * 从代码安装脚本
     */
    fun installFromCode(code: String): UserScript {
        val script = repository.install(code)
        if (script.enabled) {
            injector.installScript(script)
        }
        Log.d(TAG, "已安装: ${script.name}")
        return script
    }

    /**
     * 从 GreasyFork 等 URL 安装脚本
     */
    fun installFromUrl(url: String, onComplete: (UserScript?) -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = HttpClient.execute(HttpClient.Request(url = url))
                if (response.statusCode == 200) {
                    val script = installFromCode(response.body)
                    withContext(Dispatchers.Main) { onComplete(script) }
                } else {
                    withContext(Dispatchers.Main) { onComplete(null) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    /**
     * 删除脚本
     */
    fun deleteScript(scriptId: Long) {
        injector.uninstallScript(scriptId)
        repository.delete(scriptId)
    }

    /**
     * 切换启用/禁用
     */
    fun toggleScript(scriptId: Long) {
        repository.toggleEnabled(scriptId)
        val script = repository.getScriptById(scriptId)
        if (script != null) {
            if (script.enabled) {
                injector.installScript(script)
            } else {
                injector.uninstallScript(scriptId)
            }
        }
    }

    /**
     * 获取匹配给定 URL 的脚本
     */
    fun getMatchingScripts(url: String): List<UserScript> {
        return repository.getMatchingScripts(url)
    }

    /**
     * 安装/重新安装所有脚本
     */
    fun installAll() {
        repository.scripts.value.forEach { script ->
            if (script.enabled) {
                injector.installScript(script)
            }
        }
    }

    /**
     * 设置 WebExtension 控制器
     */
    fun setExtensionController(controller: WebExtensionController?) {
        injector.setExtensionController(controller)
    }

    // ===== 回调 =====
    var onOpenInTab: ((String) -> Unit)? = null

    fun destroy() {
        injector.destroy()
    }
}
