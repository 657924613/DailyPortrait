package com.missyun.dailyportrait.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * 动效 Token
 *
 * 按交付文档 v1.1 §4.5 定义统一动画时长与缓动曲线。
 * 配合 [androidx.compose.animation.core.tween] 使用。
 */
object DPMotion {
    /** 标准缓动 (Material Standard) */
    val EaseStandard: Easing = FastOutSlowInEasing

    /** 强调缓动 (Material Emphasized,用于入场) */
    val EaseEmphasized: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    /** 微反馈：按钮按下、Tab 切换 */
    const val DurationFast = 150

    /** 标准过渡：颜色变化、淡入淡出 */
    const val DurationBase = 250

    /** 慢速过渡：Sheet / 模态 / 大卡片入场 */
    const val DurationSlow = 400

    /** 数字滚动专用 */
    const val DurationCounter = 800

    /** 卡片 staggered 入场延迟单位 */
    const val StaggerDelay = 50
}
