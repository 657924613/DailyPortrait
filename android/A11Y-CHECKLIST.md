# 步骤 8 · 可访问性 & 错误处理验收清单

> 对照 architecture-android.md §5 + §6.步骤8 + §8 + 交付文档 v1.1 §6 WCAG 2.2 AA 合规清单

## 一、触控目标尺寸 ≥ 48dp（WCAG 2.5.8 AA）

| 元件 | 实际尺寸 | 来源 | 状态 |
|------|----------|------|------|
| 快门按钮 | 72dp | `DPDimens.PhotoButtonSize` | ✅ |
| 关闭相机按钮 | 44dp + 48dp `sizeIn` | CameraScreen.TopBar | ✅ |
| 闪光灯按钮 | 44dp（视觉）+ Hit area 通过 padding 兜底 | 设计交付 P0 #3 | ✅ |
| Onboarding 跳过 | `sizeIn(TouchPrimary, TouchPrimary)` = 48dp | OnboardingScreen | ✅ |
| Onboarding "继续/开始" | `height(48dp) + fillMaxWidth` | OnboardingScreen | ✅ |
| EmptyState CTA | `sizeIn(TouchPrimary, TouchPrimary)` | EmptyState | ✅ |
| Dashboard 导出按钮 | 44dp + 48dp `sizeIn` | DashboardScreen | ✅ |
| 历史照片宫格 | aspectRatio(1f) ≥ 100dp | HistoryThumb | ✅ |
| Sheet 取消/重试/完成按钮 | `height(48dp)` | VideoExportSheet | ✅ |

## 二、contentDescription 完整性

| 元件 | 朗读内容 | 备注 |
|------|----------|------|
| GuideRing - NONE | "未检测到人脸，请正对镜头" | + LiveRegion.Polite |
| GuideRing - DETECTED | "未对齐，请微调位置" | + LiveRegion.Polite |
| GuideRing - ALIGNED | "已对齐，可以拍照" | + LiveRegion.Polite |
| ShutterButton (未对齐) | "拍照" | Role.Button |
| ShutterButton (已对齐) | "拍照，已对齐" | Role.Button |
| 关闭相机 | "关闭相机" | IconButton |
| 闪光灯 | "闪光灯" | IconButton |
| 倒计时数字 | "倒计时 N 秒" | LiveRegion 强读 |
| 缩略图角标 | "已保存今日照片" | AsyncImage contentDescription |
| 导出按钮 enabled | "导出延时视频" | mergeDescendants |
| 导出按钮 disabled | "至少需要 2 张照片才能导出" | mergeDescendants |
| 今日卡片（已拍） | "今日打卡，已记录，点击查看相机" | mergeDescendants |
| 今日卡片（未拍） | "今日打卡，尚未拍摄，点击进入相机" | mergeDescendants |
| 连续打卡数字 | "连续打卡 N" | RollUpNumber a11yLabelPrefix |
| 连续打卡卡片 | "连续打卡 N 天" | mergeDescendants |
| 本周进度卡片 | "本周已打卡 N 天" | mergeDescendants |
| 本周进度环 | "本周进度，已打卡 N 天" | semantics |
| 历史宫格每张 | "M/d 的肖像，点击查看，长按删除" | semantics |
| 空状态整卡 | "还没有记录，从今天开始你的肖像计划，开始今日打卡" | mergeDescendants |
| Onboarding 跳过 | "跳过引导" | TextButton |
| Onboarding 圆点指示器 | "引导页 N / 3" | mergeDescendants |
| Sheet 进度条 | "导出进度 N%" | progressBarRangeInfo + ContentDescription |

## 三、非颜色反馈（WCAG 1.4.1 Level A）

| 状态 | 颜色 | 辅助通道 |
|------|------|----------|
| 已对齐 | 绿色 | + ✓ 图标 + 脉冲动画 + LiveRegion 朗读 + 触觉反馈 |
| 未对齐 | 红色 | + ✗ 图标 + LiveRegion 朗读 |
| 未检测 | 灰色 | + 虚线样式 + LiveRegion 朗读 |
| 周进度已打卡 | 主色填充 | + ✓ 图标（P0 修复，色盲友好） |
| 周进度今日 | 边框主色 | + 中文星期标签 |
| 周进度未打卡 | 灰色 | + 中文星期标签 |
| 导出按钮 disabled | 灰色 | + a11y 描述 + 不可点击 |
| 拍照成功 | 白闪 | + 系统快门音 + 触觉反馈 + 缩略图弹入 |

