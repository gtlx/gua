package com.gua.browser

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gua.browser.engine.EngineManager
import com.gua.browser.ui.BrowserState
import com.gua.browser.ui.BrowserStateSaver
import com.gua.browser.ui.ViaToolbar
import com.gua.browser.ui.QuickSettingsPanel
import com.gua.browser.ui.TabSwitcherPanel
import com.gua.browser.ui.FindInPagePanel
import com.gua.browser.ui.bookmark.BookmarkScreen
import com.gua.browser.ui.bookmark.HistoryScreen
import com.gua.browser.ui.settings.ScriptManagerScreen
import com.gua.browser.ui.settings.SettingsScreen
/**
 * GuaBrowser 主界面
 *
 * Via 风格布局 + 完整引擎回调绑定
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            BrowserContent()
        }
    }
}

@Composable
fun GuaBrowserTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color(0xFF1A1A1A),
            surface = Color(0xFF2D2D2D),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF1E1E1E),
            onBackground = Color(0xFFE0E0E0)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            surface = Color.White,
            onSurface = Color(0xFF1C1B1F),
            background = Color.White,
            onBackground = Color(0xFF1C1B1F)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable fun BrowserContent() {
    val context = LocalContext.current
    val app = context.applicationContext as GuaApp

    // ===== 浏览器状态 =====
    val state = remember { BrowserState() }
    var engineManager by remember { mutableStateOf<EngineManager?>(null) }
    val stateSaver = remember { BrowserStateSaver(context) }

    // 加载持久化的状态
    LaunchedEffect(Unit) {
        stateSaver.load(state)
    }

    // 每次状态变化时自动保存
    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(
                state.isNightMode, state.isAdblockEnabled, state.isDesktopMode,
                state.toolbarPosition, state.showUrlBar,
                state.showBackBtn, state.showForwardBtn, state.showHomeBtn,
                state.showTabsBtn, state.showMenuBtn,
                state.activeSearchEngineIndex, state.searchEngines.toList()
            )
        }.collect { stateSaver.save(state) }
    }

    // 主题跟随夜间模式
    val isDark = state.isNightMode
    GuaBrowserTheme(darkTheme = isDark) {

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // ===== 正常浏览模式 =====
            if (!state.showTabSwitcher && !state.showScriptManager) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.systemBars)
                ) {

                    // 工具栏位置：顶部
                    if (state.toolbarPosition == BrowserState.ToolbarPos.TOP) {
                        ViaToolbar(
                            urlText = state.url,
                            isFocused = state.isUrlFocused,
                            isSecure = state.isSecure,
                            searchEngineLabel = state.searchEngineLabel,
                            canGoBack = state.canGoBack,
                            canGoForward = state.canGoForward,
                            tabCount = state.tabs.size,
                            showBack = state.showBackBtn,
                            showForward = state.showForwardBtn,
                            showHome = state.showHomeBtn,
                            showTabs = state.showTabsBtn,
                            showMenu = state.showMenuBtn,
                            onUrlChange = { state.url = it },
                            onFocusChange = { state.isUrlFocused = it },
                            onSearchEngineSwitch = { state.switchSearchEngine() },
                            onGo = { input ->
                                state.isUrlFocused = false
                                engineManager?.activeTab?.engine
                                    ?.loadUrl(normalizeUrl(input, state.activeSearchEngine))
                                state.url = input
                            },
                            onBack = { engineManager?.activeTab?.engine?.goBack() },
                            onForward = { engineManager?.activeTab?.engine?.goForward() },
                            onHome = { engineManager?.activeTab?.engine?.loadUrl("about:start") },
                            onTabs = { state.showTabSwitcher = true },
                            onMenu = { state.showQuickSettings = !state.showQuickSettings },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Web 内容（带淡入动画）
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        val lifecycleOwner = LocalLifecycleOwner.current
                        AndroidView(
                            factory = { ctx ->
                                FrameLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    val mgr = EngineManager(this, lifecycleOwner)
                                    engineManager = mgr
                                    val tab = mgr.createTab("about:start")
                                    if (tab != null) {
                                        state.bindEngine(tab.engine)
                                        state.updateTabList(mgr)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        androidx.compose.animation.AnimatedVisibility(
                            visible = state.progress in 1..99,
                            enter = fadeIn(), exit = fadeOut()
                        ) {
                            LinearProgressIndicator(
                                progress = { state.progress / 100f },
                                modifier = Modifier.fillMaxWidth().background(Color.Transparent),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = Color.Transparent,
                            )
                        }

                        if (!state.isUrlFocused && state.pageTitle.isNotEmpty() && state.progress >= 100) {
                            Text(
                                text = state.pageTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1, overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.align(Alignment.TopCenter)
                                    .padding(top = 6.dp, start = 48.dp, end = 48.dp)
                            )
                        }
                    }

                    // 工具栏位置：底部
                    if (state.toolbarPosition == BrowserState.ToolbarPos.BOTTOM) {
                        ViaToolbar(
                            urlText = state.url,
                            isFocused = state.isUrlFocused,
                            isSecure = state.isSecure,
                            searchEngineLabel = state.searchEngineLabel,
                            canGoBack = state.canGoBack,
                            canGoForward = state.canGoForward,
                            tabCount = state.tabs.size,
                            showBack = state.showBackBtn,
                            showForward = state.showForwardBtn,
                            showHome = state.showHomeBtn,
                            showTabs = state.showTabsBtn,
                            showMenu = state.showMenuBtn,
                            onUrlChange = { state.url = it },
                            onFocusChange = { state.isUrlFocused = it },
                            onSearchEngineSwitch = { state.switchSearchEngine() },
                            onGo = { input ->
                                state.isUrlFocused = false
                                engineManager?.activeTab?.engine
                                    ?.loadUrl(normalizeUrl(input, state.activeSearchEngine))
                                state.url = input
                            },
                            onBack = { engineManager?.activeTab?.engine?.goBack() },
                            onForward = { engineManager?.activeTab?.engine?.goForward() },
                            onHome = { engineManager?.activeTab?.engine?.loadUrl("about:start") },
                            onTabs = { state.showTabSwitcher = true },
                            onMenu = { state.showQuickSettings = !state.showQuickSettings },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 快速设置浮层
                QuickSettingsPanel(
                    visible = state.showQuickSettings,
                    isNightMode = state.isNightMode,
                    isAdblockEnabled = state.isAdblockEnabled,
                    isDesktopMode = state.isDesktopMode,
                    onNightModeChange = { state.isNightMode = it; stateSaver.save(state) },
                    onAdblockChange = { state.isAdblockEnabled = it; stateSaver.save(state) },
                    onDesktopModeChange = { state.isDesktopMode = it; stateSaver.save(state) },
                    onScriptManager = {
                        state.showQuickSettings = false
                        state.showScriptManager = true
                    },
                    onBookmarks = {
                        state.showQuickSettings = false
                        state.showBookmarks = true
                    },
                    onHistory = {
                        state.showQuickSettings = false
                        state.showHistory = true
                    },
                    onFindInPage = {
                        state.showQuickSettings = false
                        state.showFindInPage = true
                    },
                    onShare = {
                        state.showQuickSettings = false
                        val url = state.url
                        if (url.isNotBlank()) {
                            val shareIntent = android.content.Intent().apply {
                                action = android.content.Intent.ACTION_SEND
                                putExtra(android.content.Intent.EXTRA_TEXT, url)
                                type = "text/plain"
                            }
                            context.startActivity(
                                android.content.Intent.createChooser(shareIntent, "分享")
                            )
                        }
                    },
                    onSettings = {
                        state.showQuickSettings = false
                        state.showSettings = true
                    },
                    onDismiss = { state.showQuickSettings = false }
                )
            }

            // ===== 标签切换界面 =====
            if (state.showTabSwitcher) {
                TabSwitcherPanel(
                    tabs = state.tabs,
                    activeIndex = state.activeTabIndex,
                    onSwitchTab = { index ->
                        engineManager?.switchToTab(index)
                        engineManager?.activeTab?.let { state.bindEngine(it.engine) }
                        state.updateTabList(engineManager!!)
                        state.showTabSwitcher = false
                    },
                    onCloseTab = { index ->
                        engineManager?.closeTab(index)
                        state.updateTabList(engineManager!!)
                        engineManager?.activeTab?.let { state.bindEngine(it.engine) }
                    },
                    onNewTab = {
                        engineManager?.createBlankTab()
                        state.updateTabList(engineManager!!)
                        engineManager?.activeTab?.let { state.bindEngine(it.engine) }
                    },
                    onDismiss = { state.showTabSwitcher = false }
                )
            }

            // ===== 脚本管理界面 =====
            if (state.showScriptManager) {
                ScriptManagerScreen(
                    scriptManager = app.scriptManager,
                    onDismiss = { state.showScriptManager = false }
                )
            }

            // ===== 书签界面 =====
            if (state.showBookmarks) {
                BookmarkScreen(
                    state = state,
                    onOpenUrl = { url ->
                        engineManager?.activeTab?.engine?.loadUrl(url)
                        state.showBookmarks = false
                    },
                    onDismiss = { state.showBookmarks = false }
                )
            }

            // ===== 历史记录界面 =====
            if (state.showHistory) {
                HistoryScreen(
                    onOpenUrl = { url ->
                        engineManager?.activeTab?.engine?.loadUrl(url)
                        state.showHistory = false
                    },
                    onDismiss = { state.showHistory = false }
                )
            }

            // ===== 页面查找 =====
            if (state.showFindInPage) {
                FindInPagePanel(
                    visible = state.showFindInPage,
                    query = state.findQuery,
                    matchCount = state.findMatchCount,
                    currentIndex = state.findCurrentIndex,
                    onQueryChange = { state.findQuery = it },
                    onNext = { /* GeckoView 查找下一个 */ },
                    onPrevious = { /* GeckoView 查找上一个 */ },
                    onClose = { state.showFindInPage = false }
                )
            }

            // ===== 设置界面 =====
            if (state.showSettings) {
                SettingsScreen(
                    state = state,
                    downloader = app.runtimeDownloader,
                    onDismiss = { state.showSettings = false }
                )
            }
        }
    }
}

// ============================================================
//  UI 组件
// ============================================================

/**
 * Via 风格地址栏 — 带搜索引擎切换
 */

// ============================================================
//  工具函数
// ============================================================

/**
 * URL 规范化：搜索词 vs 网址
 */
fun normalizeUrl(input: String, searchEngine: com.gua.browser.ui.SearchEngine): String {
    val trimmed = input.trim()
    if (trimmed.isBlank()) return "about:blank"
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) return trimmed
    if (trimmed.startsWith("file://") || trimmed.startsWith("about:")) return trimmed
    if (trimmed.contains(".") && !trimmed.contains(" ")) return "https://$trimmed"
    return searchEngine.buildSearchUrl(trimmed)
}
