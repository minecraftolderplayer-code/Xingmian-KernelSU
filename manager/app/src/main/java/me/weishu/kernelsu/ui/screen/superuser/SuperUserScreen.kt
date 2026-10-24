package me.weishu.kernelsu.ui.screen.superuser

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.*

@Composable
fun SuperUserPager(
    bottomInnerPadding: Dp,
    isCurrentPage: Boolean = true,
) {
    var showDialog by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf("") }

    // 模拟几个请求过 Root 权限的应用（实际上我们并没有给权限）
    val fakeApps = listOf(
        "微信 (com.tencent.mm)" to "已拒绝",
        "MT管理器 (bin.mt.plus)" to "已允许",
        "核心破解 (com.example.core)" to "已允许"
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Root 授权请求") },
            text = { Text("应用 $selectedApp 正在请求超级用户权限。\n\n由于当前处于 Shizuku/无 Root 模式，该请求将被拦截，仅做模拟放行。") },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("允许 (模拟)")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("拒绝")
                }
            }
        )
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
            items(fakeApps.size) { index ->
                val (appName, status) = fakeApps[index]
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedApp = appName
                            showDialog = true
                        },
                    colors = CardDefaults.defaultColors(color = top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(appName, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = status,
                            fontSize = 14.sp,
                            color = if (status == "已允许") Color(0xFF4CAF50) else Color.Red
                        )
                    }
                }
            }
        }
    }
}
