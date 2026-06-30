package com.gua.browser.engine

import android.content.Context
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

/**
 * GeckoView 运行时初始化
 *
 * 单例模式管理 GeckoRuntime，整个应用共用。
 * 配置包括：
 * - 控制台日志
 * - 多进程模式
 * - 内存缓存路径
 * - 跟踪保护
 */
object GeckoSetup {

    private var runtime: GeckoRuntime? = null

    /**
     * 初始化或获取 GeckoRuntime 实例
     */
    fun getRuntime(context: Context): GeckoRuntime {
        return runtime ?: synchronized(this) {
            runtime ?: createRuntime(context.applicationContext).also {
                runtime = it
            }
        }
    }

    private fun createRuntime(context: Context): GeckoRuntime {
        val settings = GeckoRuntimeSettings.Builder()
            .consoleOutput(true)
            .remoteDebuggingEnabled(true)
            .build()

        return GeckoRuntime.create(context, settings)
    }
}
