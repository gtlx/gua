package com.gua.browser.engine

import android.graphics.Bitmap
import android.view.View
import com.gua.browser.GuaApp
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.AllowOrDeny

/**
 * GeckoView 引擎实现
 */
class GeckoEngine(
    private val geckoView: GeckoView,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) : IEngineView {

    private val geckoSession = GeckoSession()
    private val runtime = GeckoRuntime.getDefault(geckoView.context)
    private var sessionState: GeckoSession.SessionState? = null

    private var navigationListener: NavigationListener? = null
    private var progressListener: ProgressListener? = null
    private var pageListener: PageListener? = null

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
        geckoSession.settings = GeckoSessionSettings.Builder()
            .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
            .usePrivateMode(false)
            .build()

        geckoSession.contentDelegate = object : GeckoSession.ContentDelegate {
            override fun onTitleChange(session: GeckoSession, title: String?) {
                currentTitle = title
                pageListener?.onTitleChanged(title ?: "")
            }

            override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
                pageListener?.onFullScreenChanged(fullScreen)
            }

            override fun onExternalResponse(session: GeckoSession, response: GeckoSession.WebResponseInfo) {
                navigationListener?.onDownloadStart(
                    url = response.uri,
                    contentType = response.contentType ?: "",
                    contentLength = response.contentLength
                )
            }

            override fun onCrash(session: GeckoSession) {
                session.open(runtime)
                sessionState?.let { session.restoreState(it) }
            }
        }

        geckoSession.navigationDelegate = object : GeckoSession.NavigationDelegate {
            override fun onLocationChange(session: GeckoSession, url: String?) {
                currentUrl = url
                navigationListener?.onLocationChanged(url ?: "")
            }

            override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
                _canGoBack = canGoBack
                navigationListener?.onBackForwardChanged(canGoBack, _canGoForward)
            }

            override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
                _canGoForward = canGoForward
                navigationListener?.onBackForwardChanged(_canGoBack, canGoForward)
            }

            override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
                val allow = navigationListener?.onLoadRequest(request.uri) ?: true
                return GeckoResult.fromValue(if (allow) AllowOrDeny.ALLOW else AllowOrDeny.DENY)
            }
        }

        geckoSession.progressDelegate = object : GeckoSession.ProgressDelegate {
            override fun onPageStart(session: GeckoSession, url: String) {
                progress = 10
                progressListener?.onPageStarted(url)
            }

            override fun onPageStop(session: GeckoSession, success: Boolean) {
                progress = 100
                progressListener?.onPageFinished(currentUrl ?: "")
            }

            override fun onSecurityChange(session: GeckoSession, securityInfo: GeckoSession.ProgressDelegate.SecurityInformation) {
                progressListener?.onSecurityChanged(securityInfo.isSecure, securityInfo.host ?: "")
            }

            override fun onProgressChange(session: GeckoSession, progress: Int) {
                this@GeckoEngine.progress = progress
                progressListener?.onProgressChanged(progress)
            }
        }

        geckoSession.permissionDelegate = object : GeckoSession.PermissionDelegate {
            override fun onContentPermissionRequest(session: GeckoSession, permission: GeckoSession.PermissionDelegate.ContentPermission): GeckoResult<Int>? {
                return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }

            override fun onAndroidPermissionRequest(session: GeckoSession, permissions: Array<out String>): GeckoResult<Int>? {
                return GeckoResult.fromValue(GeckoSession.PermissionDelegate.PERMISSION_DENY)
            }
        }

        geckoView.setSession(geckoSession)
        geckoSession.open(runtime)
    }

    override val view: View get() = geckoView
    override val session: GeckoSession? get() = geckoSession

    override fun loadUrl(url: String) { geckoSession.loadUri(url) }

    private var _canGoBack = false
    private var _canGoForward = false

    override fun goBack(): Boolean {
        try { geckoSession.goBack(); return true } catch (_: Exception) { return false }
    }

    override fun goForward(): Boolean {
        try { geckoSession.goForward(); return true } catch (_: Exception) { return false }
    }

    override fun reload() { geckoSession.reload() }
    override fun stopLoading() { geckoSession.stop() }
    override fun canGoBack(): Boolean = _canGoBack
    override fun canGoForward(): Boolean = _canGoForward

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        geckoSession.evaluateJavascript(script) { result -> callback?.invoke(result) }
    }

    override fun applySettings(settings: EngineSettings) {
        val builder = GeckoSessionSettings.Builder()
        builder.userAgentMode(if (settings.desktopMode)
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        else
            GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
        builder.usePrivateMode(false)
        geckoSession.settings = builder.build()

        if (settings.textSize != 100) {
            evaluateJavascript("document.body.style.zoom = '${settings.textSize / 100f}';", null)
        }
    }

    override fun captureBitmap(callback: (Bitmap?) -> Unit) {
        // captureBitmap 在当前 GeckoView 版本中不可用
        callback(null)
    }

    override fun onResume() {
        if (!geckoSession.isOpen) geckoSession.open(runtime)
    }

    override fun onPause() {
        // saveState API 在当前版本中不可用
    }

    override fun onDestroy() {
        geckoSession.close()
    }

    // ===== 回调接口 =====
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

    fun setNavigationListener(listener: NavigationListener) { this.navigationListener = listener }
    fun setProgressListener(listener: ProgressListener) { this.progressListener = listener }
    fun setPageListener(listener: PageListener) { this.pageListener = listener }
}
