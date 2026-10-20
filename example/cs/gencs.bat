if exist "Config\Loader.cs" del /q "Config\Loader.cs"

call %~dp0..\cfggen_common.bat
%CFGGEN%  -gen cs,prefix:D,dir:.,encoding:UTF-8,implicitUsings -gen bytes,cipher=xyz

