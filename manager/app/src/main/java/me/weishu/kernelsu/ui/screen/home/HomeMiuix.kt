package me.weishu.kernelsu.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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

@Composable
fun HomeMiuix(
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (state.checkUpdateEnabled) {
                        UpdateCard(state = state, actions = actions)
                    }
                    if (state.showRootWarning) {
                        WarningCard(stringResource(id = R.string.grant_root_failed))
                    }
                    
                    StatusCard(
                        state = state,
                        actions = actions
                    )
                    
                    InfoCard(
                        systemInfo = state.systemInfo,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    SupportLinks(
                        onOpenUrl = actions.onOpenUrl,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    
                    Spacer(
                        modifier = Modifier.height(
                            // 修复了这里的方法调用，加了 ()
                            bottomInnerPadding + if (Natives.isFullFeatured()) {
                                WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                            } else 0.dp
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun UpdateCard(
    state: HomeUiState,
    actions: HomeActions
) {
    val newVersion = state.latestVersionInfo
    val title = stringResource(id = R.string.module_changelog)
    val updateText = stringResource(id = R.string.module_update)
    
    // 修复了参数问题，直接传 onConfirm
    val updateDialog = rememberConfirmDialog(onConfirm = { actions.onOpenUrl(newVersion.downloadUrl) })

    AnimatedVisibility(
        visible = state.hasUpdate,
        enter = fadeIn() + expandVertically(),
        exit = shrinkVertically() + fadeOut()
    ) {
        WarningCard(
            message = stringResource(id = R.string.new_version_available, newVersion.versionCode),
            level = WarningLevel.Notice,
            onClick = {
                if (newVersion.changelog.isEmpty()) {
                    actions.onOpenUrl(newVersion.downloadUrl)
                } else {
                    updateDialog.showConfirm(
                        title = title,
                        content = newVersion.changelog,
                        markdown = true
                    )
                }
            }
        )
    }
}

@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    val context = LocalContext.current

    Column {
        when {
            state.ksuVersion != null -> {
                val workingState = buildString {
                    if (state.isSafeMode) append("${stringResource(id = R.string.safe_mode)} ")
                    if (state.isLateLoadMode) append("${stringResource(id = R.string.jailbreak_mode)} ")
                }
                val workingMode = when {
                    state.lkmMode == null -> null
                    state.lkmMode -> "LKM"
                    else -> "GKI"
                }
                val workingText = "${stringResource(R.string.home_working)}$workingState"

                Row(
                    modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.defaultColors(
                            color = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFFEA)
                        ),
                        onClick = {
                            android.widget.Toast.makeText(context, "正在尝试连接本地 ADB 桥接...", android.widget.Toast.LENGTH_SHORT).show()
                            Thread {
                                PrivilegeManager.detectAndInit(context)
                                val mode = PrivilegeManager.currentMode
                                android.os.Handler(android.os.Looper.getMainLooper()).post {
                                    when (mode) {
                                        PrivilegeManager.Mode.LOCAL_ADB -> android.widget.Toast.makeText(context, "本地 ADB 连接成功！", android.widget.Toast.LENGTH_SHORT).show()
                                        PrivilegeManager.Mode.ROOT -> android.widget.Toast.makeText(context, "Root 权限已激活！", android.widget.Toast.LENGTH_SHORT).show()
                                        PrivilegeManager.Mode.SHIZUKU -> android.widget.Toast.makeText(context, "Shizuku 授权成功！", android.widget.Toast.LENGTH_SHORT).show()
                                        else -> {
                                            android.widget.Toast.makeText(context, "连接失败，请检查是否开启了 5555 端口", android.widget.Toast.LENGTH_LONG).show()
                                            if (!state.isLateLoadMode) {
                                                actions.onInstallClick()
                                            }
                                        }
                                    }
                                }
                            }.start()
                        },
                        showIndication = !state.isLateLoadMode,
                        pressFeedbackType = PressFeedbackType.Tilt
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().offset(27.dp, 31.dp),
                            contentAlignment = Alignment.BottomEnd
                        ) {
                            Icon(
                                modifier = Modifier.size(100.dp),
                                imageVector = Icons.Rounded.CheckCircleOutline,
                                tint = Color(0xFF3D6D7),
                                contentDescription = null
                            )
                        }

                        if (workingMode != null) {
                            Box(
                                modifier = Modifier.fillMaxSize().padding(16.dp, 18.dp),
                                contentAlignment = Alignment.BottomStart
                            ) {
                                Text(text = workingMode, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                            }
                        }

                        Box(
                            modifier = Modifier.fillMaxSize().padding(16.dp, 14.dp),
                            contentAlignment = Alignment.TopStart
                        ) {
                            Column {
                                Text(text = workingText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = stringResource(R.string.home_working_version, "${state.ksuVersion}-${state.kernelUAPIVersion}"),
                                        modifier = Modifier.weight(1f, fill = false),
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
            state.kernelVersion.isGKI() -> {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Card(
                        modifier = Modifier.weight(1f),
                        onClick = { if (state.isLateLoadMode) actions.onInstallClick() },
                        showIndication = !state.isLateLoadMode,
                        pressFeedbackType = PressFeedbackType.Tilt
                    ) {
                        BasicComponent(
                            title = stringResource(R.string.home_not_installed),
                            summary = stringResource(R.string.home_click_to_install),
                            startAction = {
                                Icon(Icons.Rounded.ErrorOutline, stringResource(R.string.home_not_installed), Modifier.padding(end = 6.dp), tint = colorScheme.onBackground)
                            },
                            endActions = {
                                if (state.isSELinuxPermissive) {
                                    TextButton(text = stringResource(R.string.home_jailbreak), onClick = actions.onJailbreakClick, colors = ButtonDefaults.textButtonColorsPrimary())
                                }
                            }
                        )
                    }
                }
            }
            else -> {
                Card(
                    onClick = { if (state.isLateLoadMode) actions.onInstallClick() },
                    showIndication = !state.isLateLoadMode,
                    pressFeedbackType = PressFeedbackType.Tilt
                ) {
                    BasicComponent(
                        title = stringResource(R.string.home_unsupported),
                        summary = stringResource(R.string.home_unsupported_reason),
                        startAction = {
                            Icon(Icons.Rounded.ErrorOutline, stringResource(R.string.home_unsupported), Modifier.padding(end = 16.dp), tint = colorScheme.onBackground)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportLinks(onOpenUrl: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            startAction = { Icon(Icons.Filled.VolunteerActivism, stringResource(R.string.home_support_title), Modifier.padding(end = 6.dp), tint = colorScheme.onBackground) },
            onClick = { onOpenUrl("https://patreon.com/weishu") }
        )
        ArrowPreference(
            title = stringResource(R.string.home_learn_kernelsu),
            summary = stringResource(R.string.home_click_to_learn_kernelsu),
            startAction = { Icon(Icons.AutoMirrored.Filled.MenuBook, stringResource(R.string.home_learn_kernelsu), Modifier.padding(end = 6.dp), tint = colorScheme.onBackground) },
            onClick = { onOpenUrl(stringResource(R.string.home_learn_kernelsu_url)) }
        )
    }
}

@Composable
private fun InfoCard(systemInfo: SystemInfo, modifier: Modifier = Modifier) {
    val selinuxDisplay = when (systemInfo.selinuxStatus) {
        "Enforcing" -> stringResource(R.string.selinux_status_enforcing)
        "Permissive" -> stringResource(R.string.selinux_status_permissive)
        "Disabled" -> stringResource(R.string.selinux_status_disabled)
        else -> stringResource(R.string.selinux_status_unknown)
    }
    // 把找不到的字符串直接写死成 "已支持"
    val seccompDisplay = when (systemInfo.seccompStatus) {
        1 -> "已支持"
        0 -> stringResource(R.string.seccomp_status_disabled)
        2 -> stringResource(R.string.seccomp_status_filter)
        else -> stringResource(R.string.seccomp_status_unknown)
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.Tag, stringResource(R.string.home_manager_version), systemInfo.managerVersion)
                InfoText(Icons.Filled.DeveloperBoard, stringResource(R.string.home_kernel), systemInfo.kernelVersion)
                InfoText(Icons.Filled.Smartphone, stringResource(R.string.home_device_model), systemInfo.deviceModel)
                InfoText(Icons.Filled.Fingerprint, stringResource(R.string.home_fingerprint), systemInfo.fingerprint, 0.dp)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(Icons.Filled.Security, stringResource(R.string.home_selinux_status), selinuxDisplay)
                InfoText(Icons.Filled.FilterList, stringResource(R.string.home_seccomp_status), seccompDisplay, 0.dp)
            }
        }
    }
}

@Composable
private fun InfoText(icon: ImageVector, title: String, content: String, bottomPadding: Dp = 24.dp) {
    Row(modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, title, Modifier.padding(end = 12.dp).size(24.dp), tint = colorScheme.onSurface)
        Column {
            Text(title, fontSize = MiuixTheme.textStyles.headline1.fontSize, fontWeight = FontWeight.Medium, color = colorScheme.onSurface)
            Text(content, fontSize = MiuixTheme.textStyles.body2.fontSize, color = colorScheme.onSurfaceVariantSummary, modifier = Modifier.padding(top = 2.dp))
        }
    }
}
