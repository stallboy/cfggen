@echo off
setlocal
pushd "%~dp0"
if not defined CFGGEN_LUA set "CFGGEN_LUA=lua54"
"%CFGGEN_LUA%" -e "assert(math.type and math.maxinteger >= 9223372036854775807, 'Requires Lua 5.3+ with 64-bit integers')"
if errorlevel 1 (
    echo Set CFGGEN_LUA to the path of a Lua 5.3+ executable with 64-bit integers.
    popd
    exit /b 1
)
"%CFGGEN_LUA%" test.lua
set "result=%errorlevel%"
popd
exit /b %result%
