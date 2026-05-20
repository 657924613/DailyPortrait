package com.missyun.dailyportrait.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * DailyPortrait 色彩 Token —— Linen Journal（Soft Cloud v2）
 *
 * v2 视觉升级：
 * - 杀掉历史宫格 6 色彩虹背景 → 统一中性奶油占位，让"照片"成为主角
 * - 主色从甜橙 #FF8A5B → 焦糖橘 #D9683E，去糖果感
 * - 背景从偏黄 #F4F1EC → 偏中性 #F6F3EB，更接近真实亚麻布质感
 * - 新增 Ink 系：墨色文字层级（取代默认灰）
 * - 新增 Hairline：发丝级分隔线
 *
 * 设计哲学（参考 Day One / Linear / Arc）：
 * - 整屏不超过 3 种有色（背景米 + 文字墨 + 强调焦糖），其余靠灰阶层级
 * - 装饰元素一律 zero opacity，让用户的照片承担色彩责任
 */
object DPColors {

    // ============ 品牌色（Linen Journal · 焦糖橘） ============

    /** 主色：焦糖橘，比甜橙深、更内敛 */
    val PrimaryLight = Color(0xFFD9683E)
    val PrimaryDark = Color(0xFFE07A52)

    /** 主色容器 */
    val PrimaryContainerLight = Color(0xFFF5DDD0)
    val PrimaryContainerDark = Color(0xFF4A2516)

    val OnPrimaryLight = Color(0xFFFFFFFF)
    val OnPrimaryDark = Color(0xFF1F0A04)
    val OnPrimaryContainerLight = Color(0xFF6B2D14)
    val OnPrimaryContainerDark = Color(0xFFF5DDD0)

    // ============ 次要色 ============

    val SecondaryLight = Color(0xFF6B6660)
    val SecondaryDark = Color(0xFFB5AFA8)
    val SecondaryContainerLight = Color(0xFFEEE9DF)
    val SecondaryContainerDark = Color(0xFF3A3833)
    val OnSecondaryLight = Color(0xFFFFFFFF)
    val OnSecondaryDark = Color(0xFF1A1916)

    // ============ 第三色（绿 · 对齐成功） ============

    val TertiaryLight = Color(0xFF4A7A4F)
    val TertiaryDark = Color(0xFF7BAE7F)
    val TertiaryContainerLight = Color(0xFFD8E8DA)
    val TertiaryContainerDark = Color(0xFF1F3A23)
    val OnTertiaryLight = Color(0xFFFFFFFF)
    val OnTertiaryDark = Color(0xFF0A1A0C)

    // ============ 错误色 ============

    val ErrorLight = Color(0xFFB45A4A)
    val ErrorDark = Color(0xFFCD7464)
    val ErrorContainerLight = Color(0xFFF5DAD3)
    val ErrorContainerDark = Color(0xFF4A1F18)
    val OnErrorLight = Color(0xFFFFFFFF)
    val OnErrorDark = Color(0xFF1A0A08)

    // ============ 背景与表面 ============

    /** 页面主背景：亚麻米（去黄） */
    val SurfaceLight = Color(0xFFF6F3EB)
    val SurfaceDark = Color(0xFF1A1815)

    /** 卡片背景：象牙白（比纯白略暖） */
    val SurfaceContainerLight = Color(0xFFFEFCF7)
    val SurfaceContainerDark = Color(0xFF26231F)

    /** 高层级表面（Sheet / Dialog） */
    val SurfaceContainerHighLight = Color(0xFFFFFEFA)
    val SurfaceContainerHighDark = Color(0xFF2E2B26)

    // ============ Ink 文字层级（取代默认灰） ============

    /** 主标题文字：近黑墨，比纯黑柔和 */
    val OnSurfaceLight = Color(0xFF1A1816)
    val OnSurfaceDark = Color(0xFFEAE6DD)

    /** 次要文字 */
    val OnSurfaceVariantLight = Color(0xFF6B6660)
    val OnSurfaceVariantDark = Color(0xFF9B948B)

    /** 第三层文字（极淡） */
    val OnSurfaceTertiaryLight = Color(0xFFA39E96)
    val OnSurfaceTertiaryDark = Color(0xFF6F6A62)

    // ============ 描边与发丝 ============

    /** 标准描边 */
    val OutlineLight = Color(0xFFD8D2C5)
    val OutlineDark = Color(0xFF3D3A34)

    /** 发丝级分隔线（仅 1px 用） */
    val OutlineVariantLight = Color(0xFFEAE5D9)
    val OutlineVariantDark = Color(0xFF2A2722)

    // ============ 历史宫格背景（v2：统一中性奶油） ============
    //
    // v1 用了 6 色彩虹渐变作为占位 → 显得幼稚
    // v2 统一为单一奶油色 + 极淡阴影，让真实照片承担所有色彩
    //
    // 这些常量保留是为了向后兼容旧组件，但新代码应使用 [PhotoPlaceholderLight/Dark]

    val PhotoPlaceholderLight = Color(0xFFEDE7DA)
    val PhotoPlaceholderDark = Color(0xFF2A2722)

    /** 已废弃的彩虹渐变（v1.0 残留）—— 逐步迁移完毕后移除 */
    @Deprecated("v2 改用 PhotoPlaceholder 单一中性色，避免彩虹拼凑感")
    val HistGradient1Start = Color(0xFFEDE7DA)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient1End = Color(0xFFDED5C2)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient2Start = Color(0xFFEDE7DA)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient2End = Color(0xFFDED5C2)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient3Start = Color(0xFFEDE7DA)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient3End = Color(0xFFDED5C2)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient4Start = Color(0xFFEDE7DA)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient4End = Color(0xFFDED5C2)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient5Start = Color(0xFFEDE7DA)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient5End = Color(0xFFDED5C2)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient6Start = Color(0xFFEDE7DA)
    @Deprecated("v2 改用 PhotoPlaceholder")
    val HistGradient6End = Color(0xFFDED5C2)
}
