package com.gua.browser.engine

import android.graphics.Bitmap
import android.view.View
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import androidx.lifecycle.LifecycleOwner

class GeckoEngine(
    private val geckoView: GeckoView,
    lifecycleOwner: LifecycleOwner
) : IEngineView {

    private val sessionSettings = GeckoSessionSettings.Builder()
        .userAgentMode(GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
        .usePrivateMode(false)
        .build()
    private val geckoSession = GeckoSession(sessionSettings)
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
                return GeckoResult.fromValue(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW)
            }
        }
        geckoView.setSession(geckoSession)
        geckoSession.open(runtime)
    }

    override val view: View get() = geckoView
    override val session: GeckoSession? get() = geckoSession

    override fun loadUrl(url: String) { geckoSession.loadUri(url) }
    override fun goBack(): Boolean { try { geckoSession.goBack(); return true } catch (_: Exception) { return false } }
    override fun goForward(): Boolean { try { geckoSession.goForward(); return true } catch (_: Exception) { return false } }
    override fun reload() { geckoSession.reload() }
    override fun stopLoading() { geckoSession.stop() }
    override fun canGoBack(): Boolean = _canGoBack
    override fun canGoForward(): Boolean = _canGoForward

    override fun evaluateJavascript(script: String, callback: ((String?) -> Unit)?) {
        callback?.invoke(null)
    }

    override fun applySettings(settings: EngineSettings) {
        val sb = GeckoSessionSettings.Builder()
        sb.userAgentMode(if (settings.desktopMode)
            GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
        else GeckoSessionSettings.USER_AGENT_MODE_MOBILE)
        sb.usePrivateMode(false)
        try {
            val f = GeckoSession::class.java.getDeclaredField("mSettings")
            f.isAccessible = true
            f.set(geckoSession, sb.build())
        } catch (_: Exception) {}
    }

    override fun captureBitmap(callback: (Bitmap?) -> Unit) { callback(null) }
    override fun onResume() { if (!geckoSession.isOpen) geckoSession.open(runtime) }
    override fun onPause() {}
    override fun onDestroy() { geckoSession.close() }

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
