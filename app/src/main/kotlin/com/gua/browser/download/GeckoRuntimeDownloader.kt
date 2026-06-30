package com.gua.browser.download

import android.content.Context
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import kotlin.math.min

/**
 * GeckoView 运行时下载器
 *
 * 首次使用时从 Mozilla CDN 下载 Gecko Runtime，
 * 下载后存到 app data 目录，后续直接加载。
 */
class GeckoRuntimeDownloader(private val context: Context) {

    companion object {
        private const val RUNTIME_FILENAME = "geckoview_omni.ja"
        private const val CDN_URL = "https://ftp.mozilla.org/pub/mobile/releases/"
        private const val GECKO_VERSION = "136.0"

        // ARM64 设备的运行时下载链接
        private val DOWNLOAD_URL = "$CDN_URL${GECKO_VERSION}/android-arm64/geckoview_omni.ja"
    }

    data class DownloadState(
        val isDownloading: Boolean = false,
        val progress: Int = 0,       // 0-100
        val downloaded: Boolean = false,
        val error: String? = null
    )

    /** 运行时文件路径 */
    fun getRuntimeFile(): File = File(context.filesDir, RUNTIME_FILENAME)

    /** 是否已下载 */
    fun isRuntimeDownloaded(): Boolean = getRuntimeFile().exists()

    /** 运行时文件大小 */
    fun getRuntimeSize(): Long = getRuntimeFile().length()

    /**
     * 下载运行时（后台协程）
     * @param onProgress 进度回调 (0-100)
     */
    fun download(
        scope: CoroutineScope,
        onProgress: (Int) -> Unit,
        onComplete: (Boolean, String?) -> Unit
    ) {
        scope.launch(Dispatchers.IO) {
            try {
                val url = URL(DOWNLOAD_URL)
                val connection = url.openConnection()
                connection.connect()
                val totalSize = connection.contentLengthLong
                val inputStream = connection.getInputStream()
                val outputFile = getRuntimeFile()

                // 确保目录存在
                outputFile.parentFile?.mkdirs()

                FileOutputStream(outputFile).use { output ->
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

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    onComplete(true, null)
                }
            } catch (e: Exception) {
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
