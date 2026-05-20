package com.missyun.dailyportrait.ui.screens.dashboard

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.missyun.dailyportrait.data.local.DailyPhoto
import com.missyun.dailyportrait.data.storage.FileManager
import com.missyun.dailyportrait.domain.util.DateUtil
import com.missyun.dailyportrait.ui.theme.DPColors
import com.missyun.dailyportrait.ui.theme.DPDimens
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

/**
 * Dashboard v3 —— 时间轴叙事 (Timeline Narrative)
 *
 * 基于 v2-A-timeline 原型重构：
 * - 顶部：日期 + 第N天 + 操作按钮行
 * - 今日打卡：呼吸动画虚线圆环
 * - 最近7天：横向大缩略图滑动条
 * - 月份折叠：每月一行小方块，横向滚动
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(
    onNavigateToCamera: () -> Unit,
    onOpenExportSheet: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenStatistics: () -> Unit = {},
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var deleteTarget by remember { mutableStateOf<DailyPhoto?>(null) }
    var showExportSheet by remember { mutableStateOf(false) }
    var detailIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is DashboardEffect.NavigateToCamera -> onNavigateToCamera()
                is DashboardEffect.OpenExportSheet -> {
                    showExportSheet = true
                    onOpenExportSheet()
                }
                is DashboardEffect.ConfirmDelete -> deleteTarget = effect.photo
                is DashboardEffect.ShowUndoSnackbar -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "已删除 ${effect.deletedPhoto.date}",
                        actionLabel = "撤销",
                        duration = androidx.compose.material3.SnackbarDuration.Short
                    )
                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                        viewModel.onIntent(DashboardIntent.UndoDelete)
                    }
                }
            }
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { snackbarHostState.showSnackbar(it) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (!state.hasData && !state.isLoading) {
            EmptyHero(
                onShoot = { viewModel.onIntent(DashboardIntent.OpenCamera) },
                onSettings = onOpenSettings
            )
        } else {
            DashboardContent(
                state = state,
                onTodayClick = { viewModel.onIntent(DashboardIntent.OpenCamera) },
                onSettingsClick = onOpenSettings,
                onExportClick = { viewModel.onIntent(DashboardIntent.RequestExport) },
                onStatisticsClick = onOpenStatistics,
                onHistoryClick = { photo ->
                    detailIndex = state.allPhotos.indexOf(photo).coerceAtLeast(0)
                },
                onHistoryLongClick = { photo ->
                    viewModel.onIntent(DashboardIntent.RequestDelete(photo))
                }
            )
        }

        deleteTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { deleteTarget = null },
                title = { Text("删除这张照片？") },
                text = { Text("${target.date} 的记录将被永久删除。") },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.onIntent(DashboardIntent.ConfirmDelete(target))
                        deleteTarget = null
                    }) {
                        Text("删除", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { deleteTarget = null }) { Text("取消") }
                }
            )
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        ) { data -> Snackbar(snackbarData = data) }
    }

    // 首张照片庆祝弹窗
    if (state.showFirstShotCelebration) {
        AlertDialog(
            onDismissRequest = { viewModel.onIntent(DashboardIntent.DismissFirstShotCelebration) },
            title = { Text("恭喜你拍下第一张！") },
            text = {
                Text("每天同一时间来记录一张，坚持下去你会看到惊人的变化。明天见！")
            },
            confirmButton = {
                TextButton(onClick = { viewModel.onIntent(DashboardIntent.DismissFirstShotCelebration) }) {
                    Text("我知道了")
                }
            }
        )
    }

    detailIndex?.let { idx ->
        PhotoDetailDialog(
            photos = state.allPhotos,
            initialIndex = idx,
            onDismiss = { detailIndex = null },
            onRequestDelete = { photo ->
                detailIndex = null
                viewModel.onIntent(DashboardIntent.RequestDelete(photo))
            }
        )
    }

    if (showExportSheet) {
        com.missyun.dailyportrait.ui.screens.videoexport.VideoExportSheet(
            onDismiss = { showExportSheet = false }
        )
    }
}

/* ============ 空状态 ============ */
@Composable
private fun EmptyHero(
    onShoot: () -> Unit,
    onSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 28.dp, vertical = 60.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SmallIconButton(onClick = onSettings, icon = Icons.Filled.Settings, desc = "设置")
        }
        Spacer(modifier = Modifier.height(80.dp))
        Text(
            text = "第 0 天",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "记录从今天\n开始",
            style = MaterialTheme.typography.displaySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "每天一张自拍\n积累属于你的肖像档案",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(48.dp))
        PrimaryPill(text = "拍下第一张", onClick = onShoot)
    }
}

