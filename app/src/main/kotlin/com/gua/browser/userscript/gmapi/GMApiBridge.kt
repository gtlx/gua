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
import kotlinx.coroutines.*
import org.json.JSONObject

/**
 * GM_API 原生侧桥接
 *
 * 处理需要原生 Android API 的 GM_* 调用：
 * - GM_notification → Android Notification
 * - GM_setClipboard → ClipboardManager
 * - GM_xmlhttpRequest → 原生 HTTP 请求（绕过 CORS）
 * - GM_openInTab → 新建标签页
 *
 * 通过 @JavascriptInterface 暴露给 WebView（未来兼容），
 * 但在 GeckoView 中通过 WebExtension 消息通信。
 */
class GMApiBridge(private val context: Context) {

    private val storage = KVStorage(context)
    private val notificationId = 1001

    companion object {
        private const val CHANNEL_ID = "gm_notification"
        private const val TAG = "GMApiBridge"
    }

    // ===== GM_setValue / GM_getValue =====

    suspend fun getValue(key: String): String? {
        return storage.getSync("gm_$key")
    }

    fun setValue(key: String, value: String) {
        storage.put("gm_$key", value)
    }

    fun deleteValue(key: String) {
        storage.delete("gm_$key")
    }

    suspend fun listValues(): List<String> {
        val keys = storage.keys()
        return keys.filter { it.startsWith("gm_") }
            .map { it.removePrefix("gm_") }
    }

    // ===== GM_notification =====

    fun showNotification(title: String?, text: String?, imageUrl: String?) {
        val titleFinal = title ?: "脚本通知"
        val textFinal = text ?: ""

        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(titleFinal)
                .setContentText(textFinal)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(
                notificationId + titleFinal.hashCode(),
                notification
            )
        } catch (e: Exception) {
            Toast.makeText(context, "$titleFinal: $textFinal", Toast.LENGTH_SHORT).show()
        }
    }

    // ===== GM_setClipboard =====

    fun setClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("GM_setClipboard", text)
        clipboard.setPrimaryClip(clip)
    }

    // ===== GM_xmlhttpRequest =====

    /**
     * 原生 HTTP 请求（绕过 CORS）
     *
     * @param jsonArgs JSON 字符串包含请求参数
     * @param callbackId 回调 ID，用于返回结果
     */
    fun xmlHttpRequest(jsonArgs: String, callbackId: String) {
        // 使用 App 全局协程作用域，避免每次调用泄漏协程
        GuaApp.instance.appScope.launch {
            try {
                val args = JSONObject(jsonArgs)
                val request = HttpClient.Request(
                    url = args.getString("url"),
                    method = args.optString("method", "GET"),
                    headers = parseHeaders(args.optJSONObject("headers")),
                    body = args.optString("data", null),
                    timeout = args.optInt("timeout", 30000),
                    responseType = args.optString("responseType", "text")
                )

                val response = HttpClient.execute(request)
                val result = JSONObject().apply {
                    put("status", response.statusCode)
                    put("statusText", response.statusText)
                    put("responseText", response.body)
                    put("finalUrl", request.url)
                    put("callbackId", callbackId)
                }

                onHttpResponse(result.toString())
            } catch (e: Exception) {
                val error = JSONObject().apply {
                    put("error", e.message ?: "未知错误")
                    put("callbackId", callbackId)
                }
                onHttpError(error.toString())
            }
        }
    }

    private fun parseHeaders(json: JSONObject?): Map<String, String> {
        if (json == null) return emptyMap()
        val headers = mutableMapOf<String, String>()
        json.keys().forEach { key ->
            headers[key] = json.getString(key)
        }
        return headers
    }

    // ===== 回调（由 JavaScript 端注册） =====
    private var httpResponseCallback: ((String) -> Unit)? = null
    private var httpErrorCallback: ((String) -> Unit)? = null

    fun setHttpCallbacks(
        onResponse: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        httpResponseCallback = onResponse
        httpErrorCallback = onError
    }

    private fun onHttpResponse(json: String) {
        httpResponseCallback?.invoke(json)
    }

    private fun onHttpError(json: String) {
        httpErrorCallback?.invoke(json)
    }

    // ===== GM_openInTab =====
    private var openInTabCallback: ((String) -> Unit)? = null

    fun setOpenInTabCallback(callback: (String) -> Unit) {
        openInTabCallback = callback
    }

    fun requestOpenInTab(url: String) {
        openInTabCallback?.invoke(url)
    }
}
