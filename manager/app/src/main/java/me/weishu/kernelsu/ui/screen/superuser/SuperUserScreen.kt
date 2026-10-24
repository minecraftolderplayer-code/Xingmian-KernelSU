package me.weishu.kernelsu.ui.screen.superuser

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

// 读取真实已安装应用
data class RealAppInfo(val name: String, val packageName: String)

@Composable
fun SuperUserPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("su_allowed_apps", Context.MODE_PRIVATE) }
    
    // 真实扫描手机应用
    val installedApps = remember {
        val pm = context.packageManager
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map {
                RealAppInfo(
                    name = pm.getApplicationLabel(it).toString(),
                    packageName = it.packageName
                )
            }
            .sortedBy { it.name }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(bottom = bottomInnerPadding)
    ) {
        Text(
            text = "超级用户",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(installedApps.size) { index ->
                val app = installedApps[index]
                
                // 每次重组时读取真实的 SharedPreferences 状态
                var isChecked by remember { mutableStateOf(prefs.getBoolean(app.packageName, false)) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(color = colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(app.name, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            Text(app.packageName, fontSize = 12.sp, color = Color.Gray)
                        }
                        Switch(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                isChecked = checked
                                // 真实写入：如果开启，ShellService 就会给它放行！
                                prefs.edit().putBoolean(app.packageName, checked).apply()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4CAF50)
                            )
                        )
                    }
                }
            }
        }
    }
}
