package me.weishu.kernelsu

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

data class MyModuleInfo(val id: String, val name: String, val version: String)

object MyModuleManager {
    fun getLocalModules(context: Context): List<MyModuleInfo> {
        val list = mutableListOf<MyModuleInfo>()
        val dir = File(context.filesDir, "modules")
        if (!dir.exists()) return list
        
        dir.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                val propFile = File(file, "module.prop")
                if (propFile.exists()) {
                    val props = mutableMapOf<String, String>()
                    propFile.forEachLine {
                        val parts = it.split("=", limit = 2)
                        if (parts.size == 2) props[parts[0].trim()] = parts[1].trim()
                    }
                    list.add(MyModuleInfo(
                        id = props["id"] ?: file.name,
                        name = props["name"] ?: "未知模块",
                        version = props["version"] ?: "1.0"
                    ))
                }
            }
        }
        return list
    }

    // 新增：处理从本地选择的 ZIP 文件
    fun importLocalModule(context: Context, uri: Uri): Boolean {
        return try {
            val targetDir = File(context.filesDir, "modules/temp_extract")
            if (targetDir.exists()) targetDir.deleteRecursively()
            targetDir.mkdirs()

            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input).use { zis ->
                    var entry = zis.nextEntry
                    while (entry != null) {
                        val outFile = File(targetDir, entry.name)
                        // 防止 Zip Slip 漏洞
                        if (!outFile.canonicalPath.startsWith(targetDir.canonicalPath + File.separator)) {
                            entry = zis.nextEntry
                            continue
                        }
                        if (entry.isDirectory) {
                            outFile.mkdirs()
                        } else {
                            outFile.parentFile?.mkdirs()
                            FileOutputStream(outFile).use { fos -> zis.copyTo(fos) }
                        }
                        entry = zis.nextEntry
                    }
                }
            }

            val propFile = File(targetDir, "module.prop")
            if (propFile.exists()) {
                var id = "imported_module"
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
}
