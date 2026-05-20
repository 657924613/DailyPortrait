package com.missyun.dailyportrait.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missyun.dailyportrait.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Onboarding ViewModel
 *
 * 仅一个职责：用户完成或跳过引导时把 [AppPreferences.markOnboardingDone] 写入。
 * 之后 NavGraph 会监听 isFirstLaunch == false 决定不再显示 Onboarding。
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val preferences: AppPreferences
) : ViewModel() {

    /**
     * 标记引导完成
     * @param onDone 持久化成功后回调，由 UI 层执行 Navigation
     */
    fun finishOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            preferences.markOnboardingDone()
            onDone()
        }
    }
}
