package com.gua.browser.download

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.database.Cursor
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

    private val prefs: SharedPreferences =
        context.getSharedPreferences("download_prefs", Context.MODE_PRIVATE)

    /** 下载子目录（在 Downloads 目录下） */
    var downloadSubPath: String
        get() = prefs.getString("download_sub_path", "GuaBrowser") ?: "GuaBrowser"
        set(value) { prefs.edit().putString("download_sub_path", value).apply() }

    data class DownloadRequest(
        val url: String,
        val fileName: String? = null,
        val mimeType: String? = null,
        val contentLength: Long = -1L
    )

    data class DownloadStatus(
        val id: Long,
        val title: String,
        val url: String,
        val status: Int,
        val progress: Int,
        val bytesSoFar: Long,
        val bytesTotal: Long,
        val localUri: String?,
        val mimeType: String?,
        val lastModified: Long
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
                "$downloadSubPath/$fileName"
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

        val ext = mimeType?.let {
            MimeTypeMap.getSingleton().getExtensionFromMimeType(it)
        }
        return if (ext != null) "$path.$ext" else path
    }

    /**
     * 查询单个下载状态
     */
    fun queryStatus(downloadId: Long): DownloadStatus? {
        val cursor = downloadManager.query(
            DownloadManager.Query().setFilterById(downloadId)
        )
        return cursor.use { parseCursor(it).firstOrNull() }
    }

    /**
     * 查询所有下载
     */
    fun queryAll(): List<DownloadStatus> {
        val cursor = downloadManager.query(DownloadManager.Query())
        return cursor.use { parseCursor(it) }
    }

    /**
     * 移除下载记录
     */
    fun remove(vararg ids: Long) {
        downloadManager.remove(*ids)
    }

    private fun parseCursor(cursor: Cursor?): List<DownloadStatus> {
        if (cursor == null || !cursor.moveToFirst()) return emptyList()
        val result = mutableListOf<DownloadStatus>()
        do {
            result.add(
                DownloadStatus(
                    id = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_ID)),
                    title = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TITLE)) ?: "",
                    url = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_URI)) ?: "",
                    status = cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    progress = 0,
                    bytesSoFar = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    bytesTotal = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    localUri = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LOCAL_URI)),
                    mimeType = cursor.getString(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_MEDIA_TYPE)),
                    lastModified = cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_LAST_MODIFIED_TIMESTAMP))
                ).also { info ->
                    info.copy(
                        progress = if (info.bytesTotal > 0)
                            (info.bytesSoFar * 100 / info.bytesTotal).toInt() else 0
                    )
                }
            )
        } while (cursor.moveToNext())
        return result
    }
}
