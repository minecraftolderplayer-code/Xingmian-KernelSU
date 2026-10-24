package me.weishu.kernelsu

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.topjohnwu.superuser.Shell
import dadb.Dadb
import dadb.AdbKeyPair
import rikka.shizuku.Shizuku
import java.io.File

object PrivilegeManager {
    enum class Mode { ROOT, SHIZUKU, LOCAL_ADB, NONE }
    
    var currentMode = Mode.NONE
        private set

    // 全局单例，防止频繁连接导致 ADB 拒绝服务
    private var dadbInstance: Dadb? = null
    private var adbKeyPair: AdbKeyPair? = null

    fun detectAndInit(context: Context) {
        currentMode = Mode.NONE

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
            val keyDir = File(context.filesDir, "adbkeys")
            if (!keyDir.exists()) keyDir.mkdirs()
            val privateKey = File(keyDir, "adbkey")
            val publicKey = File(keyDir, "adbkey.pub")

            if (!privateKey.exists() || !publicKey.exists()) {
                Log.d("PrivilegeManager", "生成新的 ADB 密钥...")
                val keyPair = AdbKeyPair.generate()
                AdbKeyPair.write(privateKey, publicKey, keyPair)
            }
            adbKeyPair = AdbKeyPair.read(privateKey, publicKey)

            Log.d("PrivilegeManager", "正在尝试连接 127.0.0.1:5555 ...")
            // 连接本机 5555 端口
            dadbInstance = Dadb.create("127.0.0.1", 5555, adbKeyPair)
            val shellResult = dadbInstance?.shell("id")
            
            val output = shellResult?.output ?: ""
            if (output.contains("uid=2000") || output.contains("uid=0")) {
                currentMode = Mode.LOCAL_ADB
                Log.d("PrivilegeManager", "本地 ADB 桥接成功！")
                return
            }
        } catch (e: Exception) {
            Log.e("PrivilegeManager", "本地 ADB 连接失败: ${e.message}")
            dadbInstance = null
        }

        // 3. 作为备用，检测 Shizuku
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                currentMode = Mode.SHIZUKU
                Log.d("PrivilegeManager", "Shizuku 授权成功")
                return
            }
        } catch (e: Exception) { }
    }

    // 统一执行命令接口
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
                    // 使用已有的 dadb 实例执行
                    if (dadbInstance == null) return listOf("Error: ADB 未连接")
                    val shellResult = dadbInstance?.shell(command)
                    shellResult?.output?.split("\n") ?: emptyList()
                }
                
                else -> {
                    // 降级到本进程执行
                    val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
                    val output = process.inputStream.bufferedReader().readLines()
                    process.waitFor()
                    output
                }
            }
        } catch (e: Exception) {
            listOf("Error: ${e.message}")
        }
    }

    fun requestShizukuPermission() {
        try {
            if (currentMode != Mode.ROOT && Shizuku.pingBinder() && Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1001)
            }
        } catch (e: Exception) { }
    }
}
