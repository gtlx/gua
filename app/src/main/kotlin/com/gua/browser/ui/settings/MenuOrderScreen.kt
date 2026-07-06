package com.gua.browser.ui.settings

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.ui.QuickItem

private val allItems = listOf(
    "night_mode" to "夜间", "adblock" to "广告", "desktop" to "桌面",
    "incognito" to "无痕", "scripts" to "脚本", "extensions" to "扩展",
    "bookmarks" to "书签", "history" to "历史", "downloads" to "下载",
    "add_to_home" to "快捷", "share" to "分享", "find" to "查找",
    "settings" to "设置"
)

private val itemIcons = mapOf(
    "night_mode" to Icons.Outlined.DarkMode,
    "adblock" to Icons.Outlined.Block,
    "desktop" to Icons.Outlined.DesktopWindows,
    "incognito" to Icons.Outlined.PrivateConnectivity,
    "scripts" to Icons.Outlined.Code,
    "extensions" to Icons.Outlined.Extension,
    "bookmarks" to Icons.Outlined.BookmarkBorder,
    "history" to Icons.Outlined.History,
    "downloads" to Icons.Outlined.Download,
    "add_to_home" to Icons.Outlined.AddBox,
    "share" to Icons.Outlined.Share,
    "find" to Icons.Outlined.Search,
    "settings" to Icons.Outlined.Settings
)

fun loadMenuOrder(context: Context): List<String> {
    val prefs = context.getSharedPreferences("menu_order", Context.MODE_PRIVATE)
    val saved = prefs.getString("order", null)
    if (saved != null) {
        val parsed = saved.split(",").filter { it.isNotBlank() }
        val allIds = allItems.map { it.first }.toSet()
        if (parsed.all { it in allIds } && parsed.size == allIds.size) return parsed
    }
    return allItems.map { it.first }
}

@Composable
fun MenuOrderScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var order by remember { mutableStateOf(loadMenuOrder(context).toMutableList()) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
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
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("菜单排序", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                }
            }

            Text("点击按钮调整菜单顺序",
                fontSize = 12.sp, color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
            ) {
                itemsIndexed(order, key = { _, id -> id }) { index, id ->
                    val label = allItems.find { it.first == id }?.second ?: id
                    val icon = itemIcons[id] ?: Icons.Outlined.Circle

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White)
                            .clickable { },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(icon, contentDescription = null,
                                tint = Color(0xFF666666), modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(label, fontSize = 15.sp, color = Color(0xFF333333),
                                modifier = Modifier.weight(1f))

                            IconButton(onClick = {
                                if (index > 0) {
                                    order = order.toMutableList().also {
                                        val item = it.removeAt(index)
                                        it.add(index - 1, item)
                                    }
                                    saveMenuOrder(context, order)
                                }
                            }, modifier = Modifier.size(32.dp), enabled = index > 0) {
                                Icon(Icons.Outlined.KeyboardArrowUp,
                                    contentDescription = "上移",
                                    tint = if (index > 0) Color(0xFF666666) else Color(0xFFDDDDDD),
                                    modifier = Modifier.size(20.dp))
                            }
                            IconButton(onClick = {
                                if (index < order.size - 1) {
                                    order = order.toMutableList().also {
                                        val item = it.removeAt(index)
                                        it.add(index + 1, item)
                                    }
                                    saveMenuOrder(context, order)
                                }
                            }, modifier = Modifier.size(32.dp), enabled = index < order.size - 1) {
                                Icon(Icons.Outlined.KeyboardArrowDown,
                                    contentDescription = "下移",
                                    tint = if (index < order.size - 1) Color(0xFF666666) else Color(0xFFDDDDDD),
                                    modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(
                        onClick = {
                            order = allItems.map { it.first }.toMutableList()
                            saveMenuOrder(context, order)
                        },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) { Text("恢复默认", color = Color(0xFF999999), fontSize = 13.sp) }
                }
            }
        }
    }
}

fun saveMenuOrder(context: Context, order: List<String>) {
    context.getSharedPreferences("menu_order", Context.MODE_PRIVATE)
        .edit().putString("order", order.joinToString(",")).apply()
}

fun getMenuItems(context: Context): List<QuickItem> {
    val ids = loadMenuOrder(context)
    return ids.mapNotNull { id ->
        val label = allItems.find { it.first == id }?.second ?: return@mapNotNull null
        val icon = itemIcons[id] ?: return@mapNotNull null
        QuickItem(id, label, icon)
    }
}
