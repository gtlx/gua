package com.gua.browser

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import com.gua.browser.adblock.AdBlockEngine
import com.gua.browser.bookmark.BookmarkManager
import com.gua.browser.bookmark.HistoryManager
import com.gua.browser.download.AppDownloadManager
import com.gua.browser.download.GeckoRuntimeDownloader
import com.gua.browser.settings.AppSettings
import com.gua.browser.userscript.ScriptManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * GuaBrowser 全局应用入口
 *
 * 初始化所有全局单例：
 * - 油猴脚本管理器
 * - 广告过滤引擎
 * - 书签/历史管理器
 * - 应用设置
 * - 通知渠道
 */
class GuaApp : Application() {

    lateinit var scriptManager: ScriptManager
        private set
    lateinit var adBlockEngine: AdBlockEngine
        private set
    lateinit var bookmarkManager: BookmarkManager
        private set
    lateinit var historyManager: HistoryManager
        private set
    lateinit var downloadManager: AppDownloadManager
        private set
    lateinit var appSettings: AppSettings
        private set
    lateinit var runtimeDownloader: GeckoRuntimeDownloader
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        instance = this

        initNotificationChannels()
        initManagers()
    }

    private fun initNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "gm_notification",
                "脚本通知",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用户脚本发送的通知"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)

            val downloadChannel = NotificationChannel(
                "downloads",
                "下载",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "文件下载进度"
            }
            manager.createNotificationChannel(downloadChannel)
        }
    }

    private fun initManagers() {
        scriptManager = ScriptManager(this).also {
            it.init()
        }
        adBlockEngine = AdBlockEngine(this).also {
            scope.launch { it.init() }
        }
        bookmarkManager = BookmarkManager(this)
        historyManager = HistoryManager(this)
        downloadManager = AppDownloadManager(this)
        appSettings = AppSettings(this)
        runtimeDownloader = GeckoRuntimeDownloader(this)

        Log.d("GuaApp", "所有管理器初始化完成")
    }

    override fun onTerminate() {
        scriptManager.destroy()
        super.onTerminate()
    }

    companion object {
        lateinit var instance: GuaApp
            private set
    }
}
