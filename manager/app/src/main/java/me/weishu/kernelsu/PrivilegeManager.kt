package me.weishu.kernelsu

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.topjohnwu.superuser.Shell
import dadb.AdbKeyPair
import dadb.Dadb
import rikka.shizuku.Shizuku

object PrivilegeManager {
    enum class Mode { ROOT, SHIZUKU, LOCAL_ADB, NONE }

    var currentMode = Mode.NONE
        private set

    private var dadbInstance: Dadb? = null
    private var cachedKeyPair: AdbKeyPair? = null

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

        // 2. 尝试本地 ADB 桥接 (dadb)
        try {
            if (cachedKeyPair == null) {
                // 纯内存生成密钥对，不依赖任何文件！
                cachedKeyPair = AdbKeyPair.generate()
            }
            
            Log.d("PrivilegeManager", "正在尝试连接 127.0.0.1:5555 ...")
            // dadb 的标准连接方式
            val adb = Dadb.create("127.0.0.1", 5555, cachedKeyPair!!)
            val result = adb.shell("id").output
            
            if (result.contains("uid=2000") || result.contains("uid=0")) {
                currentMode = Mode.LOCAL_ADB
                dadbInstance = adb
                Log.d("PrivilegeManager", "本地 ADB 桥接成功！")
                return
            }
        } catch (e: Exception) {
            Log.e("PrivilegeManager", "本地 ADB 连接失败: ${e.message}")
        }

        // 3. 检测 Shizuku 作为备用
        try {
            if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                currentMode = Mode.SHIZUKU
                Log.d("PrivilegeManager", "Shizuku 授权成功")
                return
            }
        } catch (e: Exception) { }

        Log.e("PrivilegeManager", "没有任何可用的特权通道")
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
                    if (dadbInstance == null) {
                        if (cachedKeyPair == null) cachedKeyPair = AdbKeyPair.generate()
                        dadbInstance = Dadb.create("127.0.0.1", 5555, cachedKeyPair!!)
                    }
                    dadbInstance?.shell(command)?.output?.split("\n") ?: emptyList()
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
}
