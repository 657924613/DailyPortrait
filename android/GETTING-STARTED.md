# DailyPortrait · 零基础运行指南

> 跟着这份文档走，**不需要懂 Android 开发**也能把 App 跑在自己手机或模拟器上。
> 大约耗时：第一次 60-90 分钟（主要是下载 SDK），之后每次 10 秒。

---

## 第 0 步：你需要准备什么

| 准备物 | 用途 |
|--------|------|
| 一台 Windows 电脑 | 你已经有了 ✓ |
| 大约 15 GB 硬盘空间 | Android Studio + SDK + 模拟器要占这么多 |
| 一台 Android 手机（可选） | 比模拟器快很多，强烈推荐 |
| 一根数据线（可选） | 配合手机用 |
| 网络连接 | 第一次下载依赖大约 1-2 GB |

---

## 第 1 步：下载安装 Android Studio

### 1.1 下载

打开浏览器访问 https://developer.android.com/studio
点击"Download Android Studio"按钮。

如果国内网络打不开，可用镜像：
- https://developer.android.google.cn/studio （中国大陆官方镜像）

下载的文件大约 1 GB，名字像 `android-studio-2024.x.x.x-windows.exe`。

### 1.2 安装

双击运行下载的 .exe 文件，**全程默认下一步即可**，到最后点 Finish。

第一次启动 Android Studio 时它会问你：
- "Import Settings?" → 选 **Do not import settings** → OK
- 然后会出现欢迎向导，**全部默认下一步**
- 它会自动下载几个东西（约 1.5 GB），耐心等
- 看到 "Welcome to Android Studio" 主页面就成功了

---

## 第 2 步：第一次打开本项目

### 2.1 打开

在 Android Studio 主页面：
1. 点击 **Open**（不是 New Project，因为我们已经有项目了）
2. 在弹出的文件夹选择窗口里，找到并选中 `C:\Users\alinweii\Desktop\DailyPortrait\android` 这个**文件夹**
3. 点 OK

### 2.2 它会自动开始 Gradle Sync

打开后，右下角会出现进度条 "Gradle: Importing project…"。

**不要点任何按钮，让它跑完。** 第一次会下载大约 500MB 的依赖。
进度条消失 + 底部出现 "BUILD SUCCESSFUL" 才算 sync 成功。

⚠️ **第一次 sync 多半会失败**，因为本项目缺两样东西，我们下一步补上。

---

## 第 3 步：补两样缺的东西

我没法在没装 Android Studio 的环境里生成这两类文件，需要你手动一次：

### 3.1 生成 Gradle Wrapper

Android Studio 顶部菜单：
1. 点 **File → Sync Project with Gradle Files**
2. 如果它提示缺 wrapper，点 "Use default Gradle wrapper" 让它自动生成

如果上面这个不行，**用命令行兜底**：
1. 按 Win + R 打开运行窗口，输入 `cmd`，回车
2. 依次输入：
   ```
   cd C:\Users\alinweii\Desktop\DailyPortrait\android
   gradle wrapper --gradle-version 8.9
   ```
   （前提是你电脑装了 Gradle，没装也没关系，看下面方法）

**最简单的方法**：直接复制一份现成的 wrapper：
1. Android Studio 顶部菜单 → **File → New → New Project**
2. 选 "Empty Activity" → 随便填个名字（比如 TempProject） → Finish
3. 等它生成完毕，把这个临时项目里的几个文件复制到我们项目：
   - `gradlew`（无扩展名）
   - `gradlew.bat`
   - `gradle/wrapper/gradle-wrapper.jar`
   - `gradle/wrapper/gradle-wrapper.properties`
   
   都复制到 `C:\Users\alinweii\Desktop\DailyPortrait\android` 对应位置
4. 关掉临时项目，回到 DailyPortrait

### 3.2 生成应用图标

在 Android Studio 中打开 DailyPortrait 项目后：
1. 顶部菜单 → **File → New → Image Asset**
2. Icon Type 选 "Launcher Icons (Adaptive and Legacy)"
3. Foreground Layer：
   - Source Asset → Asset Type 选 **Clip Art** → 在 Clip Art 弹窗里搜 "camera" 选一个相机图标
   - Color：选珊瑚橘 `#FF8A5B`
4. Background Layer：
   - Source Asset → Color → 米色 `#F4F1EC`
5. 点 **Next** → **Finish**

它会自动生成 `mipmap-anydpi-v26/ic_launcher.xml` + 各密度 launcher 图标。

### 3.3 重新 Sync

补完上面两步：
- 顶部菜单 → **File → Sync Project with Gradle Files**
- 等到底部出现 "BUILD SUCCESSFUL"

---

## 第 4 步：选择运行设备

你有两种方式跑 App，**真机更快**。

### 方案 A：用真机（推荐）

1. **打开手机的"开发者选项"**：
   - 设置 → 关于手机 → 连续点击"版本号"7 次
   - 会提示"您已处于开发者模式"
2. **打开 USB 调试**：
   - 设置 → 系统 → 开发者选项 → 打开 "USB 调试"
