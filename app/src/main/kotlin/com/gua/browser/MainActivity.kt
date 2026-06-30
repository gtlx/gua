package com.gua.browser

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.LifecycleOwner
import android.view.ViewGroup
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gua.browser.engine.EngineManager
import com.gua.browser.ui.BrowserState
import com.gua.browser.ui.BrowserStateSaver
import com.gua.browser.ui.QuickSettingsPanel
import com.gua.browser.ui.TabSwitcherPanel
import com.gua.browser.ui.FindInPagePanel
import com.gua.browser.ui.bookmark.BookmarkScreen
import com.gua.browser.ui.bookmark.HistoryScreen
import com.gua.browser.ui.settings.ScriptManagerScreen
import com.gua.browser.ui.settings.SettingsScreen
import com.gua.browser.ui.toolbar.SearchEngineSwitch

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

    // 状态变更时自动保存
    val autoSave = remember {
        object : Any() {
            fun save() { stateSaver.save(state) }
        }
    }
    DisposableEffect(Unit) {
        onDispose { stateSaver.save(state) }
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
                Column(modifier = Modifier.fillMaxSize()) {

                    // 1. Web 内容
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        // GeckoView 容器
                        val lifecycleOwner = LocalLifecycleOwner.current
                        AndroidView(
                            factory = { ctx ->
                                FrameLayout(ctx).apply {
                                    layoutParams = ViewGroup.LayoutParams(
                                        ViewGroup.LayoutParams.MATCH_PARENT,
                                        ViewGroup.LayoutParams.MATCH_PARENT
                                    )
                                    val mgr = EngineManager(
                                        container = this,
                                        lifecycleOwner = lifecycleOwner
                                    )
                                    engineManager = mgr

                                    // 创建初始标签
                                    val tab = mgr.createTab("about:start")
                                    state.bindEngine(tab.engine)
                                    state.updateTabList(mgr)
                                }
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // 加载进度条
                        AnimatedVisibility(
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

                        // 页面标题
                        if (!state.isUrlFocused && state.pageTitle.isNotEmpty() && state.progress >= 100) {
                            Text(
                                text = state.pageTitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .padding(top = 6.dp, start = 48.dp, end = 48.dp)
                            )
                        }
                    }

                    // 2. 地址栏
                    UrlBar(
                        urlText = state.url,
                        isFocused = state.isUrlFocused,
                        isSecure = state.isSecure,
                        searchEngineLabel = state.searchEngineLabel,
                        onUrlChange = { state.url = it },
                        onFocusChange = { state.isUrlFocused = it },
                        onSearchEngineSwitch = { state.switchSearchEngine() },
                        onGo = { input ->
                            state.isUrlFocused = false
                            val tab = engineManager?.activeTab ?: return@UrlBar
                            val url = normalizeUrl(input, state.activeSearchEngine)
                            tab.engine.loadUrl(url)
                            state.url = url
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // 3. 底部工具栏
                    BottomToolbar(
                        canGoBack = state.canGoBack,
                        canGoForward = state.canGoForward,
                        tabCount = state.tabs.size,
                        onBack = { engineManager?.activeTab?.engine?.goBack() },
                        onForward = { engineManager?.activeTab?.engine?.goForward() },
                        onHome = {
                            engineManager?.activeTab?.engine?.loadUrl("about:start")
                        },
                        onTabs = { state.showTabSwitcher = true },
                        onQuickSettings = { state.showQuickSettings = !state.showQuickSettings },
                        modifier = Modifier.fillMaxWidth()
                    )
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
@Composable
@Composable fun UrlBar(
    urlText: String,
    isFocused: Boolean,
    isSecure: Boolean,
    searchEngineLabel: String,
    onUrlChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearchEngineSwitch: () -> Unit,
    onGo: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shadowElevation = 2.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 搜索引擎切换按钮（Via 风格）
            SearchEngineSwitch(
                label = searchEngineLabel,
                onClick = onSearchEngineSwitch,
                modifier = Modifier.padding(end = 2.dp)
            )

            // 安全锁图标
            val lockColor = if (isSecure) Color(0xFF0D904F) else Color(0xFF9E9E9E)
            Icon(
                painter = if (isSecure)
                    painterResource(android.R.drawable.ic_lock_lock)
                else
                    painterResource(android.R.drawable.ic_lock_idle_lock),
                contentDescription = if (isSecure) "安全连接" else "不安全连接",
                tint = lockColor,
                modifier = Modifier
                    .size(16.dp)
                    .clickable { }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 地址输入框
            OutlinedTextField(
                value = if (isFocused) urlText
                        else urlText.ifEmpty { "" },
                onValueChange = onUrlChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        "搜索或输入网址",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface
                ),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary
                ),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go
                ),
                keyboardActions = KeyboardActions(onGo = { onGo(urlText) }),
                onFocusChange = { focused: Boolean -> onFocusChange(focused) }
            )

            Spacer(modifier = Modifier.width(4.dp))

            // 菜单按钮
            IconButton(onClick = { /* 更多菜单 */ }) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_more),
                    contentDescription = "更多",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Via 风格底部工具栏
 */
@Composable fun BottomToolbar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    tabCount: Int,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onHome: () -> Unit,
    onTabs: () -> Unit,
    onQuickSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .height(52.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ToolbarBtn(
                icon = android.R.drawable.ic_media_previous,
                label = "后退",
                enabled = canGoBack,
                onClick = onBack
            )
            ToolbarBtn(
                icon = android.R.drawable.ic_media_next,
                label = "前进",
                enabled = canGoForward,
                onClick = onForward
            )
            ToolbarBtn(
                icon = android.R.drawable.ic_menu_compass,
                label = "主页",
                onClick = onHome
            )
            ToolbarBtn(
                icon = android.R.drawable.ic_menu_sort_by_size,
                label = "$tabCount",
                onClick = onTabs
            )
            ToolbarBtn(
                icon = android.R.drawable.ic_menu_manage,
                label = "工具",
                onClick = onQuickSettings
            )
        }
    }
}

@Composable fun ToolbarBtn(
    icon: Int,
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val alpha = if (enabled) 1f else 0.35f
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha),
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 10.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.7f)
        )
    }
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

    // 完整 URL
    if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
        return trimmed
    }
    if (trimmed.startsWith("file://") || trimmed.startsWith("about:")) {
        return trimmed
    }

    // 包含 . 且不含空格 → 视为域名
    if (trimmed.contains(".") && !trimmed.contains(" ")) {
        return "https://$trimmed"
    }

    // 搜索引擎
    return searchEngine.buildSearchUrl(trimmed)
}
