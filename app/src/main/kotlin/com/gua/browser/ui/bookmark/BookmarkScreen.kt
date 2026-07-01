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
import com.gua.browser.bookmark.Bookmark
import com.gua.browser.ui.BrowserState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Via 风格书签管理界面 — 扁平列表，无卡片投影
 */
@Composable
fun BookmarkScreen(
    state: BrowserState,
    onOpenUrl: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val app = GuaApp.instance
    val scope = rememberCoroutineScope()
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Bookmark?>(null) }

    LaunchedEffect(Unit) {
        bookmarks = withContext(Dispatchers.IO) { app.bookmarkManager.getAll() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Via 风格顶部栏
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
                        text = "书签 (${bookmarks.size})",
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

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无书签",
                            fontSize = 15.sp,
                            color = Color(0xFF999999)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "在页面中点击地址栏右侧 ☆ 添加书签",
                            fontSize = 12.sp,
                            color = Color(0xFFCCCCCC)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bookmark ->
                        BookmarkRow(
                            bookmark = bookmark,
                            onClick = {
                                onOpenUrl(bookmark.url)
                                onDismiss()
                            },
                            onDelete = { deleteTarget = bookmark }
                        )
                    }
                }
            }
        }

        if (showAddDialog) {
            AddBookmarkDialog(
                currentUrl = state.url,
                currentTitle = state.pageTitle,
                onAdd = { title, url ->
                    scope.launch(Dispatchers.IO) {
                        app.bookmarkManager.add(Bookmark(title = title, url = url))
                        bookmarks = app.bookmarkManager.getAll()
                    }
                    showAddDialog = false
                },
                onDismiss = { showAddDialog = false }
            )
        }

        deleteTarget?.let { bookmark ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("删除书签") },
                text = { Text("确定要删除「${bookmark.title}」吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch(Dispatchers.IO) {
                            app.bookmarkManager.delete(bookmark.id)
                            bookmarks = app.bookmarkManager.getAll()
                        }
                        deleteTarget = null
                    }) {
                        Text("删除", color = Color(0xFFE53935))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) {
                        Text("取消")
                    }
                }
            )
        }
    }
}

/**
 * Via 风格书签行 — 无卡片，无投影
 */
@Composable
fun BookmarkRow(
    bookmark: Bookmark,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 首字母圆形图标
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = bookmark.title.firstOrNull()?.uppercase() ?: "W",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = bookmark.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Normal,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = bookmark.url,
                fontSize = 12.sp,
                color = Color(0xFF999999),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(28.dp)
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "删除",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(16.dp)
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

@Composable
fun AddBookmarkDialog(
    currentUrl: String,
    currentTitle: String,
    onAdd: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(currentTitle) }
    var url by remember { mutableStateOf(currentUrl) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加书签") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("名称") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("网址") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(title, url) }) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
