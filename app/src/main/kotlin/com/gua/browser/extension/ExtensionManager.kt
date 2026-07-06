package com.gua.browser.extension

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class ExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val enabled: Boolean,
    val sourceUrl: String?,
    val filePath: String
)

class ExtensionManager(
    private val context: Context,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
) {
    companion object {
        private const val TAG = "ExtensionManager"
        private const val EXT_DIR = "extensions"
        private const val PREFS_NAME = "extensions"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val registeredExtensions = mutableMapOf<String, WebExtension>()
    private var runtime: GeckoRuntime? = null

    private val _extensions = MutableStateFlow<List<ExtensionInfo>>(emptyList())
    val extensions: StateFlow<List<ExtensionInfo>> = _extensions

    fun setRuntime(runtime: GeckoRuntime) {
        this.runtime = runtime
        restoreAll()
    }

    fun installFromUrl(url: String, onComplete: (Boolean, String) -> Unit) {
        scope.launch {
            try {
                val fileName = "ext_${System.currentTimeMillis()}.xpi"
                val dir = File(context.filesDir, EXT_DIR)
                dir.mkdirs()
                val file = File(dir, fileName)

                withContext(Dispatchers.IO) {
                    val conn = URL(url).openConnection() as HttpURLConnection
                    conn.connectTimeout = 15000
                    conn.readTimeout = 30000
                    conn.instanceFollowRedirects = true

                    val input = conn.inputStream
                    FileOutputStream(file).use { output ->
                        input.copyTo(output)
                    }
                    input.close()
                }

                installFromFile(file.absolutePath, url, onComplete)
            } catch (e: Exception) {
                Log.e(TAG, "下载扩展失败: $url", e)
                withContext(Dispatchers.Main) { onComplete(false, "下载失败: ${e.message}") }
            }
        }
    }

    fun installFromFile(filePath: String, sourceUrl: String? = null, onComplete: ((Boolean, String) -> Unit)? = null) {
        val controller = runtime?.webExtensionController ?: run {
            onComplete?.invoke(false, "运行时未就绪")
            return
        }

        val uri = File(filePath).toURI().toString()
        controller.install(uri).accept(object : org.mozilla.geckoview.GeckoResult.Consumer<WebExtension> {
            override fun accept(ext: WebExtension?) {
                if (ext == null) { onComplete?.invoke(false, "安装失败"); return }
                val extName = ext.metaData.name ?: ext.id
                val extVer = ext.metaData.version ?: "0"
                val info = ExtensionInfo(
                    id = ext.id,
                    name = extName,
                    version = extVer,
                    enabled = true,
                    sourceUrl = sourceUrl,
                    filePath = filePath
                )
                registeredExtensions[ext.id] = ext
                saveToPrefs(info)
                refreshList()
                Log.d(TAG, "已安装: $extName v$extVer")
                onComplete?.invoke(true, extName)
            }
        })
    }

    fun uninstall(extId: String) {
        val ext = registeredExtensions.remove(extId)
        if (ext != null) {
            try { runtime?.webExtensionController?.uninstall(ext) } catch (e: Exception) {
                Log.e(TAG, "卸载异常: $extId", e)
            }
        }
        removeFromPrefs(extId)
        refreshList()
    }

    fun toggle(extId: String, enabled: Boolean) {
        val all = loadFromPrefs()
        val idx = all.indexOfFirst { it.id == extId }
        if (idx < 0) return
        val updated = all.toMutableList()
        updated[idx] = updated[idx].copy(enabled = enabled)
        saveAllToPrefs(updated)
        if (!enabled) {
            registeredExtensions.remove(extId)?.let { ext ->
                try { runtime?.webExtensionController?.uninstall(ext) } catch (_: Exception) {}
            }
        } else {
            val info = updated[idx]
            installFromFile(info.filePath, info.sourceUrl)
        }
        refreshList()
    }

    private fun restoreAll() {
        val controller = runtime?.webExtensionController ?: return
        val stored = loadFromPrefs()
        stored.filter { it.enabled }.forEach { info ->
            val file = File(info.filePath)
            if (file.exists()) {
                controller.install(file.toURI().toString()).accept(object : org.mozilla.geckoview.GeckoResult.Consumer<WebExtension> {
                    override fun accept(ext: WebExtension?) {
                        if (ext != null) {
                            registeredExtensions[ext.id] = ext
                            refreshList()
                        }
                    }
                })
            } else {
                Log.w(TAG, "扩展文件缺失: ${info.name} (${info.filePath})")
            }
        }
    }

    private fun saveToPrefs(info: ExtensionInfo) {
        val all = loadFromPrefs().toMutableList()
        all.removeAll { it.id == info.id }
        all.add(info)
        saveAllToPrefs(all)
    }

    private fun removeFromPrefs(extId: String) {
        val all = loadFromPrefs().filter { it.id != extId }
        saveAllToPrefs(all)
    }

    private fun saveAllToPrefs(list: List<ExtensionInfo>) {
        val ids = list.joinToString(",") { it.id }
        val names = list.joinToString("||") { it.name }
        val versions = list.joinToString(",") { it.version }
        val enabledFlags = list.joinToString(",") { if (it.enabled) "1" else "0" }
        val urls = list.joinToString("||") { it.sourceUrl ?: "" }
        val paths = list.joinToString("||") { it.filePath }

        prefs.edit()
            .putString("ids", ids)
            .putString("names", names)
            .putString("versions", versions)
            .putString("enabled", enabledFlags)
            .putString("urls", urls)
            .putString("paths", paths)
            .apply()
    }

    private fun loadFromPrefs(): List<ExtensionInfo> {
        val ids = prefs.getString("ids", "")?.split(",")?.filter { it.isNotBlank() } ?: return emptyList()
        val names = prefs.getString("names", "")?.split("\\|\\|".toRegex())?.filter { it.isNotBlank() } ?: return emptyList()
        val versions = prefs.getString("versions", "")?.split(",")?.filter { it.isNotBlank() } ?: return emptyList()
        val enabledFlags = prefs.getString("enabled", "")?.split(",")?.filter { it.isNotBlank() } ?: return emptyList()
        val urls = prefs.getString("urls", "")?.split("\\|\\|".toRegex()) ?: return emptyList()
        val paths = prefs.getString("paths", "")?.split("\\|\\|".toRegex())?.filter { it.isNotBlank() } ?: return emptyList()

        return ids.mapIndexedNotNull { index, id ->
            if (index >= names.size || index >= versions.size || index >= enabledFlags.size || index >= paths.size) null
            else ExtensionInfo(
                id = id,
                name = names.getOrElse(index) { "未知" },
                version = versions.getOrElse(index) { "0" },
                enabled = enabledFlags.getOrElse(index) { "0" } == "1",
                sourceUrl = urls.getOrElse(index) { "" }.ifEmpty { null },
                filePath = paths.getOrElse(index) { "" }
            )
        }
    }

    private fun refreshList() {
        val stored = loadFromPrefs().map { info ->
            if (registeredExtensions.containsKey(info.id) && !info.enabled) info.copy(enabled = true)
            else info
        }
        _extensions.value = stored
    }

    fun destroy() {
        registeredExtensions.values.forEach { ext ->
            try { runtime?.webExtensionController?.uninstall(ext) } catch (_: Exception) {}
        }
        registeredExtensions.clear()
    }
}
