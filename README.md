<div align="center">

# Koishi

<p align="center">
  <img src="app/src/main/res/icon.png" alt="Koishi" width="128" height="128"/>
</p>

**轻量、优雅的现代 Material 3 工具箱**

一个完全基于 Kotlin 与 Jetpack Compose 构建的本地工具集合，把日常高频用到的小工具收进同一个 App，
开箱即用、无需登录、不收集任何数据。

[![Release](https://img.shields.io/github/v/release/NanamiKyoka/Koishi?include_prereleases&label=release)](https://github.com/NanamiKyoka/Koishi/releases)
[![License](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)

![Platform](https://img.shields.io/badge/Android-8.0%2B%20(API%2026)-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.4-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?logo=jetpackcompose&logoColor=white)

[功能特性](#功能特性) · [工具一览](#工具一览) · [设计系统](#设计系统) · [下载安装](#下载安装) · [从源码构建](#从源码构建) · [项目结构](#项目结构)

</div>

---

## 简介

Koishi 是一个**纯本地**的 Android 工具箱。所有图片处理都在设备端完成，不依赖任何服务端中转；
少数工具会按需访问第三方公开接口，且 API Key 由用户自行填写并保存在本机。

- 🎨 **10 套主题** 恋恋、动态取色（Monet）、Catppuccin、Nord、Tokyo Night、Gruvbox、Rosé Pine、Everforest、薰衣草、黑白
- 🌗 **深色 / 浅色 / 跟随系统** · 额外支持 OLED 纯黑模式
- 🧩 **模块化工具** · 每个工具独立成一个 feature 包，新增工具不影响既有代码

## 界面预览

| ![主页](/res/pic/home.jpg) | ![抽签喵](/res/pic/chouqian.jpg) |
| ------------------------------------------------------------ | ------------------------------------------------------------ |
| ![转盘喵](/res/pic/zhuanpan.jpg) | ![搜图喵](/res/pic/soutu.jpg) |




## 功能特性

| 能力 | 说明 |
| --- | --- |
| 工具箱主页 | 分类浏览 + 关键词搜索 + 搜索历史，支持收藏置顶 |
| 收藏夹 | 长按工具卡片即可加入/移出收藏 |
| 主题系统 | 10 套预设配色 + Material You 动态取色，全 App 生效 |
| 统一存储 | Room 支撑的 JSON 键值存储，工具配置集中管理、统一迁移 |
| 崩溃兜底 | 独立进程的崩溃页，展示版本号/系统信息并支持一键复制日志 |
| 相册输出 | 处理结果按工具分目录写入 `Pictures/Koishi/<工具名>`，无侵入系统相册 |

## 工具一览

### 图片应用

| 工具 | 简介 |
| --- | --- |
| 🖼️ **图片拼接** | 横向 / 纵向 / 影视台词无缝拼接，支持去黑边、间距、背景色与压缩输出 |
| 🔢 **多格切图** | 切方格与自定义行列切割，一键九宫格，批量写入相册 |
| 🌀 **图片混淆** | 番茄空间曲线（广义希尔伯特 + 黄金分割）、Logistic 混沌（PicEncrypt）、MD5 置乱四类算法，混淆与解混淆严格互逆 |
| 👻 **幻影坦克** | 基于 Alpha 通道合成表里双图，白底与黑底呈现完全不同的画面，支持彩色模式与棋盘格空间调和 |
| 📱 **二维码工具** | 生成 + 美化（数据点样式/颜色/Logo/背景图/边距），并支持相机实时与相册图片扫描识别 |
| 🔍 **以图搜图** | SauceNAO、trace.moe、Google Lens 多源并发反向识图，支持自带 SauceNAO API Key |
| 💧 **水印图** | 文字与图片水印全屏平铺，字体族、颜色、旋转、缩放、间距自由微调 |
| ✏️ **图片素描** | 灰度 → 反相 → 高斯模糊 → 颜色减淡，一键生成铅笔线稿，模糊半径可调 |

### 生活应用

| 工具 | 简介 |
| --- | --- |
| 📅 **历史上的今天** | 时间轴浏览当日历史事件，支持日期跳转、详情展开与配图，可配置 ShowAPI 提升数据稳定性 |
| 🎲 **做个决定** | 大转盘与摇签筒双模式，选项权重可调，主题支持增删改与 JSON 导入导出 |

> 新增工具只需在 `core/data/repository/ToolRepository.kt` 注册 `ToolItem`，
> 并在 `navigation/KoishiNavHost.kt` 挂载对应路由，主页与搜索会自动收录。

## 设计系统

主题能力集中在 `core/designsystem`，通过 `KoishiTheme(appTheme, amoled, darkTheme)` 统一注入：

- **多主题** · `AppTheme` 枚举驱动配色方案，每套主题在 `colorscheme/` 下独立成文件
- **动态取色** · Android 12+ 使用 Monet API，更低版本自动回退到 preset
- **纯黑模式** · 基于当前主题映射出 OLED 友好的纯黑表面色
- **统一形状与组件** · `component/` 下沉淀 PillShape、工具卡片等可复用外观

## 下载安装

前往 [Releases](https://github.com/NanamiKyoka/Koishi/releases) 下载最新版 APK：

| 文件 | 说明 |
| --- | --- |
| `Koishi-v{版本号}.apk` | 正式签名包，用于日常安装与覆盖升级 |
| `app-debug.apk` | 由 CI 构建的调试包（applicationId 带 `.debug` 后缀，可与正式包共存） |

- 系统要求：**Android 8.0（API 26）及以上**
- 首次安装需在系统设置中允许「安装未知来源应用」
- 正式包使用固定密钥签名，可直接覆盖安装后续版本

## 从源码构建

### 环境要求

| 依赖 | 版本 |
| --- | --- |
| JDK | 17 及以上 |
| Android SDK | Platform 37（`compileSdk = 37`） |
| Android Gradle Plugin | 9.3.1 |
| Gradle | 9.6.1 |
| Kotlin | 2.4.10 |

### 克隆与构建

```bash
git clone https://github.com/NanamiKyoka/Koishi.git
cd Koishi

# 指向本机 Android SDK
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

# 编译 Debug 包
./gradlew :app:assembleDebug

# 编译 Release 包（需先配置签名，见下）
./gradlew :app:assembleRelease
```

产物位于 `app/build/outputs/apk/`。

### 配置签名

Release 构建会读取项目根目录下的 `keystore.properties`（该文件已被 `.gitignore` 排除，不会进入版本库）。
若文件不存在，Release 任务会跳过签名，产出未签名包：

```properties
storeFile=koishi.keystore
storePassword=你的密钥库口令
keyAlias=koishi
keyPassword=你的密钥口令
```

> 也可以直接用命令行生成密钥库：
> ```bash
> keytool -genkeypair -v -keystore koishi.keystore -alias koishi \
>   -keyalg RSA -keysize 2048 -validity 36500
> ```

## 项目结构

```
Koishi/
├── app/
│   └── src/main/
│       ├── java/com/nanami/koishi/
│       │   ├── core/                  # 基础设施
│       │   │   ├── crash/             # 崩溃捕获与兜底页
│       │   │   ├── data/              # Room 统一存储、缓存、仓储
│       │   │   ├── designsystem/      # 主题、配色方案、通用组件
│       │   │   ├── image/             # 裁剪、预览等图片基础能力
│       │   │   ├── model/             # ToolItem / ToolCategory 等模型
│       │   │   └── util/              # 语言包装、相册目录等工具
│       │   ├── feature/
│       │   │   ├── home/              # 工具箱主页与底部导航
│       │   │   ├── favorites/         # 收藏夹
│       │   │   ├── settings/          # 设置页（外观 / 语言 / 关于）
│       │   │   └── tools/             # 每个工具一个独立子包
│       │   └── navigation/            # 类型安全路由与 NavHost
│       └── res/                       # 字符串（中/英）、主题、图标
└── gradle/libs.versions.toml          # 依赖版本目录
```

### 技术栈

`Kotlin` · `Jetpack Compose` · `Material 3` · `Navigation Compose` · `Room` · `Coroutines / Flow` ·
`kotlinx.serialization` · `OkHttp` · `Jsoup` · `Coil` · `material-kolor` · `qrcode-kotlin` ·
`ML Kit Barcode Scanning` · `CameraX` · `AndroidX Browser (Custom Tabs)`

## 开源协议

基于 [Apache License 2.0](LICENSE) 发布，可自由使用、修改与分发，但请保留原始版权声明。

---

<div align="center">

Made with 🩵 by [NanamiKyoka](https://github.com/NanamiKyoka)

[项目主页](https://github.com/NanamiKyoka/Koishi) · [问题反馈](https://github.com/NanamiKyoka/Koishi/issues)

</div>
