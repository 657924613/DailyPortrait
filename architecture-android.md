# 每日自拍延时摄影 App (DailyPortrait) - 架构与设计说明书 v2.0

> **【系统指令：致 AI 编程助手】**
> 你现在是一位拥有 10 年经验的资深 Android 架构师兼 UI/UX 设计师。请严格按照本说明书的架构规范、技术栈、设计系统和分步指令，为我输出高质量、健壮且包含详尽中文注释的 Kotlin 代码。
>
> **核心原则**：
> 1. 严格使用最新的 Jetpack Compose 声明式 UI + Material 3。
> 2. 遵循 Clean Architecture + MVI 架构，严格分离 UI 层、Domain 层与 Data 层。
> 3. 代码必须考虑异常处理（如相机权限被拒、存储空间不足、人脸识别失败、首次使用空状态）。
> 4. **绝对禁止**生成任何依赖 Google Play 服务的动态 ML Kit 代码，必须使用 Bundled 静态打包版。
> 5. 所有 UI 组件必须符合 WCAG 2.2 AA 可访问性标准（触控目标 ≥48dp、颜色非唯一反馈、contentDescription 完整）。
> 6. **绝对禁止**在主线程操作 Room 数据库或文件 I/O，必须使用 Kotlin Coroutines。
> 7. **绝对禁止**使用 `java.io.File` 绝对路径存储图片，必须通过 `FileManager` 使用沙盒相对路径。

---

## 1. 设计系统 (Design Tokens) — 先行定义

> **所有 UI 代码必须引用以下 Token，禁止硬编码颜色值或尺寸。**

### 1.1 色彩系统 (Material 3 Color Scheme)

视觉方向：**Minimalist Dark + Warm Accent**，深色基底让用户自拍成为唯一视觉焦点，暖金色强调"成就与记录"感。

| Token | Light Value | Dark Value | 用途 |
|-------|-------------|------------|------|
| Primary | `#B8860B` (DarkGoldenRod) | `#F5A623` (WarmGold) | 主按钮、打卡强调 |
| PrimaryContainer | `#FFE082` | `#3E2A00` | 按钮容器背景 |
| Secondary | `#6B7280` | `#9CA3AF` | 次要文字、图标 |
| Tertiary | `#059669` (Emerald) | `#30D158` | 对齐成功/绿色反馈 |
| Error | `#DC2626` | `#FF453A` | 未对齐/权限被拒 |
| Surface | `#FAFAFA` | `#1A1A2E` | 页面背景 |
| SurfaceVariant | `#F3F4F6` | `#252540` | Bento 卡片背景 |
| OnSurface | `#111827` | `#E5E7EB` | 主要文字 |
| OnSurfaceVariant | `#6B7280` | `#9CA3AF` | 次要文字 |
| Outline | `#D1D5DB` | `#374151` | 卡片描边 |

**对齐取景框状态色**：
- 未检测到人脸：`Outline`（灰色描边）
- 检测到但未对齐：`Error`（红色 `#FF453A`）
- 已对齐：`Tertiary`（绿色 `#30D158`）

### 1.2 字体层级 (Type Scale)

| Token | Size | Weight | LetterSpacing | LineHeight | 用途 |
|-------|------|--------|---------------|------------|------|
| DisplaySmall | 36sp | Bold (700) | 0 | 44sp | 连续打卡天数大数字 |
| HeadlineMedium | 28sp | SemiBold (600) | 0 | 36sp | Dashboard 标题 |
| TitleLarge | 22sp | Medium (500) | 0 | 28sp | Bento 卡片标题 |
| TitleMedium | 16sp | Medium (500) | 0.15sp | 24sp | 按钮文字、导航 |
| BodyLarge | 16sp | Regular (400) | 0.5sp | 24sp | 正文 |
| BodyMedium | 14sp | Regular (400) | 0.25sp | 20sp | 辅助说明 |
| LabelMedium | 12sp | Medium (500) | 0.5sp | 16sp | 标签、日期 |

