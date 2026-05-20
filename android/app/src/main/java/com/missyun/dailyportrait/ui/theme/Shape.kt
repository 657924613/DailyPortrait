package com.missyun.dailyportrait.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * DailyPortrait 形状 Token
 *
 * 严格按 architecture-android.md §1.3 与交付文档 v1.1 §4.4：
 * - CardCorner 20dp（Soft Cloud 风格扩展为 24dp,更柔和）
 * - ButtonCorner 12dp（Soft Cloud 风格扩展为 16dp,呼应卡片）
 * - ChipCorner 8dp
 *
 * 注意：
 * - 拍照按钮使用独立 [androidx.compose.foundation.shape.CircleShape]（=50%圆）,不在此处
 * - Bento 历史宫格使用 18dp 圆角，单独在组件中定义
 */
val DPShapes = Shapes(
    /** 极小圆角：Chip / Tag / Snackbar */
    extraSmall = RoundedCornerShape(8.dp),

    /** 小圆角：次级按钮、输入框 */
    small = RoundedCornerShape(12.dp),

    /** 中圆角：标准按钮、Today Time 胶囊扩展 */
    medium = RoundedCornerShape(16.dp),

    /** 大圆角：Bento 卡片标准 (Soft Cloud 24dp) */
    large = RoundedCornerShape(24.dp),

    /** 极大圆角：Sheet 顶部 / 引导页大卡 */
    extraLarge = RoundedCornerShape(28.dp)
)

/**
 * 自定义形状常量
 * 不在 Material3 [Shapes] Token 体系中、但项目内复用度高的形状
 */
object DPCustomShapes {
    /** Bottom Sheet 顶部圆角（仅上方有圆角） */
    val SheetTop = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

    /** 历史宫格图片圆角 */
    val HistoryThumb = RoundedCornerShape(18.dp)

    /** 今日打卡卡片缩略图 */
    val TodayThumb = RoundedCornerShape(22.dp)

    /** 胶囊形状（用于 Time 标签、Tab Bar） */
    val Capsule = RoundedCornerShape(percent = 50)
}
