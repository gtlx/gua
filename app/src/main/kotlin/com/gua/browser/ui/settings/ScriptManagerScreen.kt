package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.userscript.ScriptManager
import com.gua.browser.userscript.UserScript

/**
 * Via 风格脚本管理 — 扁平列表
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
            .background(Color(0xFFF5F5F5))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

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
                        text = "用户脚本",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF333333)
                    )
                    Row {
                        TextButton(
                            onClick = { showInstallDialog = true },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("+ 安装", fontSize = 13.sp, color = Color(0xFF1565C0)) }
                        Spacer(modifier = Modifier.width(4.dp))
                        TextButton(
                            onClick = onDismiss,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("完成", fontSize = 14.sp, color = Color(0xFF1565C0)) }
                    }
                }
            }

            if (scripts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("暂无用户脚本", fontSize = 15.sp, color = Color(0xFF999999))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("点击右上角「安装」从 GreasyFork 添加脚本",
                            fontSize = 12.sp, color = Color(0xFFCCCCCC))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(scripts, key = { it.id }) { script ->
                        ScriptRow(
                            script = script,
                            onToggle = { scriptManager.toggleScript(script.id) },
                            onDelete = { scriptManager.deleteScript(script.id) }
                        )
                    }
                }
            }
        }

        if (showInstallDialog) {
            InstallScriptDialog(
                url = installUrl,
                onUrlChange = { installUrl = it },
                onInstall = {
                    if (installUrl.isNotBlank()) {
                        scriptManager.installFromUrl(installUrl) { result ->
                            if (result != null) { installUrl = ""; showInstallDialog = false }
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

@Composable
fun ScriptRow(
    script: UserScript,
    onToggle: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Switch(
            checked = script.enabled,
            onCheckedChange = { onToggle() },
            modifier = Modifier.padding(end = 10.dp),
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF1565C0),
                checkedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFFDDDDDD),
                uncheckedThumbColor = Color.White
            )
        )

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = script.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF333333),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!script.description.isNullOrBlank()) {
                Text(
                    text = script.description,
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = "v${script.version}${if (script.author != null) " · ${script.author}" else ""}",
                fontSize = 10.sp,
                color = Color(0xFFCCCCCC)
            )
        }

        IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(30.dp)) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "删除",
                tint = Color(0xFFCCCCCC),
                modifier = Modifier.size(18.dp)
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(0.5.dp)
            .background(Color(0xFFEEEEEE))
    )

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("删除脚本") },
            text = { Text("确定要删除「${script.name}」吗？") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("删除", color = Color(0xFFE53935))
                }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("取消") } }
        )
    }
}

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
                    OutlinedTextField(
                        value = url, onValueChange = onUrlChange,
                        label = { Text("GreasyFork 脚本 URL") },
                        placeholder = { Text("https://greasyfork.org/...") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    TextButton(onClick = { showCodeInput = true }) { Text("或者粘贴脚本代码") }
                } else {
                    OutlinedTextField(
                        value = codeText, onValueChange = { codeText = it },
                        label = { Text("粘贴脚本代码") },
                        modifier = Modifier.fillMaxWidth().height(200.dp), maxLines = 20
                    )
                    TextButton(onClick = { showCodeInput = false }) { Text("改为 URL 安装") }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (showCodeInput && codeText.isNotBlank()) onPasteCode(codeText)
                else onInstall()
            }) { Text("安装") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
