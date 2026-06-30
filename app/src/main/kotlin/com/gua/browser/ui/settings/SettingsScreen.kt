package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gua.browser.download.GeckoRuntimeDownloader
import com.gua.browser.settings.DataManager
import com.gua.browser.ui.BrowserState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 设置主界面
 *
 * 集成各类设置项：
 * - 搜索引擎管理
 * - 外观（夜间模式）
 * - 隐私（广告过滤）
 * - 浏览（桌面模式、JavaScript）
 * - 关于
 */
@Composable
fun SettingsScreen(
    state: BrowserState,
    downloader: GeckoRuntimeDownloader,
    onDismiss: () -> Unit
) {
    val showSearchEngines = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (showSearchEngines.value) {
            SearchEngineSettings(
                searchEngines = state.searchEngines,
                activeIndex = state.activeSearchEngineIndex,
                onSetActive = { state.setActiveSearchEngine(it) },
                onAddEngine = { engine ->
                    state.addSearchEngine(engine.name, engine.urlTemplate, engine.shortName)
                },
                onRemoveEngine = { state.removeSearchEngine(it) },
                onMoveUp = { state.moveSearchEngineUp(it) },
                onMoveDown = { state.moveSearchEngineDown(it) },
                onDismiss = { showSearchEngines.value = false }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {

                // 顶部栏
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shadowElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "设置",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(onClick = onDismiss) {
                            Text("完成")
                        }
                    }
                }

                // 设置项列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ===== 搜索 =====
                    item {
                        SectionHeader("搜索")
                    }

                    item {
                        SettingsCard(
                            icon = android.R.drawable.ic_menu_search,
                            title = "搜索引擎",
                            subtitle = state.activeSearchEngine.name,
                            onClick = { showSearchEngines.value = true }
                        )
                    }

                    // ===== 外观 =====
                    item {
                        SectionHeader("外观")
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_gallery,
                            title = "夜间模式",
                            checked = state.isNightMode,
                            onCheckedChange = { state.isNightMode = it }
                        )
                    }

                    // ===== 隐私 =====
                    item {
                        SectionHeader("隐私与安全")
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_delete,
                            title = "广告过滤",
                            subtitle = "拦截广告和跟踪器",
                            checked = state.isAdblockEnabled,
                            onCheckedChange = { state.isAdblockEnabled = it }
                        )
                    }

                    // ===== 浏览 =====
                    item {
                        SectionHeader("浏览")
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_view,
                            title = "桌面版网站",
                            subtitle = "默认以桌面模式加载页面",
                            checked = state.isDesktopMode,
                            onCheckedChange = { state.isDesktopMode = it }
                        )
                    }

                    // ===== 工具栏 =====
                    item {
                        SectionHeader("工具栏")
                    }

                    item {
                        // 工具栏位置
                        SettingsCard(
                            icon = android.R.drawable.ic_menu_compass,
                            title = "工具栏位置",
                            subtitle = if (state.toolbarPosition == BrowserState.ToolbarPos.TOP) "顶部" else "底部",
                            onClick = {
                                state.toolbarPosition = if (state.toolbarPosition == BrowserState.ToolbarPos.TOP)
                                    BrowserState.ToolbarPos.BOTTOM else BrowserState.ToolbarPos.TOP
                            }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_view,
                            title = "显示地址栏",
                            checked = state.showUrlBar,
                            onCheckedChange = { state.showUrlBar = it }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_media_previous,
                            title = "后退按钮",
                            checked = state.showBackBtn,
                            onCheckedChange = { state.showBackBtn = it }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_media_next,
                            title = "前进按钮",
                            checked = state.showForwardBtn,
                            onCheckedChange = { state.showForwardBtn = it }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_compass,
                            title = "主页按钮",
                            checked = state.showHomeBtn,
                            onCheckedChange = { state.showHomeBtn = it }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_sort_by_size,
                            title = "标签按钮",
                            checked = state.showTabsBtn,
                            onCheckedChange = { state.showTabsBtn = it }
                        )
                    }

                    item {
                        SettingsSwitchCard(
                            icon = android.R.drawable.ic_menu_manage,
                            title = "菜单按钮",
                            checked = state.showMenuBtn,
                            onCheckedChange = { state.showMenuBtn = it }
                        )
                    }

                    // ===== 运行时 =====
                    item {
                        SectionHeader("引擎")
                    }

                    item {
                        RuntimeDownloadCard(downloader = downloader)
                    }

                    // ===== 数据 =====
                    item {
                        SectionHeader("数据")
                    }

                    item {
                        val exportLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/json")
                        ) { uri ->
                            if (uri != null) {
                                GlobalScope.launch {
                                    val json = DataManager.exportToJson(state)
                                    DataManager.writeUriContent(context, uri, json)
                                }
                            }
                        }
                        SettingsCard(
                            icon = android.R.drawable.ic_menu_share,
                            title = "导出数据",
                            subtitle = "书签、设置、搜索引擎",
                            onClick = { exportLauncher.launch("gua_backup.json") }
                        )
                    }

                    item {
                        val importLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                GlobalScope.launch {
                                    val content = DataManager.readUriContent(context, uri)
                                    if (content != null) DataManager.importFromJson(content, state)
                                }
                            }
                        }
                        SettingsCard(
                            icon = android.R.drawable.ic_menu_upload,
                            title = "导入数据",
                            subtitle = "恢复书签和设置",
                            onClick = { importLauncher.launch(arrayOf("application/json")) }
                        )
                    }

                    // ===== 关于 =====
                    item {
                        SectionHeader("关于")
                    }

                    item {
                        SettingsCard(
                            icon = android.R.drawable.ic_menu_info_details,
                            title = "关于 GuaBrowser",
                            subtitle = "v0.1.0 · 基于 GeckoView",
                            onClick = { }
                        )
                    }

                    // 底部留白
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

