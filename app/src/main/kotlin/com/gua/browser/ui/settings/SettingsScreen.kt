package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
 * Via 风格设置界面 — 桌面无阴影，镂空图标
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
            .background(Color(0xFFF5F5F5))
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

                // Via 风格顶部栏：纯白，无阴影，精简
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "设置",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("完成", fontSize = 14.sp, color = Color(0xFF1565C0))
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    // 搜索
                    item { SectionTitle("搜索") }
                    item {
                        SettingsItem(
                            icon = Icons.Outlined.Language,
                            title = "搜索引擎",
                            subtitle = state.activeSearchEngine.name,
                            onClick = { showSearchEngines.value = true }
                        )
                    }

                    // 外观
                    item { SectionTitle("外观") }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.DarkMode,
                            title = "夜间模式",
                            checked = state.isNightMode,
                            onCheckedChange = { state.isNightMode = it }
                        )
                    }

                    // 隐私
                    item { SectionTitle("隐私与安全") }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Shield,
                            title = "广告过滤",
                            subtitle = "拦截广告和跟踪器",
                            checked = state.isAdblockEnabled,
                            onCheckedChange = { state.isAdblockEnabled = it }
                        )
                    }

                    // 浏览
                    item { SectionTitle("浏览") }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.DesktopWindows,
                            title = "桌面版网站",
                            subtitle = "默认以桌面模式加载页面",
                            checked = state.isDesktopMode,
                            onCheckedChange = { state.isDesktopMode = it }
                        )
                    }

                    // 工具栏
                    item { SectionTitle("工具栏") }
                    item {
                        SettingsItem(
                            icon = Icons.Outlined.SwapVert,
                            title = "工具栏位置",
                            subtitle = if (state.toolbarPosition == BrowserState.ToolbarPos.TOP) "顶部" else "底部",
                            onClick = {
                                state.toolbarPosition = if (state.toolbarPosition == BrowserState.ToolbarPos.TOP)
                                    BrowserState.ToolbarPos.BOTTOM else BrowserState.ToolbarPos.TOP
                            }
                        )
                    }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Edit,
                            title = "显示地址栏",
                            checked = state.showUrlBar,
                            onCheckedChange = { state.showUrlBar = it }
                        )
                    }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.ArrowBack,
                            title = "后退按钮",
                            checked = state.showBackBtn,
                            onCheckedChange = { state.showBackBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.ArrowForward,
                            title = "前进按钮",
                            checked = state.showForwardBtn,
                            onCheckedChange = { state.showForwardBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Home,
                            title = "主页按钮",
                            checked = state.showHomeBtn,
                            onCheckedChange = { state.showHomeBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.Layers,
                            title = "标签按钮",
                            checked = state.showTabsBtn,
                            onCheckedChange = { state.showTabsBtn = it }
                        )
                    }
                    item {
                        SettingsSwitchItem(
                            icon = Icons.Outlined.MoreHoriz,
                            title = "菜单按钮",
                            checked = state.showMenuBtn,
                            onCheckedChange = { state.showMenuBtn = it }
                        )
                    }

                    // 数据
                    item { SectionTitle("数据") }
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
                        SettingsItem(
                            icon = Icons.Outlined.FileUpload,
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
                        SettingsItem(
                            icon = Icons.Outlined.Download,
                            title = "导入数据",
                            subtitle = "恢复书签和设置",
                            onClick = { importLauncher.launch(arrayOf("application/json")) }
                        )
                    }

                    // 关于
                    item { SectionTitle("关于") }
                    item {
                        SettingsItem(
                            icon = Icons.Outlined.Info,
                            title = "关于 GuaBrowser",
                            subtitle = "v0.1.0 · 基于 GeckoView"
                        )
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.Medium,
        color = Color(0xFF999999),
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp)
    )
}

/**
 * Via 风格设置项 — 无卡片，无阴影，纯文本行
 */
@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        if (onClick != null) {
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = "进入",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp)
            )
        }
    }
    // Via 风格：分割线
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFFEEEEEE))
    )
}

/**
 * Via 风格设置项（开关）— 无卡片，无阴影
 */
@Composable
fun SettingsSwitchItem(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF666666),
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                color = Color(0xFF333333)
            )
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF1565C0),
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFDDDDDD),
                uncheckedThumbColor = Color.White
            )
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFFEEEEEE))
    )
}
