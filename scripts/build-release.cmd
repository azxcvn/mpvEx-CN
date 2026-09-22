@echo off
rem ---------------------------------------------------------------------------
rem One-command release build wrapper for mpvEx-CN.
rem
rem Usage (cmd.exe / double-click / any AI tool):
rem   scripts\build-release.cmd -OutputDir "C:\path\to\output"
rem   scripts\build-release.cmd -OutputDir "..." -Flavor standard
rem
rem It just forwards everything to build-release.ps1 with the execution policy
rem bypassed, because a default Windows install has Restricted policy.
rem ---------------------------------------------------------------------------
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0build-release.ps1" %*
exit /b %ERRORLEVEL%
