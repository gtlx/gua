package com.gua.browser.engine

import android.graphics.Bitmap
import android.view.View
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * GeckoView 引擎封装
 *
 * 负责 GeckoSession 的生命周期管理和事件转发。
 */
class GeckoEngine(
    private val geckoView: GeckoView
) : IEngineView {

    private var sessionSettings = GeckoSessionSettings.Builder()
        .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
        .usePrivateMode(false)
        .build()
    private var geckoSession = GeckoSession(sessionSettings)
    private val runtime = GeckoRuntime.getDefault(geckoView.context)
    private var navigationListener: NavigationListener? = null
    private var progressListener: ProgressListener? = null
    private var pageListener: PageListener? = null

    override var currentUrl: String? = null
        private set
    override var currentTitle: String? = null
        private set
    override var progress: Int = 0
        private set
    private var _canGoBack = false
    private var _canGoForward = false

    init { setupSession() }

    private fun setupSession() {
        geckoSession.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title; pageListener?.onTitleChanged(title ?: "")
            }
            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                pageListener?.onFullScreenChanged(fullScreen)
            }
            override fun onCrash(session: GeckoSession) { session.open(runtime) }
        }
        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                _canGoBack = canGoBack
                navigationListener?.onBackForwardChanged(canGoBack, _canGoForward)
            }
            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                _canGoForward = canGoForward
                navigationListener?.onBackForwardChanged(_canGoBack, canGoForward)
            }
            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                try {
                    val allow = navigationListener?.onLoadRequest(request.uri) ?: true
                    return GeckoResult.fromValue(if (allow) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
                } catch (_: Exception) { return null }
            }
        }
        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                progress = 10; progressListener?.onPageStarted(url)
            }
            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progress = 100; progressListener?.onPageFinished(currentUrl ?: "")
            }
            override fun onSecurityChange(session: GeckoSession, info: GeckoSession.ProgressDelegate.SecurityInformation) {
                progressListener?.onSecurityChanged(info.isSecure, info.host ?: "")
            }
            override fun onProgressChange(session: GeckoSession, progress: Int) {
                this@GeckoEngine.progress = progress
                progressListener?.onProgressChanged(progress)
            }
        }
        geckoSession.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(session: GeckoSession, perm: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int>? {
                // 安全：地理位置默认允许，其他高风险权限默认拒绝
                val allow = try {
                    val typeField = GeckoSession.PermissionDelegate.ContentPermission::class.java.getField("type")
                    val permType = typeField.getInt(perm)
                    permType == GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION
                } catch (_: Exception) {
                    false
                }
                return GeckoResult.fromValue(
                    if (allow) GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW
                    else GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY
                )
            }
        }
        geckoView.setSession(geckoSession)
        geckoSession.open(runtime)
    }

    override val view: View get() = geckoView
    override val session: GeckoSession? get() = geckoSession

    override fun loadUrl(url: String) {
        if (!geckoSession.isOpen) geckoSession.open(runtime)
        geckoSession.loadUri(url)
    }
    override fun goBack(): Boolean {
        try { if (geckoSession.isOpen) geckoSession.goBack(); return true } catch (_: Exception) { return false }
    }
    override fun goForward(): Boolean {
        try { if (geckoSession.isOpen) geckoSession.goForward(); return true } catch (_: Exception) { return false }
    }
    override fun reload() {
        if (geckoSession.isOpen) geckoSession.reload()
    }
    override fun stopLoading() {
        if (geckoSession.isOpen) geckoSession.stop()
    }
    override fun canGoBack(): Boolean = _canGoBack
    override fun canGoForward(): Boolean = _canGoForward

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        // GeckoView 自 110+ 起移除 evaluateJavascript 公开 API。
        // JS 注入使用 WebExtension 机制（ScriptInjector），
        // 此处保持兼容空实现供 FindInPage 等非关键功能降级使用。
        callback?.invoke(null)
    }

    override fun applySettings(settings: EngineSettings) {
        // 保存当前 URL（在重建会话前读取）
        val savedUrl = currentUrl ?: ""

        val builder = GeckoSessionSettings.Builder()
        builder.userAgentMode(
            if (settings.desktopMode)
                GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
            else
                GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        )
        builder.usePrivateMode(settings.privateMode)
        builder.useTrackingProtection(true)
        sessionSettings = builder.build()

        // 重建会话以应用新设置
        try {
            val oldSession = geckoSession
            val newSession = GeckoSession(sessionSettings)
            copyDelegates(oldSession, newSession)
            geckoView.setSession(newSession)
            newSession.open(runtime)
            geckoSession = newSession
            oldSession.close()

            // 重建后重新加载当前页面
            if (savedUrl.isNotBlank() && !savedUrl.startsWith("about:")) {
                geckoSession.loadUri(savedUrl)
            }
        } catch (e: Exception) {
            android.util.Log.e("GeckoEngine", "applySettings failed", e)
        }
    }

    private fun copyDelegates(old: GeckoSession, new: GeckoSession) {
        new.contentDelegate = old.contentDelegate
        new.navigationDelegate = old.navigationDelegate
        new.progressDelegate = old.progressDelegate
        new.permissionDelegate = old.permissionDelegate
    }

    override fun findInPage(query: String, forward: Boolean) {
        if (query.isEmpty()) {
            geckoSession.finder.clear()
            return
        }
        val findFlags = if (forward) 0 else 1 // 1 = backwards
        geckoSession.finder.find(query, findFlags)
    }

    override fun clearFindInPage() {
        geckoSession.finder.clear()
    }

    override fun captureBitmap(callback: (Bitmap?) -> Unit) { callback(null) }
    override fun onResume() { if (!geckoSession.isOpen) geckoSession.open(runtime) }
    override fun onPause() {}
    override fun onDestroy() { geckoSession.close() }

    // ===== 监听器接口 =====
    interface NavigationListener {
        fun onLocationChanged(url: String) {}
        fun onBackForwardChanged(canGoBack: Boolean, canGoForward: Boolean) {}
        fun onLoadRequest(uri: String): Boolean = true
        fun onDownloadStart(url: String, contentType: String, contentLength: Long) {}
    }
    interface ProgressListener {
        fun onPageStarted(url: String) {}
        fun onPageFinished(url: String) {}
        fun onProgressChanged(progress: Int) {}
        fun onSecurityChanged(isSecure: Boolean, host: String) {}
    }
    interface PageListener {
        fun onTitleChanged(title: String) {}
        fun onFullScreenChanged(fullScreen: Boolean) {}
    }
    fun setNavigationListener(l: NavigationListener) { navigationListener = l }
    fun setProgressListener(l: ProgressListener) { progressListener = l }
    fun setPageListener(l: PageListener) { pageListener = l }
}
