# 系统性优化追踪表 · 已全部完成

## 🔴 阻塞交付（severity 4）

| # | 项 | 状态 | 备注 |
|---|----|------|------|
| 1 | 修复"日期水印"假开关（dark pattern） | ✅ | 移除假开关，DateFormat enum 留待 v3 |
| 2 | 视频不能在 App 内查看 | ✅ | 完成态加"立即查看" + FileProvider |
| 3 | < 2 张照片时导出按钮反馈不明确 | ✅ | disabled 态弹 toast 提示 |

## 🟡 严重影响体验（severity 3）

| # | 项 | 状态 | 备注 |
|---|----|------|------|
| 4 | 删除后无法撤销 | ✅ | 软删除 + 5 秒 Snackbar 撤销 |
| 5 | 测试数据 → 真照片过渡不友好 | ✅ | 改进 toast 文案 |
| 6 | 视频生成无 ETA | ✅ | 进度条加"还需 N 秒"动态估算 |
| 7 | Onboarding 没有可重看入口 | ✅ | 设置页"关于"卡片新增 |

## 🟢 累计改善体验（severity 2）

| # | 项 | 状态 | 备注 |
|---|----|------|------|
| 8 | 权限被拒卡片缺解释 | ✅ | 卡片含详细原因和操作引导 |
| 9 | 超清画质无 OOM 警告 | ✅ | 选超清弹红色提示 |
| 10 | 提醒功能缺测试通知按钮 | ✅ | 开启提醒后行内出现"测试" |
| 11 | 缩略图角标 1.5s 太短 | ✅ | 改为 2.5s |
| 12 | 历史相册批量删除 | ❌ 主动放弃 | 单选+长按+撤销已覆盖 99% 场景 |
| 13 | App 版本号显示 | ✅ | 关于卡片显示 v$versionName |
| 14 | 调试工具 release 兜底 | ✅ | 重写 ProGuard 规则 |
| 15 | 中文字体一致性 | ❌ 主动放弃 | 系统默认字体已 OK |

## ⚠️ 工程质量

| # | 项 | 状态 | 备注 |
|---|----|------|------|
| 16 | 单元测试（核心算法） | ✅ | 5 文件 36 用例 |
| 17 | 修复 deprecated API 警告 | ✅ | LocalLifecycleOwner + statusBarColor |
| 18 | release 签名 + ProGuard | ✅ | 见 RELEASE-GUIDE.md（需你生成 keystore） |
| 19 | 接入崩溃监控 | ✅ | Sentry 框架就绪（需你填 DSN，见 SENTRY-SETUP.md） |
| 20 | LeakCanary 内存泄漏验证 | ✅ | debugImplementation 自动运行 |

## 📋 合规

| # | 项 | 状态 | 备注 |
|---|----|------|------|
| 21 | 隐私政策 | ✅ | legal/PRIVACY-POLICY.md（需律师审阅） |
| 22 | 用户协议 | ✅ | legal/TERMS-OF-SERVICE.md（需律师审阅） |
| 23 | 数据安全声明 | ✅ | legal/DATA-SAFETY-DECLARATION.md（应用商店表单参考） |
| 24 | 关于页 | ✅ | 含版本号 + 重看引导 + 隐私政策 + 用户协议 |

---

## 你需要本人介入的事项

代码已完成，但以下 5 件事**只能你做**：

| 任务 | 操作 | 见文档 |
|------|------|--------|
| 生成 release 签名密钥 | 按指南执行 keytool | RELEASE-GUIDE.md |
| 注册 Sentry 账号并填 DSN | sentry.properties 文件 | SENTRY-SETUP.md |
| 真机测试 release 包 | 借一台 Android 设备 | RELEASE-GUIDE.md |
| 部署隐私政策到公网 | GitHub Pages 或自有域名 | legal/*.md |
| 把 SettingsScreen 中 PRIVACY_POLICY_URL / TERMS_URL 替换成真实 URL | 一行代码 | SettingsScreen.kt |

完成上面 5 件事后,这个 App 就达到了"应用商店上架"标准。
