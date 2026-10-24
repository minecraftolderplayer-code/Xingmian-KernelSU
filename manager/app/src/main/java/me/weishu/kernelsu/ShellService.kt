package me.weishu.kernelsu

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.util.Log

class ShellService : Service() {

    // 自己内部实现白名单校验，不再依赖 PrivilegeManager
    private fun isAppAllowed(packageName: String): Boolean {
        val prefs = applicationContext.getSharedPreferences("su_allowed_apps", Context.MODE_PRIVATE)
        return prefs.getBoolean(packageName, false)
    }

    private fun setAppAllowed(packageName: String, allowed: Boolean) {
        val prefs = applicationContext.getSharedPreferences("su_allowed_apps", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(packageName, allowed).apply()
    }

    private val binder = object : IShellService.Stub() {
        override fun exec(command: String): String {
            val callingPackage = getCallingPackageName()
            
            // 核心安全校验
            if (callingPackage == null || !isAppAllowed(callingPackage)) {
                Log.w("ShellService", "拒绝未授权应用 $callingPackage 执行命令")
                return "Error: Permission denied"
            }

            Log.d("ShellService", "应用 $callingPackage 请求执行: $command")
            val result = PrivilegeManager.exec(command)
            return result.joinToString("\n")
        }

        override fun checkPermission(packageName: String): Boolean {
            return isAppAllowed(packageName)
        }

        override fun requestPermission(packageName: String) {
            Log.d("ShellService", "应用 $packageName 请求授权")
            // 此处可以进一步实现：如果收到请求，去拉起一个弹窗
        }
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    private fun getCallingPackageName(): String? {
        val uid = android.os.Binder.getCallingUid()
        if (uid == android.os.Process.myUid()) {
            return packageName // 自己调用自己
        }
        val pm = packageManager
        return pm.getPackagesForUid(uid)?.firstOrNull()
    }
}
