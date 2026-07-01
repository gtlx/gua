package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.ui.SearchEngine

/**
 * Via 风格搜索引擎设置 — 扁平列表，无卡片投影
 */
@Composable
fun SearchEngineSettings(
    searchEngines: MutableList<SearchEngine>,
    activeIndex: Int,
    onSetActive: (Int) -> Unit,
    onAddEngine: (SearchEngine) -> Unit,
    onRemoveEngine: (Int) -> Unit,
    onMoveUp: (Int) -> Unit,
    onMoveDown: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<Int?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Via 风格顶部
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
                        text = "搜索引擎",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Row {
                        TextButton(
                            onClick = { showAddDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+ 添加", fontSize = 13.sp, color = Color(0xFF1565C0))
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("完成", fontSize = 14.sp, color = Color(0xFF1565C0))
                        }
                    }
                }
            }

            Text(
                text = "点击切换默认搜索引擎 · ${searchEngines.size} 个引擎",
                fontSize = 11.sp,
                color = Color(0xFF999999),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(searchEngines) { index, engine ->
                    val isActive = index == activeIndex

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(if (isActive) Color(0xFF1565C0).copy(alpha = 0.04f) else Color.White)
                            .clickable { onSetActive(index) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 缩写标签
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isActive) Color(0xFF1565C0) else Color(0xFFF0F0F0)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = engine.shortName.take(2),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = if (isActive) Color.White else Color(0xFF888888)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = engine.name,
                                    fontSize = 14.sp,
                                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                                    color = if (isActive) Color(0xFF1565C0) else Color(0xFF333333)
                                )
                                if (isActive) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(Color(0xFF1565C0).copy(alpha = 0.1f))
                                            .padding(horizontal = 5.dp, vertical = 1.dp)
                                    ) {
                                        Text("默认", fontSize = 9.sp, color = Color(0xFF1565C0))
                                    }
                                }
                            }
                            Text(
                                text = engine.urlTemplate,
                                fontSize = 11.sp,
                                color = Color(0xFF999999),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row {
                            if (index > 0) {
                                IconButton(onClick = { onMoveUp(index) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowUp, "上移", tint = Color(0xFFBBBBBB), modifier = Modifier.size(16.dp))
                                }
                            }
                            if (index < searchEngines.size - 1) {
                                IconButton(onClick = { onMoveDown(index) }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Default.KeyboardArrowDown, "下移", tint = Color(0xFFBBBBBB), modifier = Modifier.size(16.dp))
                                }
                            }
                            if (searchEngines.size > 1) {
                                IconButton(onClick = { showDeleteConfirm = index }, modifier = Modifier.size(28.dp)) {
                                    Icon(Icons.Outlined.Close, "删除", tint = Color(0xFFCCCCCC), modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(0.5.dp)
                            .background(Color(0xFFEEEEEE))
                    )
                }
            }
        }

        if (showAddDialog) {
            AddSearchEngineDialog(
                onAdd = { engine ->
                    onAddEngine(engine)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        showDeleteConfirm?.let { index ->
            val engine = searchEngines.getOrNull(index)
            if (engine != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteConfirm = null },
                    title = { Text("删除搜索引擎") },
                    text = { Text("确定要删除「${engine.name}」吗？") },
                    confirmButton = {
                        TextButton(onClick = {
                            onRemoveEngine(index)
                            showDeleteConfirm = null
                        }) { Text("删除", color = Color(0xFFE53935)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
                    }
                )
            }
        }
    }
}

@Composable
fun AddSearchEngineDialog(
    onAdd: (SearchEngine) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var urlTemplate by remember { mutableStateOf("") }
    var shortName by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加搜索引擎") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it; errorMsg = null },
                    label = { Text("名称") }, placeholder = { Text("例如：百度") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = urlTemplate, onValueChange = { urlTemplate = it; errorMsg = null },
                    label = { Text("搜索 URL") }, placeholder = { Text("https://www.baidu.com/s?wd=%s") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    supportingText = { Text("使用 %s 作为搜索词占位符", fontSize = 11.sp) })
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = shortName, onValueChange = { shortName = it },
                    label = { Text("缩写（地址栏显示）") }, placeholder = { Text("BD") },
                    modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = errorMsg!!, color = Color(0xFFE53935), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> errorMsg = "请输入名称"
                    urlTemplate.isBlank() -> errorMsg = "请输入搜索 URL"
                    !urlTemplate.contains("%s") -> errorMsg = "URL 中需要包含 %s 占位符"
                    else -> onAdd(SearchEngine(name, urlTemplate, shortName.ifBlank { name.take(2) }))
                }
            }) { Text("添加") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