**字体族**：系统默认（`FontFamily.Default`），禁止引入额外字体文件。

### 1.3 形状与间距 (Shape & Spacing)

| Token | Value | 用途 |
|-------|-------|------|
| CardCorner | 20dp | Bento 卡片圆角 |
| ButtonCorner | 12dp | 按钮圆角（拍照按钮用 CircleShape，即 50% 圆） |
| ChipCorner | 8dp | 标签、状态指示器 |
| CardGutter | 10dp | Bento 栅格卡片间距 |
| CardPadding | 16dp | 卡片内边距 |
| ScreenPadding | 16dp | 页面边距 |
| TouchTargetMin | 48dp | 所有可交互元素的最小触控区域 |
| GuideRingSize | 200dp | 对齐引导环直径 |
| PhotoButtonSize | 72dp | 拍照按钮直径 |

---

## 2. 核心技术栈与版本要求

请在生成的 `build.gradle.kts` 中使用以下（或更新的）稳定版本：

*   **开发语言**：Kotlin 1.9+ (启用 KSP)
*   **UI 框架**：Jetpack Compose (使用 Compose BOM `2025.12.00` 及以上) + Material 3
*   **架构组件**：ViewModel, Lifecycle, Hilt (依赖注入) + Navigation Compose
*   **本地数据库**：Room Database (`2.6.+`)，须包含 Migration 策略
*   **相机框架**：CameraX (`1.3.+`) - 包含 `camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view`
*   **AI 视觉库**：Google ML Kit 静态版 (`com.google.mlkit:face-detection:16.1.7`)
*   **多媒体合成**：Jetpack Media3 Transformer (`1.5.+`)
*   **异步流**：Kotlin Coroutines & StateFlow
*   **图片加载**：Coil Compose (`2.7.+`)

---

## 3. 推荐的包名与目录结构

请按照以下结构组织生成的代码，确保职能分离：

*   `com.missyun.dailyportrait`
    *   `data/`
        *   `local/` (Room 实体 Entity、Dao 接口、Database 抽象类、Migration)
        *   `storage/` (沙盒文件读写工具类 FileManager)
        *   `repository/` (数据仓库的实现类)
    *   `domain/`
        *   `model/` (UI 层需要的领域模型)
        *   `usecase/` (如 GenerateVideoUseCase, AnalyzeFaceUseCase)
        *   `repository/` (数据仓库的接口定义)
    *   `ui/`
        *   `theme/` (Color.kt, Type.kt, Shape.kt — 基于第 1 节 Design Tokens)
        *   `navigation/` (NavGraph.kt, Route 定义)
        *   `screens/`
            *   `dashboard/` (DashboardScreen、DashboardViewModel、DashboardUiState)
            *   `camera/` (CameraScreen、CameraViewModel、CameraUiState)
            *   `onboarding/` (首次引导 OnboardingScreen)
            *   `videoexport/` (导出进度 VideoExportUiState)
        *   `components/` (复用的 Compose 组件：BentoCard, GuideRing, RollUpNumber, ShutterButton, EmptyState)
    *   `service/`
        *   `VideoRenderService` (视频合成前台服务)
    *   `di/` (Hilt Modules: DatabaseModule, RepositoryModule, UseCaseModule)
    *   `MainActivity`

---

## 4. 核心模块详细技术规范

### 4.1 Data 层：Room 数据库与文件存储

*   **Entity (`DailyPhoto`)**：
    *   `id`: Long (PrimaryKey, autoGenerate = true)
    *   `date`: String (格式 `yyyy-MM-dd`，需建立 Unique 索引，保证每天只有一条最终记录)
    *   `imagePath`: String (必须存储**相对路径**，防止 Android 沙盒绝对路径变更导致图片失效)
    *   `timestamp`: Long
