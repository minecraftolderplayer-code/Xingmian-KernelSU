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
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.PrivilegeManager
import me.weishu.kernelsu.R
import me.weishu.kernelsu.ui.component.WarningLevel
import me.weishu.kernelsu.ui.component.dialog.rememberConfirmDialog
import me.weishu.kernelsu.ui.component.miuix.WarningCard
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

// 注意：入口函数名保持 HomePagerMiuix，以匹配官方调用
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
    // 使用 Compose 的状态来触发提示
    var showLoading by remember { mutableStateOf(false) }
    var resultMessage by remember { mutableStateOf<String?>(null) }

    // 监听检测结果并触发 UI 提示
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
            val workingText = stringResource(R.string.home_working) + (if (state.isSafeMode) " [安全模式]" else "")

            Row(modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min), verticalAlignment = Alignment.CenterVertically) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.defaultColors(
                        color = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFFEA)
                    ),
                    onClick = { if (!showLoading) showLoading = true },
                    showIndication = !state.isLateLoadMode,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    Box(modifier = Modifier.fillMaxSize().offset(27.dp, 31.dp), contentAlignment = Alignment.BottomEnd) {
                        Icon(Modifier.size(100.dp), Icons.Rounded.CheckCircleOutline, null, tint = Color(0xFF3D6D7))
                    }
                    if (workingMode != null) {
                        Box(modifier = Modifier.fillMaxSize().padding(16.dp, 18.dp), contentAlignment = Alignment.BottomStart) {
                            Text(workingMode, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp, 14.dp), contentAlignment = Alignment.TopStart) {
                        Column {
                            Text(workingText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(stringResource(R.string.home_working_version, "${state.ksuVersion}-${state.kernelUAPIVersion}"), fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        } else {
            Card(onClick = actions.onInstallClick, showIndication = true, pressFeedbackType = PressFeedbackType.Tilt) {
                BasicComponent(
                    title = stringResource(R.string.home_not_installed),
                    summary = stringResource(R.string.home_click_to_install),
                    startAction = { Icon(Icons.Rounded.ErrorOutline, null, Modifier.padding(end = 6.dp)) }
                )
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

@Composable
private fun SupportLinks(onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            startAction = { Icon(Icons.Filled.VolunteerActivism, null, Modifier.padding(end = 6.dp)) },
            onClick = { onOpenUrl("https://patreon.com/weishu") }
        )
        ArrowPreference(
            title = stringResource(R.string.home_learn_kernelsu),
            summary = stringResource(R.string.home_click_to_learn_kernelsu),
            startAction = { Icon(Icons.AutoMirrored.Filled.MenuBook, null, Modifier.padding(end = 6.dp)) },
            onClick = { onOpenUrl(stringResource(R.string.home_learn_kernelsu_url)) }
        )
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
        Icon(icon, title, Modifier.padding(end = 12.dp).size(24.dp), tint = colorScheme.onSurface)
        Column {
            Text(title, fontSize = MiuixTheme.textStyles.headline1.fontSize, fontWeight = FontWeight.Medium)
            Text(content, fontSize = MiuixTheme.textStyles.body2.fontSize, color = colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
