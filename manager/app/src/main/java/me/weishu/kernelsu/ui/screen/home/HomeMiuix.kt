package me.weishu.kernelsu.ui.screen.home

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.CheckCircleOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.weishu.kernelsu.Natives
import me.weishu.kernelsu.R
import top.yukonga.miuix.kmp.basic.*
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.MiuixTheme.colorScheme

@Composable
fun HomeMiuix(
    state: HomeUiState,
    actions: HomeActions,
    bottomInnerPadding: Dp
) {
    val barColor = colorScheme.surface

    Scaffold(
        topBar = {
            TopAppBar(
                color = barColor,
                title = stringResource(id = R.string.app_name)
            )
        },
        popupHost = { },
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top)
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
                    StatusCard(state = state, actions = actions)
                    
                    InfoCard(
                        systemInfo = state.systemInfo,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    SupportLinks(
                        onOpenUrl = actions.onOpenUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp + bottomInnerPadding))
                }
            }
        }
    }
}

@Composable
private fun StatusCard(state: HomeUiState, actions: HomeActions) {
    Column {
        if (state.ksuVersion != null) {
            val workingText = "${stringResource(R.string.home_working)}"
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.defaultColors(
                    color = if (isSystemInDarkTheme()) Color(0xFF1A3825) else Color(0xFFDFFFEA)
                ),
                onClick = {
                    if (!state.isLateLoadMode) actions.onInstallClick()
                }
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = workingText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.home_working_version, "${state.ksuVersion}-${state.kernelUAPIVersion}"),
                            fontSize = 15.sp
                        )
                    }
                }
            }
        } else {
            Card(
                onClick = { actions.onInstallClick() }
            ) {
                BasicComponent(
                    title = stringResource(R.string.home_not_installed),
                    summary = stringResource(R.string.home_click_to_install),
                    startAction = {
                        Icon(
                            imageVector = Icons.Rounded.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                    }
                )
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
            startAction = {
                Icon(imageVector = Icons.Filled.VolunteerActivism, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            },
            onClick = { onOpenUrl("https://patreon.com/weishu") }
        )
        ArrowPreference(
            title = stringResource(R.string.home_learn_kernelsu),
            summary = stringResource(R.string.home_click_to_learn_kernelsu),
            startAction = {
                Icon(imageVector = Icons.AutoMirrored.Filled.MenuBook, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            },
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

    val seccompDisplay = when (systemInfo.seccompStatus) {
        1 -> stringResource(R.string.seccomp_status_supported)
        0 -> stringResource(R.string.seccomp_status_disabled)
        2 -> stringResource(R.string.seccomp_status_filter)
        else -> stringResource(R.string.seccomp_status_unknown)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(icon = Icons.Filled.Tag, title = stringResource(R.string.home_manager_version), content = systemInfo.managerVersion)
                InfoText(icon = Icons.Filled.DeveloperBoard, title = stringResource(R.string.home_kernel), content = systemInfo.kernelVersion)
                InfoText(icon = Icons.Filled.Smartphone, title = stringResource(R.string.home_device_model), content = systemInfo.deviceModel)
                InfoText(icon = Icons.Filled.Fingerprint, title = stringResource(R.string.home_fingerprint), content = systemInfo.fingerprint, bottomPadding = 0.dp)
            }
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                InfoText(icon = Icons.Filled.Security, title = stringResource(R.string.home_selinux_status), content = selinuxDisplay)
                InfoText(icon = Icons.Filled.FilterList, title = stringResource(R.string.home_seccomp_status), content = seccompDisplay, bottomPadding = 0.dp)
            }
        }
    }
}

@Composable
private fun InfoText(icon: ImageVector, title: String, content: String, bottomPadding: Dp = 24.dp) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = bottomPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            modifier = Modifier.padding(end = 12.dp).size(24.dp),
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
