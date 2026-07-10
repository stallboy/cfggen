@echo off
setlocal enabledelayedexpansion
REM Run from example/ (double-click or: ./gen_run.bat). Needs Git Bash / unix tools on PATH (rm).
cd /d "%~dp0"

REM === 新增：将本地便携工具目录临时加入 PATH，让 where 命令能找到它们 ===
if exist "%~dp0lua\lua.exe" (
    set "PATH=%~dp0lua;%PATH%"
)

echo ============================================================
echo  cfggen example : gen ^& run ALL languages
echo ============================================================

if not exist "..\cfggen.jar" (
    echo [FATAL] ..\cfggen.jar not found.
    echo         Build it first: cd app ^&^& gradlew.bat fatjar
    goto :summary
)

set /a PASS=0
set /a FAIL=0
set /a SKIP=0
set "FAILS="

echo.
echo ===== GEN PHASE =====
call :gen java             genjava
call :gen java_ls          genjava_ls
call :gen cs               gencs
call :gen cs_ls            gencs_ls
call :gen cs_ls_client     gencs_ls_client
call :gen go               gengo
call :gen go_ls            gengo_ls
call :gen go_ls_client     gengo_ls_client
call :gen lua              genlua
call :gen lua_ls_client    genlua_ls_client
call :gen ts               gents
call :gen ts_ls            gents_ls
call :gen ts_ls_client     gents_ls_client
call :gen gd               gengd
call :gen gd_ls_client     gengd_ls_client
@REM call :gen i18n             gen
@REM call :gen i18n_method1     gen

echo.
echo ===== RUN PHASE =====
call :run java             java
call :run java_ls          java
call :run cs               dotnet
call :run cs_ls            dotnet
call :run cs_ls_client     dotnet
call :run go               go
call :run go_ls            go
call :run go_ls_client     go
call :run lua              lua
call :run lua_ls_client    lua
call :run ts               npx
call :run ts_ls            npx
call :run ts_ls_client     npx
echo [NOTE] gd, gd_ls_client, i18n, i18n_method1 : gen-only (no batch run; gd opens Godot GUI)

:summary
echo.
echo ============================================================
echo  SUMMARY   PASS=!PASS!   FAIL=!FAIL!   SKIP=!SKIP!
if defined FAILS echo  FAILED: !FAILS!
echo ============================================================
endlocal
exit /b 0

REM ============== gen subroutine: %1=dir  %2=genbat (no .bat) ==============
:gen
echo.
echo [GEN] %1
pushd "%~dp0%1"
call %2.bat
if errorlevel 1 (
    echo    ^>^>^> GEN FAILED: %1
    set /a FAIL+=1
    set "FAILS=!FAILS! %1[gen]"
) else (
    echo    GEN ok: %1
    set /a PASS+=1
)
popd
goto :eof

REM ============== run subroutine: %1=dir  %2=tool ==============
:run
where %2 >nul 2>nul
if errorlevel 1 (
    echo [SKIP] %1 : tool '%2' not found on PATH
    set /a SKIP+=1
    goto :eof
)
echo.
echo [RUN] %1

pushd "%~dp0%1"
call run.bat
if errorlevel 1 (
    echo    ^>^>^> RUN FAILED: %1
    set /a FAIL+=1
    set "FAILS=!FAILS! %1[run]"
) else (
    echo    RUN ok: %1
    set /a PASS+=1
)
popd
goto :eof
