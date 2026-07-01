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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class QuickItem(
    val id: String,
    val label: String,
    val emoji: String
)

private val items = listOf(
    QuickItem("night_mode", "夜间", "🌙"),
    QuickItem("adblock", "广告", "🚫"),
    QuickItem("desktop", "桌面", "🖥️"),
    QuickItem("incognito", "无痕", "👤"),
    QuickItem("scripts", "脚本", "📜"),
    QuickItem("bookmarks", "书签", "🔖"),
    QuickItem("history", "历史", "📋"),
    QuickItem("add_to_home", "快捷", "🏠"),
    QuickItem("share", "分享", "📤"),
    QuickItem("find", "查找", "🔍"),
    QuickItem("settings", "设置", "⚙️"),
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
    onFindInPage: () -> Unit,
    onShare: () -> Unit,
    onAddToHomeScreen: () -> Unit,
    onSettings: () -> Unit,
    onDismiss: () -> Unit
) {
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
            // 面板位置跟随工具栏
            val panelAlignment = if (toolbarAtTop) Alignment.TopCenter else Alignment.BottomCenter
            val slideIn = if (toolbarAtTop) slideInVertically(initialOffsetY = { -it }) else slideInVertically(initialOffsetY = { it })
            val slideOut = if (toolbarAtTop) slideOutVertically(targetOffsetY = { -it }) else slideOutVertically(targetOffsetY = { it })

            AnimatedVisibility(
                visible = true,
                enter = slideIn + fadeIn(),
                exit = slideOut + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(panelAlignment)
                        .background(
                            Color.White,
                            if (toolbarAtTop)
                                RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp)
                            else
                                RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
                        )
                        .clickable(enabled = false, onClick = {}),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // 拖拽指示条
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
                                                "add_to_home" -> onAddToHomeScreen()
                                                "share" -> onShare()
                                                "find" -> onFindInPage()
                                                "settings" -> onSettings()
                                            }
                                        }
                                        .padding(12.dp)
                                ) {
                                    Text(text = item.emoji, fontSize = 22.sp)
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