3. **数据线连接电脑**
4. 手机上会弹出 "允许 USB 调试吗?" → 勾选"始终允许" → 确定
5. Android Studio 顶部工具栏，**设备下拉框**里会出现你的手机名字（比如"Xiaomi 14"）

### 方案 B：用模拟器

如果你没 Android 手机或不想插数据线：

1. Android Studio 顶部菜单 → **Tools → Device Manager**
2. 点击 "Create Device"（或 "+ Create Virtual Device"）
3. 选一个手机型号，比如 **Pixel 7**，下一步
4. 选系统镜像，推荐 **API 34（Android 14）** 或 **API 35**，
   如果旁边有 Download 按钮就点一下下载（约 1 GB）
5. 下一步 → Finish
6. 回到工具栏，**设备下拉框**选刚创建的模拟器

---

## 第 5 步：运行！

1. 顶部工具栏，确认设备下拉框选了你的手机或模拟器
2. 旁边的下拉框选 **app**
3. 点击绿色三角形 **Run** 按钮（或 Shift + F10）
4. 等待编译 + 安装（第一次约 1-3 分钟）
5. App 自动在设备上启动 🎉

第一次启动会看到：
- Onboarding 3 页 → 滑动或点"继续"
- "开始使用" → 进入 Dashboard 空状态
- "开始今日打卡" → 申请相机权限 → 自拍 → 看到引导环对齐
- 拍完后回到 Dashboard，连续天数变成 1

---

## 第 6 步：常见问题

### "Gradle Sync Failed"

**最常见原因**：JDK 版本不对。本项目要求 JDK 17。

修复：
1. Android Studio 顶部菜单 → **File → Settings**
2. 左侧找 **Build, Execution, Deployment → Build Tools → Gradle**
3. 右侧 "Gradle JDK" 选 **JetBrains Runtime 17** 或 **17 (Embedded JDK)**
4. 点 OK，重新 sync

### "SDK location not found"

修复：
1. **File → Project Structure → SDK Location**
2. 点击 "Android SDK location" 旁的下载按钮，让它自动安装
3. 或者：**Tools → SDK Manager** → 在 SDK Platforms 标签勾选 **Android 14（API 34）** 和 **Android 15（API 35）** → Apply

### "Failed to install ... 35"

打开 SDK Manager（Tools → SDK Manager），确保以下都已安装：
- ☑ Android API 35（Android 15）
- ☑ Android API 26（Android 8.0，本项目最低支持）
- ☑ Android SDK Build-Tools（最新）
- ☑ Android SDK Command-line Tools（最新）

### 编译时报错 "Unresolved reference: ..."

很可能是某个依赖没下完。
- 点击右下角 "Build" 标签查看具体错误
- 大多数情况下，**重新 sync 一次**就能解决：File → Sync Project with Gradle Files
- 还不行：**File → Invalidate Caches → Invalidate and Restart**

### "Cannot find symbol" / 红色波浪线遍布代码

不要慌，大概率是 Gradle 还没 sync 完。
- 等待右下角进度条结束
- 如果一直不结束，重启 Android Studio（File → Invalidate Caches）

### App 安装失败 "INSTALL_FAILED_USER_RESTRICTED"

手机厂商额外的安全机制（小米/华为/OPPO 比较常见）：
- 小米：设置 → 应用设置 → 授权管理 → USB 安装管理 → 关闭
- 华为：设置 → 安全 → 更多安全设置 → 仅充电模式下允许 ADB 调试 → 关闭
- 不同厂商位置不同，搜"USB 安装"就能找到

### 第一次拍照后崩溃

打开 **Logcat 标签**（Android Studio 底部），过滤 `DailyPortrait`，看红色异常堆栈。
最常见：
- ML Kit 初始化失败 → 设备不支持，会自动降级为手动拍摄
- 存储写入失败 → 检查"应用信息 → 存储"是否够空间

---

## 第 7 步：后续修改与运行

第一次成功跑起来后，每次改完代码：
- 点绿色 Run 按钮（Shift + F10）
- 等 5-10 秒就能看到新版本

如果只是改了 Compose UI 的颜色 / 文字 / 布局，可以用 **Apply Changes**（左边那个闪电小图标），1-2 秒就能热更新。

---

## 出现解决不了的报错怎么办

**给我看具体错误信息**，我能帮你定位：
1. Android Studio 底部的 **Build** 或 **Logcat** 标签里红色的几行
2. 把它们截图或复制粘贴给我
3. 顺便告诉我哪一步执行到了哪里

这份文档没覆盖到的报错都属于个例，需要看具体环境处理。

---

## 学完一遍后你就掌握了

- ✅ 怎么导入一个 Android 项目
- ✅ Gradle Sync 是干什么的
- ✅ 真机 / 模拟器调试
- ✅ 怎么排查最常见的环境问题

后续如果想自己改 UI / 改文案，所有 Compose 文件都在
`app/src/main/java/com/missyun/dailyportrait/ui/screens/` 下，
中文注释完整，对照修改即可。
