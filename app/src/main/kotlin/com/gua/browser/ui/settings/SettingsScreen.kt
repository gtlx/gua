package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.gua.browser.settings.DataManager
import com.gua.browser.ui.BrowserState
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    state: BrowserState,
    onDismiss: () -> Unit
) {
    val showSearchEngines = remember { mutableStateOf(false) }
    val showToolbarSettings = remember { mutableStateOf(false) }
    val showAdBlockRules = remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        when {
            showSearchEngines.value -> SearchEngineSettings(
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
            showToolbarSettings.value -> ToolbarSettingsScreen(
                state = state,
                onDismiss = { showToolbarSettings.value = false }
            )
            showAdBlockRules.value -> AdBlockRulesScreen(
                state = state,
                onDismiss = { showAdBlockRules.value = false }
            )
            else -> MainSettingsScreen(
                state = state,
                onDismiss = onDismiss,
                onOpenSearchEngines = { showSearchEngines.value = true },
                onOpenToolbarSettings = { showToolbarSettings.value = true },
                onOpenAdBlockRules = { showAdBlockRules.value = true },
                context = context
            )
        }
    }
}

// ============================================================
//  主设置界面
// ============================================================

@Composable
private fun MainSettingsScreen(
    state: BrowserState,
    onDismiss: () -> Unit,
    onOpenSearchEngines: () -> Unit,
    onOpenToolbarSettings: () -> Unit,
    onOpenAdBlockRules: () -> Unit,
    context: android.content.Context
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 顶部栏
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
                Text("设置", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
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
                    onClick = onOpenSearchEngines
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

            // 隐私与安全
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
            item {
                SettingsItem(
                    icon = Icons.Outlined.ListAlt,
                    title = "自定义广告规则",
                    subtitle = "${state.customAdRules.size} 条规则",
                    onClick = onOpenAdBlockRules
                )
            }
            item {
                SettingsSwitchItem(
                    icon = Icons.Outlined.PrivateConnectivity,
                    title = "无痕模式",
                    subtitle = "不保存浏览历史",
                    checked = state.isIncognito,
                    onCheckedChange = { state.isIncognito = it }
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

            // 工具栏（父菜单）
            item { SectionTitle("工具栏") }
            item {
                SettingsItem(
                    icon = Icons.Outlined.Tune,
                    title = "工具栏设置",
                    subtitle = "位置、按钮显示、地址栏",
                    onClick = onOpenToolbarSettings
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

// ============================================================
//  工具栏设置子界面
// ============================================================

@Composable
fun ToolbarSettingsScreen(
    state: BrowserState,
    onDismiss: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color(0xFF333333))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("工具栏设置", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            item { SectionTitle("布局") }
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

            item { SectionTitle("按钮显示") }
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

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

// ============================================================
//  自定义广告规则子界面
// ============================================================

@Composable
fun AdBlockRulesScreen(
    state: BrowserState,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var inputText by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = "返回", tint = Color(0xFF333333))
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text("自定义广告规则", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = { showAddDialog = true }) {
                    Text("+ 添加", fontSize = 14.sp, color = Color(0xFF1565C0))
                }
            }
        }

        if (state.customAdRules.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无自定义规则\n点击右上角「添加」新增",
                    fontSize = 14.sp,
                    color = Color(0xFF999999),
                    lineHeight = 22.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(state.customAdRules) { index, rule ->
                    SettingsItem(
                        icon = if (rule.enabled) Icons.Outlined.Block else Icons.Outlined.CheckCircleOutline,
                        title = rule.pattern,
                        subtitle = if (rule.enabled) "已启用" else "已禁用",
                        onClick = {
                            val list = state.customAdRules.toMutableList()
                            list[index] = rule.copy(enabled = !rule.enabled)
                            state.customAdRules = list
                        }
                    )
                    // 删除按钮（右侧）
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .clickable { state.removeCustomAdRule(index) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("删除", fontSize = 13.sp, color = Color(0xFFE53935))
                    }
                }
                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        // 添加规则弹窗
        if (showAddDialog) {
            AlertDialog(
                onDismissRequest = { showAddDialog = false },
                title = { Text("添加过滤规则") },
                text = {
                    Column {
                        Text(
                            text = "输入要拦截的 URL 关键词，包含该关键词的请求将被拦截。",
                            fontSize = 13.sp,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = { inputText = it },
                            label = { Text("关键词") },
                            placeholder = { Text("例如：example-ad.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = {
                                if (inputText.isNotBlank()) {
                                    state.addCustomAdRule(inputText)
                                    inputText = ""
                                    showAddDialog = false
                                }
                            })
                        )
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        if (inputText.isNotBlank()) {
                            state.addCustomAdRule(inputText)
                            inputText = ""
                            showAddDialog = false
                        }
                    }) {
                        Text("添加", color = Color(0xFF1565C0))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddDialog = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

// ============================================================
//  通用组件
// ============================================================

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
        Icon(icon, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = Color(0xFF333333))
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF999999), modifier = Modifier.padding(top = 2.dp))
            }
        }
        if (onClick != null) {
            Icon(Icons.Outlined.ChevronRight, contentDescription = "进入", tint = Color(0xFFCCCCCC), modifier = Modifier.size(18.dp))
        }
    }
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFEEEEEE)))
}

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
        Icon(icon, contentDescription = null, tint = Color(0xFF666666), modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, color = Color(0xFF333333))
            if (subtitle.isNotEmpty()) {
                Text(subtitle, fontSize = 12.sp, color = Color(0xFF999999), modifier = Modifier.padding(top = 2.dp))
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
    Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFEEEEEE)))
}
