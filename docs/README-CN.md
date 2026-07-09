![banner](fastlane/metadata/android/en-US/images/featureGraphic.png)

# mpvExtended
[![GitHub release (latest SemVer)](https://img.shields.io/github/v/release/marlboro-advance/mpvex.svg?logo=github&label=GitHub&cacheSeconds=3600)](https://github.com/marlboro-advance/mpvex/releases/latest)
[![GitHub all releases](https://img.shields.io/github/downloads/marlboro-advance/mpvex/total?logo=github&cacheSeconds=3600)](https://github.com/marlboro-advance/mpvex/releases/latest)


**mpvExtended 是 [mpv-android](https://github.com/mpv-android/mpv-android) 的一个分支版本，基于 libmpv 库构建。它旨在将 mpv 的强大功能与易于使用的界面以及更多附加特性相结合。**

- 更简洁易用的界面
- Material 3 风格设计
- 高级配置与脚本支持
- 增强的播放功能
- 画中画播放
- 后台播放
- 高质量渲染
- 网络流媒体
- 文件管理
- 完全免费开源，无广告、无多余权限
- 支持树状和文件夹视图的媒体选择器
- 外挂字幕支持
- 缩放手势
- 外部音频支持
- 搜索功能
- SMB/FTP/WebDAV 支持
- 自定义播放列表管理

**本项目仍在开发中，可能存在一些 Bug。如发现问题请在 [Issues](https://github.com/marlboro-advance/mpvEx/issues) 区反馈。**

---

## 安装

### 稳定版
从 [GitHub Releases](https://github.com/marlboro-advance/mpvEx/releases) 页面下载最新的稳定版本。

[![Download Release](https://img.shields.io/badge/Download-Release-blue?style=for-the-badge)](https://github.com/marlboro-advance/mpvEx/releases)

或者你也可以从这里获取稳定版：

[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroidButtonGreyBorder_nofont.png" height="50" alt="Get it at IzzyOnDroid">](https://apt.izzysoft.de/packages/app.marlboroadvance.mpvex)

### 预览版
仅供测试使用

[![Download Preview Builds](https://img.shields.io/badge/Download-Preview%20Builds-red?style=for-the-badge)](https://marlboro-advance.github.io/mpvEx/)

---

## 截图展示
<div class="image-row" align="center">
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/player.png" width="98%" />
</div>

<div class="image-row" align="center" justify-content="space-between">
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/folderscreen.png" width="23.5%"/>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/videoscreen.png" width="23.5%"/>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/about.png" width="23.5%"/>
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/pip.png" width="23.5%"/>
</div>

<div class="image-row" align="center">
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/framenavigation.png" width="48.5%" />
  <img src="/fastlane/metadata/android/en-US/images/phoneScreenshots/chapters.png" width="48.5%" />
</div>

---

## 构建

### 前置条件

- JDK 17
- Android SDK（Build Tools 34.0.0 及以上）
- Git（用于构建中的版本信息）

### APK 架构变体

应用会为不同 CPU 架构生成多个 APK 变体：

- **universal**：兼容所有设备（体积较大）
- **arm64-v8a**：现代 64 位 ARM 设备（推荐大多数用户使用）
- **armeabi-v7a**：较旧的 32 位 ARM 设备
- **x86**：Intel/AMD 32 位设备
- **x86_64**：Intel/AMD 64 位设备

---

## 发布

### 配置发布签名

要在 GitHub Actions 中启用 Release 构建的自动签名，需要在 GitHub 仓库中配置以下 Secrets：

1. 进入 GitHub 上的仓库页面
2. 前往 **Settings** → **Secrets and variables** → **Actions**
3. 添加以下仓库 Secrets：

| Secret 名称              | 说明                                         |
|--------------------------|---------------------------------------------|
| `SIGNING_KEYSTORE`       | Base64 编码的密钥库文件（`.jks` 或 `.keystore`） |
| `SIGNING_KEY_ALIAS`      | 创建密钥库时使用的别名                            |
| `SIGNING_STORE_PASSWORD` | 密钥库文件的密码                                 |
| `KEY_PASSWORD`           | 密钥的密码（可与密钥库密码相同）                      |

#### 编码密钥库文件

将密钥库文件编码为 Base64：

**Linux/macOS：**

```bash
base64 -i your-keystore.jks | tr -d '\n' > keystore.txt
```

**Windows（PowerShell）：**

```powershell
[Convert]::ToBase64String([IO.File]::ReadAllBytes("your-keystore.jks")) | Out-File -FilePath keystore.txt -NoNewline
```

将 `keystore.txt` 的内容复制并粘贴为 `SIGNING_KEYSTORE` Secret 的值。

### 创建正式发布

1. 更新 `app/build.gradle.kts` 中的 `versionCode` 和 `versionName`
2. 提交更改
3. 创建并推送标签：
   ```bash
   git tag -a v1.0.0 -m "Release version 1.0.0"
   git push origin v1.0.0
   ```
4. GitHub Actions 将自动构建、签名并创建草稿发布

### 创建预览发布

1. 创建并推送预览标签：
   ```bash
   git tag -a v1.0.0-preview.1 -m "Preview release"
   git push origin v1.0.0-preview.1
   ```
2. GitHub Actions 将自动创建预发布版本

---

## 致谢

- [mpv-android](https://github.com/mpv-android)
- [mpvKt](https://github.com/abdallahmehiz/mpvKt)
- [Next player](https://github.com/anilbeesetti/nextplayer)
- [Gramophone](https://github.com/FoedusProgramme/Gramophone)

---

## 支持项目 <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Smilies/Heart%20with%20Ribbon.png" alt="Heart with Ribbon" width="25" height="25" />

如果你觉得 mpvExtended 对你有帮助，可以考虑支持开发：

[![UPI](https://img.shields.io/badge/UPI-aadiinarvekar@upi-blue?style=for-the-badge&logo=google-pay&logoColor=white)](upi://pay?pa=aadiinarvekar@upi)

---
## Star 历史 <img src="https://raw.githubusercontent.com/Tarikul-Islam-Anik/Animated-Fluent-Emojis/master/Emojis/Travel%20and%20places/Star.png" alt="Star" width="25" height="25" />

<a href="https://www.star-history.com/#marlboro-advance/mpvEx&type=date&legend=top-left">
 <picture>
   <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=marlboro-advance/mpvEx&type=date&theme=dark&legend=top-left" />
   <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=marlboro-advance/mpvEx&type=date&legend=top-left" />
   <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=marlboro-advance/mpvEx&type=date&legend=top-left" />
 </picture>
</a>
