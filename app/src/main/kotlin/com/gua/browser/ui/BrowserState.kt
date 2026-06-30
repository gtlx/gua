package com.gua.browser.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.gua.browser.engine.EngineManager
import com.gua.browser.engine.EngineSettings
import com.gua.browser.engine.GeckoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mozilla.geckoview.GeckoSession

/**
 * 浏览器全局状态
 *
 * 在 Compose 层和引擎层之间建立双向绑定。
 * 所有 UI 可观察状态都集中在这里。
 */
class BrowserState {

    // ===== 当前标签状态 =====
    var url by mutableStateOf("")
    var pageTitle by mutableStateOf("")
    var progress by mutableIntStateOf(0)
    var isSecure by mutableStateOf(true)
    var securityHost by mutableStateOf("")

    // ===== 导航状态 =====
    var canGoBack by mutableStateOf(false)
    var canGoForward by mutableStateOf(false)

    // ===== UI 状态 =====
    var isUrlFocused by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var showQuickSettings by mutableStateOf(false)
    var showTabSwitcher by mutableStateOf(false)
    var showScriptManager by mutableStateOf(false)
    var showFindInPage by mutableStateOf(false)
    var showSearchEngineSettings by mutableStateOf(false)
    var showSettings by mutableStateOf(false)
    var showBookmarks by mutableStateOf(false)
    var showHistory by mutableStateOf(false)

    // ===== 查找 =====
    var findQuery by mutableStateOf("")
    var findMatchCount by mutableIntStateOf(0)
    var findCurrentIndex by mutableIntStateOf(0)

    // ===== 标签列表 =====
    var tabs by mutableStateOf<List<EngineManager.Tab>>(emptyList())
    var activeTabIndex by mutableIntStateOf(0)

    // ===== 布局设置 =====
    enum class ToolbarPos { TOP, BOTTOM }
    var toolbarPosition by mutableStateOf(ToolbarPos.BOTTOM)
    var showUrlBar by mutableStateOf(true)

    // ===== 自定义颜色 =====
    var toolbarColor by mutableStateOf(-1)
    var urlBarColor by mutableStateOf(-1)
    var bgColor by mutableStateOf(-1)

    // ===== 工具栏按钮显示 =====
    var showBackBtn by mutableStateOf(true)
    var showForwardBtn by mutableStateOf(true)
    var showHomeBtn by mutableStateOf(true)
    var showTabsBtn by mutableStateOf(true)
    var showMenuBtn by mutableStateOf(true)

    // ===== 功能设置 =====
    var isNightMode by mutableStateOf(false)
    var isAdblockEnabled by mutableStateOf(true)
    var isDesktopMode by mutableStateOf(false)

    // ===== 书签状态 =====
    var isBookmarked by mutableStateOf(false)

    // ===== 引擎引用（查找功能需要） =====
    var geckoSession: GeckoSession? by mutableStateOf(null)

    /** 当前引用的引擎（用于应用设置） */
    private var currentEngine: GeckoEngine? = null

    // ===== 搜索 =====
    var searchEngines by mutableStateOf(
        mutableListOf(
            SearchEngine("百度", "https://www.baidu.com/s?wd=%s", "B"),
            SearchEngine("必应", "https://www.bing.com/search?q=%s", "G"),
            SearchEngine("谷歌", "https://www.google.com/search?q=%s", "Gg"),
            SearchEngine("搜狗", "https://www.sogou.com/web?query=%s", "Sg"),
            SearchEngine("DuckDuckGo", "https://duckduckgo.com/?q=%s", "D")
        )
    )
    var activeSearchEngineIndex by mutableIntStateOf(0)

    /** 当前搜索引擎 */
    val activeSearchEngine: SearchEngine
        get() = searchEngines.getOrElse(activeSearchEngineIndex) { searchEngines.first() }

    /** 搜索图标显示的简短标签 */
    val searchEngineLabel: String
        get() = activeSearchEngine.shortName

    /** 是否在主页 */
    val isHomePage: Boolean
        get() = url == "about:start" || url == "about:blank" || url.isEmpty()

