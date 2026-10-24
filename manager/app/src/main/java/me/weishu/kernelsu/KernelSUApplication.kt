package me.weishu.kernelsu

import android.app.Application
import android.content.pm.ApplicationInfo
import android.os.Build
import android.os.UserManager
import android.system.Os
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import me.weishu.kernelsu.data.repository.SettingsRepositoryImpl
import me.weishu.kernelsu.ui.viewmodel.SuperUserViewModel
import okhttp3.Cache
import okhttp3.OkHttpClient
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.io.File
import java.util.Locale

lateinit var ksuApp: KernelSUApplication

class KernelSUApplication : Application(), ViewModelStoreOwner {

    companion object {
        fun setEnableOnBackInvokedCallback(appInfo: ApplicationInfo, enable: Boolean) {
            runCatching {
                val applicationInfoClass = ApplicationInfo::class.java
                val method = applicationInfoClass.getDeclaredMethod("setEnableOnBackInvokedCallback", Boolean::class.javaPrimitiveType)
                method.isAccessible = true
                method.invoke(appInfo, enable)
            }
        }
    }

    lateinit var okHttpClient: OkHttpClient
    private val appViewModelStore by lazy { ViewModelStore() }

    private fun isUserUnlocked(): Boolean =
        getSystemService(UserManager::class.java)?.isUserUnlocked == true

    override fun onCreate() {
        super.onCreate()
        ksuApp = this

        // 👇 核心：启动后台线程，延迟 2 秒去连本地 ADB 👇
        Thread {
            try {
                Thread.sleep(2000)
                PrivilegeManager.detectAndInit(applicationContext)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
        // 👆 插入结束，下面的官方代码完美保留 👆

        if (!isUserUnlocked()) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val enable = SettingsRepositoryImpl().enablePredictiveBack
            HiddenApiBypass.addHiddenApiExemptions("Landroid/content/pm/ApplicationInfo;->setEnableOnBackInvokedCallback")
            setEnableOnBackInvokedCallback(applicationInfo, enable)
        }

        val superUserViewModel = ViewModelProvider(this)[SuperUserViewModel::class.java]
        superUserViewModel.loadAppList()

        val webroot = File(dataDir, "webroot")
        if (!webroot.exists()) {
            webroot.mkdir()
        }

        // Provide working env for rust's temp_dir()
        Os.setenv("TMPDIR", cacheDir.absolutePath, true)

        okhttpClient =
            OkHttpClient.Builder().cache(Cache(File(cacheDir, "okhttp"), 10 * 1024 * 1024))
                .addInterceptor { block ->
                    block.proceed(
                        block.request().newBuilder()
                            .header("User-Agent", "KernelSU/${BuildConfig.VERSION_CODE}")
                            .header("Accept-Language", Locale.getDefault().toLanguageTag()).build()
                    )
                }.build()
    }

    override val viewModelStore: ViewModelStore
        get() = appViewModelStore
}
