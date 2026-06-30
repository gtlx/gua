package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gua.browser.settings.DataManager
import com.gua.browser.ui.BrowserState
import kotlinx.coroutines.launch

/**
 * 设置主界面 — Via 风格
 *
 * 所有图标已统一为 Material Icons。
 */
@Composable
fun SettingsScreen(
    state: BrowserState,
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

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // ===== 搜索 =====
                    item { SectionHeader("搜索") }
                    item {
                        SettingsCard(
                            icon = Icons.Default.Search,
                            title = "搜索引擎",
                            subtitle = state.activeSearchEngine.name,
                            onClick = { showSearchEngines.value = true }
                        )
                    }

                    // ===== 外观 =====
                    item { SectionHeader("外观") }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.DarkMode,
                            title = "夜间模式",
                            checked = state.isNightMode,
                            onCheckedChange = { state.isNightMode = it }
                        )
                    }

                    // ===== 隐私 =====
                    item { SectionHeader("隐私与安全") }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.Shield,
                            title = "广告过滤",
                            subtitle = "拦截广告和跟踪器",
                            checked = state.isAdblockEnabled,
                            onCheckedChange = { state.isAdblockEnabled = it }
                        )
                    }

                    // ===== 浏览 =====
                    item { SectionHeader("浏览") }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.DesktopWindows,
                            title = "桌面版网站",
                            subtitle = "默认以桌面模式加载页面",
                            checked = state.isDesktopMode,
                            onCheckedChange = { state.isDesktopMode = it }
                        )
                    }

                    // ===== 工具栏 =====
                    item { SectionHeader("工具栏") }
                    item {
                        SettingsCard(
                            icon = Icons.Default.SwapVert,
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
                            icon = Icons.Default.Edit,
                            title = "显示地址栏",
                            checked = state.showUrlBar,
                            onCheckedChange = { state.showUrlBar = it }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.ArrowBack,
                            title = "后退按钮",
                            checked = state.showBackBtn,
                            onCheckedChange = { state.showBackBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.ArrowForward,
                            title = "前进按钮",
                            checked = state.showForwardBtn,
                            onCheckedChange = { state.showForwardBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.Home,
                            title = "主页按钮",
                            checked = state.showHomeBtn,
                            onCheckedChange = { state.showHomeBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.Layers,
                            title = "标签按钮",
                            checked = state.showTabsBtn,
                            onCheckedChange = { state.showTabsBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchCard(
                            icon = Icons.Default.MoreHoriz,
                            title = "菜单按钮",
                            checked = state.showMenuBtn,
                            onCheckedChange = { state.showMenuBtn = it }
                        )
                    }

                    // ===== 数据 =====
                    item { SectionHeader("数据") }
                    item {
                        val exportScope = rememberCoroutineScope()
                        val exportLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.CreateDocument("application/json")
                        ) { uri ->
                            if (uri != null) {
                                exportScope.launch {
                                    val json = DataManager.exportToJson(state)
                                    DataManager.writeUriContent(context, uri, json)
                                }
                            }
                        }
                        SettingsCard(
                            icon = Icons.Default.Upload,
                            title = "导出数据",
                            subtitle = "书签、设置、搜索引擎",
                            onClick = { exportLauncher.launch("gua_backup.json") }
                        )
                    }
                    item {
                        val importScope = rememberCoroutineScope()
                        val importLauncher = rememberLauncherForActivityResult(
                            ActivityResultContracts.OpenDocument()
                        ) { uri ->
                            if (uri != null) {
                                importScope.launch {
                                    val content = DataManager.readUriContent(context, uri)
                                    if (content != null) DataManager.importFromJson(content, state)
                                }
                            }
                        }
                        SettingsCard(
                            icon = Icons.Default.Download,
                            title = "导入数据",
                            subtitle = "恢复书签和设置",
                            onClick = { importLauncher.launch(arrayOf("application/json")) }
                        )
                    }

                    // ===== 关于 =====
                    item { SectionHeader("关于") }
                    item {
                        SettingsCard(
                            icon = Icons.Default.Info,
                            title = "关于 GuaBrowser",
                            subtitle = "v0.1.0 · 基于 GeckoView",
                            onClick = { }
                        )
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

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
    icon: ImageVector,
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
                icon,
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
                Icons.Default.ChevronRight,
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
    icon: ImageVector,
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
                icon,
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