*   **Migration 策略**：`AppDatabase` 必须包含 `Room.databaseBuilder` 的 `.addMigrations(MIGRATION_1_2, ...)` 占位逻辑。
*   **FileManager 规范 (防坑必读)**：
    *   所有图片保存在 `Context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)`。
    *   保存时，**必须**使用 `ExifInterface` 和 `Matrix` 解决 **CameraX 拍照偶尔带有 EXIF 旋转角（Orientation）导致图片倒置的经典 Bug**。
    *   强制将图片压缩为 **WEBP 格式 (质量 80%)**，宽度最大限制为 1080px，以大幅节省存储空间。
    *   返回路径始终为相对路径（相对于 `getExternalFilesDir(Environment.DIRECTORY_PICTURES)`）。

### 4.2 UI & Domain 层：CameraX 与 ML Kit 深度结合

*   **MVI 状态管理**：`CameraUiState` 需包含：
    *   `isCameraReady: Boolean` — 相机初始化状态
    *   `latestPhotoPath: String?` — 用于洋葱皮叠加，`null` 表示首次使用
    *   `faceAlignmentStatus: FaceAlignment` — 枚举：`NONE`（未检测到人脸）, `DETECTED`（检测到但未对齐）, `ALIGNED`（已对齐，可拍照）
    *   `cameraPermissionGranted: Boolean` — 权限状态
    *   `isCapturing: Boolean` — 正在拍照中（防连点）
    *   `error: CameraError?` — 错误状态
*   **`CameraError` 密封类**：
    *   `PermissionDenied` — 引导用户去设置开启权限
    *   `StorageFull(availableBytes: Long)` — 存储空间不足
    *   `CameraUnavailable(throwable: Throwable)` — 相机硬件不可用
    *   `FaceDetectionInitFailed(throwable: Throwable)` — ML Kit 初始化失败（降级为无辅助手动拍）
*   **洋葱皮叠加 (Onion Skinning)**：
    *   在 Compose 中，底层是 `PreviewView`（AndroidView 包装），上层放置一个 `AsyncImage`（使用 Coil 库加载 `latestPhotoPath`）。
    *   `alpha` 设置为 `0.35f`，`contentScale = ContentScale.Crop`。
    *   **首次使用**：`latestPhotoPath == null` 时，不显示叠加层，改为显示半透明引导文字："拍摄你的第一张自拍。明天起，这里将显示昨天的照片作为对齐参考。"
*   **对齐引导环 (Guide Ring)**：
    *   居中显示一个直径 `GuideRingSize`(200dp) 的圆环（`border = 2.dp, Brush`）。
    *   颜色根据 `faceAlignmentStatus` 使用动画过渡（`animateColorAsState`, `tween(300ms)`）：
        *   `NONE` → `Outline` 灰色 + 虚线样式
        *   `DETECTED` → `Error` 红色 + 实线
        *   `ALIGNED` → `Tertiary` 绿色 + 实线 + 轻微脉冲放大 (`scale` 1.0→1.05→1.0, `repeatable`, `tween(600ms)`)
    *   对齐误差阈值：**8%**（约 30px），比 10% 更严格的体验
*   **ML Kit 分析器 (`FaceAnalyzer`)**：
    *   实现 CameraX 的 `ImageAnalysis.Analyzer`。
    *   `FaceDetectorOptions` 配置：使用 `PERFORMANCE_MODE_FAST`，不需要检测 Landmarks，只需获取 `BoundingBox` (人脸边框)。
    *   **对齐逻辑**：计算当前画面中人脸的中心点，**首次拍摄对齐屏幕中心，后续拍摄对齐上一张照片的人脸中心点**。误差在 8% 以内视为对齐。
    *   **防内存泄漏**：分析完成后必须在 `finally` 块中调用 `imageProxy.close()`。
*   **拍照反馈**：
    *   点击按钮 → 屏幕瞬间白色闪烁（`animateColorAsState` 白→透明，100ms）+ 系统快门音（`MediaActionSound`）+ 缩略图从拍照按钮飞入右上角角标（`AnimatedVisibility` + `graphicsLayer` 位移缩放）。
