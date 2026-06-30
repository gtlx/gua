package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.ui.BrowserState

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
    onDismiss: () -> Unit
) {
    val showSearchEngines = remember { mutableStateOf(false) }

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
