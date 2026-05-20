package com.missyun.dailyportrait.ui.screens.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.missyun.dailyportrait.BuildConfig

/**
 * 设置页 v2 —— 极简列表风格
 *
 * 与 Dashboard 时间轴叙事风格统一：
 * - 无圆形图标背景，无阴影卡片
 * - 用细线分隔 + section label
 * - LazyColumn 确保底部内容不被遮挡
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onReplayOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.toast.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.navigateToOnboarding.collect {
            onReplayOnboarding()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.toggleReminder(true)
    }

    var showWipeConfirm = false // unused, kept to avoid compile error if referenced elsewhere

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // 顶部栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 56.dp, start = 8.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = "设置",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
        }

        // 可滚动内容
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp) // 底部安全区域
        ) {
            // ===== 拍照提醒 =====
            item { SectionHeader("拍照提醒") }

            item {
                SettingItem(
                    title = "每日提醒",
                    subtitle = if (state.reminderEnabled) "已开启" else "关闭",
                    onClick = {
                        val newValue = !state.reminderEnabled
                        if (newValue) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                val granted = ContextCompat.checkSelfPermission(
                                    context, Manifest.permission.POST_NOTIFICATIONS
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) viewModel.toggleReminder(true)
                                else notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.toggleReminder(true)
                            }
                        } else {
                            viewModel.toggleReminder(false)
                        }
                    },
                    trailing = {
                        Switch(
                            checked = state.reminderEnabled,
                            onCheckedChange = null, // handled by row click
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                )
            }

            item { ItemDivider() }

            item {
                SettingItem(
                    title = "提醒时间",
                    subtitle = if (state.reminderEnabled) "每天 ${state.reminderTimeLabel}" else "开启提醒后生效",
                    onClick = {
                        if (state.reminderEnabled) {
                            TimePickerDialog(
                                context,
                                { _, hour, minute -> viewModel.setReminderTime(hour, minute) },
                                state.reminderHour,
                                state.reminderMinute,
                                true
                            ).show()
                        }
                    },
                    trailing = {
                        Text(
                            text = state.reminderTimeLabel,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (state.reminderEnabled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
            }

            if (state.reminderEnabled) {
                item { ItemDivider() }
                item {
                    SettingItem(
                        title = "发送测试通知",
                        subtitle = "验证通知功能是否正常",
                        onClick = { viewModel.sendTestNotification(context) },
                        trailing = {
                            Text(
                                text = "测试",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    )
                }
            }

            // ===== 关于 =====
            item { SectionHeader("关于") }
            item {
                val versionName = remember {
                    runCatching { BuildConfig.VERSION_NAME }.getOrDefault("1.0.0")
                }
                SettingItem(
                    title = "版本",
                    subtitle = "v$versionName"
                )
            }
            item { ItemDivider() }
            item {
                SettingItem(
                    title = "重新观看引导",
                    subtitle = "回顾使用方法",
                    onClick = { viewModel.replayOnboarding() }
                )
            }
            item { ItemDivider() }
            item {
                SettingItem(
                    title = "隐私政策",
                    subtitle = "了解我们如何保护你的数据",
                    onClick = { openUrl(context, PRIVACY_POLICY_URL) }
                )
            }
            item { ItemDivider() }
            item {
                SettingItem(
                    title = "用户协议",
                    subtitle = "使用应用前请阅读",
                    onClick = { openUrl(context, TERMS_URL) }
                )
            }
        }
    }

}

/* ============ 通用组件 ============ */

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = 28.dp, top = 24.dp, bottom = 8.dp)
    )
}

@Composable
private fun ItemDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 28.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun SettingItem(
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 28.dp, vertical = 16.dp)
            .sizeIn(minHeight = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = titleColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    try {
        val intent = android.content.Intent(
            android.content.Intent.ACTION_VIEW,
            android.net.Uri.parse(url)
        )
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "无法打开链接", Toast.LENGTH_SHORT).show()
    }
}

private const val PRIVACY_POLICY_URL = "https://example.com/dailyportrait/privacy"
private const val TERMS_URL = "https://example.com/dailyportrait/terms"
