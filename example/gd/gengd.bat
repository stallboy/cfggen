if exist "config\ConfigPackage.gd" del /q "config\ConfigPackage.gd"
if exist "config\ConfigStream.gd" del /q "config\ConfigStream.gd"
if exist "config\ConfigLoader.gd" del /q "config\ConfigLoader.gd"
if exist "config\ConfigErrors.gd" del /q "config\ConfigErrors.gd"
if exist "config\TextPoolManager.gd" del /q "config\TextPoolManager.gd"

call %~dp0..\cfggen_common.bat
%CFGGEN% -gen gd,own:-nogd,dir:config -gen bytes,own:-nogd,dir:.
