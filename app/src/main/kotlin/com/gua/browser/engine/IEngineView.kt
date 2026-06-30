package com.gua.browser.engine

import android.graphics.Bitmap
import android.view.View
import org.mozilla.geckoview.GeckoSession

/**
 * 浏览器引擎统一抽象接口
 *
 * 当前仅实现 GeckoView 版本，但保留接口抽象以便未来扩展。
 * 所有引擎差异都在此接口后隔离。
 */
interface IEngineView {

    /** 返回原生 View，用于嵌入 UI 容器 */
    val view: View

    /** GeckoSession 引用（用于 WebExtension 注入等操作） */
    val session: GeckoSession?

    // ===== 导航 =====
    fun loadUrl(url: String)
    fun goBack(): Boolean
    fun goForward(): Boolean
    fun reload()
    fun stopLoading()
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean

    // ===== 页面状态 =====
    val currentUrl: String?
    val currentTitle: String?
    val progress: Int  // 0-100

    // ===== JS 交互 =====
    fun evaluateJavascript(script: String, callback: ((String?) -> Unit)? = null)

    // ===== 设置 =====
    fun applySettings(settings: EngineSettings)

    // ===== 页面截图 =====
    fun captureBitmap(callback: (Bitmap?) -> Unit)

    // ===== 生命周期 =====
    fun onResume()
    fun onPause()
    fun onDestroy()
}

/**
 * 引擎设置值对象
 */
data class EngineSettings(
    val nightMode: Boolean = false,
    val adblockEnabled: Boolean = true,
    val javascriptEnabled: Boolean = true,
    val desktopMode: Boolean = false,
    val textSize: Int = 100,
    val userAgent: String? = null
)