*   **快门按钮**：
    *   外层白色描边圆环（`border = 4.dp, Color.White`），内层实心白圆（`size = PhotoButtonSize - 12dp`）。
    *   按下时 `scale` 缩至 0.9（`animateFloatAsState`, `spring(dampingRatio = 0.6)`），松手回弹。
    *   最小触控区域 48dp × 48dp（由按钮本身 72dp 保证）。
*   **前后摄像头**：硬编码为**前置摄像头**（`CameraSelector.DEFAULT_FRONT_CAMERA`），不自拍不拍，无需切换逻辑。
*   **定时拍照**：长按快门按钮触发 3 秒倒计时（`LaunchedEffect` + `delay`），倒计时数字居中放大显示然后淡出，倒计时结束自动拍照。

### 4.3 UI 层：Dashboard 主页 (Bento 风格)

*   **空状态 (`EmptyState` 组件)**：当 Room 数据库中没有记录时，显示居中的大图标（200dp）+ 文案"还没有记录。点击下方按钮开始你的第一张自拍。"+ "开始"按钮。
*   **Bento 栅格布局**：
    *   顶部：今日打卡入口（**2 列 × 1.5 行**，跨 `LazyVerticalGrid` 的 `span`），大圆角 20dp，展示当前日期 + "去拍照" CTA + 如果已拍则显示今日缩略图。
    *   打卡统计卡片（**1 列 × 1 行**）：`RollUpNumber` 显示连续天数 + 🔥 图标 + "连续打卡"标签。
    *   本周统计卡片（**1 列 × 1 行**）：迷你周进度环（7 个圆点，打卡的天填充 Primary 色）。
    *   历史缩略图：`LazyVerticalGrid(columns = Fixed(3))` 展示所有历史照片，按时间倒序。每张卡片 `.aspectRatio(1f)` + `clip(RoundedCornerShape(12.dp))`。
*   **卡片的入场动画**：使用 `AnimatedVisibility` + `slideInVertically` + `fadeIn`，每张卡片 `delay` 递增 50ms（staggered），产生涟漪式入场效果。
*   **连续天数计数动画**：当数字变化时，`RollUpNumber` 组件使用 `animateIntAsState(tween(800ms, easing = FastOutSlowInEasing))` 滚动数字。
*   **顶部栏**：显示 App 名称"每日自拍"，右侧为"导出视频"按钮（仅在有 ≥2 张照片时可用）和设置图标。
*   **错误/加载状态**：
    *   数据库读取失败 → Snackbar "加载失败，请重试" + 重试按钮
    *   删除照片 → SwipeToDismiss + 确认对话框

### 4.4 Service 层：Media3 视频合成

*   **Foreground Service (前台服务)**：
    *   类型声明：在 Manifest 中加入 `<service android:name=".service.VideoRenderService" android:foregroundServiceType="mediaProcessing" />` (兼容 Android 14+)。
    *   通知栏需使用 `NotificationCompat.Builder`，并在合成过程中实时更新进度条。
    *   通知栏需包含"取消"操作按钮（`addAction`），点击后取消合成。
*   **最少照片数**：少于 2 张时不允许导出，UI 上该按钮置灰并提示"至少需要 2 张照片才能生成视频"。
*   **Transformer 配置**：
    *   将查询到的所有照片路径转换为 `MediaItem` 列表。
    *   配置帧率：设定为 `10 fps`。
    *   分辨率设定：在加入轨道时，统一将画面拉伸或居中裁剪为 `1080x1920` (9:16) 尺寸，防止不同尺寸或比例的图片导致底层的 `MediaCodec` 编码器崩溃。
    *   过渡效果：每张照片之间加入 **250ms crossfade**。
*   **进度回传**：通过 `SharedFlow<ExportState>` 向外暴露进度（`sealed class ExportState { data class Progress(val percent: Int); object Completed; data class Failed(val error: String) }`）。
*   **错误处理**：合成失败时不清除临时文件，保存错误日志，通知栏显示"导出失败"，点击通知重新尝试。
*   **取消机制**：Service 必须响应 `onDestroy()` 并调用 `Transformer.cancel()`，已生成的临时文件清理。

