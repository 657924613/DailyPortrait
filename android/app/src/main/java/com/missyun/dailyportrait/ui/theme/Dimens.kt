package com.missyun.dailyportrait.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 间距与尺寸 Token
 *
 * 严格按交付文档 v1.1 §4.3 (4pt 网格) 与 architecture-android.md §1.3 双重校验。
 * UI 层禁止硬编码 dp 值,必须引用此 object。
 */
object DPDimens {
    // ============ Spacing (4pt 网格) ============
    val Space1: Dp = 4.dp     // 图标内边距
    val Space2: Dp = 8.dp     // 紧凑列表项间距
    val Space3: Dp = 12.dp    // 按钮内边距 / 卡片间距
    val Space4: Dp = 16.dp    // 标准间距 / 卡片内边距 / 屏幕边距
    val Space5: Dp = 20.dp    // 卡片间距加大
    val Space6: Dp = 24.dp    // Section 间距
    val Space8: Dp = 32.dp    // 大区块间距
    val Space10: Dp = 40.dp   // 极大间距
    val Space12: Dp = 48.dp   // Hero 区域间距

    // ============ 触控目标 (WCAG 2.5.8 + Material) ============
    val TouchMin: Dp = 24.dp           // WCAG 2.5.8 最小（仅密集列表内）
    val TouchRecommended: Dp = 44.dp   // iOS HIG / 通用最佳实践
    val TouchPrimary: Dp = 48.dp       // 主操作（CTA / 快门 / Material 推荐）

    // ============ 组件尺寸 ============
    /** 对齐引导环直径 */
    val GuideRingSize: Dp = 200.dp

    /** 拍照按钮直径 */
    val PhotoButtonSize: Dp = 72.dp

    /** 拍照按钮内层白圆 */
    val PhotoButtonInnerSize: Dp = 60.dp

    /** Bento 卡片间距 (Card Gutter) */
    val CardGutter: Dp = 12.dp

    /** Bento 卡片内边距 */
    val CardPadding: Dp = 18.dp

    /** 屏幕水平边距 */
    val ScreenPadding: Dp = 20.dp

    /** 底部 Tab 高度 */
    val TabBarHeight: Dp = 76.dp

    /** Bento 主卡（今日打卡）高度，跨 2 列 */
    val TodayCardHeight: Dp = 120.dp

    /** 历史宫格图片角标内边距 */
    val HistDatePadding: Dp = 8.dp

    // ============ 描边宽度 ============
    val BorderThin: Dp = 1.dp
    val BorderMedium: Dp = 2.dp
    val BorderThick: Dp = 3.dp        // 引导环未对齐 / 已对齐
    val BorderHero: Dp = 4.dp         // 拍照按钮外圈

    // ============ 阴影 (Elevation) ============
    val Elev0: Dp = 0.dp
    val Elev1: Dp = 2.dp
    val Elev2: Dp = 6.dp
    val Elev3: Dp = 12.dp
}
