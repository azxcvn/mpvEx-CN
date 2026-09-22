# 构建与发布（mpvEx-CN）

本文档说明本汉化分支如何编译 **签名 release 包**并输出到发布目录。
给 AI 代理看的精简版约定在仓库根目录 [`AGENTS.md`](../AGENTS.md)。

---

## 1. 环境要求

| 项 | 要求 |
|---|---|
| JDK | 21（`java -version` 能输出 21.x；Gradle 用 PATH 上的 JDK） |
| Android SDK | 路径写在 `local.properties` 的 `sdk.dir`；需要 **Platform 37** 与 Build-Tools |
| Gradle | 用仓库自带 wrapper（`gradlew.bat`），首次会自动下载 Gradle 9.6.0（约 140 MB） |
| 网络 | 首次构建需要下载 AGP 9.4 / Kotlin 2.3.20 / Compose BOM 等依赖；`gradle.properties` 里已配 `127.0.0.1:10808` 代理，按需修改 |

`compileSdk = 37`、`minSdk = 26`、`targetSdk = 36`。

---

## 2. 签名配置

签名配置写在 [`app/build.gradle.kts`](../app/build.gradle.kts) 里，凭据从**项目根目录**的 `keystore.properties` 读取：

```properties
# 已被 .gitignore 忽略，切勿提交
# storeFile 请用正斜杠：.properties 里反斜杠是转义符
storeFile=C:/Users/root/Desktop/签名/mpv/mpv
storePassword=****
keyAlias=key0
keyPassword=****
```

要点：

- 文件**不存在**时，release 变体为**未签名包**，构建不会失败 —— 别人 clone 仓库或以后接 CI 都不需要密钥。
- 文件存在但缺字段时，构建会直接报 `keystore.properties is missing 'xxx'`，不会静默产出未签名包。
- 路径按 **UTF-8** 读取，因此含中文的目录（如 `C:/Users/root/Desktop/签名/...`）可以正常使用。
- 签名方案固定为 **v1=false / v2=true / v3=false**，与历史发布包一致。要做密钥轮换时，把
  `enableV3Signing = false` 改成 `true`。
- `preview` 构建类型会强制 `signingConfig = null`（不签名），不受影响。
- `standard` / `fdroid` / `playstore` 三个变体的 release 都会用同一份密钥签名。

当前使用的密钥：别名 **key0**，证书 SHA-256
`9f3aceedf43bfc8c79ef5eb448fb383169bddef2dd9c707199ee2a35c7d0fada`。

> keystore 里还有另一个别名 `key1`（证书 `9bc2e844…`），那是更早期的签名，**不要**再用它，否则与现有发布包签名不一致。

---

## 3. 编译

```powershell
cd C:\Users\root\Desktop\mpvEx-master

# 签名 release（standard 变体）
.\gradlew.bat :app:assembleStandardRelease

# 调试包
.\gradlew.bat :app:assembleStandardDebug

# 其它变体
.\gradlew.bat :app:assembleFdroidRelease
.\gradlew.bat :app:assemblePlaystoreRelease
```

产物目录：

```
app\build\outputs\apk\standard\release\
├── app-standard-arm64-v8a-release.apk
├── app-standard-armeabi-v7a-release.apk
├── output-metadata.json
└── baselineProfiles\0\, 1\
```

发布时**整目录拷贝**（APK + output-metadata.json + baselineProfiles），与历史发布保持一致。

---

## 4. 一键脚本（推荐）

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -OutputDir "C:\Users\root\Desktop\release输出目录\mpvExCN\standard\release"
```

也可以走 `.cmd` 包装（自动带上 `-ExecutionPolicy Bypass`，在 cmd 窗口、双击或任何工具里都能调用）：

```
scripts\build-release.cmd -OutputDir "C:\Users\root\Desktop\release输出目录\mpvExCN\standard\release"
```

脚本会：编译 → 用 `apksigner` 校验签名并打印证书指纹 → 拷贝 APK、`output-metadata.json`、
`baselineProfiles` 到目标目录 → 打印文件大小与 SHA-256。

参数：

| 参数 | 默认 | 说明 |
|---|---|---|
| `-OutputDir` | 空（只编译不拷贝） | 发布输出目录 |
| `-Flavor` | `standard` | `standard` / `fdroid` / `playstore` |
| `-SkipVerify` | 关 | 跳过 apksigner 校验 |

注意：

- 本机**没有安装 PowerShell 7（`pwsh`）**，上面的命令用的是系统自带的 Windows PowerShell 5.1，足够了。
- 脚本本身是纯 ASCII 的，PS 5.1 与 PS 7 都能直接跑；`.cmd` 包装里已经带了 `-ExecutionPolicy Bypass`，
  避免默认 `Restricted` 策略拦截。
- 不要用 `-OutputDir ""` 这种空参数形式（会报参数缺失）；只想编译不拷贝时直接省略 `-OutputDir`。

---

## 5. 手动流程（等价于脚本）

```powershell
cd C:\Users\root\Desktop\mpvEx-master
.\gradlew.bat :app:assembleStandardRelease

$src = "app\build\outputs\apk\standard\release"
$dst = "C:\Users\root\Desktop\release输出目录\mpvExCN\standard\release"
Copy-Item "$src\*.apk" $dst -Force
Copy-Item "$src\output-metadata.json" $dst -Force
Copy-Item "$src\baselineProfiles" $dst -Recurse -Force
```

校验（可选）：

```powershell
$bt = "C:\Users\root\AppData\Local\Android\Sdk\build-tools\36.1.0"
& "$bt\apksigner.bat" verify --print-certs "$dst\app-standard-arm64-v8a-release.apk"
& "$bt\zipalign.exe" -c -p 4 "$dst\app-standard-arm64-v8a-release.apk"
```

---

## 6. 发新版本时要做的事

1. 改 `app/build.gradle.kts` 里的 `versionCode` / `versionName`（汉化版历史上比上游高一位，如上游 1.3.0 → 汉化版 1.3.2）。
2. 若要同步上游：按 `AGENTS.md` 的铁律**逐文件手工同步**，不要 `git merge`。
3. 执行第 4 节的一键脚本。
4. 验证：`apksigner verify` 通过、证书指纹为 `9f3aceed…`、versionCode 符合预期（arm64 为 `versionCode*10+2`）。
5. 把 APK 传到 GitHub Releases，并在 `README.md` 的「使用须知」里更新已同步的上游版本号。

---

## 7. 常见问题

**Q：`compileSdk 37` 报错 / 找不到 Platform 37**
在 Android Studio 的 SDK Manager 里安装 Android SDK Platform 37（本机装在
`C:\Users\root\AppData\Local\Android\Sdk\platforms\android-37.0`）。

**Q：构建产物里没有 x86_64 包，模拟器装不上**
正常。仓库只打 arm64-v8a / armeabi-v7a。见 `AGENTS.md` 的「ABI 拆分」一节，临时加 `x86_64` 后记得改回。

**Q：release 包是未签名的**
检查 `keystore.properties` 是否存在、字段是否齐全，以及 `storeFile` 路径是否存在（正斜杠）。

**Q：签名指纹和历史发布包不一致**
`keyAlias` 必须是 `key0`，不能是 `key1`。

**Q：`keystore.properties` 会不会被提交？**
不会，已在 `.gitignore` 中（同时忽略了 `*.jks` / `*.keystore`）。换电脑时请单独备份该文件与 keystore。

**Q：改了文案后编译报 `Unresolved reference 'xxx中文'`**
Kotlin 里 `$变量` 紧跟中文时中文会被并进标识符。改成 `${变量}`。详见 `AGENTS.md`。
