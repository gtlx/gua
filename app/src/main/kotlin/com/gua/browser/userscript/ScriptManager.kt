package com.gua.browser.userscript

import android.content.Context
import android.util.Log
import com.gua.browser.core.network.HttpClient
import com.gua.browser.userscript.gmapi.GMApiBridge
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.GeckoRuntime

class ScriptManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {

    companion object {
        private const val TAG = "ScriptManager"
    }

    val repository: ScriptRepository = ScriptRepository(context)
    val injector: ScriptInjector = ScriptInjector(context)
    val apiBridge: GMApiBridge = GMApiBridge(context)

    val scripts: StateFlow<List<UserScript>> = repository.scripts

    fun init() {
        apiBridge.onOpenInTab = { url ->
            Log.d(TAG, "GM_openInTab: $url")
            onOpenInTab?.invoke(url)
        }
    }

    fun installFromCode(code: String): UserScript {
        val script = repository.install(code)
        if (script.enabled) {
            injector.installScript(script, apiBridge)
        }
        Log.d(TAG, "已安装: ${script.name}")
        return script
    }

    fun installFromUrl(url: String, onComplete: (UserScript?) -> Unit) {
        scope.launch {
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

    fun deleteScript(scriptId: Long) {
        injector.uninstallScript(scriptId)
        repository.delete(scriptId)
    }

    fun toggleScript(scriptId: Long) {
        repository.toggleEnabled(scriptId)
        val script = repository.getScriptById(scriptId)
        if (script != null) {
            if (script.enabled) {
                injector.installScript(script, apiBridge)
            } else {
                injector.uninstallScript(scriptId)
            }
        }
    }

    fun getMatchingScripts(url: String): List<UserScript> {
        return repository.getMatchingScripts(url)
    }

    fun installAll() {
        repository.scripts.value.forEach { script ->
            if (script.enabled) {
                injector.installScript(script, apiBridge)
            }
        }
    }

    fun setRuntime(runtime: GeckoRuntime?) {
        if (runtime != null) injector.setRuntime(runtime)
    }

    var onOpenInTab: ((String) -> Unit)? = null

    fun destroy() {
        injector.destroy()
    }
}
