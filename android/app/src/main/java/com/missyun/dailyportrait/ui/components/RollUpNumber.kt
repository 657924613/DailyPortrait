package com.missyun.dailyportrait.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import com.missyun.dailyportrait.ui.theme.DPMotion
import com.missyun.dailyportrait.ui.util.rememberReduceMotion

/**
 * 数字滚动计数组件
 *
 * 严格按 architecture-android.md §4.3：[value] 变化时，
 * 用 800ms tween + FastOutSlowInEasing 平滑滚动到目标值。
 *
 * 步骤 8 增强：
 * - 尊重系统"减少动画"偏好（reduce motion）
 * - 添加 contentDescription 让屏幕阅读器朗读完整数字（不朗读中间过程）
 *
 * @param value 目标值
 * @param style 文本样式
 * @param color 文本颜色
 * @param a11yLabelPrefix 屏幕阅读器朗读前缀，例如"连续打卡"→ "连续打卡 37 天"
 */
@Composable
fun RollUpNumber(
    value: Int,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current,
    a11yLabelPrefix: String? = null
) {
    val reduceMotion by rememberReduceMotion()

    val animated by animateIntAsState(
        targetValue = value,
        animationSpec = tween(
            durationMillis = if (reduceMotion) 1 else DPMotion.DurationCounter,
            easing = FastOutSlowInEasing
        ),
        label = "RollUpNumber"
    )

    Text(
        text = animated.toString(),
        style = style,
        color = color,
        // 屏幕阅读器只朗读最终值（基于 value，不基于 animated）
        modifier = modifier.semantics {
            contentDescription = a11yLabelPrefix?.let { "$it $value" } ?: value.toString()
        }
    )
}
