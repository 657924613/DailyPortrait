package com.missyun.dailyportrait.ui.util

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * 系统级"减少动画"偏好检测（对应 Web 的 prefers-reduced-motion）
 *
 * 检测三类信号（任一命中即为 reduce）：
 * 1. Android 14+：[AccessibilityManager.isAudioDescriptionRequested] 不是这个，
 *    应该使用反射或者 13+ 的 [Settings.Global.ANIMATOR_DURATION_SCALE]
 * 2. ANIMATOR_DURATION_SCALE == 0 → 用户在开发者选项 / 辅助功能中关掉了动画
 * 3. AccessibilityManager.getRecommendedTimeoutMillis 已开高对比/简化模式时
 *
 * 当前实现只读 [Settings.Global.ANIMATOR_DURATION_SCALE]，简单可靠。
 *
 * UI 层使用：
 * ```kotlin
 * val reduceMotion by rememberReduceMotion()
 * val pulse = if (reduceMotion) 1f else animatedPulse
 * ```
 */
@Composable
fun rememberReduceMotion(): State<Boolean> {
    val context = LocalContext.current
    val state = remember { mutableStateOf(checkReduceMotion(context)) }

    // 注册 AccessibilityManager 监听器，运行时变化也能感知
    DisposableEffect(context) {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
        val listener = AccessibilityManager.AccessibilityStateChangeListener { _ ->
            state.value = checkReduceMotion(context)
        }
        am?.addAccessibilityStateChangeListener(listener)

        onDispose {
            am?.removeAccessibilityStateChangeListener(listener)
        }
    }
    return state
}

/**
 * 即时检测系统动画缩放。
 * 0 表示用户禁用动画（Settings → Accessibility → Remove animations）
 */
private fun checkReduceMotion(context: Context): Boolean {
    return try {
        val animScale = Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        )
        animScale == 0f
    } catch (_: Throwable) {
        false
    }
}

/**
 * 在 reduce motion 时把时长压到极小（保留布局逻辑，仅去掉视觉动效）
 *
 * 用法：
 * ```
 * tween(durationMillis = if (reduce) 1 else DPMotion.DurationBase)
 * ```
 *
 * 不直接置 0，是因为部分 Compose API（如 LinearProgressIndicator 内部）
 * 对 0 时长有特殊处理，可能反而异常。
 */
@Suppress("unused")
fun reducedDuration(reduce: Boolean, original: Int): Int = if (reduce) 1 else original
