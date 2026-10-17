# mpvEx 中文版

本项目基于 [mpvEx](https://github.com/marlboro-advance/mpvEx) 进行中文本地化。

想了解原项目的完整特性，请查看已汉化的原项目说明：[docs/README-CN.md](docs/README-CN.md)

## 使用须知

本版本已同步官方最新 1.3.1 的所有修改，汉化版版本号 1.3.3。请勿使用软件内的更新检测功能，升级后会导致中文汉化丢失。

## 主要修改

本汉化版相对原项目只做中文化及少量必要调整：

- 界面与文案全部翻译为简体中文（含硬编码在代码中的文案）
- 移除捐赠相关模块
- 禁用自动更新

各版本同步上游的改动记录见 [docs/release-notes](docs/release-notes)。

## 反馈

本人精力与时间有限，如有未汉化到的地方，欢迎在 GitHub 提交 Issue，我会及时处理。

## 下载

从 [GitHub Releases](https://github.com/azxcvn/mpvEx-CN/releases) 下载最新版本。

## 自行编译

```powershell
# 签名 release（standard 变体）
.\gradlew.bat :app:assembleStandardRelease

# 一键编译 + 校验签名 + 输出到发布目录
scripts\build-release.cmd -OutputDir "C:\Users\Administrator\Desktop\release输出目录\mpvExCN\standard\release"
```

签名凭据从项目根目录的 `keystore.properties` 读取（该文件已被 git 忽略；缺失时 release 为未签名包，不影响构建）。
完整说明见 [docs/BUILD-RELEASE.md](docs/BUILD-RELEASE.md)；给 AI 编码代理的项目约定见 [AGENTS.md](AGENTS.md)。

## 原项目

[https://github.com/marlboro-advance/mpvEx](https://github.com/marlboro-advance/mpvEx)

## 许可证

[Apache-2.0](LICENSE)
