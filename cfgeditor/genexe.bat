@echo off
setlocal
pushd "%~dp0" || exit /b 1
call pnpm tauri build
if errorlevel 1 goto done
copy /B /Y src-tauri\target\release\cfgeditor.exe cfgeditor.exe
:done
set "build_exit=%errorlevel%"
popd
exit /b %build_exit%
