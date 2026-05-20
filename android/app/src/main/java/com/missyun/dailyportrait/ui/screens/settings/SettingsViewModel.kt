package com.missyun.dailyportrait.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.missyun.dailyportrait.data.debug.TestDataSeeder
import com.missyun.dailyportrait.data.notification.ReminderScheduler
import com.missyun.dailyportrait.data.preferences.AppPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 设置页 ViewModel
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val preferences: AppPreferences,
    private val scheduler: ReminderScheduler,
    private val testDataSeeder: TestDataSeeder
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _toast = Channel<String>(Channel.BUFFERED)
    val toast get() = _toast.receiveAsFlow()

    /** 一次性事件：重看引导后回到 App 入口 */
    private val _navigateToOnboarding = Channel<Unit>(Channel.BUFFERED)
    val navigateToOnboarding get() = _navigateToOnboarding.receiveAsFlow()

    init {
        viewModelScope.launch {
            combine(
                preferences.reminderEnabled,
                preferences.reminderHour,
                preferences.reminderMinute
            ) { enabled, hour, minute ->
                SettingsUiState(
                    isLoading = false,
                    reminderEnabled = enabled,
                    reminderHour = hour,
                    reminderMinute = minute
                )
            }.collect { newState ->
                _uiState.value = newState.copy(isSeeding = _uiState.value.isSeeding)
            }
        }
    }

    fun toggleReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReminderEnabled(enabled)
            if (enabled) {
                val config = preferences.reminderConfig()
                scheduler.schedule(config.hour, config.minute)
            } else {
                scheduler.cancel()
            }
        }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch {
            preferences.setReminderTime(hour, minute)
            val config = preferences.reminderConfig()
            if (config.enabled) {
                scheduler.cancel()
                scheduler.schedule(hour, minute)
            }
        }
    }

    /**
     * 调试：生成 30 天测试照片
     */
    fun seedTestData(days: Int = 30) {
        if (_uiState.value.isSeeding) return
        _uiState.value = _uiState.value.copy(isSeeding = true)
        viewModelScope.launch {
            try {
                val written = testDataSeeder.seed(days)
                _toast.send("已生成 $written 张测试占位图，拍真照片可覆盖")
            } catch (e: Exception) {
                _toast.send("生成失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSeeding = false)
            }
        }
    }

    /**
     * 调试：清空所有照片
     */
    fun wipeAll() {
        if (_uiState.value.isSeeding) return
        _uiState.value = _uiState.value.copy(isSeeding = true)
        viewModelScope.launch {
            try {
                val removed = testDataSeeder.wipeAll()
                _toast.send("已清除 $removed 张照片，可以重新开始")
            } catch (e: Exception) {
                _toast.send("清除失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isSeeding = false)
            }
        }
    }

    /**
     * 重新观看引导 —— 重置首次启动标志，触发导航回 Onboarding
     */
    fun replayOnboarding() {
        viewModelScope.launch {
            preferences.resetOnboarding()
            _navigateToOnboarding.send(Unit)
        }
    }

    /**
     * 发送一条测试通知 —— 用户开启提醒后能立即验证是否生效
     * @param context Activity Context（NotificationManagerCompat 需要）
     */
    fun sendTestNotification(context: android.content.Context) {
        viewModelScope.launch {
            try {
                com.missyun.dailyportrait.data.notification.ReminderNotifier
                    .show(context.applicationContext, isTest = true)
                _toast.send("已发送测试通知")
            } catch (e: Exception) {
                _toast.send("发送失败：${e.message}")
            }
        }
    }
}

data class SettingsUiState(
    val isLoading: Boolean = true,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val isSeeding: Boolean = false
) {
    val reminderTimeLabel: String
        get() = "%02d:%02d".format(reminderHour, reminderMinute)
}
