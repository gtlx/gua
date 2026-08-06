package com.gua.browser.userscript.gmapi

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.gua.browser.GuaApp
import com.gua.browser.core.network.HttpClient
import com.gua.browser.core.storage.KVStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult

/**
 * GM_API 原生侧桥接
 *
 * 通过 WebExtension MessageDelegate 接收 JS 端的 GM_* 调用，
 * 利用原生 Android API 实现跨域 HTTP、系统通知、剪贴板等功能。
 */
class GMApiBridge(private val context: Context) {

    private val storage = KVStorage(context)
    private val notificationId = 1001

    companion object {
        private const val CHANNEL_ID = "gm_notification"
        private const val TAG = "GMApiBridge"
    }

    /**
     * 处理来自 WebExtension 的 GM API 消息
     * 返回 GeckoResult，解析为 JS Promise
     */
    fun handleMessage(message: Any): GeckoResult<Any>? {
        val msg = message as? JSONObject ?: return null
        val api = msg.optString("api", "") ?: return null
        val args = msg.optJSONObject("args") ?: JSONObject()
        val msgId = msg.optString("id", "")

        return when (api) {
            "GM_getValue" -> handleGetValue(args)
            "GM_setValue" -> handleSetValue(args)
            "GM_deleteValue" -> handleDeleteValue(args)
            "GM_listValues" -> handleListValues()
            "GM_xmlhttpRequest" -> handleXhr(args, msgId)
            "GM_notification" -> handleNotification(args)
            "GM_setClipboard" -> handleSetClipboard(args)
            "GM_openInTab" -> handleOpenInTab(args)
            else -> null
        }
    }

    private fun handleGetValue(args: JSONObject): GeckoResult<Any> {
        val key = args.optString("key", "")
        val defaultVal = args.opt("default")
        val result = GeckoResult<Any>()
        GuaApp.instance.appScope.launch {
            val value = withContext(Dispatchers.IO) { storage.getSync("gm_$key") }
            result.complete(if (value != null) value else defaultVal)
        }
        return result
    }

    private fun handleSetValue(args: JSONObject): GeckoResult<Any> {
        val key = args.optString("key", "")
        val value = args.opt("value")?.toString() ?: ""
        storage.put("gm_$key", value)
        return GeckoResult.fromValue(null)
    }

    private fun handleDeleteValue(args: JSONObject): GeckoResult<Any> {
        val key = args.optString("key", "")
        storage.delete("gm_$key")
        return GeckoResult.fromValue(null)
    }

    private fun handleListValues(): GeckoResult<Any> {
        val result = GeckoResult<Any>()
        GuaApp.instance.appScope.launch {
            val keys = withContext(Dispatchers.IO) { storage.keys() }
            val filtered = keys.filter { it.startsWith("gm_") }
                .map { it.removePrefix("gm_") }
            result.complete(JSONArray(filtered).toString())
        }
        return result
    }

    private fun handleXhr(args: JSONObject, msgId: String): GeckoResult<Any> {
        val result = GeckoResult<Any>()
        GuaApp.instance.appScope.launch {
            try {
                val request = HttpClient.Request(
                    url = args.getString("url"),
                    method = args.optString("method", "GET"),
                    headers = parseHeaders(args.optJSONObject("headers")),
                    body = args.optString("data", null),
                    timeout = args.optInt("timeout", 30000),
                    responseType = args.optString("responseType", "text")
                )
                val response = withContext(Dispatchers.IO) { HttpClient.execute(request) }
                val resp = JSONObject().apply {
                    put("id", msgId)
                    put("status", response.statusCode)
                    put("statusText", response.statusText)
                    put("responseText", response.body)
                    put("readyState", 4)
                }
                // 解析响应头
                val headers = JSONObject()
                response.headers?.forEach { (k, v) -> headers.put(k, v) }
                resp.put("responseHeaders", headers.toString())
                result.complete(resp)
            } catch (e: Exception) {
                result.complete(JSONObject().apply {
                    put("id", msgId)
                    put("error", e.message ?: "请求失败")
                    put("readyState", 4)
                })
            }
        }
        return result
    }

    private fun handleNotification(args: JSONObject): GeckoResult<Any> {
        val title = args.optString("title", "脚本通知")
        val text = args.optString("text", "") ?: args.optString("body", "")
        val imageUrl = args.optString("image", null) ?: args.optString("icon", null)
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setAutoCancel(true)
                .build()
            NotificationManagerCompat.from(context).notify(
                notificationId + title.hashCode(), notification
            )
        } catch (_: Exception) {
            Toast.makeText(context, "$title: $text", Toast.LENGTH_SHORT).show()
        }
        return GeckoResult.fromValue(null)
    }

    private fun handleSetClipboard(args: JSONObject): GeckoResult<Any> {
        val text = args.optString("text", "")
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("GM_setClipboard", text))
        return GeckoResult.fromValue(null)
    }

    private fun handleOpenInTab(args: JSONObject): GeckoResult<Any> {
        val url = args.optString("url", "")
        onOpenInTab?.invoke(url)
        return GeckoResult.fromValue(null)
    }

    private fun parseHeaders(json: JSONObject?): Map<String, String> {
        if (json == null) return emptyMap()
        val headers = mutableMapOf<String, String>()
        json.keys().forEach { key -> headers[key] = json.getString(key) }
        return headers
    }

    var onOpenInTab: ((String) -> Unit)? = null
}