    // ===== 引擎回调绑定 =====
    fun bindEngine(engine: GeckoEngine?) {
        engine ?: return
        currentEngine = engine
        geckoSession = engine.session

        engine.setNavigationListener(object : GeckoEngine.NavigationListener {
            override fun onLocationChanged(url: String) {
                this@BrowserState.url = url
                // 每次 URL 变化时检查书签状态
                checkBookmarkStatus(url)
            }

            override fun onBackForwardChanged(canGoBack: Boolean, canGoForward: Boolean) {
                this@BrowserState.canGoBack = canGoBack
                this@BrowserState.canGoForward = canGoForward
            }

            override fun onLoadRequest(uri: String): Boolean {
                return if (isAdblockEnabled) {
                    !com.gua.browser.GuaApp.instance.adBlockEngine.shouldBlock(uri)
                } else true
            }
        })

        engine.setProgressListener(object : GeckoEngine.ProgressListener {
            override fun onPageStarted(url: String) {
                isLoading = true
                progress = 10
            }

            override fun onPageFinished(url: String) {
                isLoading = false
                progress = 100
            }

            override fun onProgressChanged(progress: Int) {
                this@BrowserState.progress = progress
            }

            override fun onSecurityChanged(isSecure: Boolean, host: String) {
                this@BrowserState.isSecure = isSecure
                this@BrowserState.securityHost = host
            }
        })

        engine.setPageListener(object : GeckoEngine.PageListener {
            override fun onTitleChanged(title: String) {
                pageTitle = title
            }
        })

        // 应用当前桌面模式设置
        applyDesktopMode()
    }

    /** 应用桌面模式设置到当前引擎 */
    fun applyDesktopMode() {
        currentEngine?.applySettings(
            EngineSettings(desktopMode = isDesktopMode)
        )
    }

    /** 检查当前 URL 是否已收藏 */
    private fun checkBookmarkStatus(url: String) {
        if (url.isBlank() || url.startsWith("about:")) {
            isBookmarked = false
            return
        }
        // 异步检查
        CoroutineScope(Dispatchers.IO).launch {
            val exists = com.gua.browser.GuaApp.instance.bookmarkManager.exists(url)
            if (exists != isBookmarked) {
                isBookmarked = exists
            }
        }
    }

    fun updateTabList(manager: EngineManager) {
        tabs = manager.allTabs
        activeTabIndex = manager.currentIndex
    }

    fun switchSearchEngine() {
        activeSearchEngineIndex = (activeSearchEngineIndex + 1) % searchEngines.size
    }

    // ===== 搜索引擎管理 =====
    fun addSearchEngine(name: String, url: String, shortName: String = name.take(2)) {
        searchEngines = (searchEngines + SearchEngine(name, url, shortName)).toMutableList()
    }

    fun removeSearchEngine(index: Int) {
        if (searchEngines.size <= 1) return
        val newList = searchEngines.toMutableList()
        newList.removeAt(index)
        searchEngines = newList
        if (activeSearchEngineIndex >= newList.size) {
            activeSearchEngineIndex = newList.size - 1
        }
    }

    fun setActiveSearchEngine(index: Int) {
        if (index in searchEngines.indices) {
            activeSearchEngineIndex = index
        }
    }

    fun moveSearchEngineUp(index: Int) {
        if (index <= 0) return
        val list = searchEngines.toMutableList()
        val item = list.removeAt(index)
        list.add(index - 1, item)
        searchEngines = list
        if (activeSearchEngineIndex == index) {
            activeSearchEngineIndex = index - 1
        } else if (activeSearchEngineIndex == index - 1) {
            activeSearchEngineIndex = index
        }
    }

    fun moveSearchEngineDown(index: Int) {
        if (index >= searchEngines.size - 1) return
        val list = searchEngines.toMutableList()
        val item = list.removeAt(index)
        list.add(index + 1, item)
        searchEngines = list
        if (activeSearchEngineIndex == index) {
            activeSearchEngineIndex = index + 1
        } else if (activeSearchEngineIndex == index + 1) {
            activeSearchEngineIndex = index
        }
    }
}

/**
 * 搜索引擎数据模型
 */
data class SearchEngine(
    val name: String,
    val urlTemplate: String,  // 含 %s 占位符
    val shortName: String     // 地址栏显示的 1-2 字符标签
) {
    /** 将搜索词转为完整 URL */
    fun buildSearchUrl(query: String): String {
        return urlTemplate.replace("%s", java.net.URLEncoder.encode(query, "UTF-8"))
    }
}
