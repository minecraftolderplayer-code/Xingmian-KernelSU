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

    private var dadbInstance: Dadb? = null
    private var adbKeyPair: AdbKeyPair? = null

    fun detectAndInit(context: Context) {
        currentMode = Mode.NONE

        // 1. 优先检测 Root
        try {
            if (Shell.getShell().isRoot) {
                currentMode = Mode.ROOT
                Log.d("PrivilegeManager", "检测到真实 Root")
                return
            }
        } catch (e: Exception) { }

        // 2. 尝试本地 ADB 桥接 (dadb)
        try {
            val keyDir = File(context.filesDir, "adbkeys")
            if (!keyDir.exists()) keyDir.mkdirs()
            val privateKey = File(keyDir, "adbkey")
            val publicKey = File(keyDir, "adbkey.pub")

            // 如果私钥存在，直接读取；如果不存在，利用 dadb 默认的自动生成能力建立连接
            if (privateKey.exists() && publicKey.exists()) {
                adbKeyPair = AdbKeyPair.read(privateKey, publicKey)
                dadbInstance = Dadb.create("127.0.0.1", 5555, adbKeyPair)
            } else {
                dadbInstance = Dadb.create("127.0.0.1", 5555)
            }

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

        // 3. 检测 Shizuku 作为备用
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                currentMode = Mode.SHIZUKU
                Log.d("PrivilegeManager", "Shizuku 授权成功")
                return
            }
        } catch (e: Exception) { }
    }

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
                    if (dadbInstance == null) return listOf("Error: ADB 未连接")
                    dadbInstance?.shell(command)?.output?.split("\n") ?: emptyList()
                }
                else -> listOf("Error: 没有可用权限通道")
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
