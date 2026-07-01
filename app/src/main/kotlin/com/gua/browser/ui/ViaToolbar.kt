package com.gua.browser.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Via 风格工具栏 — 极简无阴影，镂空图标
 *
 * 普通状态：◀ ▶ ↻ ☆ [可滚动 URL] □□ ⋮
 * 编辑状态：[B] [可滚动输入___________] [→]
 */
@Composable
fun ViaToolbar(
    urlText: String,
    isFocused: Boolean,
    isSecure: Boolean,
    searchEngineLabel: String,
    canGoBack: Boolean,
    canGoForward: Boolean,
    isLoading: Boolean,
    isBookmarked: Boolean,
    tabCount: Int,
    showBack: Boolean = true,
    showForward: Boolean = true,
    showHome: Boolean = true,
    showTabs: Boolean = true,
    showMenu: Boolean = true,
    onUrlChange: (String) -> Unit,
    onFocusChange: (Boolean) -> Unit,
    onSearchEngineSwitch: () -> Unit,
    onGo: (String) -> Unit,
    onBack: () -> Unit,
    onForward: () -> Unit,
    onRefresh: () -> Unit,
    onStop: () -> Unit,
    onHome: () -> Unit,
    onBookmark: () -> Unit,
    onTabs: () -> Unit,
    onMenu: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Via 风格：纯白背景，无阴影
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        if (isFocused) {
            // ===== Via 编辑状态 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 4.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 搜索引擎缩写按钮
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable(onClick = onSearchEngineSwitch),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = searchEngineLabel.take(2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF333333)
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // 可滚动输入框
                val scrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = urlText,
                        onValueChange = onUrlChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .horizontalScroll(scrollState),
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 16.sp,
                            color = Color(0xFF333333)
                        ),
                        decorationBox = { innerTextField ->
                            Box {
                                if (urlText.isEmpty()) {
                                    Text(
                                        "搜索或输入网址",
                                        fontSize = 15.sp,
                                        color = Color(0xFFBBBBBB)
                                    )
                                }
                                innerTextField()
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Uri,
                            imeAction = ImeAction.Go
                        ),
                        keyboardActions = KeyboardActions(
                            onGo = { onGo(urlText); onFocusChange(false) }
                        )
                    )
                }

                // 前往
                IconButton(
                    onClick = { onGo(urlText); onFocusChange(false) },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        Icons.Outlined.ArrowForward,
                        contentDescription = "前往",
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else {
            // ===== Via 普通状态：极简工具栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp, vertical = 1.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 后退
                ToolbarIcon(
                    icon = Icons.Outlined.ArrowBack,
                    contentDesc = "后退",
                    enabled = canGoBack,
                    onClick = onBack
                )
                // 前进
                ToolbarIcon(
                    icon = Icons.Outlined.ArrowForward,
                    contentDesc = "前进",
                    enabled = canGoForward,
                    onClick = onForward
                )
                // 刷新/停止
                if (isLoading) {
                    ToolbarIcon(
                        icon = Icons.Outlined.Close,
                        contentDesc = "停止",
                        tint = Color(0xFFE53935),
                        onClick = onStop
                    )
                } else {
                    ToolbarIcon(
                        icon = Icons.Outlined.Refresh,
                        contentDesc = "刷新",
                        onClick = onRefresh
                    )
                }
                // 主页
                if (showHome) {
                    ToolbarIcon(
                        icon = Icons.Outlined.Home,
                        contentDesc = "主页",
                        onClick = onHome
                    )
                }
                // 收藏
                IconButton(
                    onClick = onBookmark,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                        tint = if (isBookmarked) Color(0xFFFFB300) else Color(0xFF999999),
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Via 风格 URL 胶囊 — 可水平滚动
                val urlScrollState = rememberScrollState()
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(32.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF5F5F5))
                        .clickable { onFocusChange(true) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 10.dp)
                            .fillMaxHeight()
                            .horizontalScroll(urlScrollState),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = if (isSecure) Color(0xFF0D904F) else Color(0xFFBBBBBB),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (urlText.isNotEmpty()) urlText else "搜索或输入网址",
                            fontSize = 13.sp,
                            maxLines = 1,
                            color = if (urlText.isNotEmpty())
                                Color(0xFF333333)
                            else
                                Color(0xFFBBBBBB),
                            softWrap = false
                        )
                    }
                }

                Spacer(modifier = Modifier.width(2.dp))

                // 标签数
                if (showTabs) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTabs),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$tabCount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF666666)
                        )
                    }
                }
                // 菜单
                if (showMenu) {
                    IconButton(
                        onClick = onMenu,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Outlined.MoreVert,
                            contentDescription = "菜单",
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Via 风格工具栏图标按钮
 */
@Composable
private fun ToolbarIcon(
    icon: ImageVector,
    contentDesc: String,
    enabled: Boolean = true,
    tint: Color = Color(0xFF666666),
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = if (enabled) tint else tint.copy(alpha = 0.25f),
            modifier = Modifier.size(20.dp)
        )
    }
}
