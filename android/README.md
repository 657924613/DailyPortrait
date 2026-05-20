# DailyPortrait · Android

按 `architecture-android.md` 分步骤构建，视觉系统采用 Soft Cloud（米色 + 珊瑚橘）。

## 步骤进度（全部完成）

| 步骤 | 内容 | 状态 |
|------|------|------|
| 0 | 设计系统 Token + 项目脚手架 | ✅ |
| 1 | Data 层基建（Room Entity/Dao/Database） | ✅ |
| 2 | FileManager + PhotoRepository | ✅ |
| 3 | ML Kit FaceAnalyzer | ✅ |
| 4 | Camera UI + ViewModel（核心难点） | ✅ |
| 5 | Dashboard 主页（Bento 风格） | ✅ |
| 6 | 导航 + Onboarding | ✅ |
| 7 | Media3 视频合成 | ✅ |
| 8 | A11y 全面检查 + 错误处理补全 | ✅ |

## 项目结构

```
android/
├─ app/
│  ├─ build.gradle.kts
│  ├─ proguard-rules.pro
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/missyun/dailyportrait/
│     │  ├─ DailyPortraitApp.kt              # @HiltAndroidApp
│     │  ├─ MainActivity.kt                  # Compose 入口（NavHost 挂载）
│     │  ├─ data/
│     │  │  ├─ analyzer/
│     │  │  │  ├─ FaceAnalyzer.kt
│     │  │  │  └─ FaceAnalyzerFactory.kt
│     │  │  ├─ local/
│     │  │  │  ├─ AppDatabase.kt
│     │  │  │  ├─ DailyPhoto.kt
│     │  │  │  ├─ DailyPhotoDao.kt
│     │  │  │  └─ Migrations.kt
│     │  │  ├─ preferences/AppPreferences.kt # DataStore
│     │  │  ├─ repository/PhotoRepositoryImpl.kt
│     │  │  ├─ storage/
│     │  │  │  ├─ FileManager.kt              # 沙盒 IO + EXIF + WEBP
│     │  │  │  └─ VideoOutputManager.kt
│     │  │  └─ video/Media3VideoExporter.kt
│     │  ├─ di/
│     │  │  ├─ DatabaseModule.kt
│     │  │  ├─ RepositoryModule.kt
│     │  │  └─ VideoModule.kt
│     │  ├─ domain/
│     │  │  ├─ model/
│     │  │  │  ├─ ExportState.kt
│     │  │  │  └─ FaceAlignment.kt
│     │  │  ├─ repository/PhotoRepository.kt
│     │  │  ├─ usecase/GenerateVideoUseCase.kt
│     │  │  └─ util/
│     │  │     ├─ BitmapUtil.kt
│     │  │     └─ DateUtil.kt
│     │  ├─ service/VideoRenderService.kt
│     │  └─ ui/
│     │     ├─ components/
│     │     │  ├─ BentoCard.kt
│     │     │  ├─ EmptyState.kt
│     │     │  ├─ GuideRing.kt
│     │     │  ├─ HistoryThumb.kt
│     │     │  ├─ RollUpNumber.kt
│     │     │  ├─ ShutterButton.kt
│     │     │  └─ WeekProgressRing.kt
│     │     ├─ navigation/
│     │     │  ├─ AppNavHost.kt
│     │     │  └─ Route.kt
│     │     ├─ screens/
│     │     │  ├─ camera/
│     │     │  │  ├─ CameraIntent.kt
│     │     │  │  ├─ CameraScreen.kt
│     │     │  │  ├─ CameraUiState.kt
│     │     │  │  └─ CameraViewModel.kt
│     │     │  ├─ dashboard/
│     │     │  │  ├─ DashboardScreen.kt
│     │     │  │  ├─ DashboardUiState.kt
│     │     │  │  └─ DashboardViewModel.kt
│     │     │  ├─ onboarding/
│     │     │  │  ├─ OnboardingScreen.kt
│     │     │  │  └─ OnboardingViewModel.kt
│     │     │  └─ videoexport/
│     │     │     ├─ VideoExportSheet.kt
│     │     │     └─ VideoExportViewModel.kt
│     │     ├─ theme/
│     │     │  ├─ Color.kt
│     │     │  ├─ Dimens.kt
│     │     │  ├─ Motion.kt
│     │     │  ├─ Shape.kt
│     │     │  ├─ Theme.kt
│     │     │  └─ Type.kt
│     │     └─ util/MotionPreference.kt        # reduce motion 检测
│     └─ res/
│        ├─ values/                            # strings / themes / colors
│        ├─ values-night/                      # 深色模式
│        └─ xml/                               # backup_rules / data_extraction_rules
├─ gradle/libs.versions.toml                   # 版本目录
├─ build.gradle.kts
├─ settings.gradle.kts
├─ gradle.properties
├─ A11Y-CHECKLIST.md                           # 步骤 8 完整验收清单
└─ README.md
```

