@echo off
setlocal DisableDelayedExpansion
set "output=%~f1"
set "marker=%output%\.cfggen-output"
set "directory=%output%"

:check_directory
for %%F in ("%directory%") do set "outputAttributes=%%~aF"
if not defined outputAttributes goto invalid_directory
if "%outputAttributes:d=%"=="%outputAttributes%" goto invalid_directory
if not "%outputAttributes:l=%"=="%outputAttributes%" goto invalid_directory
for %%F in ("%directory%\..") do set "parent=%%~fF"
if /i "%parent%"=="%directory%" goto check_marker
set "directory=%parent%"
goto check_directory

:check_marker
for %%F in ("%marker%") do set "markerAttributes=%%~aF"
if not defined markerAttributes goto invalid
if not "%markerAttributes:d=%"=="%markerAttributes%" goto invalid
if not "%markerAttributes:l=%"=="%markerAttributes%" goto invalid
exit /b 0

:invalid_directory
>&2 echo Expected a direct cfggen output directory at "%directory%" before refreshing this example.
exit /b 1

:invalid
>&2 echo Expected a regular cfggen output marker at "%marker%" before refreshing this example.
exit /b 1
