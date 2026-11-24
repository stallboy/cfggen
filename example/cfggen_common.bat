@set "CFGGEN="
@if not exist "%~dp0..\cfggen.jar" (
    @echo cfggen.jar not found. Run genjar.bat from the repository root first. 1>&2
    @exit /b 1
)

@set CFGGEN=java --sun-misc-unsafe-memory-access=allow -jar "%~dp0..\cfggen.jar"
@exit /b 0
