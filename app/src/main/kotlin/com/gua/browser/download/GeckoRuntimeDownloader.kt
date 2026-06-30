package com.gua.browser.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL

/**
 * GeckoView 运行时下载器
 *
 * 从 Maven Central 下载 GeckoView AAR 并解压出所需文件。
 *
 * 注意：现代 GeckoView 已不再使用 omni.ja，
 * 此下载器下载的是 GeckoView 的 AAR 包本身。
 */
class GeckoRuntimeDownloader(private val context: Context) {

    companion object {
        private const val TAG = "GeckoDownloader"
        private const val RUNTIME_FILENAME = "geckoview.aar"
        // GeckoView AAR 下载地址（Maven Central）
        private const val AAR_URL = "https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/136.0.20250227124745/geckoview-136.0.20250227124745.aar"
    }

    /** 运行时文件路径 */
    fun getRuntimeFile(): File = File(context.filesDir, RUNTIME_FILENAME)

    /** 是否已下载 */
    fun isRuntimeDownloaded(): Boolean = getRuntimeFile().exists()

    /** 运行时文件大小 */
    fun getRuntimeSize(): Long = getRuntimeFile().length()

    /**
     * 下载运行时（后台协程）
     */
    fun download(
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val runtimeFile = getRuntimeFile()
                runtimeFile.parentFile?.mkdirs()

                val url = URL(AAR_URL)
                val connection = url.openConnection()
                connection.connect()
                val totalSize = connection.contentLengthLong
                val inputStream = connection.getInputStream()

                FileOutputStream(runtimeFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L
                    var lastProgress = 0

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        if (totalSize > 0) {
                            val p = ((totalRead * 100) / totalSize).toInt()
                            if (p != lastProgress) {
                                lastProgress = p
                                withContext(Dispatchers.Main) { onProgress(p) }
                            }
                        }
                    }
                }
                inputStream.close()

                Log.d(TAG, "Runtime 已下载: ${getRuntimeSize()} bytes")

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "下载失败", e)
                withContext(Dispatchers.Main) {
                    onComplete(false, e.message ?: "下载失败")
                }
            }
        }
    }

    /** 删除已下载的运行时 */
    fun deleteRuntime() {
        getRuntimeFile().delete()
    }
}