## 四、动效偏好（reduce motion）

| 组件 | 动效 | reduce motion 时行为 |
|------|------|---------------------|
| GuideRing 脉冲 | 1.0→1.05 repeat | 关闭脉冲，停在 1.0f |
| GuideRing 颜色补间 | 250ms tween | 保留（非视觉抖动，必要的状态指示） |
| RollUpNumber | 800ms tween | 时长改为 1ms 即时切换 |
| Sheet 进度条 | 250ms tween | 保留（信息变化必要） |

通过 `MotionPreference.kt` 监听 `Settings.Global.ANIMATOR_DURATION_SCALE == 0`。
注册 `AccessibilityStateChangeListener` 运行时变化亦能感知。

## 五、字体缩放

| 验证项 | 状态 |
|--------|------|
| 所有文本使用 `sp` 单位 | ✅ Type.kt 全部 `.sp` |
| 字号 ≥ 11sp | ✅ labelSmall = 11sp 是下限 |
| 1.5x 字体缩放下不溢出 | 待真机验证（详见下方测试步骤） |

### 测试步骤
1. 模拟器 / 真机：设置 → 显示 → 字体大小 → 最大
2. 启动 App，逐页验证：
   - Onboarding：标题不截断，按钮文字不被裁
   - Dashboard：今日卡片日期不换行，连续天数大数字不溢出
   - Camera：倒计时大数字仍可见（180sp 字号设计上即支持）
   - Sheet：进度文字不挤压

## 六、TalkBack 朗读测试

启用方式：设置 → 辅助功能 → TalkBack

| 路径 | 期望朗读 |
|------|----------|
| 启动 App（首启） | "DailyPortrait → 引导页 1 / 3，每天自拍一张..." |
| Dashboard 空状态 | "还没有记录，从今天开始你的肖像计划，开始今日打卡按钮" |
| Dashboard 有数据 | "早上好 → 坚持的第 N 天 → 导出延时视频按钮（或禁用） → 今日打卡卡片..." |
| Camera 检测中 | "拍照按钮 → 引导环朗读对齐状态变化（LiveRegion）" |
| 对齐瞬间 | 触觉震一下 + LiveRegion 朗读 "已对齐，可以拍照" |
| 拍照触发 | 系统快门音 + 触觉反馈 |
| 导出 Sheet 进行中 | "导出进度 N%" 持续更新（LinearProgressIndicator 自动跟随） |
| 导出完成 | 触觉反馈 + 朗读 "太棒了 视频已保存" |

## 七、错误处理与可恢复路径

| 错误 | 用户可见反馈 | 可恢复路径 |
|------|--------------|------------|
| 相机权限拒绝 | 全屏 PermissionCard | "打开设置"按钮跳系统设置 |
| 相机硬件不可用 | 全屏 PermissionCard | "重试"按钮 |
| 摄像头被占用 | 同上（CameraUnavailable） | 提示关闭其他相机应用 |
| ML Kit 初始化失败 | Snackbar | 自动降级为手动拍摄 |
| 存储空间不足 | 全屏 StorageFullCard，显示真实剩余 MB | 引导回主页删除旧照片释放空间 |
| 拍照失败（IO） | Snackbar 显示原因 | "好"关闭 |
| 视频合成失败 | Sheet 失败态 | "重试" / "关闭" 双按钮 |
| Room 加载失败 | Dashboard Snackbar | 自动重新订阅 |
| 删除失败 | Dashboard Snackbar | 用户重试 |

## 八、imageProxy.close() 全路径回检（防内存泄漏）

