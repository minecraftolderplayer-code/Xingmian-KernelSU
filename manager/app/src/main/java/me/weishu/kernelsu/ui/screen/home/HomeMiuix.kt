package me.weishu.kernelsu.ui.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.KernelVersion
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.PrivilegeManager
import me.weishu.kernelsu.R
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun HomeMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp
) {
    val scrollBehavior = MiuixScrollBehavior()
    val enableBlur = LocalEnableBlur.current
    val backdrop = rememberBlurBackdrop(enableBlur)
    val blurActive = backdrop != null
    val barColor = if (blurActive) colorScheme.surface.copy(alpha = 0.7f) else colorScheme.surface

    Scaffold(
        topBar = {
            TopBar(
                scrollBehavior = scrollBehavior,
                backdrop = backdrop,
                color = barColor,
                title = stringResource(id = R.string.app_name),
                actions = {
                    // 官方原版的右上角菜单保留
                    RobotListPopupMiuix()
                }
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .scrollEndHaptic()
                .overscrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .padding(horizontal = 12.dp),
            contentPadding = innerPadding
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .padding(bottom = bottomInnerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 直接进入状态卡片，去掉了红条和黄条
                    StatusCard(
                        state = state,
                        actions = actions
                    )
                    
                    InfoCard(
                        systemInfo = state.systemInfo,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    SupportLinks(
                        onOpenUrl = actions.onOpenUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(
    state: HomeUiState,
    actions: HomeActions,
) {
    // 获取我们的权限状态
    val currentMode = PrivilegeManager.currentMode
    val workingText = when (currentMode) {
        PrivilegeManager.Mode.ROOT -> "Root 模式"
        PrivilegeManager.Mode.SHIZUKU -> "无线 ADB 模式"
        else -> "未激活 (点击授权)"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.defaultColors(
                color = when {
                    isDynamicColor() -> colorScheme.secondaryContainer
                    isSystemInDarkTheme() -> Color(0xFF1A3825)
                    else -> Color(0xFFDFFFEA)
                }
            ),
            onClick = {
                // 如果没激活，点击卡片尝试去请求权限
                if (currentMode == PrivilegeManager.Mode.NONE) {
                    actions.onInstallClick()
                }
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .offset(27.dp, 31.dp)
                        .align(Alignment.BottomEnd)
                ) {
                    Icon(
                        modifier = Modifier.size(100.dp),
                        imageVector = Icons.Rounded.CheckCircleOutline,
                        contentDescription = null,
                        tint = if (isDynamicColor()) {
                            colorScheme.primary.copy(alpha = 0.8f)
                        } else {
                            Color(0xFF3D6D7)
                        }
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Text(
                        text = workingText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(4.dp))
                    // 这里读取底层写死的版本号（32513）
                    Text(
                        text = "版本: ${state.ksuVersion ?: "32513"}-1",
                        fontSize = 15.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                }
            }
        }
    }
}

@Composable
private fun SupportLinks(
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val learnMoreUrl = stringResource(R.string.home_learn_kernelsu_url)
    Card(modifier = modifier) {
        ArrowPreference(
            title = stringResource(R.string.home_support_title),
            summary = stringResource(R.string.home_support_content),
            startAction = {
                Icon(
                    imageVector = Icons.Filled.VolunteerActivism,
                    contentDescription = stringResource(R.string.home_support_title),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground
                )
            },
            onClick = { onOpenUrl("https://patreon.com/weishu") }
        )
        ArrowPreference(
            title = stringResource(R.string.home_learn_kernelsu),
            summary = stringResource(R.string.home_click_to_learn_kernelsu),
            startAction = {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.MenuBook,
                    contentDescription = stringResource(R.string.home_learn_kernelsu),
                    modifier = Modifier.padding(end = 6.dp),
                    tint = colorScheme.onBackground
                )
            },
            onClick = { onOpenUrl(learnMoreUrl) }
        )
    }
}

@Composable
private fun InfoCard(
    systemInfo: SystemInfo,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(
                    icon = Icons.Filled.Tag,
                    title = stringResource(R.string.home_manager_version),
                    content = systemInfo.managerVersion,
                )
                InfoText(
                    icon = Icons.Filled.DeveloperBoard,
                    title = stringResource(R.string.home_kernel),
                    content = systemInfo.kernelVersion,
                )
                InfoText(
                    icon = Icons.Filled.Smartphone,
                    title = stringResource(R.string.home_device_model),
                    content = systemInfo.deviceModel,
                )
                InfoText(
                    icon = Icons.Filled.Fingerprint,
                    title = stringResource(R.string.home_fingerprint),
                    content = systemInfo.fingerprint,
                    bottomPadding = 0.dp
                )
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(
                    icon = Icons.Filled.Security,
                    title = stringResource(R.string.home_selinux_status),
                    content = if (systemInfo.selinuxStatus == "Enforcing") "强制" else "宽容",
                )
                InfoText(
                    icon = Icons.Filled.FilterList,
                    title = stringResource(R.string.home_seccomp_status),
                    content = if (systemInfo.seccompStatus == "1") "过滤模式" else "已禁用",
                    bottomPadding = 0.dp
                )
            }
        }
    }
}

@Composable
private fun InfoText(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: String,
    bottomPadding: Dp = 24.dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier
                .padding(end = 12.dp)
                .size(24.dp),
            tint = colorScheme.onSurface
        )
        Column {
            Text(
                text = title,
                fontSize = MiuixTheme.textStyles.headline1.fontSize,
                fontWeight = FontWeight.Medium,
                color = colorScheme.onSurface,
            )
            Text(
                text = content,
                fontSize = MiuixTheme.textStyles.body2.fontSize,
                color = colorScheme.onSurfaceVariantSummary,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}