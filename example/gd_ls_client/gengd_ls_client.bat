cd /d "%~dp0" || exit /b 1
call "%~dp0..\cfggen_common.bat" || exit /b 1

if exist "config\ConfigPackage.gd" del /q "config\ConfigPackage.gd"
REM 删除通过 copyFileIfNotExist 复制的支持文件
if exist "config\ConfigStream.gd" del /q "config\ConfigStream.gd"
if exist "config\ConfigLoader.gd" del /q "config\ConfigLoader.gd"
if exist "config\ConfigErrors.gd" del /q "config\ConfigErrors.gd"
if exist "config\TextPoolManager.gd" del /q "config\TextPoolManager.gd"
if exist "config\ConfigText.gd" del /q "config\ConfigText.gd"

%CFGGEN% -datadir ../config -langswitchdir ../i18n/langs -gen gd,own:-nogd,dir:config -gen bytes,own:-nogd
if errorlevel 1 exit /b %errorlevel%

echo Generated GDScript client code to gd_ls_client/