## 视觉系统：Soft Cloud

| Token | 值 | 用途 |
|-------|-----|------|
| `colorScheme.primary` | `#FF8A5B`（珊瑚橘） | CTA / 强调 / 数字 |
| `colorScheme.surface` Light | `#F4F1EC`（米色） | 页面背景 |
| `colorScheme.surface` Dark | `#1C1A18`（暖深棕） | 深色背景非纯黑 |
| `colorScheme.tertiary` | `#38A56F` / `#4CC38A` | 对齐成功 |
| `colorScheme.error` | `#F26B5E` / `#FF7A6E` | 未对齐 / 错误 |
| `dynamicColor` | **关闭** | 品牌色不跟随系统壁纸 |

## 文件统计

| 层 | 文件数 |
|----|-------|
| `ui/theme/` | 6 |
| `ui/components/` | 7 |
| `ui/screens/camera/` | 4 |
| `ui/screens/dashboard/` | 3 |
| `ui/screens/onboarding/` | 2 |
| `ui/screens/videoexport/` | 2 |
| `ui/navigation/` | 2 |
| `ui/util/` | 1 |
| `data/local/` | 4 |
| `data/storage/` | 2 |
| `data/repository/` | 1 |
| `data/analyzer/` | 2 |
| `data/video/` | 1 |
| `data/preferences/` | 1 |
| `domain/model/` | 2 |
| `domain/repository/` | 1 |
| `domain/usecase/` | 1 |
| `domain/util/` | 2 |
| `service/` | 1 |
| `di/` | 3 |
| 根（MainActivity + Application） | 2 |
| **合计 Kotlin 源文件** | **50** |
| Gradle / 资源 / Manifest | 12 |

## 待补资源（需打开项目同步一次）

Android Studio 创建项目时自动生成的二进制 / 平台特定文件，本仓库未生成：

- `res/mipmap-anydpi-v26/ic_launcher.xml` 与各密度 launcher 图标
- `gradle/wrapper/gradle-wrapper.jar` + `gradle-wrapper.properties`
- `gradlew` / `gradlew.bat`

**快速补齐方式**：在 Android Studio 中 `File → New → New Project` 创建空项目，把生成的上述文件拷到 `android/` 对应位置。或在已安装 Gradle 的命令行执行 `gradle wrapper --gradle-version 8.9`。

## 验收

详见 [A11Y-CHECKLIST.md](./A11Y-CHECKLIST.md) —— 12 大项验收清单覆盖：

1. 触控目标尺寸 ≥ 48dp
2. contentDescription 完整性
3. 非颜色反馈（WCAG 1.4.1）
4. 动效偏好 reduce motion
5. 字体缩放 1.5x
6. TalkBack 朗读测试
7. 错误处理与可恢复路径
8. imageProxy.close() 全路径
9. 协程 / 主线程禁项回检
10. 深色模式
11. Manifest / 权限 / 备份
12. architecture-android.md §8 最终验收勾选

## 已知妥协 / 后续 Polish

- **crossfade 250ms 未实现**：Media3 1.5 标准 API transition 支持有限，当前是无缝衔接。
- **缩略图飞入路径**：当前 scaleIn + fadeIn 简化（视觉等价但路径不是从快门飞出）。
- **launcher 图标 / gradle wrapper**：需在 Android Studio 打开后同步一次。
- **真机字体 1.5x 验证**：建议交付前由人工目检主流程一次。
