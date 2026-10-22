@echo off
setlocal
pushd "%~dp0"
if not defined CFGGEN_GODOT set "CFGGEN_GODOT=Godot_v4.6-stable_win64_console.exe"
"%CFGGEN_GODOT%" --headless --path . --editor --import --quit
if errorlevel 1 goto failed
"%CFGGEN_GODOT%" --headless --path . --log-file .godot/verify.log --script verify.gd
set "result=%errorlevel%"
popd
exit /b %result%
:failed
popd
exit /b 1
