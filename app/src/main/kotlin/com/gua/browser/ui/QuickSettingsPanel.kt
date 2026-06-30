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
    QuickItem("scripts", "脚本", "📜"),
    QuickItem("bookmarks", "书签", "🔖"),
    QuickItem("history", "历史", "📋"),
    QuickItem("add_to_home", "桌面快捷", "🏠"),
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
    onNightModeChange: (Boolean) -> Unit,
    onAdblockChange: (Boolean) -> Unit,
    onDesktopModeChange: (Boolean) -> Unit,
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
                Column(modifier = Modifier.padding(16.dp)) {
                    Box(
                        modifier = Modifier
                            .width(40.dp).height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            .align(Alignment.CenterHorizontally)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("快捷工具", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(items) { item ->
                            val active = when (item.id) {
                                "night_mode" -> isNightMode
                                "adblock" -> isAdblockEnabled
                                "desktop" -> isDesktopMode
                                else -> false
                            }
                            val bg = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                     else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            val fg = if (active) MaterialTheme.colorScheme.primary
                                     else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(bg)
                                    .clickable {
                                        when (item.id) {
                                            "night_mode" -> onNightModeChange(!isNightMode)
                                            "adblock" -> onAdblockChange(!isAdblockEnabled)
                                            "desktop" -> onDesktopModeChange(!isDesktopMode)
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
