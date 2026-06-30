package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.userscript.ScriptManager
import com.gua.browser.userscript.UserScript

/**
 * 脚本管理界面
 *
 * 显示所有已安装的用户脚本。
 * 支持：启用/禁用、删除、查看详情。
 */
@Composable
fun ScriptManagerScreen(
    scriptManager: ScriptManager,
    onDismiss: () -> Unit
) {
    val scripts by scriptManager.scripts.collectAsState()
    var showInstallDialog by remember { mutableStateOf(false) }
    var installUrl by remember { mutableStateOf("") }

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
                        text = "用户脚本",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Row {
                        FilledTonalButton(
                            onClick = { showInstallDialog = true },
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("+ 安装", fontSize = 13.sp)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = onDismiss) {
                            Text("完成")
                        }
                    }
                }
            }

            // 脚本列表
            if (scripts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "暂无用户脚本",
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "点击右上角「安装」从 GreasyFork 添加脚本",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(scripts, key = { it.id }) { script ->
                        ScriptCard(
                            script = script,
                            onToggle = { scriptManager.toggleScript(script.id) },
                            onDelete = { scriptManager.deleteScript(script.id) }
                        )
                    }
                }
            }
        }

        // 安装弹窗
        if (showInstallDialog) {
            InstallScriptDialog(
                url = installUrl,
                onUrlChange = { installUrl = it },
                onInstall = {
                    if (installUrl.isNotBlank()) {
                        scriptManager.installFromUrl(installUrl) { result ->
                            if (result != null) {
                                installUrl = ""
                                showInstallDialog = false
                            }
                        }
                    }
                },
                onPasteCode = { code ->
                    scriptManager.installFromCode(code)
                    showInstallDialog = false
                },
                onDismiss = { showInstallDialog = false }
            )
        }
    }
}

/**
 * 单条脚本卡片
 */
@Composable
fun ScriptCard(
    script: UserScript,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 启用开关
            Switch(
                checked = script.enabled,
                onCheckedChange = { onToggle() },
                modifier = Modifier.padding(end = 12.dp)
            )

            // 脚本信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = script.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!script.description.isNullOrBlank()) {
                    Text(
                        text = script.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "v${script.version}${if (script.author != null) " · ${script.author}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }

            // 删除按钮
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }
    }

    // 删除确认弹窗
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除脚本") },
            text = { Text("确定要删除「${script.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("取消")
                }
            }
        )
    }
}

/**
 * 安装脚本弹窗
 */
@Composable
fun InstallScriptDialog(
    url: String,
    onUrlChange: (String) -> Unit,
    onInstall: () -> Unit,
    onPasteCode: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var showCodeInput by remember { mutableStateOf(false) }
    var codeText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("安装脚本") },
        text = {
            Column {
                if (!showCodeInput) {
                    // 从 URL 安装
                    OutlinedTextField(
                        value = url,
                        onValueChange = onUrlChange,
                        label = { Text("GreasyFork 脚本 URL") },
                        placeholder = { Text("https://greasyfork.org/...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showCodeInput = true }) {
                        Text("或者粘贴脚本代码")
                    }
                } else {
                    // 粘贴代码安装
                    OutlinedTextField(
                        value = codeText,
                        onValueChange = { codeText = it },
                        label = { Text("粘贴脚本代码") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        maxLines = 20
                    )
                    TextButton(onClick = { showCodeInput = false }) {
                        Text("改为 URL 安装")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (showCodeInput && codeText.isNotBlank()) {
                    onPasteCode(codeText)
                } else {
                    onInstall()
                }
            }) {
                Text("安装")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}
