rm -rf configgen/

call %~dp0..\cfggen_common.bat
%CFGGEN% -gen java,own:-noserver,dir:.,builders:../config/builders.txt,configgendir:.,beautifulName -gen bytes,own:-noserver,schema
