package me.weishu.kernelsu

import android.content.Context
import java.io.File

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
}