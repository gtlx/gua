package com.gua.browser.ui.settings

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gua.browser.GuaApp
import com.gua.browser.settings.DataManager
import com.gua.browser.ui.BrowserState
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun ImportExportScreen(
    state: BrowserState,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

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
                    Text("导入/导出", fontSize = 17.sp, fontWeight = FontWeight.Medium, color = Color(0xFF333333))
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                item { SectionTitle("书签") }

                item {
                    BookmarkExportItem(context)
                }
                item {
                    BookmarkImportItem(context)
                }

                item { SectionTitle("全部数据") }

                item {
                    AllExportItem(context, state)
                }
                item {
                    AllImportItem(context, state)
                }
            }
        }
    }
}

@Composable
private fun BookmarkExportItem(context: Context) {
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val json = exportBookmarksOnly()
                val uriData = json.toByteArray(Charsets.UTF_8)
                context.contentResolver.openOutputStream(uri)?.use { it.write(uriData) }
            }
        }
    }
    SettingsItem(
        icon = Icons.Outlined.FileUpload,
        title = "导出书签",
        subtitle = "仅导出书签到 JSON 文件",
        onClick = { exportLauncher.launch("gua_bookmarks.json") }
    )
}

@Composable
private fun BookmarkImportItem(context: Context) {
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val data = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (data != null) importBookmarksOnly(data)
            }
        }
    }
    SettingsItem(
        icon = Icons.Outlined.Download,
        title = "导入书签",
        subtitle = "从 JSON 文件恢复书签",
        onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) }
    )
}

@Composable
private fun AllExportItem(context: Context, state: BrowserState) {
    val scope = rememberCoroutineScope()
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val data = DataManager.exportToZip(state)
                DataManager.writeUriBytes(context, uri, data)
            }
        }
    }
    SettingsItem(
        icon = Icons.Outlined.FileUpload,
        title = "导出数据",
        subtitle = "书签、设置、搜索引擎",
        onClick = { exportLauncher.launch("gua_backup.zip") }
    )
}

@Composable
private fun AllImportItem(context: Context, state: BrowserState) {
    val scope = rememberCoroutineScope()
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                val data = DataManager.readUriBytes(context, uri)
                if (data != null) DataManager.importFromZip(data, state)
            }
        }
    }
    SettingsItem(
        icon = Icons.Outlined.Download,
        title = "导入数据",
        subtitle = "恢复书签和设置",
        onClick = { importLauncher.launch(arrayOf("application/zip")) }
    )
}

private suspend fun exportBookmarksOnly(): String {
    val app = GuaApp.instance
    val bookmarks = app.bookmarkManager.getAll()
    val arr = JSONArray()
    bookmarks.forEach { b ->
        arr.put(JSONObject().apply {
            put("title", b.title)
            put("url", b.url)
            put("position", b.position)
        })
    }
    return JSONObject().apply {
        put("version", 1)
        put("count", arr.length())
        put("bookmarks", arr)
    }.toString(2)
}

private suspend fun importBookmarksOnly(data: ByteArray) {
    val app = GuaApp.instance
    val json = data.toString(Charsets.UTF_8)
    val root = JSONObject(json)
    val bkArr = root.getJSONArray("bookmarks")
    for (i in 0 until bkArr.length()) {
        val obj = bkArr.getJSONObject(i)
        val url = obj.getString("url")
        if (!app.bookmarkManager.exists(url)) {
            app.bookmarkManager.add(
                com.gua.browser.bookmark.Bookmark(
                    title = obj.getString("title"),
                    url = url,
                    position = obj.optInt("position", 0)
                )
            )
        }
    }
}
