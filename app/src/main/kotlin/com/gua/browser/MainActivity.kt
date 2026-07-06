package com.gua.browser

import android.os.Bundle
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.view.ViewGroup
import com.gua.browser.bookmark.Bookmark
import com.gua.browser.engine.EngineManager
import com.gua.browser.ui.BrowserState
import com.gua.browser.ui.BrowserStateSaver
import com.gua.browser.ui.ViaToolbar
import com.gua.browser.ui.QuickSettingsPanel
import com.gua.browser.ui.ShortcutHelper
import com.gua.browser.ui.TabSwitcherPanel
import com.gua.browser.ui.FindInPagePanel
import com.gua.browser.ui.bookmark.BookmarkScreen
import com.gua.browser.ui.bookmark.HistoryScreen
import com.gua.browser.ui.download.DownloadScreen
import com.gua.browser.ui.home.StartPage
import com.gua.browser.ui.settings.ExtensionScreen
import com.gua.browser.ui.settings.PermissionScreen
import com.gua.browser.ui.settings.ScriptManagerScreen
import com.gua.browser.ui.settings.SettingsScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession

/**
 * GuaBrowser 主界面
 *
 * Via 风格布局 + 完整引擎回调绑定
 * - 底部/顶部工具栏
 * - 桌面模式实时生效
 * - 收藏 / 刷新 / 主页按钮
 * - 起始页
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
    // Via 风格极简配色：纯白背景，深灰文字，蓝色强调
    val colorScheme = if (darkTheme) {
        lightColorScheme(
            primary = Color(0xFF90CAF9),
            onPrimary = Color(0xFF1A1A1A),
            surface = Color(0xFF2D2D2D),
            onSurface = Color(0xFFE0E0E0),
            background = Color(0xFF1E1E1E),
            onBackground = Color(0xFFE0E0E0),
            surfaceVariant = Color(0xFF3D3D3D)
        )
    } else {
        lightColorScheme(
            primary = Color(0xFF1565C0),
            onPrimary = Color.White,
            surface = Color.White,
            onSurface = Color(0xFF333333),
            background = Color(0xFFF5F5F5),
            onBackground = Color(0xFF333333),
            surfaceVariant = Color(0xFFF5F5F5)
        )
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun BrowserContent() {
    val context = LocalContext.current
    val app = context.applicationContext as GuaApp
    val activity = context as? MainActivity
    // ===== 浏览器状态 =====
    val state = remember { BrowserState() }
    var engineManager by remember { mutableStateOf<EngineManager?>(null) }
    val stateSaver = remember { BrowserStateSaver(context) }

    // 加载持久化的状态
    // 初始化 WebExtension 控制器（GeckoRuntime 单例）
    LaunchedEffect(Unit) {
        val runtime = GeckoRuntime.getDefault(context)
        app.scriptManager.setRuntime(runtime)
        app.extensionManager.setRuntime(runtime)
    }

    LaunchedEffect(Unit) {
        stateSaver.load(state)
    }

    // 状态变化自动保存（防抖）— 拆分为两组，减少不必要的触发
    LaunchedEffect(Unit) {
        snapshotFlow { listOf(state.isNightMode, state.isAdblockEnabled, state.isDesktopMode, state.isIncognito) }
            .collect { stateSaver.save(state) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow {
            listOf(
                state.toolbarPosition, state.showUrlBar,
                state.showBackBtn, state.showForwardBtn, state.showHomeBtn,
                state.showTabsBtn, state.showMenuBtn
            )
        }.collect { stateSaver.save(state) }
    }
    LaunchedEffect(Unit) {
        snapshotFlow { state.activeSearchEngineIndex to state.searchEngines.toList() }
            .collect { stateSaver.save(state) }
    }

    // 桌面/隐私模式变化 → 重建会话（重载页面）
    LaunchedEffect(state.isDesktopMode, state.isIncognito) {
        state.applyDesktopMode()
    }
    // 夜间模式变化 → 仅切换 WebExtension CSS（不重载页面）
    LaunchedEffect(state.isNightMode) {
        app.scriptManager.injector.installNightMode(state.isNightMode)
    }

    // 返回键处理
    // 1. 关闭面板 → 2. 页面回退 → 3. 退出
    BackHandler {
        when {
            state.showSearchEnginePicker -> state.showSearchEnginePicker = false
            state.showExtensions -> state.showExtensions = false
            state.showSettings -> state.showSettings = false
            state.showScriptManager -> state.showScriptManager = false
            state.showBookmarks -> state.showBookmarks = false
            state.showHistory -> state.showHistory = false
            state.showDownloads -> state.showDownloads = false
            state.showTabSwitcher -> state.showTabSwitcher = false
            state.showQuickSettings -> state.showQuickSettings = false
            state.showFindInPage -> state.showFindInPage = false
            state.showPermissionSettings -> state.showPermissionSettings = false
            state.isUrlFocused -> state.isUrlFocused = false
            engineManager?.activeTab?.engine?.canGoBack() == true ->
                engineManager?.activeTab?.engine?.goBack()
            else -> activity?.finish()
        }
    }

    // 权限请求对话框
    state.pendingPermission?.let { perm ->
        AlertDialog(
            onDismissRequest = { state.respondToPermission(false) },
            title = { Text("权限请求") },
            text = {
                Column {
                    Text("${perm.uri}\n\n请求 ${perm.typeName} 权限")
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            state.pendingPermissionRemember = !state.pendingPermissionRemember
                        }
                    ) {
                        Checkbox(
                            checked = state.pendingPermissionRemember,
                            onCheckedChange = { state.pendingPermissionRemember = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color(0xFF1565C0))
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("记住选择", fontSize = 14.sp, color = Color(0xFF666666))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    state.respondToPermission(true, state.pendingPermissionRemember)
                }) {
                    Text("允许", color = Color(0xFF1565C0))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    state.respondToPermission(false, state.pendingPermissionRemember)
                }) {
                    Text("拒绝", color = Color(0xFFE53935))
                }
            }
        )
    }

    // 主题
    val isDark = state.isNightMode
    GuaBrowserTheme(darkTheme = isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .windowInsetsPadding(WindowInsets.systemBars)
                .windowInsetsPadding(WindowInsets.ime)
        ) {
            // ===== 正常浏览模式（含快速设置面板，共享系统栏内边距）=====
            if (!state.showTabSwitcher && !state.showScriptManager) {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // 顶部工具栏
                        if (state.toolbarPosition == BrowserState.ToolbarPos.TOP) {
                            BuildToolbar(state, engineManager)
                        }

                        // Web 内容区
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                        ) {
                            AndroidView(
                                factory = { ctx ->
                                    FrameLayout(ctx).apply {
                                        layoutParams = ViewGroup.LayoutParams(
                                            ViewGroup.LayoutParams.MATCH_PARENT,
                                            ViewGroup.LayoutParams.MATCH_PARENT
                                        )
                                        val mgr = EngineManager(this)
                                        engineManager = mgr
                                        val tab = mgr.createTab("about:blank")
                                        if (tab != null) {
                                            state.bindEngine(tab.engine)
                                            state.updateTabList(mgr)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            if (state.isHomePage && !state.isUrlFocused && !state.isLoading) {
                                StartPage(
                                    state = state,
                                    onOpenUrl = { url ->
                                        state.showHomePage = false
                                        engineManager?.activeTab?.engine?.loadUrl(url)
                                    },
                                    onFocusSearch = { state.isUrlFocused = true }
                                )
                            }

                            androidx.compose.animation.AnimatedVisibility(
                                visible = state.progress in 1..99,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                LinearProgressIndicator(
                                    progress = { state.progress / 100f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color.Transparent),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Transparent,
                                )
                            }
                        }

                        // 底部工具栏
                        if (state.toolbarPosition == BrowserState.ToolbarPos.BOTTOM) {
                            BuildToolbar(state, engineManager)
                        }
                    }

                    // 快速设置浮层 — 与工具栏共享内边距，紧贴工具栏
                    QuickSettingsPanel(
                        visible = state.showQuickSettings,
                        isNightMode = state.isNightMode,
                        isAdblockEnabled = state.isAdblockEnabled,
                        isDesktopMode = state.isDesktopMode,
                        isIncognito = state.isIncognito,
                        toolbarAtTop = state.toolbarPosition == BrowserState.ToolbarPos.TOP,
                        onNightModeChange = { state.isNightMode = it; stateSaver.save(state) },
                        onAdblockChange = { state.isAdblockEnabled = it; stateSaver.save(state) },
                        onDesktopModeChange = {
                            state.isDesktopMode = it
                            stateSaver.save(state)
                        },
                        onIncognitoChange = {
                            state.isIncognito = it
                            stateSaver.save(state)
                        },
                        onScriptManager = {
                            state.showQuickSettings = false
                            state.showScriptManager = true
                        },
                        onExtensions = {
                            state.showQuickSettings = false
                            state.showExtensions = true
                        },
                        onBookmarks = {
                            state.showQuickSettings = false
                            state.showBookmarks = true
                        },
                        onHistory = {
                            state.showQuickSettings = false
                            state.showHistory = true
                        },
                        onDownloads = {
                            state.showQuickSettings = false
                            state.showDownloads = true
                        },
                        onFindInPage = {
                            state.showQuickSettings = false
                            state.showFindInPage = true
                        },
                        onAddToHomeScreen = {
                            state.showQuickSettings = false
                            ShortcutHelper.createShortcut(
                                context,
                                state.pageTitle.ifEmpty { "GuaBrowser" },
                                state.url.ifEmpty { "about:blank" }
                            )
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
            }

            // ===== 标签切换界面 =====
            if (state.showTabSwitcher) {
                TabSwitcherPanel(
                    tabs = state.tabs,
                    activeIndex = state.activeTabIndex,
                    onSwitchTab = { index ->
                        engineManager?.switchToTab(index)
                        engineManager?.activeTab?.let {
                            state.bindEngine(it.engine)
                            state.showHomePage = false
                        }
                        state.updateTabList(engineManager!!)
                        state.showTabSwitcher = false
                    },
                    onCloseTab = { index ->
                        engineManager?.closeTab(index)
                        state.updateTabList(engineManager!!)
                        engineManager?.activeTab?.let { state.bindEngine(it.engine) }
                    },
                    onNewTab = {
                        state.showHomePage = true
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
                        state.showHomePage = false
                        engineManager?.activeTab?.engine?.loadUrl(url)
                        state.showBookmarks = false
                    },
                    onDismiss = { state.showBookmarks = false }
                )
            }

            // ===== 下载界面 =====
            if (state.showDownloads) {
                DownloadScreen(
                    onDismiss = { state.showDownloads = false }
                )
            }

            // ===== 历史记录界面 =====
            if (state.showHistory) {
                HistoryScreen(
                    onOpenUrl = { url ->
                        state.showHomePage = false
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
                    onQueryChange = { query ->
                        state.findQuery = query
                        engineManager?.activeTab?.engine?.findInPage(query)
                    },
                    onNext = {
                        engineManager?.activeTab?.engine?.findInPage(state.findQuery, forward = true)
                    },
                    onPrevious = {
                        engineManager?.activeTab?.engine?.findInPage(state.findQuery, forward = false)
                    },
                    onClose = {
                        state.showFindInPage = false
                        engineManager?.activeTab?.engine?.clearFindInPage()
                    }
                )
            }

            // ===== 搜索引擎选择器 =====
            if (state.showSearchEnginePicker) {
                AlertDialog(
                    onDismissRequest = { state.showSearchEnginePicker = false },
                    title = { Text("选择搜索引擎", fontWeight = FontWeight.Medium) },
                    text = {
                        Column {
                            state.searchEngines.forEachIndexed { index, engine ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            state.activeSearchEngineIndex = index
                                            stateSaver.save(state)
                                            state.showSearchEnginePicker = false
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = engine.shortName.take(2),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = if (index == state.activeSearchEngineIndex)
                                            MaterialTheme.colorScheme.primary else Color(0xFF888888),
                                        modifier = Modifier.width(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = engine.name,
                                        fontSize = 15.sp,
                                        color = if (index == state.activeSearchEngineIndex)
                                            MaterialTheme.colorScheme.primary else Color(0xFF333333)
                                    )
                                    if (index == state.activeSearchEngineIndex) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("✓", fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { state.showSearchEnginePicker = false }) {
                            Text("取消")
                        }
                    }
                )
            }

            // ===== 扩展管理界面 =====
            if (state.showExtensions) {
                ExtensionScreen(
                    onDismiss = { state.showExtensions = false }
                )
            }

            // ===== 权限管理界面 =====
            if (state.showPermissionSettings) {
                PermissionScreen(
                    onDismiss = { state.showPermissionSettings = false }
                )
            }

            // ===== 设置界面 =====
            if (state.showSettings) {
                SettingsScreen(
                    state = state,
                    onDismiss = { state.showSettings = false }
                )
            }
        }
    }
}

// ============================================================
//  工具栏构建（消除 TOP/BOTTOM 重复）
// ============================================================

@Composable
private fun BuildToolbar(
    state: BrowserState,
    engineManager: EngineManager?
) {
    ViaToolbar(
        urlText = state.url,
        isFocused = state.isUrlFocused,
        isSecure = state.isSecure,
        searchEngineLabel = state.searchEngineLabel,
        canGoBack = state.canGoBack,
        canGoForward = state.canGoForward,
        isLoading = state.isLoading,
        isBookmarked = state.isBookmarked,
        tabCount = state.tabs.size,
        showBack = state.showBackBtn,
        showForward = state.showForwardBtn,
        showHome = state.showHomeBtn,
        showTabs = state.showTabsBtn,
        showMenu = state.showMenuBtn,
        onUrlChange = { state.url = it },
        onFocusChange = { state.isUrlFocused = it },
        onSearchEngineSwitch = { state.showSearchEnginePicker = true },
        onGo = { input ->
            state.isUrlFocused = false
            val resolvedUrl = normalizeUrl(input, state.activeSearchEngine)
            state.showHomePage = false
            engineManager?.activeTab?.engine?.loadUrl(resolvedUrl)
            state.url = resolvedUrl
        },
        onBack = { engineManager?.activeTab?.engine?.goBack() },
        onForward = { engineManager?.activeTab?.engine?.goForward() },
        onRefresh = { engineManager?.activeTab?.engine?.reload() },
        onStop = { engineManager?.activeTab?.engine?.stopLoading() },
        onHome = {
            state.showHomePage = true
            engineManager?.activeTab?.engine?.loadUrl("about:blank")
        },
        onBookmark = {
            val app = com.gua.browser.GuaApp.instance
            val url = state.url
            val title = state.pageTitle
            if (url.isNotBlank() && !url.startsWith("about:")) {
                // 使用 App 全局协程作用域，避免泄漏
                app.appScope.launch {
                    if (app.bookmarkManager.exists(url)) {
                        val all = app.bookmarkManager.getAll()
                        val bm = all.find { it.url == url }
                        if (bm != null) app.bookmarkManager.delete(bm.id)
                        state.isBookmarked = false
                    } else {
                        app.bookmarkManager.add(
                            Bookmark(title = title.ifEmpty { url }, url = url)
                        )
                        state.isBookmarked = true
                    }
                }
            }
        },
        onTabs = { state.showTabSwitcher = true },
        onMenu = { state.showQuickSettings = !state.showQuickSettings },
        modifier = Modifier.fillMaxWidth()
    )
}

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
