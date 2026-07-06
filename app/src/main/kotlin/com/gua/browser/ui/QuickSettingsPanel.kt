package com.gua.browser.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickItem(
    val id: String,
    val label: String,
    val icon: ImageVector
)

private val items = listOf(
    QuickItem("night_mode", "夜间", Icons.Outlined.DarkMode),
    QuickItem("adblock", "广告", Icons.Outlined.Block),
    QuickItem("desktop", "桌面", Icons.Outlined.DesktopWindows),
    QuickItem("incognito", "无痕", Icons.Outlined.PrivateConnectivity),
    QuickItem("scripts", "脚本", Icons.Outlined.Code),
    QuickItem("bookmarks", "书签", Icons.Outlined.BookmarkBorder),
    QuickItem("history", "历史", Icons.Outlined.History),
    QuickItem("downloads", "下载", Icons.Outlined.Download),
    QuickItem("add_to_home", "快捷", Icons.Outlined.AddBox),
    QuickItem("share", "分享", Icons.Outlined.Share),
    QuickItem("find", "查找", Icons.Outlined.Search),
    QuickItem("settings", "设置", Icons.Outlined.Settings),
)

@Composable
fun QuickSettingsPanel(
    visible: Boolean,
    isNightMode: Boolean,
    isAdblockEnabled: Boolean,
    isDesktopMode: Boolean,
    isIncognito: Boolean = false,
    toolbarAtTop: Boolean = true,
    onNightModeChange: (Boolean) -> Unit,
    onAdblockChange: (Boolean) -> Unit,
    onDesktopModeChange: (Boolean) -> Unit,
    onIncognitoChange: (Boolean) -> Unit = {},
    onScriptManager: () -> Unit,
    onBookmarks: () -> Unit,
    onHistory: () -> Unit,
    onDownloads: () -> Unit,
    onFindInPage: () -> Unit,
    onShare: () -> Unit,
    onAddToHomeScreen: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val alignToTop = toolbarAtTop
    val panelAlignment = if (alignToTop) Alignment.TopCenter else Alignment.BottomCenter
    val slideAnim = if (alignToTop)
        slideInVertically(initialOffsetY = { -it }) + fadeIn()
    else
        slideInVertically(initialOffsetY = { it }) + fadeIn()
    val slideOutAnim = if (alignToTop)
        slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    else
        slideOutVertically(targetOffsetY = { it }) + fadeOut()
    val shape = if (alignToTop)
        RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
    else
        RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f))
                .clickable(onClick = onDismiss)
        ) {
            AnimatedVisibility(
                visible = visible,
                enter = slideAnim,
                exit = slideOutAnim,
                modifier = Modifier.align(panelAlignment)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, shape)
                        .clickable(enabled = false, onClick = {})
                        .padding(16.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .width(36.dp).height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFFDDDDDD))
                                .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(4),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(items) { item ->
                                val active = when (item.id) {
                                    "night_mode" -> isNightMode
                                    "adblock" -> isAdblockEnabled
                                    "desktop" -> isDesktopMode
                                    "incognito" -> isIncognito
                                    else -> false
                                }
                                val bg = if (active) Color(0xFFE3F2FD)
                                         else Color(0xFFF5F5F5)
                                val fg = if (active) Color(0xFF1565C0)
                                         else Color(0xFF666666)

                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .clickable {
                                            when (item.id) {
                                                "night_mode" -> onNightModeChange(!isNightMode)
                                                "adblock" -> onAdblockChange(!isAdblockEnabled)
                                                "desktop" -> onDesktopModeChange(!isDesktopMode)
                                                "incognito" -> onIncognitoChange(!isIncognito)
                                                "scripts" -> onScriptManager()
                                                "bookmarks" -> onBookmarks()
                                                "history" -> onHistory()
                                                "downloads" -> onDownloads()
                                                "add_to_home" -> onAddToHomeScreen()
                                                "share" -> onShare()
                                                "find" -> onFindInPage()
                                                "settings" -> onSettings()
                                            }
                                        }
                                        .padding(vertical = 10.dp)
                                ) {
                                    Icon(
                                        item.icon,
                                        contentDescription = item.label,
                                        tint = fg,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.label, fontSize = 10.sp,
                                        textAlign = TextAlign.Center, color = fg, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}