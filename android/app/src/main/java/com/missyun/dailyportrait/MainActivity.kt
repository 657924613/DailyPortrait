package com.missyun.dailyportrait

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.missyun.dailyportrait.ui.navigation.AppNavHost
import com.missyun.dailyportrait.ui.theme.DailyPortraitTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 应用主入口 Activity
 *
 * 严格按 architecture-android.md §4.5：MainActivity 仅承担两件事
 * 1. 设置 Compose 主题
 * 2. 挂载 [AppNavHost] 接管全部导航
 *
 * 业务逻辑（首启 Onboarding / 切换路由）全部在 NavHost 内完成。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DailyPortraitTheme {
                AppNavHost()
            }
        }
    }
}
