package me.weishu.kernelsu.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.PrivilegeManager
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.WarningLevel
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.miuix.WarningCard
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun HomePagerMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp
) {
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            TopAppBar(
                scrollBehavior = scrollBehavior,
                color = colorScheme.surface,
                title = stringResource(id = R.string.app_name)
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.systemBars.add(WindowInsets.displayCutout).only(WindowInsetsSides.Top)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier.padding(top = 12.dp).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.checkUpdateEnabled) UpdateCard(state, actions)
                    if (state.showRootWarning) WarningCard(stringResource(id = R.string.grant_root_failed))
                    
                    StatusCard(state, actions)
                    InfoCard(state.systemInfo, Modifier.fillMaxWidth())
                    SupportLinks(actions.onOpenUrl, Modifier.fillMaxWidth())
                    
                    Spacer(modifier = Modifier.height(bottomInnerPadding + 16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState, actions: HomeActions) {
    val context = LocalContext.current
    var showLoading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(showLoading) {
        if (showLoading) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                PrivilegeManager.detectAndInit(context)
            }
            val mode = PrivilegeManager.currentMode
            resultMessage = when (mode) {
                PrivilegeManager.Mode.ROOT -> "Root 权限已激活"
                PrivilegeManager.Mode.SHIZUKU -> "Shizuku 授权成功"
                PrivilegeManager.Mode.LOCAL_ADB -> "本地 ADB 桥接成功"
                else -> "连接失败，请检查 5555 端口是否开启"
            }
            showLoading = false
        }
    }

    LaunchedEffect(resultMessage) {
        resultMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            resultMessage = null
        }
    }

    Column {
        if (state.ksuVersion != null) {
            val workingMode = if (state.lkmMode == null) null else if (state.lkmMode) "LKM" else "GKI"
            val workingText = stringResource(R.string.home_working)

            // 全部改用原生 Material3 的 Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFFEA)
                ),
                onClick = { if (!showLoading) showLoading = true }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = Color(0xFF3D6D7)
                        )
                    }
                    Column(modifier = Modifier.padding(20.dp)) {
                        if (workingMode != null) {
                            Text(workingMode, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.Gray)
                            Spacer(modifier = Modifier.height(4.dp))
                        }
                        Text(workingText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(stringResource(R.string.home_working_version, "${state.ksuVersion}-${state.kernelUAPIVersion}"), fontSize = 14.sp)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                onClick = actions.onInstallClick
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.ErrorOutline, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(stringResource(R.string.home_not_installed), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.home_click_to_install), fontSize = 12.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(state: HomeUiState, actions: HomeActions) {
    val newVersion = state.latestVersionInfo
    val updateDialog = rememberConfirmDialog(onConfirm = { actions.onOpenUrl(newVersion.downloadUrl) })
    AnimatedVisibility(state.hasUpdate, enter = fadeIn() + expandVertically(), exit = shrinkVertically() + fadeOut()) {
        WarningCard(
            message = stringResource(id = R.string.new_version_available, newVersion.versionCode),
            level = WarningLevel.Notice,
            onClick = {
                if (newVersion.changelog.isEmpty()) actions.onOpenUrl(newVersion.downloadUrl)
                else updateDialog.showConfirm(title = stringResource(R.string.module_changelog), content = newVersion.changelog, markdown = true)
            }
        )
    }
}

// 彻底抛弃 Miuix 的 ArrowPreference，改用原生 Card
@Composable
private fun SupportLinks(onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
                Icon(Icons.Filled.VolunteerActivism, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.home_support_title), fontWeight = FontWeight.Medium)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, tint = Color.Gray)
                Spacer(modifier = Modifier.width(12.dp))
                Text(stringResource(R.string.home_learn_kernelsu), fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InfoCard(systemInfo: SystemInfo, modifier: Modifier = Modifier) {
    val selinux = if (systemInfo.selinuxStatus == "Enforcing") "强制" else "宽容"
    val seccomp = if (systemInfo.seccompStatus == 1) "已支持" else "已禁用"

    Column(modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.Tag, stringResource(R.string.home_manager_version), systemInfo.managerVersion)
                InfoText(Icons.Filled.DeveloperBoard, stringResource(R.string.home_kernel), systemInfo.kernelVersion)
                InfoText(Icons.Filled.Smartphone, stringResource(R.string.home_device_model), systemInfo.deviceModel)
                InfoText(Icons.Filled.Fingerprint, stringResource(R.string.home_fingerprint), systemInfo.fingerprint, 0.dp)
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.Security, stringResource(R.string.home_selinux_status), selinux)
                InfoText(Icons.Filled.FilterList, stringResource(R.string.home_seccomp_status), seccomp, 0.dp)
            }
        }
    }
}

@Composable
private fun InfoText(icon: ImageVector, title: String, content: String, bottomPadding: Dp = 24.dp) {
    Row(Modifier.fillMaxWidth().padding(bottom = bottomPadding), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = title, modifier = Modifier.padding(end = 12.dp).size(24.dp), tint = Color.Gray)
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(content, fontSize = 12.sp, color = Color.Gray, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