### 4.5 导航架构

`NavGraph.kt` 定义：

| Route | Screen | 参数 |
|-------|--------|------|
| `dashboard` | DashboardScreen | 无 |
| `camera` | CameraScreen | `hasTakenToday: Boolean` |
| `onboarding` | OnboardingScreen | 无（仅首次） |
| `videoexport` | 底部 Sheet（非全屏路由） | 无 |

*   首次启动 → 检查 SharedPreferences `isFirstLaunch` → true → onboarding，完成后写 `isFirstLaunch = false`。
*   Dashboard → Camera 使用 `slideInVertically` 转场动画（300ms）。
*   Camera → Dashboard 使用 `slideOutVertically` 返回。

### 4.6 权限申请流程

*   相机权限：进入 Camera 前检查 `Manifest.permission.CAMERA`。
    *   已授权 → 直接进入
    *   首次请求 → 系统弹窗
    *   永久拒绝 → 显示 `PermissionDeniedScreen`（说明为什么需要权限 + 跳转设置按钮）
*   存储：使用 `getExternalFilesDir` 无需运行时权限，但需在代码中 catch `IOException` 处理存储空间不足。

---

## 5. 可访问性规范 (A11y) — 强制遵守

*   **拍照按钮**：`contentDescription = "拍照"`，对齐后变为 `"拍照，已对齐"`
*   **引导环**：`contentDescription` 根据状态动态变化："引导环，未检测到人脸" / "引导环，未对齐" / "引导环，已对齐，可以拍照"
*   **Bento 卡片**：每张卡片有 `contentDescription`，如"今日打卡，已拍照"或"今日打卡，尚未拍照，点击进入相机"
*   **历史缩略图**：合并为一个 `LazyVerticalGrid` 的 `contentDescription = "历史照片列表，共 N 张"`
*   **颜色非唯一反馈**：对齐状态除颜色变化外，还需显示 ✓ 图标（对齐）或 ✗ 图标（未对齐）
*   **触控目标**：所有可点击/可按压元素最小 48dp × 48dp
*   **字体缩放**：所有 `Text` 需响应系统字体大小设置（使用 `sp` 单位），布局在 1.5x 缩放比下不溢出

---

## 6. 🚀 分步代码生成指令 (Chain of Thought Prompting)

> **请 AI 助手每次只执行一个步骤的代码生成！**
> **完成当前步骤并经我（人类开发者）确认无误后，再进行下一步，绝对不要一次性输出所有代码！**
> **每个步骤的完成标准：代码可编译，`./gradlew assembleDebug` 成功。**

### 步骤 0：设计系统 Token 与项目脚手架
输出内容：
1. `build.gradle.kts` (App 级别，含所有依赖版本)
2. `ui/theme/Color.kt` — 按 1.1 节定义所有 Color Token
3. `ui/theme/Type.kt` — 按 1.2 节定义 Typography
4. `ui/theme/Shape.kt` — 按 1.3 节定义 Shapes
5. `di/DatabaseModule.kt` — Hilt Module，提供 Room Database 实例
6. `AndroidManifest.xml` — 权限声明（CAMERA）+ Foreground Service 声明

**完成标准**：项目可编译，`Color.primary` 等 Token 可在代码中引用。

---

### 步骤 1：Data 层基建
输出内容：
1. `data/local/DailyPhoto.kt` — Room Entity
2. `data/local/DailyPhotoDao.kt` — DAO 接口（含 Flow 查询）
3. `data/local/AppDatabase.kt` — Database 抽象类 + Migration 占位
4. `di/RepositoryModule.kt` — Hilt Module，绑定 Repository 接口与实现

**完成标准**：Room 数据库可编译，DAO 方法签名正确。

---

### 步骤 2：文件管理与仓储实现
输出内容：
1. `data/storage/FileManager.kt` — 图片保存、EXIF 旋转修复、WEBP 压缩（80% 质量，1080px 最大宽度）
2. `data/repository/PhotoRepositoryImpl.kt` — Room + FileManager 结合，返回 Flow<DailyPhoto>
3. `domain/repository/PhotoRepository.kt` — 接口定义

