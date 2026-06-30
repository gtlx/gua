package com.gua.browser.engine

import android.graphics.Bitmap
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView

/**
 * GeckoView 引擎实现
 *
 * 基于 Mozilla GeckoView 的浏览器引擎封装。
 * 提供页面加载、导航、JS 交互等核心功能。
 */
class GeckoEngine(
    private val geckoView: GeckoView,
    private val lifecycleOwner: LifecycleOwner
) : IEngineView {

    private val geckoSession: GeckoSession = GeckoSession()
    private var sessionState: GeckoSession.SessionState? = null

    // ===== 回调监听 =====
    private var navigationListener: NavigationListener? = null
    private var progressListener: ProgressListener? = null
    private var pageListener: PageListener? = null

    // ===== 页面状态 =====
    override var currentUrl: String? = null
        private set
    override var currentTitle: String? = null
        private set
    override var progress: Int = 0
        private set

    init {
        setupSession()
    }

    private fun setupSession() {
        val settings = GeckoSessionSettings.Builder()
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .usePrivateMode(false)
            .build()

        geckoSession.settings = settings

        // 注册内容代理（处理页面事件）
        geckoSession.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title
                pageListener?.onTitleChanged(title ?: "")
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                pageListener?.onFullScreenChanged(fullScreen)
            }

            override fun onExternalResponse(session: GeckoSession, response: GeckoSession.WebResponseInfo) {
                // 处理下载
                navigationListener?.onDownloadStart(
                    url = response.uri,
                    contentType = response.contentType ?: "",
                    contentLength = response.contentLength
                )
            }

            override fun onCrash(session: GeckoSession) {
                // 会话崩溃时恢复
                session.open(geckoView.getRuntime())
                sessionState?.let { state ->
                    session.restoreState(state)
                }
            }
        }

        // 注册导航代理
        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(
                session: GeckoSession,
                url: String?,
                perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>
            ) {
                currentUrl = url
                navigationListener?.onLocationChanged(url ?: "")
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                navigationListener?.onBackForwardChanged(canGoBack, canGoForward())
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                navigationListener?.onBackForwardChanged(canGoBack(), canGoForward)
            }

            override fun onLoadRequest(
                session: GeckoSession,
                request: GeckoSession.NavigationDelegate.LoadRequest
            ): GeckoResult<AllowOrDeny>? {
                // 广告过滤钩子（由 AdBlockEngine 使用）
                return navigationListener?.onLoadRequest(request.uri)
                    ?.let { result ->
                        if (result) GeckoResult.fromValue(AllowOrDeny.ALLOW)
                        else GeckoResult.fromValue(AllowOrDeny.DENY)
                    }
            }
        }

        // 注册进度代理
        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                progress = 10
                progressListener?.onPageStarted(url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progress = 100
                progressListener?.onPageFinished(currentUrl ?: "")
            }

            override fun onSecurityChange(
                session: GeckoSession,
                securityInfo: GeckoSession.ProgressDelegate.SecurityInformation
            ) {
                progressListener?.onSecurityChanged(
                    isSecure = securityInfo.isSecure,
                    host = securityInfo.host ?: ""
                )
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                this@GeckoEngine.progress = progress
                progressListener?.onProgressChanged(progress)
            }
        }

        // 权限代理（定位、相机、麦克风等）
        geckoSession.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(
                session: GeckoSession,
                permission: ContentPermission
            ): GeckoResult<Int>? {
                // 默认允许（后续可改为用户选择）
                return GeckoResult.fromValue(ContentPermission.VALUE_ALLOW)
            }

            override fun onAndroidPermissionRequest(
                session: GeckoSession,
                permissions: Array<out String>
            ): GeckoResult<Int>? {
                return GeckoResult.fromValue(GeckoSession.PermissionDelegate.PERMISSION_ALLOW)
            }
        }

        // 将 Session 绑定到 View
        geckoView.setSession(geckoSession)
        geckoSession.open(geckoView.getRuntime())
    }

    override val view: View get() = geckoView
    override val session: GeckoSession? get() = geckoSession

    // ===== 导航实现 =====
    override fun loadUrl(url: String) {
        geckoSession.loadUri(url)
    }

    override fun goBack(): Boolean {
        if (geckoSession.canGoBack) {
            geckoSession.goBack()
            return true
        }
        return false
    }

    override fun goForward(): Boolean {
        if (geckoSession.canGoForward) {
            geckoSession.goForward()
            return true        }
        return false
    }

    override fun reload() {
        geckoSession.reload()
    }

    override fun stopLoading() {
        geckoSession.stop()
    }

    override fun canGoBack(): Boolean = geckoSession.canGoBack
    override fun canGoForward(): Boolean = geckoSession.canGoForward

    // ===== JS 交互 =====
    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        geckoSession.evaluateJavascript(script) { value ->
            callback?.invoke(value)
        }
    }

    // ===== 设置 =====
    override fun applySettings(settings: EngineSettings) {
        val builder = GeckoSessionSettings.Builder()

        val userAgentMode = if (settings.desktopMode) {
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        } else {
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE
        }
        builder.userAgentMode(userAgentMode)

        builder.usePrivateMode(false)

        // 文本缩放（通过 JS 注入实现）
        if (settings.textSize != 100) {
            val scale = settings.textSize / 100f
            evaluateJavascript(
                "document.body.style.zoom = '$scale';",
                null
            )
        }

        geckoSession.settings = builder.build()
    }

    // ===== 截图 =====
    override fun captureBitmap(callback: (Bitmap?) -> Unit) {
        geckoView.captureBitmap().then { bitmap ->
            callback(bitmap)
            GeckoResult.fromValue(bitmap)
        }
    }

    // ===== 生命周期 =====
    override fun onResume() {
        geckoSession?.let { session ->
            if (!session.isOpen) {
                session.open(geckoView.getRuntime())
            }
        }
    }

    override fun onPause() {
        geckoSession?.let { session ->
            sessionState = session.saveState()
        }
    }

    override fun onDestroy() {
        geckoSession.close()
    }

    // ===== 回调接口 =====
    interface NavigationListener {
        fun onLocationChanged(url: String) {}
        fun onBackForwardChanged(canGoBack: Boolean, canGoForward: Boolean) {}
        fun onLoadRequest(uri: String): Boolean = true  // true = allow
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

    fun setNavigationListener(listener: NavigationListener) {
        this.navigationListener = listener
    }

    fun setProgressListener(listener: ProgressListener) {
        this.progressListener = listener
    }

    fun setPageListener(listener: PageListener) {
        this.pageListener = listener
    }
}
