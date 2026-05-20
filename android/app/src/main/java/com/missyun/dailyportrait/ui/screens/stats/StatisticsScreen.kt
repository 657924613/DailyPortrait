package com.missyun.dailyportrait.ui.screens.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.missyun.dailyportrait.ui.theme.DPColors
import com.missyun.dailyportrait.ui.theme.DPDimens
import java.time.DayOfWeek
import java.time.YearMonth

/**
 * 统计大图页
 *
 * 包含：
 * - 三个数据卡片（总天数 / 当前连续 / 最长连续）
 * - 月度热力图（日历格子，拍过的天高亮）
 * - 月份前后切换
 */
@Composable
fun StatisticsScreen(
    onBack: () -> Unit,
    viewModel: StatisticsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(horizontal = DPDimens.ScreenPadding)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Text(
                text = "统计",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // 三个数据卡片
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(DPDimens.CardGutter)
        ) {
            StatCard(
                label = "总天数",
                value = state.totalDays.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "当前连续",
                value = state.currentStreak.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "最长连续",
                value = state.longestStreak.toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 月份切换
        MonthSelector(
            yearMonth = state.selectedMonth,
            onPrevious = {
                viewModel.onMonthChange(state.selectedMonth.minusMonths(1))
            },
            onNext = {
                val next = state.selectedMonth.plusMonths(1)
                if (!next.isAfter(YearMonth.now())) {
                    viewModel.onMonthChange(next)
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 热力图
        HeatmapCalendar(
            yearMonth = state.selectedMonth,
            activeDays = state.heatmapDays
        )
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = modifier
            .aspectRatio(0.9f)
            .semantics { contentDescription = "$label $value 天" }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "天",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MonthSelector(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val canGoNext = !yearMonth.plusMonths(1).isAfter(YearMonth.now())

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = "上个月"
            )
        }
        Text(
            text = "${yearMonth.year}年${yearMonth.monthValue}月",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "下个月",
                tint = if (canGoNext) MaterialTheme.colorScheme.onSurface
                       else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * 月度热力图日历
 *
 * 7 列（周一~周日），每个格子代表一天。
 * 拍过照的天用主色填充，未拍的用淡色占位。
 */
@Composable
private fun HeatmapCalendar(
    yearMonth: YearMonth,
    activeDays: Set<Int>
) {
    val daysInMonth = yearMonth.lengthOfMonth()
    // 该月 1 号是周几（周一=1 ... 周日=7）
    val firstDayOfWeek = yearMonth.atDay(1).dayOfWeek.value // 1=Monday
    // 前面需要空几格（周一开始，所以 Monday=0 offset）
    val startOffset = firstDayOfWeek - 1

    val weekDayLabels = listOf("一", "二", "三", "四", "五", "六", "日")

    Column {
        // 星期标题行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            weekDayLabels.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 日历格子
        val totalCells = startOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        for (row in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0 until 7) {
                    val cellIndex = row * 7 + col
                    val day = cellIndex - startOffset + 1

                    if (day in 1..daysInMonth) {
                        val isActive = day in activeDays
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .padding(2.dp)
                                .background(
                                    color = if (isActive) MaterialTheme.colorScheme.primary
                                            else DPColors.PhotoPlaceholderLight,
                                    shape = RoundedCornerShape(6.dp)
                                )
                                .semantics {
                                    contentDescription = "${day}日" +
                                        if (isActive) "，已记录" else "，未记录"
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = day.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isActive) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        // 空格占位
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        )
                    }
                }
            }
        }
    }
}
