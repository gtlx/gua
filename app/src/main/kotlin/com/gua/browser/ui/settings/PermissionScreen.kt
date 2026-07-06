package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.GuaApp
import com.gua.browser.settings.PermissionStore

@Composable
fun PermissionScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GuaApp
    var entries by remember { mutableStateOf(app.permissionStore.getGroupedByOrigin()) }
    var showClearConfirm by remember { mutableStateOf(false) }
    var selectedOrigin by remember { mutableStateOf<String?>(null) }

    fun refresh() {
        entries = app.permissionStore.getGroupedByOrigin()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 标题栏
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Outlined.Security, contentDescription = "返回",
                                tint = Color(0xFF333333))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("权限管理", fontSize = 17.sp, fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333))
                    }
                    if (entries.isNotEmpty()) {
                        TextButton(onClick = { showClearConfirm = true }) {
                            Text("清空", fontSize = 13.sp, color = Color(0xFFE53935))
                        }
                    }
                }
            }

            if (entries.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Lock, contentDescription = null,
                            tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无权限记录", fontSize = 15.sp, color = Color(0xFF999999))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("网站请求权限时会显示在这里", fontSize = 12.sp, color = Color(0xFFCCCCCC))
                    }
                }
            } else if (selectedOrigin != null) {
                // 某网站的权限详情
                val origin = selectedOrigin!!
                val siteEntries = entries[origin] ?: emptyList()
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedOrigin = null; refresh() },
                                modifier = Modifier.size(36.dp)) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack,
                                    contentDescription = "返回", tint = Color(0xFF333333))
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(origin, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                                color = Color(0xFF333333), maxLines = 1,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(siteEntries) { entry ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.typeName, fontSize = 15.sp,
                                        color = Color(0xFF333333))
                                }
                                Text(
                                    if (entry.allowed) "已允许" else "已拒绝",
                                    fontSize = 13.sp,
                                    color = if (entry.allowed) Color(0xFF0D904F) else Color(0xFFE53935)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = {
                                    app.permissionStore.remove(entry.origin, entry.type)
                                    refresh()
                                    if (app.permissionStore.getAll().none { it.origin == origin }) {
                                        selectedOrigin = null
                                    }
                                }) {
                                    Text("删除", fontSize = 12.sp, color = Color(0xFF999999))
                                }
                            }
                            Box(modifier = Modifier.fillMaxWidth().height(0.5.dp)
                                .background(Color(0xFFEEEEEE)))
                        }
                    }
                }
            } else {
                // 网站列表
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(entries.entries.toList(), key = { it.key }) { (origin, perms) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color.White)
                                .clickable { selectedOrigin = origin }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFF5F5F5)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Outlined.Lock, contentDescription = null,
                                    tint = Color(0xFF666666), modifier = Modifier.size(18.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(origin, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                                    color = Color(0xFF333333), maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                                Text("${perms.size} 个权限",
                                    fontSize = 12.sp, color = Color(0xFF999999))
                            }
                            Icon(Icons.Outlined.KeyboardArrowRight, contentDescription = "查看",
                                tint = Color(0xFFCCCCCC), modifier = Modifier.size(18.dp))
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp)
                            .background(Color(0xFFEEEEEE)))
                    }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("清空权限记录") },
                text = { Text("确定要清空所有网站的权限设置吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        app.permissionStore.clear()
                        showClearConfirm = false
                        refresh()
                    }) { Text("清空", color = Color(0xFFE53935)) }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) { Text("取消") }
                }
            )
        }
    }
}