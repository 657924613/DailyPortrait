# ProGuard / R8 规则
#
# 修订原则：
# - 不要"-keep class com.missyun.** { *; }"——这会让 R8 完全失效
# - 仅 keep 反射 / 序列化 / Native 调用真实需要的类
# - Hilt / Room / Compose 各自的 consumer rules 已通过依赖自动包含

# ==================== Room Entity ====================
# Room 需要保留 Entity 字段名（KSP 生成的 _Impl 类按字段名反射）
-keep class com.missyun.dailyportrait.data.local.DailyPhoto { *; }
-keep class com.missyun.dailyportrait.data.local.DailyPhoto$* { *; }

# ==================== Domain 模型 ====================
# ExportConfig / ExportState 是 Service / UI / UseCase 共享的 sealed class，保留以防 R8 误删 sealed 子类
-keep class com.missyun.dailyportrait.domain.model.** { *; }

# ==================== Hilt / Dagger ====================
# Hilt 自带的 consumer rules 已足够。这里仅显式 keep BroadcastReceiver / Service 入口
-keep class com.missyun.dailyportrait.service.VideoRenderService { *; }
-keep class com.missyun.dailyportrait.data.notification.ReminderReceiver { *; }
-keep class com.missyun.dailyportrait.data.notification.BootReceiver { *; }
-keep class com.missyun.dailyportrait.MainActivity { *; }
-keep class com.missyun.dailyportrait.DailyPortraitApp { *; }

# ==================== Kotlin 协程 ====================
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# ==================== Media3 Transformer ====================
# Media3 内部用反射加载 codec，必须保留相关 keep
-keep class androidx.media3.** { *; }

# ==================== ML Kit 静态版 ====================
-keep class com.google.mlkit.** { *; }

# ==================== 调试代码剔除 ====================
# Release 包剔除 TestDataSeeder（防止误开发者菜单暴露）
# 由 BuildConfig.DEBUG 守门 + 此处 R8 优化双保险
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
}
