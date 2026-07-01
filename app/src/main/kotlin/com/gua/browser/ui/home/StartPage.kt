package com.gua.browser.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.GuaApp
import com.gua.browser.bookmark.Bookmark
import com.gua.browser.ui.BrowserState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Via 风格起始页 — 纯白，无投影，极简
 */
@Composable
fun StartPage(
    state: BrowserState,
    onOpenUrl: (String) -> Unit,
    onFocusSearch: () -> Unit
) {
    val app = GuaApp.instance
    var bookmarks by remember { mutableStateOf<List<Bookmark>>(emptyList()) }

    LaunchedEffect(Unit) {
        bookmarks = withContext(Dispatchers.IO) { app.bookmarkManager.getAll() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        // Logo
        Text(
            text = "GuaBrowser",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1565C0)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 搜索框
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFFF5F5F5))
                .clickable(onClick = onFocusSearch)
                .padding(horizontal = 16.dp, vertical = 11.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Search,
                    contentDescription = null,
                    tint = Color(0xFFBBBBBB),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "搜索或输入网址",
                    fontSize = 14.sp,
                    color = Color(0xFFBBBBBB)
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // 快捷入口
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            QuickTile(Icons.Outlined.Bookmarks, "书签",
                onClick = { state.showBookmarks = true })
            QuickTile(Icons.Outlined.History, "历史",
                onClick = { state.showHistory = true })
            QuickTile(Icons.Outlined.Download, "下载",
                onClick = { })
            QuickTile(Icons.Outlined.Settings, "设置",
                onClick = { state.showSettings = true })
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (bookmarks.isNotEmpty()) {
            Text(
                text = "书签",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF999999),
                modifier = Modifier
                    .align(Alignment.Start)
                    .padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                contentPadding = PaddingValues(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(bookmarks.take(20), key = { it.id }) { bookmark ->
                    BookmarkTile(
                        title = bookmark.title,
                        url = bookmark.url,
                        onClick = { onOpenUrl(bookmark.url) }
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickTile(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = label,
                tint = Color(0xFF666666),
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Color(0xFF888888)
        )
    }
}

@Composable
private fun BookmarkTile(title: String, url: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = title.firstOrNull()?.uppercase() ?: "W",
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )
        }
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = title,
            fontSize = 9.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = Color(0xFF888888)
        )
    }
}
