package com.missyun.dailyportrait.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.missyun.dailyportrait.ui.theme.DPDimens

/**
 * 7 点周进度环
 *
 * 严格按交付文档 v1.1 §6 P0 修复：
 * - 已打卡圆点叠加 ✓ 图标（双通道反馈，色盲友好）
 * - 仅靠颜色区分违反 WCAG 1.4.1 Level A
 *
 * 显示规则：
 * - 已打卡：填充主色 + ✓ 图标
 * - 今日（未打卡）：边框主色 + 中文星期 "M T W T F S S" 简写
 * - 其他未打卡：浅灰背景 + 简写
 *
 * @param weekProgress 长度 7 的布尔数组（周一→周日）
 * @param todayIndex 今日在数组中的下标（0=周一, 6=周日）
 */
@Composable
fun WeekProgressRing(
    weekProgress: BooleanArray,
    todayIndex: Int,
    modifier: Modifier = Modifier
) {
    require(weekProgress.size == 7) { "weekProgress 必须长度 7，实际 ${weekProgress.size}" }
    val labels = listOf("一", "二", "三", "四", "五", "六", "日")

    val doneCount = weekProgress.count { it }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "本周进度，已打卡 $doneCount 天" },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        weekProgress.forEachIndexed { i, done ->
            DayDot(label = labels[i], done = done, isToday = i == todayIndex)
        }
    }
}

@Composable
private fun DayDot(
    label: String,
    done: Boolean,
    isToday: Boolean
) {
    val size = 32.dp
    when {
        done -> Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // ✓ 图标 —— P0 修复：双通道反馈
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
        isToday -> Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
        }
        else -> Box(
            modifier = Modifier
                .size(size)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
