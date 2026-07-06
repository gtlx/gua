package com.gua.browser.ui.download

import android.app.DownloadManager
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.GuaApp
import com.gua.browser.download.AppDownloadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun DownloadScreen(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val app = context.applicationContext as GuaApp
    var downloads by remember { mutableStateOf<List<AppDownloadManager.DownloadStatus>>(emptyList()) }
    var showDeleteConfirm by remember { mutableStateOf<Long?>(null) }

    LaunchedEffect(Unit) {
        while (isActive) {
            downloads = withContext(Dispatchers.IO) { app.downloadManager.queryAll() }
            kotlinx.coroutines.delay(2000)
        }
    }

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
                    Text("下载", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                    TextButton(onClick = onDismiss, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)) {
                        Text("完成", fontSize = 14.sp, color = Color(0xFF1565C0))
                    }
                }
            }

            if (downloads.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Download, contentDescription = null,
                            tint = Color(0xFFCCCCCC), modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("暂无下载", fontSize = 15.sp, color = Color(0xFF999999))
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(downloads, key = { it.id }) { item ->
                        DownloadRow(item, onDelete = { showDeleteConfirm = item.id })
                    }
                }
            }
        }

        showDeleteConfirm?.let { id ->
            AlertDialog(
                onDismissRequest = { showDeleteConfirm = null },
                title = { Text("删除下载记录") },
                text = { Text("确定要删除该下载记录吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        app.downloadManager.remove(id)
                        showDeleteConfirm = null
                    }) { Text("删除", color = Color(0xFFE53935)) }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirm = null }) { Text("取消") }
                }
            )
        }
    }
}

@Composable
private fun DownloadRow(
    item: AppDownloadManager.DownloadStatus,
    onDelete: () -> Unit
) {
    val statusText = when (item.status) {
        DownloadManager.STATUS_PENDING -> "等待中"
        DownloadManager.STATUS_RUNNING -> "下载中 ${item.progress}%"
        DownloadManager.STATUS_PAUSED -> "已暂停"
        DownloadManager.STATUS_SUCCESSFUL -> "已完成"
        DownloadManager.STATUS_FAILED -> "下载失败"
        else -> "未知"
    }
    val statusColor = when (item.status) {
        DownloadManager.STATUS_SUCCESSFUL -> Color(0xFF0D904F)
        DownloadManager.STATUS_FAILED -> Color(0xFFE53935)
        DownloadManager.STATUS_RUNNING -> Color(0xFF1565C0)
        else -> Color(0xFF999999)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
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
                    .background(Color(0xFFF5F5F5)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Download, contentDescription = null,
                    tint = statusColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(text = statusText, fontSize = 12.sp, color = statusColor)
            }
            if (item.status == DownloadManager.STATUS_SUCCESSFUL) {
                IconButton(onClick = {
                    val file = item.localUri?.let { UriUtils.openFile(it) }
                    if (file != null) {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            setDataAndType(
                                androidx.core.content.FileProvider.getUriForFile(
                                    com.gua.browser.GuaApp.instance,
                                    "${com.gua.browser.GuaApp.instance.packageName}.fileprovider",
                                    file
                                ),
                                item.mimeType ?: "*/*"
                            )
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        com.gua.browser.GuaApp.instance.startActivity(intent)
                    }
                }, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Outlined.FolderOpen, contentDescription = "打开",
                        tint = Color(0xFF666666), modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Outlined.Delete, contentDescription = "删除",
                    tint = Color(0xFFCCCCCC), modifier = Modifier.size(18.dp))
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(0.5.dp).background(Color(0xFFEEEEEE)))
    }
}

private object UriUtils {
    fun openFile(uri: String): File? {
        if (uri.startsWith("file://")) return File(uri.removePrefix("file://"))
        if (uri.startsWith("content://")) return null
        return File(uri)
    }
}