# One-command release build for mpvEx-CN.
#
#   powershell -NoProfile -ExecutionPolicy Bypass -File scripts\build-release.ps1 -OutputDir "C:\path\to\output"
#   scripts\build-release.cmd -OutputDir "..."            (same thing, policy bypass included)
#   ... -Flavor standard|fdroid|playstore
#   ... -SkipVerify
#
# Signing credentials are read by Gradle from the git-ignored keystore.properties
# in the repo root. If that file is missing, the release APK is UNSIGNED.
#
# This script is intentionally ASCII-only so that Windows PowerShell 5.1 parses it
# correctly regardless of the file encoding. PowerShell 7 is NOT required.

[CmdletBinding()]
param(
    [string]$OutputDir = "",
    [string]$Flavor = "standard",
    [switch]$SkipVerify
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$capFlavor = $Flavor.Substring(0, 1).ToUpper() + $Flavor.Substring(1)
$taskName = ":app:assemble${capFlavor}Release"
$apkDir = Join-Path $repoRoot "app\build\outputs\apk\$Flavor\release"

Push-Location $repoRoot
try {
    if (Test-Path "keystore.properties") {
        $alias = (Get-Content "keystore.properties" |
                  Where-Object { $_ -match '^\s*keyAlias\s*=' } |
                  Select-Object -First 1)
        Write-Host "[release] signing: keystore.properties found ($alias)"
    } else {
        Write-Warning "[release] keystore.properties not found - the APK will be UNSIGNED"
    }

    Write-Host "[release] running .\gradlew.bat $taskName"
    & .\gradlew.bat $taskName --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle build failed with exit code $LASTEXITCODE" }

    $apks = @(Get-ChildItem $apkDir -Filter *.apk -ErrorAction SilentlyContinue)
    if ($apks.Count -eq 0) { throw "No APK produced in $apkDir" }

    # Locate apksigner from the Android SDK declared in local.properties (fallback: env vars).
    function Get-ApkSigner {
        $sdk = $null
        if (Test-Path "local.properties") {
            $line = Get-Content "local.properties" | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
            if ($line) {
                $sdk = ($line -replace '^sdk\.dir=', '').Replace('\\', '\').Replace('\:', ':')
            }
        }
        if (-not $sdk) { $sdk = $env:ANDROID_HOME }
        if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
        if (-not $sdk -or -not (Test-Path $sdk)) { return $null }

        $tools = Get-ChildItem (Join-Path $sdk "build-tools") -Directory -ErrorAction SilentlyContinue |
                 Sort-Object { [version]($_.Name -replace '[^0-9\.].*$', '') } |
                 Select-Object -Last 1
        if (-not $tools) { return $null }
        $exe = Join-Path $tools.FullName "apksigner.bat"
        if (Test-Path $exe) { return $exe }
        return $null
    }

    $apksigner = Get-ApkSigner
    if (-not $SkipVerify) {
        if ($apksigner) {
            foreach ($apk in $apks) {
                Write-Host "[release] verify $($apk.Name)"
                & $apksigner verify --print-certs $apk.FullName | Select-String "SHA-256 digest" | ForEach-Object { "    $($_.Line.Trim())" }
                if ($LASTEXITCODE -ne 0) { throw "Signature verification FAILED for $($apk.Name)" }
            }
        } else {
            Write-Warning "[release] apksigner not found - skipping signature verification"
        }
    }

    if ($OutputDir -ne "") {
        New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null
        Copy-Item (Join-Path $apkDir "*.apk") $OutputDir -Force
        if (Test-Path (Join-Path $apkDir "output-metadata.json")) {
            Copy-Item (Join-Path $apkDir "output-metadata.json") $OutputDir -Force
        }
        if (Test-Path (Join-Path $apkDir "baselineProfiles")) {
            Copy-Item (Join-Path $apkDir "baselineProfiles") $OutputDir -Recurse -Force
        }
        Write-Host "[release] copied to $OutputDir"
    }

    Write-Host ""
    Write-Host "[release] artifacts:"
    foreach ($apk in $apks) {
        $hash = (Get-FileHash $apk.FullName -Algorithm SHA256).Hash
        Write-Host ("  {0,-42} {1,7:N2} MB  sha256={2}" -f $apk.Name, ($apk.Length / 1MB), $hash)
    }
    Write-Host "[release] done."
}
finally {
    Pop-Location
}
