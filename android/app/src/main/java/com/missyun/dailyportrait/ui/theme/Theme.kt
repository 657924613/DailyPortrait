package com.missyun.dailyportrait.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 浅色配色方案 (Soft Cloud)
 * 使用 [DPColors] 定义的 Light Token
 */
private val SoftCloudLightScheme = lightColorScheme(
    primary = DPColors.PrimaryLight,
    onPrimary = DPColors.OnPrimaryLight,
    primaryContainer = DPColors.PrimaryContainerLight,
    onPrimaryContainer = DPColors.OnPrimaryContainerLight,

    secondary = DPColors.SecondaryLight,
    onSecondary = DPColors.OnSecondaryLight,
    secondaryContainer = DPColors.SecondaryContainerLight,
    onSecondaryContainer = DPColors.OnSurfaceLight,

    tertiary = DPColors.TertiaryLight,
    onTertiary = DPColors.OnTertiaryLight,
    tertiaryContainer = DPColors.TertiaryContainerLight,
    onTertiaryContainer = DPColors.OnSurfaceLight,

    error = DPColors.ErrorLight,
    onError = DPColors.OnErrorLight,
    errorContainer = DPColors.ErrorContainerLight,
    onErrorContainer = DPColors.OnSurfaceLight,

    background = DPColors.SurfaceLight,
    onBackground = DPColors.OnSurfaceLight,
    surface = DPColors.SurfaceLight,
    onSurface = DPColors.OnSurfaceLight,
    surfaceVariant = DPColors.SurfaceContainerLight,
    onSurfaceVariant = DPColors.OnSurfaceVariantLight,
    surfaceContainer = DPColors.SurfaceContainerLight,
    surfaceContainerHigh = DPColors.SurfaceContainerHighLight,

    outline = DPColors.OutlineLight,
    outlineVariant = DPColors.OutlineVariantLight
)

/**
 * 深色配色方案 (Soft Cloud Dark)
 * 背景仍偏暖（非纯黑），保持 Soft Cloud 调性
 */
private val SoftCloudDarkScheme = darkColorScheme(
    primary = DPColors.PrimaryDark,
    onPrimary = DPColors.OnPrimaryDark,
    primaryContainer = DPColors.PrimaryContainerDark,
    onPrimaryContainer = DPColors.OnPrimaryContainerDark,

    secondary = DPColors.SecondaryDark,
    onSecondary = DPColors.OnSecondaryDark,
    secondaryContainer = DPColors.SecondaryContainerDark,
    onSecondaryContainer = DPColors.OnSurfaceDark,

    tertiary = DPColors.TertiaryDark,
    onTertiary = DPColors.OnTertiaryDark,
    tertiaryContainer = DPColors.TertiaryContainerDark,
    onTertiaryContainer = DPColors.OnSurfaceDark,

    error = DPColors.ErrorDark,
    onError = DPColors.OnErrorDark,
    errorContainer = DPColors.ErrorContainerDark,
    onErrorContainer = DPColors.OnSurfaceDark,

    background = DPColors.SurfaceDark,
    onBackground = DPColors.OnSurfaceDark,
    surface = DPColors.SurfaceDark,
    onSurface = DPColors.OnSurfaceDark,
    surfaceVariant = DPColors.SurfaceContainerDark,
    onSurfaceVariant = DPColors.OnSurfaceVariantDark,
    surfaceContainer = DPColors.SurfaceContainerDark,
    surfaceContainerHigh = DPColors.SurfaceContainerHighDark,

    outline = DPColors.OutlineDark,
    outlineVariant = DPColors.OutlineVariantDark
)

/**
 * 应用主题入口
 *
 * @param darkTheme 是否使用深色主题，默认跟随系统
 * @param dynamicColor 是否启用 Material You 动态取色（仅 Android 12+）
 *                     默认关闭：DailyPortrait 是品牌驱动的肖像应用，
 *                     固定的珊瑚橘是核心识别符，不允许跟随用户壁纸变化
 */
@Composable
fun DailyPortraitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> SoftCloudDarkScheme
        else -> SoftCloudLightScheme
    }

    // 状态栏图标颜色：Android 15+ edge-to-edge 模式下背景已透明,
    // 仅需控制状态栏图标的明暗（深色背景 → 浅色图标，反之）
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = DPTypography,
        shapes = DPShapes,
        content = content
    )
}