**完成标准**：`FileManager.save()` 可被调用，输入 Bitmap 返回相对路径 String。

---

### 步骤 3：ML Kit 图像分析器
输出内容：
1. `domain/model/FaceAlignment.kt` — 枚举 (NONE, DETECTED, ALIGNED)
2. `data/FaceAnalyzer.kt` — 实现 `ImageAnalysis.Analyzer`，人脸 BoundingBox 提取与对齐计算

**关键逻辑**：首次拍摄对齐屏幕中心 (0.5, 0.5)，后续对齐上一张照片的归一化人脸中心坐标。误差阈值 8%。`imageProxy.close()` 在 finally 中调用。

**完成标准**：`FaceAnalyzer` 可被 CameraX 的 `ImageAnalysis` 绑定。

---

### 步骤 4：Camera UI + ViewModel（项目核心难点）
输出内容：
1. `ui/screens/camera/CameraUiState.kt` — MVI 状态（含 CameraError 密封类）
2. `ui/screens/camera/CameraIntent.kt` — MVI 意图（Capture, Retake, TimerCapture, DismissError）
3. `ui/screens/camera/CameraViewModel.kt` — ViewModel（权限检查、拍照、ML Kit 分析结果订阅）
4. `ui/screens/camera/CameraScreen.kt` — 完整 Compose UI
5. `ui/components/GuideRing.kt` — 对齐引导环组件（含颜色动画 + 脉冲动效）
6. `ui/components/ShutterButton.kt` — 快门按钮组件（含按压动画 + 长按倒计时）

**UI 层叠顺序（从底到顶）**：
1. AndroidView(CameraX Preview) — 全屏
2. AsyncImage(洋葱皮叠加, alpha=0.35f) — 仅当 `latestPhotoPath != null`
3. GuideRing — 居中
4. 首次引导文字 — 仅当 `latestPhotoPath == null`
5. ✓/✗ 图标（对齐状态辅助反馈）—— 色盲友好
6. ShutterButton — 底部居中
7. Snackbar（错误状态弹出）

**完成标准**：应用启动 → 请求权限 → 相机 Preview 可见 → 人脸检测 → 引导环变色 → 拍照 → 叠加层出现 → 错误状态可触发。

---

### 步骤 5：Dashboard 主页 (Bento 风格)
输出内容：
1. `ui/screens/dashboard/DashboardUiState.kt`
2. `ui/screens/dashboard/DashboardViewModel.kt`
3. `ui/screens/dashboard/DashboardScreen.kt`
4. `ui/components/BentoCard.kt` — 通用 Bento 卡片组件（圆角 20dp、内边距 16dp）
5. `ui/components/RollUpNumber.kt` — 数字滚动计数组件
6. `ui/components/EmptyState.kt` — 空状态组件（图标 + 文案 + CTA 按钮）
7. `ui/components/WeekProgressRing.kt` — 7 点周进度环

**布局**：
```
┌────────────────────────────┐
│     今日打卡（主卡片 2x1.5）  │
│     日期 + 缩略图/CTA       │
├──────────────┬─────────────┤
│ 🔥 37 天     │ 本周统计 ◉◉◎○○  │
│ 连续打卡      │ 周进度环     │
├──────────────┴─────────────┤
│ LazyVerticalGrid(3 列)     │
│ [图][图][图]               │
│ [图][图][图]               │
│ ...                        │
└────────────────────────────┘
```

**动画**：卡片 staggered 入场（50ms 延迟递增），数字变化滚动计数动画（800ms）。

**完成标准**：Dashboard 显示卡片 + 历史缩略图，空状态可展示，点击"去拍照"跳转 Camera。

---

### 步骤 6：导航 + 首次引导
输出内容：
1. `ui/navigation/NavGraph.kt` — 路由定义与转场动画
2. `ui/navigation/Route.kt` — 路由常量
3. `ui/screens/onboarding/OnboardingScreen.kt` — 3 页水平滑动引导（Pager + 指示器圆点）
4. `MainActivity.kt` — 入口，主题设置，NavHost 挂载

