# mpvEx 中文版

本项目基于 [mpvEx](https://github.com/marlboro-advance/mpvEx) 进行中文本地化。

想了解原项目的完整特性，请查看已汉化的原项目说明：[docs/README-CN.md](docs/README-CN.md)

## 使用须知

本版本已同步官方最新 1.3.0 的所有修改。请勿使用软件内的更新检测功能，升级后会导致中文汉化丢失。

## 主要修改

- 界面与文案全部翻译为简体中文
- 移除捐赠相关模块
- 禁用自动更新
- 同步上游 1.3.0：按上游移除氛围模式、Lua 脚本、在线字幕搜索（Wyzie）、自定义按钮与树形视图旧扫描实现
- 版本号 1.3.2

## 反馈

本人精力与时间有限，如有未汉化到的地方，欢迎在 GitHub 提交 Issue，我会及时处理。

## 下载

从 [GitHub Releases](https://github.com/azxcvn/mpvEx-CN/releases) 下载最新版本。

## 自行编译

```powershell
# 签名 release（standard 变体）
.\gradlew.bat :app:assembleStandardRelease

# 一键编译 + 校验签名 + 输出到发布目录
scripts\build-release.cmd -OutputDir "C:\Users\root\Desktop\release输出目录\mpvExCN\standard\release"
```

签名凭据从项目根目录的 `keystore.properties` 读取（该文件已被 git 忽略；缺失时 release 为未签名包，不影响构建）。
完整说明见 [docs/BUILD-RELEASE.md](docs/BUILD-RELEASE.md)；给 AI 编码代理的项目约定见 [AGENTS.md](AGENTS.md)。

## 原项目

[https://github.com/marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx)

## 许可证

[Apache-2.0](LICENSE)