/* ============ 主内容：时间轴叙事布局 ============ */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DashboardContent(
    state: DashboardUiState,
    onTodayClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit,
    onStatisticsClick: () -> Unit,
    onHistoryClick: (DailyPhoto) -> Unit,
    onHistoryLongClick: (DailyPhoto) -> Unit
) {
    val context = LocalContext.current
    val photos = state.allPhotos
    val today = LocalDate.now()
    val recentPhotos = photos.take(7)
    val monthGroups = remember(photos) { groupByMonth(photos) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 56.dp, bottom = 40.dp)
    ) {
        // ===== 顶部：日期 + 第N天 + 按钮 =====
        item {
            HeaderSection(
                streakDays = state.streakDays,
                canExport = state.canExport,
                onSettingsClick = onSettingsClick,
                onExportClick = onExportClick
            )
        }

        // ===== 今日打卡卡片 =====
        item {
            TodayCard(
                today = state.today,
                streakDays = state.streakDays,
                weekDone = state.weekProgress.count { it },
                onClick = onTodayClick
            )
        }

        // ===== 最近 7 天横向大缩略图 =====
        if (recentPhotos.isNotEmpty()) {
            item {
                SectionLabel(text = "最近 7 天")
            }
            item {
                RecentStrip(
                    photos = recentPhotos,
                    onClick = onHistoryClick,
                    onLongClick = onHistoryLongClick
                )
            }
        }

        // ===== 月份折叠行 =====
        if (monthGroups.isNotEmpty()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 28.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionLabel(text = "月度记录", noPadding = true)
                    TextButton(onClick = onStatisticsClick) {
                        Text("统计", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            items(monthGroups) { monthData ->
                MonthRow(
                    yearMonth = monthData.yearMonth,
                    filledDays = monthData.filledDays,
                    totalDays = monthData.totalDays
                )
            }
        }
    }
}

/* ============ 顶部区域 ============ */
@Composable
private fun HeaderSection(
    streakDays: Int,
    canExport: Boolean,
    onSettingsClick: () -> Unit,
    onExportClick: () -> Unit
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val dateStr = "${today.monthValue}月${today.dayOfMonth}日 · ${weekDayName(today)}"

    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 0.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column {
                Text(
                    text = dateStr,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "第 $streakDays 天",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Light),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallIconButton(onClick = onExportClick, icon = Icons.Filled.PlayArrow, desc = "导出视频")
                SmallIconButton(onClick = onSettingsClick, icon = Icons.Filled.Settings, desc = "设置")
            }
        }
        // 操作按钮行
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionChip(text = "📷 拍照", primary = true, onClick = {
                // 通过 parent 的 onTodayClick 触发
            })
            ActionChip(text = "▶ 视频", primary = false, onClick = {
                if (canExport) {
                    onExportClick()
                } else {
                    android.widget.Toast.makeText(context, "至少需要 2 张照片才能生成延时视频", android.widget.Toast.LENGTH_SHORT).show()
                }
            })
        }
        Spacer(modifier = Modifier.height(20.dp))
    }
}