**Onboarding 3 页内容**：
1. "每天自拍一张" — 插图 + 核心价值
2. "洋葱皮叠加帮你对齐" — 示意图（虚线半透明叠加说明）
3. "坚持打卡，生成你的延时视频" — 视频图标 + 开始按钮

**完成标准**：首次启动展示引导 → 完成后进入 Dashboard → Dashboard ↔ Camera 双向导航流畅。

---

### 步骤 7：Media3 视频合成
输出内容：
1. `service/VideoRenderService.kt` — Foreground Service
2. `domain/usecase/GenerateVideoUseCase.kt` — 读取所有照片 + 调用 Transformer + 返回 ExportState Flow
3. `ui/screens/dashboard/VideoExportUiState.kt` — 导出进度状态（底部 Sheet 用）

**最少 2 张照片才能导出。** 合成失败保留错误信息，可重试。取消时清理临时文件。

**完成标准**：≥2 张照片 → 点击导出 → 通知栏进度 + App 内进度 Sheet → 导出完成 → Gallery 可找到视频。

---

### 步骤 8：A11y 全面检查 + 错误处理补全
1. 逐一检查所有 screen 的 `contentDescription`
2. 确保所有颜色反馈有辅助图标或文字
3. 验证所有触控目标 ≥48dp
4. 验证字体缩放（模拟器设置 1.5x 字体 + 开发者选项最宽显示）
5. 验证空状态、错误状态、首次使用引导流程的完整链路
6. 确认 `imageProxy.close()` 在所有代码路径都被调用
7. 确认 Room 操作全部在协程中执行

**完成标准**：TalkBack 开启后所有元素可朗读，色盲模拟模式下状态可辨识。

---

## 7. 禁止事项清单 (Anti-Patterns)

*   ❌ 硬编码颜色值（如 `Color(0xFF0000)`）——必须引用 `MaterialTheme.colorScheme`
*   ❌ `java.io.File` 绝对路径——必须用 FileManager + 相对路径
*   ❌ 主线程 I/O——必须用 `withContext(Dispatchers.IO)`
*   ❌ 超过 3 种非灰度颜色出现在同一页面
*   ❌ 引入自定义字体文件——只用系统默认 `FontFamily.Default`
*   ❌ Box shadow 替代 Material 3 `elevation` / `shadow` token
*   ❌ 跳过 Room Migration——每次 schema 变更必须有 Migration
*   ❌ 硬编码 `1080x1920` 以外的屏幕尺寸假设——布局必须响应式
*   ❌ 仅用颜色表达状态（色盲用户无法区分红/绿引导环）
*   ❌ 文字 `sp` 写死为 `dp`——必须用 `sp` 响应系统字体缩放

---

## 8. 验收标准全览

- [ ] 首次启动 → Onboarding 3 页 → Dashboard 空状态
- [ ] 点击拍照 → 请求相机权限 → Camera Preview 可见
- [ ] 人脸对准引导环 → 红色→绿色 + ✓ 图标 + 震动 + 环脉冲
- [ ] 点击快门 → 白闪 + 快门音 + 缩略图飞入 + 叠加层更新
- [ ] 已拍照后返回 Dashboard → 今日卡片显示缩略图 + 连续天数更新
- [ ] 累计 7 天 → 周进度环全部填充
- [ ] 2 张以上 → 导出视频 → 进度可见 → 视频在 Gallery 可播放
- [ ] TalkBack 开启 → 所有元素可朗读，引导环状态可听
- [ ] 1.5x 字体缩放 → 布局不溢出
- [ ] 相机权限被拒 → PermissionDenied 状态 + 跳转设置
- [ ] 存储空间不足 → Snackbar 提示
- [ ] Room 版本升级 → Migration 不丢数据
- [ ] 深色模式 → 按 1.1 节 Dark Value 自动切换
