package me.weishu.kernelsu.ui.screen.module

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.MyModuleManager
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun ModulePager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var myModules by remember { mutableStateOf(MyModuleManager.getLocalModules(context)) }
    var showStore by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // 关键点：处理底栏的边距，防止吃到底部导航栏
            .padding(bottom = bottomInnerPadding) 
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showStore) "ADB 模块仓库" else "已安装模块",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            TextButton(
                text = if (showStore) "返回本地" else "模块仓库",
                onClick = { showStore = !showStore }
            )
        }

        if (showStore) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable {
                            if (isDownloading) return@clickable
                            isDownloading = true
                            scope.launch(Dispatchers.IO) {
                                // 换成你的直链
                                val adbModuleUrl = "https://github.com/你的仓库/raw/main/adb_module.zip"
                                val success = downloadModule(context, adbModuleUrl)
                                withContext(Dispatchers.Main) {
                                    isDownloading = false
                                    if (success) {
                                        Toast.makeText(context, "下载成功！", Toast.LENGTH_SHORT).show()
                                        myModules = MyModuleManager.getLocalModules(context)
                                        showStore = false 
                                    } else {
                                        Toast.makeText(context, "下载失败", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = CardDefaults.defaultColors(color = colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (isDownloading) "正在下载中..." else "ADB 无线调试增强模块", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("点击下载并解压", fontSize = 14.sp, color = Color.Gray)
                        }
                    }
                }
            }
        } else {
            if (myModules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无模块，请去仓库下载", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(myModules.size) { index ->
                        val module = myModules[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.defaultColors(color = colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(module.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                Text("v${module.version}", fontSize = 14.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun downloadModule(context: android.content.Context, urlStr: String): Boolean {
    return try {
        val url = URL(urlStr)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.connect()

        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val tempFile = File(context.cacheDir, "temp_module.zip")
            connection.inputStream.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            val targetDir = File(context.filesDir, "modules/adb_module")
            if (targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()
            
            val outZip = File(targetDir, "module.zip")
            tempFile.copyTo(outZip, overwrite = true)
            true
        } else {
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
