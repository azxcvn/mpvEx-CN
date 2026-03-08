# mpvEx 中文版

此项目基于 [mpvEx](https://github.com/marlboro-advance/mpvEx) 进行中文本地化。

## ⚠️ 重要说明

本版本基于官方最新的 1.2.8-hotfix 修改而来。请**不要**使用软件内的更新检测功能！软件内的更新检测是基于官方版本推送的，升级后会导致中文汉化消失。请忽略软件内的自动更新提醒。

## 主要修改

- 将界面和文案全部翻译为中文
- 移除捐赠相关内容
- 禁用自动更新

## 💢 构建吐槽

原项目使用了以下本地 AAR 依赖库：
- LazyColumnScrollbar-2.2.0.aar
- mediainfoAndroid-v1.0.0-fix.aar
- sardine-android-0.8.aar
- seeker-2.0.1.aar

然而原作者提供的依赖库下载地址**完全无法访问**，导致构建时卡了整整两个小时！最后不得不自己逐个查找并构建这些依赖库才解决问题。若有人想尝试构建原作者的项目，建议来此仓库下载所提到的四个依赖，其余依赖在使用代理或者镜像后都可以正常下载，唯独这四个无法访问下载，依赖位于：`\app\libs` 下。

## 原项目特性

mpvEx 是一款基于 mpv-android 的 Android 媒体播放器，使用 Jetpack Compose 构建：

- Material 3 设计
- 画中画播放
- 后台播放
- 网络流媒体支持（SMB/FTP/WebDAV）
- 外部字幕和音频支持
- 文件管理器
- 完全开源，无广告

## 下载

从 [GitHub Releases](https://github.com/azxcvn/mpvEx-CN/releases) 下载最新版本。

## 致谢

- [mpv-android](https://github.com/mpv-android/mpv-android)
- [mpvKt](https://github.com/abdallahmehiz/mpvKt)
- [mpvEx](https://github.com/marlboro-advance/mpvEx)

## 许可证

[Apache-2.0](LICENSE)
