@echo off
setlocal
set "output=%~f1"
set "marker=%output%\.cfggen-output"
for %%F in ("%output%") do set "outputAttributes=%%~aF"
for %%F in ("%marker%") do set "markerAttributes=%%~aF"
if not defined markerAttributes goto invalid
if not "%markerAttributes:d=%"=="%markerAttributes%" goto invalid
if not "%markerAttributes:l=%"=="%markerAttributes%" goto invalid
if not "%outputAttributes:l=%"=="%outputAttributes%" goto invalid
exit /b 0

:invalid
>&2 echo Expected a regular cfggen output marker at "%marker%" before refreshing this example.
exit /b 1
