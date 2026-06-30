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
            // 控制台输出（调试时有用）
            .consoleOutput(true)
            // 使用多进程模式（默认开启）
            .useMultiprocess(true)
            // 跟踪保护级别
            .trackingProtectionOptions(
                GeckoRuntimeSettings.TrackingProtectionOptions.Builder()
                    .strict(true)  // 严格模式
                    .build()
            )
            // 自动播放策略
            .autoplayDefault(false)
            // 远程调试（调试用）
            .remoteDebuggingEnabled(true)
            .build()

        return GeckoRuntime.create(context, settings)
    }
}
