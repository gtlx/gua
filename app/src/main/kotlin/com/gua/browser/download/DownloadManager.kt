package com.gua.browser.download

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap

/**
 * 下载管理器
 *
 * 使用系统 DownloadManager 处理文件下载。
 * 支持断点续传、通知栏进度显示。
 */
class AppDownloadManager(private val context: Context) {

    private val downloadManager =
        context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    data class DownloadRequest(
        val url: String,
        val fileName: String? = null,
        val mimeType: String? = null,
        val contentLength: Long = -1L
    )

    /**
     * 开始下载
     * @return downloadId
     */
    fun startDownload(request: DownloadRequest): Long {
        val uri = Uri.parse(request.url)
        val fileName = request.fileName ?: guessFileName(request.url, request.mimeType)

        val req = DownloadManager.Request(uri).apply {
            setTitle(fileName)
            setDescription("正在下载...")
            setNotificationVisibility(
                DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
            )
            setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS,
                "GuaBrowser/$fileName"
            )
            setMimeType(request.mimeType ?: "*/*")
            setAllowedOverMetered(true)
            setAllowedOverRoaming(false)
        }

        return downloadManager.enqueue(req)
    }

    private fun guessFileName(url: String, mimeType: String?): String {
        val path = Uri.parse(url).lastPathSegment ?: "download"
        if (path.contains(".")) return path

        // 根据 MIME 类型添加扩展名
        val ext = mimeType?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
        return if (ext != null) "$path.$ext" else path
    }

    /**
     * 查询下载状态
     */
    fun queryStatus(downloadId: Long): DownloadStatus? {
        val cursor = downloadManager.query(
            DownloadManager.Query().setFilterById(downloadId)
        )
        cursor.use {
            if (it.moveToFirst()) {
                val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
                val bytesTotal = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))
                val bytesSoFar = it.getLong(it.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val uri = it.getString(it.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI))

                return DownloadStatus(
                    status = status,
                    progress = if (bytesTotal > 0) (bytesSoFar * 100 / bytesTotal).toInt() else 0,
                    localUri = uri
                )
            }
        }
        return null
    }

    data class DownloadStatus(
        val status: Int,
        val progress: Int,
        val localUri: String?
    )
}
