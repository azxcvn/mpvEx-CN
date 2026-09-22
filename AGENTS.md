# AGENTS.md — 给 AI 编码代理的项目约定

本仓库是 [mpvEx](https://github.com/marlboro-advance/mpvEx) 的**简体中文汉化分支**（作者 azxcvn，仓库 mpvEx-CN）。

## 铁律

1. **不要用 `git merge` 同步上游。** 本仓库在原项目基础上做了大量汉化，直接 merge 会大量冲突且难以收拾。
   正确做法：取上游两个版本（例如 tag `v1.2.9` → `v1.3.0`）逐文件比对差异，再手工把改动落到本地**已汉化**的文件里，中文必须保留。
2. **不要擅自 `git commit` / `git push`。** 改完交给用户确认。
3. **不要提交 `keystore.properties`**（含签名口令，已在 `.gitignore` 中）。
4. 遇到需要拍板的事（例如上游删掉了某个功能要不要跟、某个文件要不要删），**先问用户**，不要自作主张。

## 汉化约定

- 界面文案一律简体中文，使用全角标点（，。：？！）。
- 字符串资源集中在 `app/src/main/res/values/strings.xml`；但本项目**大量文案是硬编码在 Kotlin 里的**，改文案时两边都要留意。
- Kotlin 里 `$变量` 后面若紧跟中文字符，**必须写成 `${变量}`**，否则中文会被当作标识符的一部分，直接编译失败：
  - ❌ `"共 $count 个"`（`$count` 与中文之间没有空格时必然出错）
  - ✅ `"共 ${count} 个"` / `"共 $count 个"`（有空格也建议加花括号，统一写 `${count}`）
  - 自检：全仓正则搜 `\$[A-Za-z_]\w*[\u4e00-\u9fff]`，应无结果。
- **不要翻译**：`Log.*` 日志、mpv 属性名/取值（`glsl-shaders`、`panscan` 等）、URL、mime type、文件名、
  协议命令（`OPTS UTF8 ON`）、用于匹配第三方库报错的英文串（如 SMB 的 `Access is denied` / `does not exist`）、
  `@SerialName`、SharedPreferences 键、Compose `key =`、以及被当作比较哨兵使用的字符串。
- 改完建议至少做静态自检：冲突标记、UTF-8 BOM、`R.string.*` 是否都有定义、是否还引用已删除的符号。

## 构建

环境要求：JDK 21、Android SDK（路径写在 `local.properties` 的 `sdk.dir`）、Gradle wrapper 9.6.0、compileSdk 37。

| 目的 | 命令 |
|---|---|
| 调试包 | `.\gradlew.bat :app:assembleStandardDebug` |
| **签名 release 包** | `.\gradlew.bat :app:assembleStandardRelease` |
| fdroid / playstore release | `.\gradlew.bat :app:assembleFdroidRelease` / `.\gradlew.bat :app:assemblePlaystoreRelease` |

- release 的签名配置在 `app/build.gradle.kts`（约 14–38 行读取配置、98–117 行应用），凭据来自项目根目录的
  **`keystore.properties`**（已被 git 忽略）。该文件不存在时 release 产出**未签名包**，不会报错——所以别人 clone 后照样能构建。
- 当前密钥：别名 `key0`；证书 SHA-256 `9f3aceedf43bfc8c79ef5eb448fb383169bddef2dd9c707199ee2a35c7d0fada`；
  签名方案固定 **v2（v1/v3 关闭）**，与历史发布包一致。
- 产物目录：`app\build\outputs\apk\<flavor>\release\`，内含按 ABI 拆分的 APK + `output-metadata.json` + `baselineProfiles\`。

## 发布产物输出位置

```
C:\Users\root\Desktop\release输出目录\mpvExCN\<flavor>\release
```

一键「编译 → 校验签名 → 拷贝」（本机**没有** PowerShell 7，用 5.1；脚本是纯 ASCII，5.1 可直接跑）：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -OutputDir "C:\Users\root\Desktop\release输出目录\mpvExCN\standard\release"
```

或走 `.cmd` 包装（自动带上 `-ExecutionPolicy Bypass`，cmd 窗口/双击/任何工具都能调）：

```
scripts\build-release.cmd -OutputDir "C:\Users\root\Desktop\release输出目录\mpvExCN\standard\release"
```

更多细节（环境准备、手动步骤、常见报错）见 [`docs/BUILD-RELEASE.md`](docs/BUILD-RELEASE.md)。

## ABI 拆分

`splits.abi` 只打 **arm64-v8a / armeabi-v7a**（debug 与 release 都是）。不要在仓库里长期保留 x86_64。
要在 x86_64 模拟器（MuMu、夜神等）上调试时，临时改成：

```kotlin
include("arm64-v8a", "armeabi-v7a", "x86_64")
```

调试结束记得改回 `include("arm64-v8a", "armeabi-v7a")`，否则发布目录会多出 x86_64 包。
另外注意：模拟器上无法安装 arm64 包时，仅是因为 ABI 不匹配，不代表构建有问题。

## 版本号

在 `app/build.gradle.kts` 的 `versionCode` / `versionName`。汉化版历史上比上游高（上游 1.3.0 对应汉化版 1.3.2），递增即可。
APK 实际 versionCode 会在 Gradle 里按 ABI 再拼接（`versionCode * 10 + abiCode`，arm64-v8a = 2、armeabi-v7a = 1）。

## 已知遗留（改动时不必理会）

- `ui/UpdateFeature.kt` 已删除；真正生效的更新逻辑在 `utils/update/UpdateFeature.kt`，且「自动更新」已被禁用。
- `utils/storage/FolderScanUtils.kt`、`utils/storage/StorageScanUtils.kt`、
  `ui/preferences/components/PlayerLayoutPreview.kt` 是很早版本上游遗留下来的死代码（上游早已删除），
  本项目仍保留。它们会被编译，但没有任何地方引用。
