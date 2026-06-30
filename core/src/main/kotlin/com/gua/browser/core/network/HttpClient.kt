package com.gua.browser.core.network

import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 简单的 HTTP 客户端
 * 用于 GM_xmlhttpRequest 的原生实现（绕过 CORS）
 */
object HttpClient {

    data class Response(
        val statusCode: Int,
        val statusText: String,
        val headers: Map<String, String>,
        val body: String,
        val responseType: String = "text"
    )

    data class Request(
        val url: String,
        val method: String = "GET",
        val headers: Map<String, String> = emptyMap(),
        val body: String? = null,
        val timeout: Int = 30000,
        val responseType: String = "text"
    )

    suspend fun execute(request: Request): Response = withContext(Dispatchers.IO) {
        val connection = URL(request.url).openConnection() as HttpURLConnection

        connection.apply {
            connectTimeout = request.timeout
            readTimeout = request.timeout
            requestMethod = request.method.uppercase()
            instanceFollowRedirects = true

            // 设置请求头
            request.headers.forEach { (k, v) ->
                setRequestProperty(k, v)
            }
        }

        // 写入请求体
        if (!request.body.isNullOrEmpty() && request.method.uppercase() in listOf("POST", "PUT", "PATCH")) {
            connection.doOutput = true
            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(request.body)
                writer.flush()
            }
        }

        val statusCode = connection.responseCode
        val statusText = connection.responseMessage ?: ""

        // 读取响应头
        val headers = mutableMapOf<String, String>()
        connection.headerFields?.forEach { (key, values) ->
            if (key != null && values.isNotEmpty()) {
                headers[key] = values.joinToString(", ")
            }
        }

        // 读取响应体
        val body = try {
            val stream = if (statusCode in 200..399) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            stream?.let { inputStream ->
                BufferedReader(InputStreamReader(inputStream, "UTF-8"))
                    .readText()
            } ?: ""
        } catch (e: Exception) {
            ""
        }

        connection.disconnect()

        Response(
            statusCode = statusCode,
            statusText = statusText,
            headers = headers,
            body = body,
            responseType = request.responseType
        )
    }
}
