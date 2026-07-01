package com.gua.browser.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Via 风格页面查找 — 纯白，无投影
 */
@Composable
fun FindInPagePanel(
    visible: Boolean,
    query: String,
    matchCount: Int,
    currentIndex: Int,
    onQueryChange: (String) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.Close,
                        contentDescription = "关闭查找",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("查找", color = Color(0xFFCCCCCC)) },
                    singleLine = true,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF1565C0),
                        unfocusedBorderColor = Color(0xFFEEEEEE),
                        unfocusedContainerColor = Color(0xFFF5F5F5),
                        focusedContainerColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Search
                    ),
                    keyboardActions = KeyboardActions(onSearch = { onNext() })
                )

                if (query.isNotEmpty()) {
                    Text(
                        text = "$currentIndex/$matchCount",
                        fontSize = 11.sp,
                        color = Color(0xFF999999),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                IconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.KeyboardArrowUp,
                        contentDescription = "上一个",
                        tint = Color(0xFF888888),
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                    Icon(
                        Icons.Outlined.KeyboardArrowDown,
                        contentDescription = "下一个",
                        tint = Color(0xFF1565C0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
