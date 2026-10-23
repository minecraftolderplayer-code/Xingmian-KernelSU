package me.weishu.kernelsu

import android.content.Context
import android.content.pm.PackageManager
import com.topjohnwu.superuser.Shell
import rikka.shizuku.Shizuku

object PrivilegeManager {
    enum class Mode { ROOT, SHIZUKU, NONE }
    
    // 记录当前处于什么模式
    var currentMode = Mode.NONE
        private set

    // 初始化检测逻辑
    fun detectAndInit(context: Context) {
        // 1. 优先检测 Root
        try {
            if (Shell.getShell().isRoot) {
                currentMode = Mode.ROOT
                return
            }
        } catch (e: Exception) { }

        // 2. 检测 Shizuku
        try {
            if (Shizuku.pingBinder()) {
                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                    currentMode = Mode.SHIZUKU
                } else {
                    // 尝试隐秘拉起授权
                    requestShizukuPermission { granted ->
                        if (granted) currentMode = Mode.SHIZUKU
                    }
                }
                return
            }
        } catch (e: Exception) { }

        currentMode = Mode.NONE
    }

    // 统一的命令执行接口（无论是Root还是Shizuku，以后模块都走这里）
    fun exec(command: String): List<String> {
        return try {
            when (currentMode) {
                Mode.ROOT -> Shell.cmd(command).exec().out
                Mode.SHIZUKU -> {
                    val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                    val reader = process.inputStream.bufferedReader()
                    val output = reader.readLines()
                    process.waitFor()
                    output
                }
                else -> emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun requestShizukuPermission(callback: (Boolean) -> Unit) {
        try {
            Shizuku.addRequestPermissionResultListener(object : Shizuku.OnRequestPermissionResultListener {
                override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
                    Shizuku.removeRequestPermissionResultListener(this)
                    callback(grantResult == PackageManager.PERMISSION_GRANTED)
                }
            })
            Shizuku.requestPermission(1001)
        } catch (e: Exception) {
            callback(false)
        }
    }
}