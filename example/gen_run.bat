@echo off
setlocal enabledelayedexpansion
cd /d "%~dp0"

if not exist "..\app\build\libs\cfggen.jar" (
    echo Build cfggen first: app\gradlew.bat -p app fatJar
    exit /b 1
)

set /a PASS=0
set /a FAIL=0
set "FAILS="

call :verify java genjava
call :verify java_ls genjava_ls
call :verify cs gencs
call :verify cs_ls gencs_ls
call :verify cs_ls_client gencs_ls_client
call :verify go gengo
call :verify go_ls gengo_ls
call :verify go_ls_client gengo_ls_client
call :verify lua genlua
call :verify lua_ls_client genlua_ls_client
call :verify ts gents
call :verify ts_ls gents_ls
call :verify ts_ls_client gents_ls_client
call :verify gd gengd
call :verify gd_ls_client gengd_ls_client

echo.
echo SUMMARY PASS=!PASS! FAIL=!FAIL!
if !FAIL! neq 0 (
    echo FAILED:!FAILS!
    exit /b 1
)
exit /b 0

:verify
echo.
echo [VERIFY] %1
pushd "%~dp0%1"
call %2.bat
if errorlevel 1 goto :failed
call run.bat
if errorlevel 1 goto :failed
popd
set /a PASS+=1
echo [PASS] %1
exit /b 0

:failed
popd
set /a FAIL+=1
set "FAILS=!FAILS! %1"
echo [FAIL] %1
exit /b 0