/* ============ 今日打卡卡片 ============ */
@Composable
private fun TodayCard(
    today: DailyPhoto?,
    streakDays: Int,
    weekDone: Int,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    val fileManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DashboardFileManagerEntryPoint::class.java
        ).fileManager()
    }

    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "breathe")
    val breatheScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val breatheAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .semantics {
                contentDescription = if (today != null) "今日已记录" else "今日未记录，点击拍照"
            }
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 圆形区域
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .then(
                        if (today == null) Modifier
                            .scale(breatheScale)
                            .alpha(breatheAlpha)
                        else Modifier
                    )
                    .border(
                        width = 2.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = CircleShape
                    )
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (today != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fileManager.resolveAbsolutePath(today.imagePath))
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.CameraAlt,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            // 文字信息
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "今日打卡",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (today != null) "已记录" else "等待你的记录",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "连续 $streakDays 天 · 本周 $weekDone/7",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/* ============ 最近 7 天横向缩略图 ============ */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RecentStrip(
    photos: List<DailyPhoto>,
    onClick: (DailyPhoto) -> Unit,
    onLongClick: (DailyPhoto) -> Unit
) {
    val context = LocalContext.current
    val fileManager = remember(context) {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            DashboardFileManagerEntryPoint::class.java
        ).fileManager()
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 28.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(bottom = 20.dp)
    ) {
        items(photos, key = { it.id }) { photo ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.combinedClickable(
                    onClick = { onClick(photo) },
                    onLongClick = { onLongClick(photo) }
                )
            ) {
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DPColors.PhotoPlaceholderLight)
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(fileManager.resolveAbsolutePath(photo.imagePath))
                            .build(),
                        contentDescription = "${photo.date} 的照片",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = formatShortDate(photo.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/* ============ 月份折叠行 ============ */
@Composable
private fun MonthRow(
    yearMonth: YearMonth,
    filledDays: Set<Int>,
    totalDays: Int
) {
    Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${yearMonth.monthValue}月",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "${filledDays.size} / $totalDays 天",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            items(totalDays) { dayIndex ->
                val day = dayIndex + 1
                val isFilled = day in filledDays
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isFilled) DPColors.PhotoPlaceholderLight
                            else MaterialTheme.colorScheme.surfaceContainer
                        )
                        .then(
                            if (!isFilled) Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            ) else Modifier
                        )
                )
            }
        }
    }
}

/* ============ 通用组件 ============ */
@Composable
private fun SmallIconButton(
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    desc: String
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(36.dp)
            .background(MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            .semantics { contentDescription = desc }
    ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun ActionChip(text: String, primary: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = if (primary) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.surfaceContainer,
        border = if (!primary) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                color = if (primary) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String, noPadding: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = if (noPadding) Modifier else Modifier.padding(start = 28.dp, bottom = 10.dp)
    )
}

@Composable
private fun PrimaryPill(text: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.sizeIn(minHeight = DPDimens.TouchPrimary)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.surface
            )
        }
    }
}

/* ============ 工具函数 ============ */
private fun weekDayName(date: LocalDate): String {
    return when (date.dayOfWeek.value) {
        1 -> "星期一"; 2 -> "星期二"; 3 -> "星期三"
        4 -> "星期四"; 5 -> "星期五"; 6 -> "星期六"
        7 -> "星期日"; else -> ""
    }
}

private fun formatShortDate(isoDate: String): String {
    val parts = isoDate.split("-")
    if (parts.size != 3) return isoDate
    val month = parts[1].toIntOrNull()?.toString() ?: parts[1]
    val day = parts[2].toIntOrNull()?.toString() ?: parts[2]
    return "$month.$day"
}

private data class MonthData(
    val yearMonth: YearMonth,
    val filledDays: Set<Int>,
    val totalDays: Int
)

private fun groupByMonth(photos: List<DailyPhoto>): List<MonthData> {
    if (photos.isEmpty()) return emptyList()

    val byMonth = photos.groupBy { photo ->
        val date = LocalDate.parse(photo.date)
        YearMonth.of(date.year, date.month)
    }

    return byMonth.entries
        .sortedByDescending { it.key }
        .map { (ym, monthPhotos) ->
            val filledDays = monthPhotos.map { LocalDate.parse(it.date).dayOfMonth }.toSet()
            val totalDays = if (ym == YearMonth.now()) LocalDate.now().dayOfMonth else ym.lengthOfMonth()
            MonthData(yearMonth = ym, filledDays = filledDays, totalDays = totalDays)
        }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface DashboardFileManagerEntryPoint {
    fun fileManager(): FileManager
}
