package com.gua.browser.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Via 风格工具栏 — 地址栏与工具栏合一
 *
 * 普通状态：◀ ▶ 🔄 ☆ [URL___________] □□  ☰
 * 编辑状态：[B] [输入框________________] [→]
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
    Surface(
        modifier = modifier,
        shadowElevation = 6.dp,
        color = MaterialTheme.colorScheme.surface
    ) {
        if (isFocused) {
            // ===== 编辑状态：地址栏展开 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp)
                    .height(48.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 搜索引擎切换
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .clickable(onClick = onSearchEngineSwitch),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = searchEngineLabel.take(2),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // 输入框 — 大字体
                OutlinedTextField(
                    value = urlText,
                    onValueChange = onUrlChange,
                    modifier = Modifier.weight(1f).height(48.dp),
                    placeholder = {
                        Text(
                            "搜索或输入网址",
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    shape = RoundedCornerShape(24.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.Transparent,
                        cursorColor = MaterialTheme.colorScheme.primary
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Go
                    ),
                    keyboardActions = KeyboardActions(onGo = { onGo(urlText); onFocusChange(false) })
                )

                Spacer(modifier = Modifier.width(4.dp))

                // 前往按钮
                IconButton(onClick = { onGo(urlText); onFocusChange(false) }) {
                    Icon(
                        Icons.Default.ArrowForward,
                        contentDescription = "前往",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        } else {
            // ===== 普通状态：Via 风格组合工具栏 =====
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .height(52.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 后退
                if (showBack) {
                    IconButton(
                        onClick = onBack,
                        enabled = canGoBack,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "后退",
                            tint = if (canGoBack)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }

                // 前进
                if (showForward) {
                    IconButton(
                        onClick = onForward,
                        enabled = canGoForward,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = "前进",
                            tint = if (canGoForward)
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
                        )
                    }
                }

                // 刷新/停止
                if (isLoading) {
                    IconButton(
                        onClick = onStop,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "停止",
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                } else {
                    IconButton(
                        onClick = onRefresh,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "刷新",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // 主页
                if (showHome) {
                    IconButton(
                        onClick = onHome,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.Home,
                            contentDescription = "主页",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // 收藏
                IconButton(
                    onClick = onBookmark,
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        if (isBookmarked) Icons.Default.Star else Icons.Outlined.StarBorder,
                        contentDescription = "收藏",
                        tint = if (isBookmarked)
                            Color(0xFFFFB300)
                        else
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // 地址栏（紧凑 — 圆角胶囊）
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable { onFocusChange(true) },
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .fillMaxHeight(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // 安全锁指示
                        val lockColor = if (isSecure)
                            Color(0xFF0D904F)
                        else
                            Color(0xFF9E9E9E)
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = lockColor,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // URL 文本 — 增大字号
                        Text(
                            text = if (urlText.isNotEmpty()) urlText else "搜索或输入网址",
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (urlText.isNotEmpty())
                                MaterialTheme.colorScheme.onSurface
                            else
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(4.dp))

                // 标签数
                if (showTabs) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable(onClick = onTabs),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "$tabCount",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                // 菜单
                if (showMenu) {
                    IconButton(
                        onClick = onMenu,
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "菜单",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}
