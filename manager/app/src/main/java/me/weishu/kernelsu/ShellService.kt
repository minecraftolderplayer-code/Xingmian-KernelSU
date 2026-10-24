package me.weishu.kernelsu

import android.app.Service
import android.content.Intent
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log

class ShellService : Service() {

    // 在这里实例化我们的 Binder 服务端（由 AIDL 自动生成）
    private val binder = object : IShellService.Stub() {
        override fun exec(command: String): String {
            // 核心安全校验：只有白名单里的 App 才能执行命令
            val callingPackage = getCallingPackageName()
            if (callingPackage == null || !PrivilegeManager.isAppAllowed(callingPackage)) {
                Log.w("ShellService", "拒绝未授权应用 $callingPackage 执行命令")
                return "Error: Permission denied"
            }

            // 验证通过，通过底层的 Shizuku/Root 引擎执行
            Log.d("ShellService", "应用 $callingPackage 请求执行: $command")
            val result = PrivilegeManager.exec(command)
            return result.joinToString("\n")
        }

        override fun checkPermission(packageName: String): Boolean {
            return PrivilegeManager.isAppAllowed(packageName)
        }

        override fun requestPermission(packageName: String) {
            // 这里可以触发一个弹窗，但为了后台静默，我们先简单记录日志
            Log.d("ShellService", "应用 $packageName 请求授权")
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    // 获取调用当前 Binder 的 App 包名（用于安全判断）
    private fun getCallingPackageName(): String? {
        val uid = android.os.Binder.getCallingUid()
        if (uid == android.os.Process.myUid()) {
            return packageName // 自己调用自己
        }
        val pm = packageManager
        return pm.getPackagesForUid(uid)?.firstOrNull()
    }
}
