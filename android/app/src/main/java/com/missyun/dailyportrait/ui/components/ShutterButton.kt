package com.missyun.dailyportrait.ui.components

import android.media.MediaActionSound
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.missyun.dailyportrait.ui.theme.DPDimens

/**
 * 拍照快门按钮
 *
 * 严格按 architecture-android.md §4.2：
 * - 外层白色描边圆环 (4.dp)
 * - 内层实心填充
 * - 按下 scale → 0.9，松手 spring 回弹
 * - 长按 → 触发 [onLongPress] 启动倒计时
 * - 最小触控 48dp（由按钮 72dp 直径保证）
 *
 * 步骤 8 增强：
 * - **触觉反馈**：单击 LongPress / 长按 TextHandleMove 触觉
 * - **听觉反馈**：系统 [MediaActionSound.SHUTTER_CLICK] 快门音
 * - **A11y 状态**：aligned=true 时 contentDescription = "拍照，已对齐"
 *
 * @param onClick 单击：立即拍照
 * @param onLongPress 长按：启动 3 秒倒计时
 * @param enabled 是否启用
 * @param aligned 是否已对齐，影响无障碍语义
 */
@Composable
fun ShutterButton(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    aligned: Boolean = false
) {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ShutterScale"
    )

    val haptic = LocalHapticFeedback.current

    // 系统快门音（MediaActionSound 重型对象，整个生命周期内只创建一次）
    val sound = remember { MediaActionSound() }
    DisposableEffect(Unit) {
        sound.load(MediaActionSound.SHUTTER_CLICK)
        onDispose { sound.release() }
    }

    val a11y = if (aligned) "拍照，已对齐" else "拍照"

    Box(
        modifier = modifier
            .size(DPDimens.PhotoButtonSize)
            .scale(scale)
            .semantics {
                contentDescription = a11y
                role = Role.Button
            }
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = {
                        pressed = true
                        tryAwaitRelease()
                        pressed = false
                    },
                    onTap = {
                        if (!enabled) return@detectTapGestures
                        sound.play(MediaActionSound.SHUTTER_CLICK)
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onClick()
                    },
                    onLongPress = {
                        if (!enabled) return@detectTapGestures
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onLongPress()
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // 外圈白色描边
        Box(
            modifier = Modifier
                .size(DPDimens.PhotoButtonSize)
                .clip(CircleShape)
                .border(
                    width = DPDimens.BorderHero,
                    color = if (enabled) Color.White else Color.White.copy(alpha = 0.5f),
                    shape = CircleShape
                )
        )
        // 内圈实心
        Box(
            modifier = Modifier
                .size(DPDimens.PhotoButtonInnerSize)
                .clip(CircleShape)
                .background(
                    color = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    }
                )
        )
    }
}