/**
 * 运行时下载卡片
 */
@Composable
fun RuntimeDownloadCard(downloader: GeckoRuntimeDownloader) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var isDownloaded by remember { mutableStateOf(downloader.isRuntimeDownloaded()) }
    var statusText by remember { mutableStateOf("") }

    // 检查状态
    LaunchedEffect(Unit) {
        isDownloaded = downloader.isRuntimeDownloaded()
        if (isDownloaded) {
            val mb = downloader.getRuntimeSize() / (1024 * 1024)
            statusText = "已下载 (${mb}MB)"
        } else {
            statusText = "未下载"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(android.R.drawable.ic_menu_compass),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "GeckoView 内核",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "下载中 $progress%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = {
                    if (!isDownloaded && !isDownloading) {
                        isDownloading = true
                        progress = 0
                        downloader.download(scope, { p ->
                            progress = p
                        }, { success, error ->
                            isDownloading = false
                            isDownloaded = success
                            if (success) {
                                val mb = downloader.getRuntimeSize() / (1024 * 1024)
                                statusText = "已下载 (${mb}MB)"
                            } else {
                                statusText = error ?: "下载失败"
                            }
                        })
                    }
                },
                enabled = !isDownloading && !isDownloaded,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (isDownloaded) "✓ 已下载" else if (isDownloading) "下载中..." else "下载内核")
            }
        }
    }
}

/**
 * 设置分区标题
 */
@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp, top = 12.dp, bottom = 4.dp)
    )
}

/**
 * 设置项卡片（点击进入下一级）
 */
@Composable
fun SettingsCard(
    icon: Int,
    title: String,
    subtitle: String = "",
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Icon(
                painter = painterResource(android.R.drawable.ic_menu_more),
                contentDescription = "进入",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * 设置项卡片（开关）
 */
@Composable
fun SettingsSwitchCard(
    icon: Int,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}