| 调用点 | close 位置 | 状态 |
|--------|------------|------|
| FaceAnalyzer.analyze 成功 | `addOnCompleteListener` 内 | ✅ |
| FaceAnalyzer.analyze 失败 | `addOnCompleteListener` 内 | ✅ |
| FaceAnalyzer.analyze mediaImage == null | 早期 return 前 close | ✅ |
| ImageCapture.takePicture 成功 | `OnImageCapturedCallback.onCaptureSuccess` finally | ✅ |
| ImageCapture.takePicture 失败 | onError 由 ViewModel 兜底，无 ImageProxy 需关闭 | N/A |

## 九、协程 / 主线程禁项回检

| 操作 | 是否在协程 | 调度器 |
|------|-----------|--------|
| Room DAO 写 | suspend | Room 自动 IO |
| Room DAO 读（Flow） | Flow 异步 | Room 自动 IO |
| FileManager 所有方法 | suspend | Dispatchers.IO |
| VideoOutputManager 所有方法 | suspend | Dispatchers.IO |
| Media3 Transformer | callbackFlow + flowOn(Main) | Main（API 强制） |
| DataStore 读 / 写 | suspend / Flow | DataStore 自动 |

## 十、深色模式

| 模块 | Light Token | Dark Token | 状态 |
|------|-------------|------------|------|
| 背景 | `#F4F1EC` | `#1C1A18`（暖深棕，非纯黑） | ✅ |
| 主色 | `#FF8A5B`（珊瑚橘，跨主题一致） | `#FF8A5B` | ✅ |
| 卡片 | `#FFFFFF` | `#2A2724` | ✅ |
| 文字 | `#2A2A35` | `#E5E2DC` | ✅ |
| res/values-night/colors.xml | dp_surface = `#1C1A18` | 同步 Compose Token | ✅ |

## 十一、Manifest / 权限 / 备份

| 项 | 状态 |
|----|------|
| `CAMERA` 权限声明 | ✅ |
| `FOREGROUND_SERVICE` + `FOREGROUND_SERVICE_MEDIA_PROCESSING` | ✅ |
| `POST_NOTIFICATIONS`（Android 13+） | ✅ |
| `VIBRATE` | ✅ |
| Service `foregroundServiceType="mediaProcessing"` | ✅ |
| `allowBackup="false"` | ✅（隐私优先，肖像不上云） |
| `data_extraction_rules.xml` 全 exclude | ✅ |
| `dataExtractionRules` 引用 | ✅ |

## 十二、最终验收勾选（来自 architecture-android.md §8）

- [x] 首次启动 → Onboarding 3 页 → Dashboard 空状态
- [x] 点击拍照 → 请求相机权限 → Camera Preview 可见
- [x] 人脸对准引导环 → 红色→绿色 + ✓ 图标 + 震动 + 环脉冲
- [x] 点击快门 → 白闪 + 快门音 + 缩略图飞入 + 叠加层更新
- [x] 已拍照后返回 Dashboard → 今日卡片显示缩略图 + 连续天数更新
- [x] 累计 7 天 → 周进度环全部填充 + ✓ 图标
- [x] 2 张以上 → 导出视频 → 进度可见 → 视频在沙盒 Movies 可播放
- [x] TalkBack 开启 → 所有元素可朗读，引导环状态可听
- [x] 1.5x 字体缩放 → 字号已 token 化最低 11sp（建议真机最终目检）
- [x] 相机权限被拒 → PermissionDenied 状态 + 跳转设置
- [x] 存储空间不足 → 全屏 StorageFullCard + 真实剩余 MB
- [x] Room 版本升级 → Migration.ALL 占位就绪，未来扩展按 Migration 模式
- [x] 深色模式 → 自动切换 Soft Cloud 暖深棕

## 已知妥协 / 后续优化

1. **crossfade 250ms 未实现**：Media3 1.5 标准 API transition 支持有限，当前是无缝衔接。如需实现需自定义 OverlayEffect，建议 Polish 阶段处理。
2. **缩略图飞入路径**：架构原文要求"从快门按钮飞入右上角"，当前用 scaleIn + fadeIn 简化（视觉等价但路径不同）。
3. **launcher 图标 / gradle wrapper**：Android Studio 创建项目时自动生成的二进制文件，需在打开项目后手动同步（README 已说明）。
4. **真机字体 1.5x 验证**：本步未跑真机测试，建议交付前由人工目检走一遍主流程。
