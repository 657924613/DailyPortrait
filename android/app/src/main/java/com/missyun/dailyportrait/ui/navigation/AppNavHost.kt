package com.missyun.dailyportrait.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.missyun.dailyportrait.data.preferences.AppPreferences
import com.missyun.dailyportrait.ui.screens.camera.CameraScreen
import com.missyun.dailyportrait.ui.screens.dashboard.DashboardScreen
import com.missyun.dailyportrait.ui.screens.onboarding.OnboardingScreen
import com.missyun.dailyportrait.ui.screens.settings.SettingsScreen
import com.missyun.dailyportrait.ui.screens.stats.StatisticsScreen
import com.missyun.dailyportrait.ui.theme.DPMotion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 应用导航根
 *
 * 严格按 architecture-android.md §4.5：
 * - 启动时检查 isFirstLaunch → true 则起点为 Onboarding，否则 Dashboard
 * - Dashboard ↔ Camera 使用 slideInVertically / slideOutVertically 转场
 * - Onboarding 完成后 popBackStack 防止用户回退
 *
 * 步骤 7 的 VideoExportSheet 会在 Dashboard 内以 ModalBottomSheet 形式呈现，
 * 不走独立路由，因此此处只声明 3 个 composable destination。
 */
@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    rootViewModel: AppRootViewModel = hiltViewModel()
) {
    val rootState by rootViewModel.uiState.collectAsState()

    // 仍在加载 isFirstLaunch 时显示空白防止闪烁
    if (rootState is AppRootUiState.Loading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val startDestination = when (rootState) {
        is AppRootUiState.NeedsOnboarding -> Route.Onboarding
        is AppRootUiState.Ready -> Route.Dashboard
        else -> Route.Dashboard
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize()
    ) {
        composable(Route.Onboarding) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Route.Dashboard) {
                        popUpTo(Route.Onboarding) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.Dashboard,
            enterTransition = {
                fadeIn(animationSpec = tween(DPMotion.DurationBase))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(DPMotion.DurationFast))
            }
        ) {
            DashboardScreen(
                onNavigateToCamera = { navController.navigate(Route.Camera) },
                onOpenExportSheet = {
                    // 步骤 7 实现，目前留空（Dashboard 内部 ModalBottomSheet）
                },
                onOpenSettings = { navController.navigate(Route.Settings) },
                onOpenStatistics = { navController.navigate(Route.Statistics) }
            )
        }

        composable(
            route = Route.Settings,
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(DPMotion.DurationBase, easing = DPMotion.EaseStandard),
                    initialOffsetY = { it / 4 }
                ) + fadeIn()
            },
            exitTransition = {
                fadeOut(animationSpec = tween(DPMotion.DurationFast))
            }
        ) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onReplayOnboarding = {
                    // 跳到 Onboarding 路由，并清掉栈历史避免按返回键回到设置页
                    navController.navigate(Route.Onboarding) {
                        popUpTo(Route.Dashboard) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = Route.Statistics,
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(DPMotion.DurationBase, easing = DPMotion.EaseStandard),
                    initialOffsetY = { it / 4 }
                ) + fadeIn()
            },
            exitTransition = {
                fadeOut(animationSpec = tween(DPMotion.DurationFast))
            }
        ) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Route.Camera,
            enterTransition = {
                slideInVertically(
                    animationSpec = tween(DPMotion.DurationSlow, easing = DPMotion.EaseEmphasized),
                    initialOffsetY = { it }
                )
            },
            exitTransition = {
                slideOutVertically(
                    animationSpec = tween(DPMotion.DurationSlow, easing = DPMotion.EaseEmphasized),
                    targetOffsetY = { it }
                )
            }
        ) {
            CameraScreen(
                onClose = { navController.popBackStack() }
            )
        }
    }
}

/* ============ 根状态 ViewModel：决定首启走 Onboarding 还是 Dashboard ============ */
sealed class AppRootUiState {
    data object Loading : AppRootUiState()
    data object NeedsOnboarding : AppRootUiState()
    data object Ready : AppRootUiState()
}

@HiltViewModel
class AppRootViewModel @Inject constructor(
    preferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppRootUiState>(AppRootUiState.Loading)
    val uiState: StateFlow<AppRootUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // 只取首次值即可，后续切换由 NavGraph popBackStack 接管
            val firstLaunch = preferences.isFirstLaunch.first()
            _uiState.value = if (firstLaunch) AppRootUiState.NeedsOnboarding
                             else AppRootUiState.Ready
        }
    }
}
