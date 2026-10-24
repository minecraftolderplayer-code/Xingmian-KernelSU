package me.weishu.kernelsu

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.topjohnwu.superuser.Shell
import dadb.Adb
import dadb.AdbKeyPair
import rikka.shizuku.Shizuku
import java.io.File

object PrivilegeManager {
    // 确保这里有 LOCAL_ADB 枚举
    enum class Mode { ROOT, SHIZUKU, LOCAL_ADB, NONE }
    
    var currentMode = Mode.NONE
        private set

    // 初始化检测逻辑（点击主页卡片时会触发）
    fun detectAndInit(context: Context) {
        // 1. 优先检测真 Root
        try {
            if (Shell.getShell().isRoot) {
                currentMode = Mode.ROOT
                Log.d("PrivilegeManager", "检测到真实 Root")
                return
            }
        } catch (e: Exception) { }

        // 2. 尝试自己连本机无线 ADB (dadb 桥接)
        try {
            val keyFile = File(context.filesDir, "adbkey")
            if (!keyFile.exists()) {
                // 如果没有密钥，生成一个并保存
                val keyPair = AdbKeyPair.generate()
                AdbKeyPair.write(keyFile, null, keyPair)
            }
            val keyPair = AdbKeyPair.read(keyFile, null)
            
            // 连接本机 5555 端口（要求用户手动开启无线调试并 tcpip 5555）
            val adb = Adb.createTcpAdb(keyPair, "127.0.0.1", 5555)
            val idResult = adb.shell("id")
            
            if (idResult.contains("uid=2000") || idResult.contains("uid=0")) {
                currentMode = Mode.LOCAL_ADB
                Log.d("PrivilegeManager", "本地 ADB 桥接成功！")
                return
            }
        } catch (e: Exception) {
            Log.e("PrivilegeManager", "本地 ADB 连接失败: ${e.message}")
        }

        // 3. 作为备用，检测 Shizuku
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                currentMode = Mode.SHIZUKU
                Log.d("PrivilegeManager", "Shizuku 授权成功")
                return
            }
        } catch (e: Exception) { }

        currentMode = Mode.NONE
        Log.e("PrivilegeManager", "没有任何可用的特权通道")
    }

    // 统一的执行命令接口
    fun exec(command: String): List<String> {
        return try {
            when (currentMode) {
                Mode.ROOT -> Shell.cmd(command).exec().out
                
                Mode.SHIZUKU -> {
                    val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
                    val output = process.inputStream.bufferedReader().readLines()
                    process.waitFor()
                    output
                }
                
                Mode.LOCAL_ADB -> {
                    // 通过我们内嵌的 dadb 客户端执行
                    val context = KernelSUApplication.ksuApp
                    val keyFile = File(context.filesDir, "adbkey")
                    val keyPair = AdbKeyPair.read(keyFile, null)
                    val adb = Adb.createTcpAdb(keyPair, "127.0.0.1", 5555)
                    adb.shell(command).split("\n")
                }
                
                else -> listOf("Error: 没有可用权限。请检查 Root 或开启 5555 端口。")
            }
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }

    // 供外部调用的请求授权（针对 Shizuku 备用）
    fun requestShizukuPermission() {
        try {
            if (currentMode != Mode.ROOT && Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Exception) { }
    }
}
