package com.gua.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.engine.EngineManager

/**
 * Via 风格标签切换面板 — 扁平卡片，无投影
 */
@Composable
fun TabSwitcherPanel(
    tabs: List<EngineManager.Tab>,
    activeIndex: Int,
    onSwitchTab: (Int) -> Unit,
    onCloseTab: (Int) -> Unit,
    onNewTab: () -> Unit,
    onDismiss: () -> Unit
) {
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
                        text = "标签页 (${tabs.size})",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Row {
                        TextButton(
                            onClick = onNewTab,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("+ 新建", fontSize = 13.sp, color = Color(0xFF1565C0))
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

            if (tabs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "没有打开的标签页",
                        color = Color(0xFF999999),
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyRow(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(tabs) { index, tab ->
                        TabCard(
                            tab = tab,
                            isActive = index == activeIndex,
                            onClick = { onSwitchTab(index) },
                            onClose = { onCloseTab(index) },
                            modifier = Modifier.width(220.dp)
                        )
                    }
                    item { NewTabCard(onClick = onNewTab) }
                }
            }
        }
    }
}

/**
 * Via 风格标签卡片 — 纯白背景，无投影，选中时有蓝边
 */
@Composable
fun TabCard(
    tab: EngineManager.Tab,
    isActive: Boolean,
    onClick: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxHeight(0.65f)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isActive) Color.White else Color(0xFFEEEEEE))
            .clickable(onClick = onClick)
            .then(
                if (isActive) Modifier.padding(1.5.dp) else Modifier
            ),
        contentAlignment = Alignment.BottomStart
    ) {
        // 选中高亮边框（用叠加层模拟）
        if (isActive) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color(0xFF1565C0).copy(alpha = 0.08f))
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            // 关闭按钮（右上）
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(RoundedCornerShape(11.dp))
                        .background(Color.Black.copy(alpha = 0.06f))
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        fontSize = 11.sp,
                        color = Color(0xFF888888)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 标题
            Text(
                text = tab.title.ifEmpty { "新标签" },
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) Color(0xFF333333) else Color(0xFF666666),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            // URL
            Text(
                text = tab.url.ifEmpty { "about:blank" },
                fontSize = 11.sp,
                color = if (isActive) Color(0xFF999999) else Color(0xFFBBBBBB),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * 新建标签卡片
 */
@Composable
fun NewTabCard(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(180.dp)
            .fillMaxHeight(0.65f)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF0F0F0))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "+",
                fontSize = 32.sp,
                color = Color(0xFF1565C0),
                fontWeight = FontWeight.Light
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = "新建标签",
                fontSize = 12.sp,
                color = Color(0xFF888888)
            )
        }
    }
}
