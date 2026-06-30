package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.ui.SearchEngine

/**
 * 搜索引擎设置界面
 *
 * Via 风格：
 * - 列表展示所有搜索引擎
 * - 长按拖动排序（简化版：上移/下移按钮）
 * - 点击切换为默认
 * - 支持添加/删除
 * - 添加时可自定义名称、URL 模板、缩写
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
            .background(MaterialTheme.colorScheme.background)
    ) {
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
                        text = "搜索引擎",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        FilledTonalButton(
                            onClick = { showAddDialog = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("+ 添加", fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text("完成")
                        }
                    }
                }
            }

            // 说明
            Text(
                text = "点击切换默认搜索引擎 · ${searchEngines.size} 个引擎",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 引擎列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(searchEngines) { index, engine ->
                    val isActive = index == activeIndex

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSetActive(index) }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 缩写标签
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isActive)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = engine.shortName.take(2),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isActive)
                                        MaterialTheme.colorScheme.onPrimary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            // 引擎信息
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = engine.name,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (isActive) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "默认",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = engine.urlTemplate,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // 操作按钮：上移/下移/删除
                            Row {
                                // 上移（非第一个）
                                if (index > 0) {
                                    IconButton(
                                        onClick = { onMoveUp(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(android.R.drawable.ic_menu_upload),
                                            contentDescription = "上移",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                // 下移（非最后一个）
                                if (index < searchEngines.size - 1) {
                                    IconButton(
                                        onClick = { onMoveDown(index) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(android.R.drawable.ic_menu_download),
                                            contentDescription = "下移",
                                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                // 删除（至少保留一个）
                                if (searchEngines.size > 1) {
                                    IconButton(
                                        onClick = { showDeleteConfirm = index },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(android.R.drawable.ic_menu_close_clear_cancel),
                                            contentDescription = "删除",
                                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.6f),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 添加搜索引擎弹窗
        if (showAddDialog) {
            AddSearchEngineDialog(
                onAdd = { engine ->
                    onAddEngine(engine)
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        // 删除确认弹窗
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
                        }) {
                            Text("删除", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteConfirm = null }) {
                            Text("取消")
                        }
                    }
                )
            }
        }
    }
}

/**
 * 添加搜索引擎弹窗
 */
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
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; errorMsg = null },
                    label = { Text("名称") },
                    placeholder = { Text("例如：百度") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = urlTemplate,
                    onValueChange = { urlTemplate = it; errorMsg = null },
                    label = { Text("搜索 URL") },
                    placeholder = { Text("https://www.baidu.com/s?wd=%s") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("使用 %s 作为搜索词占位符", fontSize = 11.sp)
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = shortName,
                    onValueChange = { shortName = it },
                    label = { Text("缩写（地址栏显示）") },
                    placeholder = { Text("BD") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    maxLines = 1
                )

                if (errorMsg != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMsg!!,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                when {
                    name.isBlank() -> errorMsg = "请输入名称"
                    urlTemplate.isBlank() -> errorMsg = "请输入搜索 URL"
                    !urlTemplate.contains("%s") -> errorMsg = "URL 中需要包含 %s 占位符"
                    else -> {
                        val sn = shortName.ifBlank { name.take(2) }
                        onAdd(SearchEngine(name, urlTemplate, sn))
                    }
                }
            }) {
                Text("添加")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
