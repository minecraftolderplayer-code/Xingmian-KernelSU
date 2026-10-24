package me.weishu.kernelsu.ui.screen.module

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.weishu.kernelsu.MyModuleManager
import me.weishu.kernelsu.PrivilegeManager
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.ZipInputStream

data class RepoModule(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val zipUrl: String
)

val adbModulesRepo = listOf(
    RepoModule(
        id = "autotcp",
        name = "AutoTCP 无线调试增强",
        version = "1.2",
        description = "自动在后台保持 ADB 的 TCP 5555 端口开启，防止无线调试断开。",
        zipUrl = "https://github.com/lowji194/Module-Magisk/raw/refs/heads/main/AutoTCP.zip"
    ),
    RepoModule(
        id = "adb_enable",
        name = "Auto ADB 强制唤醒",
        version = "1.1",
        description = "无需每次手动配置，开机自动尝试通过 Shell 激活 ADB 调试服务。",
        zipUrl = "https://github.com/lowji194/Module-Magisk/raw/refs/heads/main/Auto_ADB_Enable.zip"
    )
)

@Composable
fun ModulePager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var localModules by remember { mutableStateOf(MyModuleManager.getLocalModules(context)) }
    var showStore by remember { mutableStateOf(false) }
    var downloadingId by remember { mutableStateOf<String?>(null) }

    // 本地导入 ZIP 选择器
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch(Dispatchers.IO) {
                val success = MyModuleManager.importLocalModule(context, uri)
                withContext(Dispatchers.Main) {
                    if (success) {
                        Toast.makeText(context, "导入成功", Toast.LENGTH_SHORT).show()
                        localModules = MyModuleManager.getLocalModules(context)
                        showStore = false
                    } else {
                        Toast.makeText(context, "导入失败：不是有效的模块", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
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
            Row {
                if (!showStore) {
                    TextButton(
                        text = "导入",
                        onClick = { filePicker.launch("application/zip") }
                    )
                }
                TextButton(
                    text = if (showStore) "返回本地" else "浏览仓库",
                    onClick = { showStore = !showStore }
                )
            }
        }

        if (showStore) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(adbModulesRepo.size) { index ->
                    val module = adbModulesRepo[index]
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.defaultColors(color = colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(module.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(text = "v${module.version} · ${module.id}", fontSize = 12.sp, color = Color.Gray)
                                Text(text = module.description, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
                            }
                            TextButton(
                                text = if (downloadingId == module.id) "安装中" else "下载",
                                onClick = {
                                    if (downloadingId != null) return@TextButton
                                    downloadingId = module.id
                                    scope.launch(Dispatchers.IO) {
                                        val success = downloadAndUnzipModule(context, module.zipUrl)
                                        withContext(Dispatchers.Main) {
                                            downloadingId = null
                                            if (success) {
                                                Toast.makeText(context, "${module.name} 安装成功", Toast.LENGTH_SHORT).show()
                                                localModules = MyModuleManager.getLocalModules(context)
                                                showStore = false
                                            } else {
                                                Toast.makeText(context, "下载失败，网络异常", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            if (localModules.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无模块，请导入或去仓库下载", color = Color.Gray)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(localModules.size) { index ->
                        val module = localModules[index]
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.defaultColors(color = colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(module.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                                    Text("v${module.version}", fontSize = 14.sp, color = Color.Gray)
                                }
                                TextButton(
                                    text = "启动",
                                    onClick = {
                                        scope.launch(Dispatchers.IO) {
                                            // 尝试去执行模块里的 service.sh 或 post-fs-data.sh
                                            val moduleDir = File(context.filesDir, "modules/${module.id}")
                                            val serviceSh = File(moduleDir, "service.sh")
                                            val postFsDataSh = File(moduleDir, "post-fs-data.sh")
                                            
                                            var cmd = ""
                                            if (serviceSh.exists()) {
                                                cmd = "sh ${serviceSh.absolutePath}"
                                            } else if (postFsDataSh.exists()) {
                                                cmd = "sh ${postFsDataSh.absolutePath}"
                                            } else {
                                                withContext(Dispatchers.Main) {
                                                    Toast.makeText(context, "此模块没有可执行脚本", Toast.LENGTH_SHORT).show()
                                                }
                                                return@launch
                                            }

                                            // 调用我们的双模式执行器！
                                            val result = PrivilegeManager.exec(cmd)
                                            withContext(Dispatchers.Main) {
                                                if (result.isNotEmpty()) {
                                                    Toast.makeText(context, "脚本已执行", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "执行失败或无输出，请检查权限", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// 下载与解压逻辑保持不变
fun downloadAndUnzipModule(context: android.content.Context, urlStr: String): Boolean {
    return try {
        val tempFile = File(context.cacheDir, "temp_module.zip")
        var currentUrl = urlStr
        var redirects = 5
        var connection: HttpURLConnection

        while (redirects > 0) {
            val url = URL(currentUrl)
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")
            
            val status = connection.responseCode
            if (status in listOf(301, 302, 307, 308)) {
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
        
        val propFile = File(targetDir, "module.prop")
        if (propFile.exists()) {
            var id = "downloaded_module"
            propFile.forEachLine {
                if (it.startsWith("id=")) id = it.substringAfter("=").trim()
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
