package com.gua.browser.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
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
import com.gua.browser.extension.ExtensionInfo
import kotlinx.coroutines.launch

@Composable
fun ExtensionScreen(onDismiss: () -> Unit) {
    val app = GuaApp.instance
    val extManager = app.extensionManager
    val extList by extManager.extensions.collectAsState()
    var showInstallDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf<ExtensionInfo?>(null) }

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
                    Text("扩展", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    TextButton(
                        onClick = { showInstallDialog = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text("+ 安装", fontSize = 13.sp, color = Color(0xFF1565C0)) }
                }
            }

            if (extList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Extension, contentDescription = null,
                            tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无扩展", fontSize = 15.sp, color = Color(0xFF999999))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("从 Firefox 附加组件商店安装", fontSize = 13.sp, color = Color(0xFFBBBBBB))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    itemsIndexed(extList) { _, ext ->
                        ExtensionRow(
                            info = ext,
                            onToggle = { extManager.toggle(ext.id, it) },
                            onDelete = { showDeleteConfirm = ext }
                        )
                    }
                }
            }
        }
    }

    if (showInstallDialog) {
        InstallExtensionDialog(
            onDismiss = { showInstallDialog = false },
            onInstallFromUrl = { url ->
                extManager.installFromUrl(url) { success, msg ->
                    showInstallDialog = false
                }
            }
        )
    }

    showDeleteConfirm?.let { ext ->
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = null },
            title = { Text("卸载扩展") },
            text = { Text("确定要卸载「${ext.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    extManager.uninstall(ext.id)
                    showDeleteConfirm = null
                }) { Text("卸载", color = Color(0xFFE53935)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun ExtensionRow(info: ExtensionInfo, onToggle: (Boolean) -> Unit, onDelete: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F0)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Outlined.Extension, contentDescription = null,
                tint = if (info.enabled) Color(0xFF1565C0) else Color(0xFFCCCCCC),
                modifier = Modifier.size(22.dp))
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(info.name, fontSize = 15.sp, fontWeight = FontWeight.Normal, color = Color(0xFF333333),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (info.version.isNotBlank()) {
                Text("v${info.version}", fontSize = 12.sp, color = Color(0xFF999999))
            }
        }

        Switch(
            checked = info.enabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Color(0xFF1565C0),
                checkedThumbColor = Color.White
            ),
            modifier = Modifier.height(20.dp)
        )

        Spacer(modifier = Modifier.width(8.dp))

        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Outlined.Delete, contentDescription = "卸载",
                tint = Color(0xFFCCCCCC), modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun InstallExtensionDialog(onDismiss: () -> Unit, onInstallFromUrl: (String) -> Unit) {
    var urlInput by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(false) } // false=url

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("安装扩展", fontWeight = FontWeight.Medium) },
        text = {
            Column {
                Row {
                    TextButton(onClick = { mode = false }) {
                        Text("URL", fontSize = 13.sp,
                            color = if (!mode) Color(0xFF1565C0) else Color(0xFF999999))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text(if (!mode) "https://addons.mozilla.org/..." else "扩展文件名",
                        color = Color(0xFFCCCCCC)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (urlInput.isNotBlank()) {
                    onInstallFromUrl(urlInput.trim())
                }
            }) { Text("安装", color = Color(0xFF1565C0)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
