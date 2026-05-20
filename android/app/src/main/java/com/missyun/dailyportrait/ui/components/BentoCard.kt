package com.missyun.dailyportrait.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.missyun.dailyportrait.ui.theme.DPDimens

/**
 * Bento 通用卡片
 *
 * 严格按 architecture-android.md §4.3 与 Soft Cloud 视觉系统：
 * - 圆角 24dp
 * - 内边距 18dp
 * - 浅色阴影，避免硬边
 * - 支持纯色 / 渐变背景
 * - 整卡可点击（提供无障碍语义入口）
 *
 * @param modifier 外部传入约束尺寸（grid span）
 * @param onClick 点击行为，null 表示纯展示卡片
 * @param contentDescription 整卡 a11y 描述
 * @param backgroundBrush 渐变背景，传 null 走 [backgroundColor]
 * @param backgroundColor 纯色背景
 * @param cornerRadius 圆角
 * @param contentPadding 内边距
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentDescription: String? = null,
    backgroundBrush: Brush? = null,
    backgroundColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = DPDimens.CardPadding,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    var m: Modifier = modifier
        .shadow(elevation = DPDimens.Elev1, shape = shape, clip = false)
        .clip(shape)
    m = if (backgroundBrush != null) {
        m.background(brush = backgroundBrush, shape = shape)
    } else {
        m.background(color = backgroundColor, shape = shape)
    }
    if (onClick != null) m = m.clickable(onClick = onClick)
    if (contentDescription != null) {
        m = m.semantics(mergeDescendants = true) { this.contentDescription = contentDescription }
    }

    Box(modifier = m.padding(contentPadding)) {
        content()
    }
}
