package com.missyun.dailyportrait.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用级偏好（DataStore）
 *
 * 严格遵循 architecture-android.md：
 * - 单例由 Hilt 注入
 * - 所有读写都是 suspend 或 Flow
 *
 * v1.2 扩展：
 * - 提醒开关 / 提醒时间（小时 + 分钟）
 */
private val Context.dataStore by preferencesDataStore(name = "daily_portrait_prefs")

@Singleton
class AppPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // ============ Onboarding ============

    val isFirstLaunch: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_FIRST_LAUNCH] ?: true }

    suspend fun markOnboardingDone() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = false
        }
    }

    /**
     * 重置 onboarding 状态 —— 让用户下次进 App 重新看 3 页引导
     */
    suspend fun resetOnboarding() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_LAUNCH] = true
        }
    }

    // ============ 拍照提醒 ============

    /** 是否开启每日提醒，默认关闭（避免新装用户被打扰） */
    val reminderEnabled: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_REMINDER_ENABLED] ?: false }

    /** 提醒小时，0~23，默认 9（早上 9 点） */
    val reminderHour: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_REMINDER_HOUR] ?: DEFAULT_HOUR }

    /** 提醒分钟，0~59，默认 0 */
    val reminderMinute: Flow<Int> = context.dataStore.data
        .map { prefs -> prefs[KEY_REMINDER_MINUTE] ?: DEFAULT_MINUTE }

    /**
     * 一次性读取完整提醒配置（调度 AlarmManager 时用）
     */
    suspend fun reminderConfig(): ReminderConfig {
        val snapshot = context.dataStore.data.first()
        return ReminderConfig(
            enabled = snapshot[KEY_REMINDER_ENABLED] ?: false,
            hour = snapshot[KEY_REMINDER_HOUR] ?: DEFAULT_HOUR,
            minute = snapshot[KEY_REMINDER_MINUTE] ?: DEFAULT_MINUTE
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun setReminderTime(hour: Int, minute: Int) {
        require(hour in 0..23 && minute in 0..59) { "时间无效: $hour:$minute" }
        context.dataStore.edit { prefs ->
            prefs[KEY_REMINDER_HOUR] = hour
            prefs[KEY_REMINDER_MINUTE] = minute
        }
    }

    // ============ 首拍庆祝引导 ============

    /** 是否已展示过"首张照片庆祝"引导卡片 */
    val hasShownFirstShotCelebration: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[KEY_FIRST_SHOT_CELEBRATED] ?: false }

    suspend fun markFirstShotCelebrated() {
        context.dataStore.edit { prefs ->
            prefs[KEY_FIRST_SHOT_CELEBRATED] = true
        }
    }

    private companion object {
        val KEY_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val KEY_REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val KEY_FIRST_SHOT_CELEBRATED = booleanPreferencesKey("first_shot_celebrated")

        const val DEFAULT_HOUR = 9
        const val DEFAULT_MINUTE = 0
    }
}

/**
 * 提醒配置快照
 */
data class ReminderConfig(
    val enabled: Boolean,
    val hour: Int,
    val minute: Int
)
