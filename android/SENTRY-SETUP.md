# Sentry 崩溃监控接入指南

## 现状

Sentry 依赖已加入项目，但**默认未启用**。
这是设计上的选择——开发者本地开发不需要给开发者电脑装上崩溃上报。

## 启用步骤

### 1. 注册 Sentry 账号

打开 https://sentry.io/signup/ 注册免费账号。
或者用国内镜像：https://sentry.io/auth/login/cn/

免费额度：5K 事件/月，对个人 App 完全够用。

### 2. 创建项目

登录后：
1. 点 **Create Project**
2. 平台选 **Android**
3. 项目名填 `daily-portrait`
4. 创建后会得到一个 DSN，形如：
   ```
   https://abc123def@o1234567.ingest.sentry.io/9876543
   ```

### 3. 在项目根目录创建 sentry.properties

文件位置：`C:\Users\alinweii\Desktop\DailyPortrait\android\sentry.properties`

内容：
```properties
dsn=粘贴上一步的 DSN
```

⚠️ 这个文件**不会进版本库**（已在 .gitignore 中）。
不要把 DSN 提交到公开仓库。

### 4. 重新构建 App

在 Android Studio：
- **Build → Rebuild Project**

或命令行：
```cmd
cd C:\Users\alinweii\Desktop\DailyPortrait\android
gradlew assembleDebug
```

启动 App 后看 logcat 应该能看到：
```
DailyPortraitApp: Sentry initialized for environment debug
```

### 5. 触发一次测试崩溃验证

随便在某个按钮的 onClick 加：
```kotlin
throw RuntimeException("Sentry test crash")
```

跑一次 → 崩溃 → 30 秒内 Sentry 控制台应该收到事件。
验证完毕后删掉这行代码。

## 想关掉怎么办

直接删除 `sentry.properties` 文件，Rebuild 即可。

## 上线前注意

- DSN 不要硬编码在源代码里
- 不要在 release 包发往 sentry 的事件中携带用户照片路径等隐私信息
- App 启动崩溃如果 Sentry 还没初始化好，可能漏报（这是 Sentry 本身的限制）
- 国内用户访问 sentry.io 默认走 CloudFlare CDN，速度还行
- 想要本土化可以考虑 Sentry 自部署或换成 **Bugly**（腾讯，国内免费）
