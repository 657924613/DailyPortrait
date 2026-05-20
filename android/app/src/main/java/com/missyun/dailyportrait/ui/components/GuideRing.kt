package com.missyun.dailyportrait.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.missyun.dailyportrait.domain.model.FaceAlignment
import com.missyun.dailyportrait.ui.theme.DPDimens
import com.missyun.dailyportrait.ui.theme.DPMotion
import com.missyun.dailyportrait.ui.util.rememberReduceMotion

/**
 * 对齐引导环
 *
 * 严格按 architecture-android.md §4.2 与交付文档 v1.1：
 * - NONE → 灰色虚线
 * - DETECTED → 红色实线 (300ms 颜色补间)
 * - ALIGNED → 绿色实线 + 脉冲缩放 1.0→1.05→1.0 (600ms repeatable)
 *
 * 步骤 8 增强：
 * - 对齐瞬间触觉反馈（Confirm haptic）
 * - 状态文本走 [LiveRegionMode.Polite]，让 TalkBack 自动朗读变化
 * - reduce motion 时关闭脉冲
 * - ✓/✗ 双通道反馈（色盲友好）
 */
@Composable
fun GuideRing(
    alignment: FaceAlignment,
    modifier: Modifier = Modifier,
    diameter: Dp = DPDimens.GuideRingSize
) {
    val targetColor = guideRingColor(alignment)
    val animatedColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(
            durationMillis = DPMotion.DurationBase,
            easing = DPMotion.EaseStandard
        ),
        label = "GuideRingColor"
    )

    val reduceMotion by rememberReduceMotion()

    val pulseScale: Float = if (alignment == FaceAlignment.ALIGNED && !reduceMotion) {
        val transition = rememberInfiniteTransition(label = "GuideRingPulse")
        val s by transition.animateFloat(
            initialValue = 1f,
            targetValue = 1.05f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 600, easing = DPMotion.EaseStandard),
                repeatMode = RepeatMode.Reverse
            ),
            label = "GuideRingPulseScale"
        )
        s
    } else {
        val s by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(DPMotion.DurationBase),
            label = "GuideRingResetScale"
        )
        s
    }

    // 对齐瞬间触觉反馈：从非 ALIGNED 切换到 ALIGNED 时震一下
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(alignment) {
        if (alignment == FaceAlignment.ALIGNED) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val a11y = remember(alignment) {
        when (alignment) {
            FaceAlignment.NONE -> "未检测到人脸，请正对镜头"
            FaceAlignment.DETECTED -> "未对齐，请微调位置"
            FaceAlignment.ALIGNED -> "已对齐，可以拍照"
        }
    }

    Box(
        modifier = modifier
            .size(diameter)
            .scale(pulseScale)
            .semantics {
                contentDescription = a11y
                liveRegion = LiveRegionMode.Polite
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val strokeWidthPx = (if (alignment == FaceAlignment.NONE) 3.dp else 4.dp).toPx()
            val pathEffect = if (alignment == FaceAlignment.NONE) {
                PathEffect.dashPathEffect(floatArrayOf(20f, 14f), 0f)
            } else null

            drawArc(
                color = animatedColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(strokeWidthPx / 2f, strokeWidthPx / 2f),
                size = Size(size.width - strokeWidthPx, size.height - strokeWidthPx),
                style = Stroke(width = strokeWidthPx, pathEffect = pathEffect)
            )
        }

        AlignmentIcon(alignment = alignment)
    }
}

@Composable
private fun guideRingColor(alignment: FaceAlignment): Color = when (alignment) {
    FaceAlignment.NONE -> MaterialTheme.colorScheme.outline
    FaceAlignment.DETECTED -> MaterialTheme.colorScheme.error
    FaceAlignment.ALIGNED -> MaterialTheme.colorScheme.tertiary
}

/**
 * 对齐状态辅助图标 ✓ / ✗（顶部居中），NONE 不渲染
 * 双通道反馈，色盲友好
 */
@Composable
private fun BoxScope.AlignmentIcon(alignment: FaceAlignment) {
    if (alignment == FaceAlignment.NONE) return

    val (icon, tint, label) = when (alignment) {
        FaceAlignment.DETECTED -> Triple(
            Icons.Filled.Close,
            MaterialTheme.colorScheme.error,
            "未对齐"
        )
        FaceAlignment.ALIGNED -> Triple(
            Icons.Filled.Check,
            MaterialTheme.colorScheme.tertiary,
            "已对齐"
        )
        else -> return
    }

    Box(
        modifier = Modifier
            .align(Alignment.TopCenter)
            .offset(y = (-20).dp)
            .size(40.dp)
            .background(color = tint, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(24.dp)
        )
    }
}
