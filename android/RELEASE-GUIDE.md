# Release 构建指南

## 第一次发布前必须做的 4 件事

### 1. 生成 Release 签名密钥

打开 PowerShell 或 cmd，进入项目根目录：

```cmd
cd C:\Users\alinweii\Desktop\DailyPortrait\android
mkdir keystore
keytool -genkey -v -keystore keystore\release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias daily_portrait
```

按提示输入：
- 密钥库口令：自己设一个，**记牢**（应用商店要求至少 6 位）
- 姓名 / 单位 / 城市等可以留空或随便填
- 密钥口令：可以与密钥库口令相同

完成后会在 `keystore/release.jks` 生成文件。

### 2. 创建 keystore.properties

在项目根目录（`android/`）创建 `keystore.properties` 文件，内容：

```properties
storeFile=keystore/release.jks
storePassword=你刚才设置的密钥库口令
keyAlias=daily_portrait
keyPassword=你刚才设置的密钥口令
```

⚠️ **`.gitignore` 已配置忽略 .jks / .properties / keystore/ 目录**，
不会进版本库。但你电脑上要妥善备份，**密钥丢了等于这个 App 永远不能再更新**。

### 3. 构建 release APK

在 Android Studio：
1. 顶部菜单 → **Build → Generate Signed App Bundle / APK**
2. 选 APK
3. 选刚才创建的 keystore（路径填绝对路径或浏览选择）
4. Next → 选 release 构建类型 → Finish
5. 完成后桌面右下角通知会有路径，APK 在 `app/release/app-release.apk`

或者命令行：
```cmd
cd C:\Users\alinweii\Desktop\DailyPortrait\android
gradlew assembleRelease
```

输出在 `app/build/outputs/apk/release/app-release.apk`。

### 4. 真机自测一次 release 包

**这一步极其重要**——release 启用了 ProGuard/R8 代码混淆，可能因 keep 规则不全导致 ML Kit / Hilt / Media3 在某条路径崩溃。
debug 包跑得好不代表 release 跑得好。

测试清单：
- [ ] 启动 → Onboarding → Dashboard
- [ ] 拍照（含人脸对齐识别）
- [ ] 历史相册查看 + 删除 + 撤销
- [ ] 视频导出（标清 / 高清 / 超清各试一次）
- [ ] 视频"立即查看"能正常调起播放器
- [ ] 提醒设置（含测试通知）
- [ ] 重新观看引导

任何一步崩溃 → 用 `adb logcat` 抓堆栈，按 ProGuard 规则补充 keep。

---

## 常见 Release 崩溃排查

### 崩溃 1：Hilt 找不到注入

```
java.lang.IllegalStateException: ... was not found in the Hilt graph
```

修复：在 `proguard-rules.pro` 加：
```
-keep class * extends javax.inject.Provider
-keep @dagger.hilt.android.AndroidEntryPoint class *
```

### 崩溃 2：Media3 找不到 Codec

```
NoClassDefFoundError: androidx.media3.exoplayer.HevcConfig
```

修复：proguard-rules.pro 已包含 `-keep class androidx.media3.** { *; }`，
如果还失败检查 Media3 版本是否一致。

### 崩溃 3：Room 字段名错误

```
java.lang.RuntimeException: cannot find implementation for ... DatabaseImpl
```

修复：DailyPhoto 的 keep 规则已配置。如果用了新 Entity，也要加 keep。

---

## 上架准备清单

应用商店硬要求：
- [ ] release.jks 妥善备份（云盘 + 本地双备）
- [ ] 隐私政策 URL（HTML 公网可访问）
- [ ] 用户协议 URL
- [ ] App 图标 512×512
- [ ] 应用截图 4-8 张
- [ ] 中文应用描述
- [ ] 数据安全表单填写
- [ ] 软著（中国大陆，可选）
- [ ] ICP 备案（中国大陆，部分商店要求）
