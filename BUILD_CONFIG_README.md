# mpvEx 项目构建配置说明

## 代理配置

如果您需要配置 Gradle 使用代理，请修改 `gradle.properties` 文件，取消以下注释并填入您的代理信息：

```properties
# HTTP 代理主机
systemProp.http.proxyHost=您的代理地址
# HTTP 代理端口
systemProp.http.proxyPort=代理端口
# HTTPS 代理主机
systemProp.https.proxyHost=您的代理地址
# HTTPS 代理端口
systemProp.https.proxyPort=代理端口
# 代理用户名（如果需要认证）
systemProp.http.proxyUser=用户名
# 代理密码（如果需要认证）
systemProp.http.proxyPassword=密码
```

## 手动下载大文件

对于某些下载缓慢的文件，您可以手动下载并放到本地缓存目录：

### 1. Gradle Wrapper 文件
- **下载地址**: https://services.gradle.org/distributions/gradle-8.13-bin.zip
- **目标目录**: `C:\Users\27350\.gradle\wrapper\dists\gradle-8.13-bin\`

### 2. Android SDK 组件
- **下载地址**: https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip
- **目标目录**: `%LOCALAPPDATA%\Android\Sdk\cmdline-tools\`

### 3. NDK (如果需要)
- **下载地址**: https://dl.google.com/android/repository/ndk-26r-android24.zip
- **目标目录**: `%LOCALAPPDATA%\Android\Sdk\ndk\26r`

### 4. 手动下载依赖库到本地仓库

如果您需要手动下载特定依赖，可以：

1. 访问 Maven Central: https://repo1.maven.org/maven2/
2. 或 JCenter: https://jcenter.bintray.com/ (已停止服务，替代品见下)
3. 或 Aliyun Maven: https://maven.aliyun.com/

下载后将 `.jar` 或 `.aar` 文件放入本地 Maven 仓库：
```
C:\Users\27350\.m2\repository\（Maven）
或
C:\Users\27350\.gradle\caches\modules-2\files-2.1\（Gradle）
```

## IDE 同步步骤

1. 打开 Android Studio / Cursor IDE
2. 等待 Gradle 同步完成（首次可能需要 10-30 分钟）
3. 如果下载过慢，尝试：
   - 配置代理（见上文）
   - 使用离线模式：`gradle --offline`
   - 手动下载大文件（见上文）

## Debug 版本 APK 大小优化

当前配置已优化：
- 只编译 arm64-v8a ABI（减少约 75% 大小）
- 启用 R8 代码混淆和资源压缩
- 禁用不必要的 ABI splits

Debug 版本预计大小：30-50MB（取决于依赖库）
Release 版本预计大小：< 40MB

## 依赖说明

本项目需要的核心依赖（不建议移除）：

| 依赖库 | 用途 | 大小影响 |
|--------|------|----------|
| mpv-android-lib | 核心播放器库 | ~10MB |
| Coil | 图片加载 | ~2MB |
| Room | 数据库 | ~1MB |
| Koin | 依赖注入 | ~500KB |
| OkHttp | HTTP 客户端 | ~500KB |
| SMBj | SMB 网络共享 | ~1MB |
| MediaInfo | 媒体信息读取 | ~1MB |

所有依赖都是必需的，移除任何一项都可能导致功能缺失。
