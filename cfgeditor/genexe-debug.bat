@echo off
setlocal
pushd "%~dp0" || exit /b 1
call pnpm tauri build --debug
if errorlevel 1 goto done
copy /B /Y src-tauri\target\debug\cfgeditor.exe cfgeditor-d.exe
:done
set "build_exit=%errorlevel%"
popd
exit /b %build_exit%
