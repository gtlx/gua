package com.gua.browser.engine

import android.content.Context

/**
 * GeckoView 运行时
 */
object GeckoSetup {

    private var runtime: Any? = null
    private var available = false

    fun isAvailable(): Boolean = available

    /**
     * 检查 GeckoView 是否可用（类是否存在）
     */
    fun checkAvailability(): Boolean {
        if (available) return true
        available = try {
            Class.forName("org.mozilla.geckoview.GeckoRuntime")
            true
        } catch (_: Exception) {
            false
        }
        return available
    }
}
