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
import java.util.zip.ZipInputStream

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
            .statusBarsPadding() // 👈 完美避开状态栏撞车！
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
                                // 👇 这是一个 GitHub 上真实的 KernelSU 模块测试包（体积小） 👇
                                val realModuleUrl = "https://github.com/ZLAsl/ksu_test_module/releases/download/1.0/ksu_test_module.zip"
                                
                                val success = downloadAndUnzipModule(context, realModuleUrl)
                                withContext(Dispatchers.Main) {
                                    isDownloading = false
                                    if (success) {
                                        Toast.makeText(context, "下载并解压成功！", Toast.LENGTH_SHORT).show()
                                        myModules = MyModuleManager.getLocalModules(context)
                                        showStore = false 
                                    } else {
                                        Toast.makeText(context, "下载失败，请检查网络或链接", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        colors = CardDefaults.defaultColors(color = colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(if (isDownloading) "正在下载并解压..." else "ADB 无线调试增强模块", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            Text("点击下载并自动安装", fontSize = 14.sp, color = Color.Gray)
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

// 真实的下载、处理重定向、并解压 ZIP 的逻辑
fun downloadAndUnzipModule(context: android.content.Context, urlStr: String): Boolean {
    return try {
        val tempFile = File(context.cacheDir, "temp_module.zip")
        var currentUrl = urlStr
        var redirects = 5
        var connection: HttpURLConnection

        // 1. 处理 GitHub 的 302 重定向下载
        while (redirects > 0) {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            val status = connection.responseCode
            if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 301 || status == 302 || status == 307 || status == 308) {
                currentUrl = connection.getHeaderField("Location")
                redirects--
            } else if (status == HttpURLConnection.HTTP_OK) {
                connection.inputStream.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                break
            } else {
                return false
            }
        }

        if (!tempFile.exists() || tempFile.length() == 0L) return false

        // 2. 解压到沙盒目录（解压后 MyModuleManager 才能读到 module.prop）
        val targetDir = File(context.filesDir, "modules/temp_extract")
        if (targetDir.exists()) targetDir.deleteRecursively()
        targetDir.mkdirs()

        ZipInputStream(tempFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                }
                entry = zis.nextEntry
            }
        }
        
        // 3. 检查 module.prop，如果有效，重命名文件夹
        val propFile = File(targetDir, "module.prop")
        if (propFile.exists()) {
            // 简单解析一下 id
            var id = "downloaded_module"
            propFile.forEachLine {
                if (it.startsWith("id=")) {
                    id = it.substringAfter("=").trim()
                }
            }
            val finalDir = File(context.filesDir, "modules/$id")
            if (finalDir.exists()) finalDir.deleteRecursively()
            targetDir.renameTo(finalDir)
            true
        } else {
            targetDir.deleteRecursively()
            false
        }
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}
