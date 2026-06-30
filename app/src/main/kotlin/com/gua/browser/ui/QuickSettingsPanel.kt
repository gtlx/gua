package com.gua.browser.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 快速设置面板 — Via 风格
 *
 * 从底部滑出的功能快捷入口：
 *   夜间模式 | 广告过滤 | 桌面视图 | 脚本
 *   书签     | 历史    | 设置    | 查找
 */
@Composable
fun QuickSettingsPanel(
    visible: Boolean,
    isNightMode: Boolean,
    isAdblockEnabled: Boolean,
    isDesktopMode: Boolean,
    onNightModeChange: (Boolean) -> Unit,
    onAdblockChange: (Boolean) -> Unit,
    onDesktopModeChange: (Boolean) -> Unit,
    onScriptManager: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onFindInPage: () -> Unit,
    onShare: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.4f))
                .clickable(onClick = onDismiss)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .clickable(enabled = false, onClick = {}),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // 拖拽指示条
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "快捷工具",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(quickSettingsItems) { item ->
                            QuickSettingTile(
                                item = item,
                                isActive = when (item.id) {
                                    "night_mode" -> isNightMode
                                    "adblock" -> isAdblockEnabled
                                    "desktop" -> isDesktopMode
                                    else -> false
                                },
                                onClick = {
                                    when (item.id) {
                                        "night_mode" -> onNightModeChange(!isNightMode)
                                        "adblock" -> onAdblockChange(!isAdblockEnabled)
                                        "desktop" -> onDesktopModeChange(!isDesktopMode)
                                        "scripts" -> onScriptManager()
                                        "bookmarks" -> onBookmarks()
                                        "history" -> onHistory()
                                        "find" -> onFindInPage()
                                        "share" -> onShare()
                                        "settings" -> onSettings()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

data class QuickSettingItem(
    val id: String,
    val label: String,
    val icon: Int
)

private val quickSettingsItems = listOf(
    QuickSettingItem("night_mode", "夜间", android.R.drawable.ic_menu_gallery),
    QuickSettingItem("adblock", "广告", android.R.drawable.ic_menu_delete),
    QuickSettingItem("desktop", "桌面", android.R.drawable.ic_menu_view),
    QuickSettingItem("scripts", "脚本", android.R.drawable.ic_menu_edit),
    QuickSettingItem("bookmarks", "书签", android.R.drawable.ic_menu_myplaces),
    QuickSettingItem("history", "历史", android.R.drawable.ic_menu_recent_history),
    QuickSettingItem("share", "分享", android.R.drawable.ic_menu_share),
    QuickSettingItem("find", "查找", android.R.drawable.ic_menu_search),
    QuickSettingItem("settings", "设置", android.R.drawable.ic_menu_manage),
)

@Composable
fun QuickSettingTile(
    item: QuickSettingItem,
    isActive: Boolean,
    onClick: () -> Unit
) {
    val bgColor = if (isActive)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)

    val iconColor = if (isActive)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            painter = painterResource(item.icon),
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = iconColor,
            maxLines = 1
        )
    }
}
