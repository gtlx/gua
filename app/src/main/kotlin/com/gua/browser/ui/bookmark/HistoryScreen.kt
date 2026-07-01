package com.gua.browser.ui.bookmark

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
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
import com.gua.browser.GuaApp
import com.gua.browser.bookmark.HistoryItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Via 风格历史记录 — 扁平列表，日期分组
 */
@Composable
fun HistoryScreen(
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val app = GuaApp.instance
    val scope = rememberCoroutineScope()
    var history by remember { mutableStateOf<List<HistoryItem>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showClearConfirm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        history = withContext(Dispatchers.IO) { app.historyManager.getRecent(200) }
    }

    val filteredHistory = remember(history, searchQuery) {
        if (searchQuery.isBlank()) history
        else history.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
            it.url.contains(searchQuery, ignoreCase = true)
        }
    }

    val groupedHistory = remember(filteredHistory) {
        groupByDate(filteredHistory)
    }

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
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "历史记录 (${history.size})",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF333333)
                        )
                        Row {
                            if (history.isNotEmpty()) {
                                TextButton(
                                    onClick = { showClearConfirm = true },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("清空", fontSize = 13.sp, color = Color(0xFFE53935))
                                }
                            }
                            TextButton(
                                onClick = onDismiss,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("完成", fontSize = 14.sp, color = Color(0xFF1565C0))
                            }
                        }
                    }

                    // 搜索栏
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("搜索历史记录", color = Color(0xFFCCCCCC)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        leadingIcon = {
                            Icon(
                                Icons.Outlined.Search,
                                contentDescription = null,
                                tint = Color(0xFF999999),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = Color(0xFFEEEEEE),
                            focusedBorderColor = Color(0xFF1565C0),
                            unfocusedContainerColor = Color(0xFFF5F5F5),
                            focusedContainerColor = Color.White
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }

            if (filteredHistory.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isNotEmpty()) "未找到匹配的记录"
                               else "暂无浏览记录",
                        color = Color(0xFF999999),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    groupedHistory.forEach { (dateLabel, items) ->
                        item {
                            Text(
                                text = dateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF999999),
                                modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 2.dp)
                            )
                        }

                        items(items, key = { it.id }) { item ->
                            HistoryRow(
                                item = item,
                                onClick = {
                                    onOpenUrl(item.url)
                                    onDismiss()
                                },
                                onDelete = {
                                    scope.launch(Dispatchers.IO) {
                                        app.historyManager.delete(item.id)
                                        history = app.historyManager.getRecent(200)
                                    }
                                }
                            )
                        }
                    }

                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }

        if (showClearConfirm) {
            AlertDialog(
                onDismissRequest = { showClearConfirm = false },
                title = { Text("清空历史记录") },
                text = { Text("确定要删除所有历史记录吗？此操作不可撤销。") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            app.historyManager.clearAll()
                            history = emptyList()
                        }
                        showClearConfirm = false
                    }) {
                        Text("清空", color = Color(0xFFE53935))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearConfirm = false }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

@Composable
fun HistoryRow(
    item: HistoryItem,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = item.title.firstOrNull()?.uppercase() ?: "W",
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = Color(0xFF666666)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.title.ifEmpty { item.url },
                fontSize = 14.sp,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row {
                Text(
                    text = item.url,
                    fontSize = 11.sp,
                    color = Color(0xFF999999),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "x${item.visitCount}",
                    fontSize = 10.sp,
                    color = Color(0xFFCCCCCC)
                )
            }
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(26.dp)
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "删除",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(14.dp)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFFEEEEEE))
    )
}

private fun groupByDate(items: List<HistoryItem>): List<Pair<String, List<HistoryItem>>> {
    val calendar = Calendar.getInstance()
    val today = Calendar.getInstance()
    val yesterday = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
    val thisWeek = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -7) }
    val dateFormat = SimpleDateFormat("M月d日", Locale.CHINESE)

    return items.groupBy { item ->
        calendar.timeInMillis = item.lastVisited
        when {
            isSameDay(calendar, today) -> "今天"
            isSameDay(calendar, yesterday) -> "昨天"
            calendar.after(thisWeek) -> "本周"
            else -> dateFormat.format(calendar.time)
        }
    }.entries.sortedByDescending { entry ->
        when (entry.key) {
            "今天" -> 0
            "昨天" -> 1
            "本周" -> 2
            else -> 3
        }
    }.map { it.key to it.value }
}

private fun isSameDay(c1: Calendar, c2: Calendar): Boolean {
    return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
           c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
}
