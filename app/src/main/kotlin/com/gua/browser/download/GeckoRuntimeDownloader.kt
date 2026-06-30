package com.gua.browser.download

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.util.zip.ZipFile

/**
 * GeckoView 运行时下载器
 */
class GeckoRuntimeDownloader(private val context: Context) {

    companion object {
        private const val TAG = "GeckoDownloader"
        private const val RUNTIME_FILENAME = "geckoview_omni.ja"
        // GeckoView AAR 下载地址（Maven Central）
        private const val AAR_URL = "https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/136.0.20250227124745/geckoview-136.0.20250227124745.aar"
        // AAR 中 omni.ja 的路径
        private const val OMNI_PATH = "omni.ja"
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
                val aarFile = File(context.cacheDir, "geckoview_temp.aar")
                val url = URL(AAR_URL)
                val connection = url.openConnection()
                connection.connect()
                val totalSize = connection.contentLengthLong
                val inputStream = connection.getInputStream()

                // 下载 AAR
                aarFile.parentFile?.mkdirs()
                FileOutputStream(aarFile).use { output ->
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
                                // 前 80% 是下载 AAR，后 20% 是解压
                                withContext(Dispatchers.Main) { onProgress(p / 2) }
                            }
                        }
                    }
                }
                inputStream.close()

                withContext(Dispatchers.Main) { onProgress(40) }

                // 从 AAR 中解压 omni.ja
                Log.d(TAG, "Extracting omni.ja from AAR...")
                ZipFile(aarFile).use { zip ->
                    val entry = zip.getEntry(OMNI_PATH) ?: zip.getEntry("assets/$OMNI_PATH")
                        ?: throw Exception("omni.ja not found in AAR")

                    val runtimeFile = getRuntimeFile()
                    runtimeFile.parentFile?.mkdirs()
                    zip.getInputStream(entry).use { zis ->
                        FileOutputStream(runtimeFile).use { out ->
                            zis.copyTo(out)
                        }
                    }
                }

                // 删除临时 AAR
                aarFile.delete()
                Log.d(TAG, "Runtime extracted: ${getRuntimeSize()} bytes")

                withContext(Dispatchers.Main) {
                    onProgress(100)
                    onComplete(true, null)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Download failed", e)
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
