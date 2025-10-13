---
layout: page
title: 立时生效
parent: 编辑器
nav_order: 2
---

# 立时生效
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

## cfgeditor_server自动重载

```bat
java -jar ../cfggen.jar -datadir ../example/config  -gen server,watch=1
```

如上加入watch=1，表明监控 -datadir 目录修改，如有能影响实际配表数据或结构的改动，则自动重读。
1是则表示变化发生1秒内未有进一步文件变化，则自动重载配置，可以更大一点。

这样当你使用cfgeditor.exe期间，更新svn，或手动更改excel文件或.cfg或.json，就无需重启cfgeditor_server了。
只要等cfgeditor_server重载完了，在cfgeditor.exe里按F5刷新就可以了。


## 游戏服务器或客户端自动重载

```bat
java -jar ../cfggen.jar -datadir ../example/config  -gen server,watch=1,postrun=upload.bat
```

使用postrun=upload.bat参数， upload.bat如下

```bat
:: -gen javadata
:: -gen lua,dir:../Unity/Lua,own:client,emmylua:true,sharedEmptyTable:true,shared

rem curl -xx upload generated confg.data to server 
```

则当配置文件更改，则会先解析出postrun里的最开始的注释行
- 如果是bat，则提取:: -gen 开头的行，来gen
- 如果是sh，则提取# -gen 开头的行，来gen

解析注释行：是因为这些gen会再当前的server java进程中直接利用已加载好的数据来生成，而不用额外启java进程来做。更高效。

然后执行此bat，加入游戏服务器没开在本机器，可以scp或curl过去。


- 游戏服务器需要检测到配置数据变化，自动加载使用
- 游戏客户端也需要

结合以上就实现了，配置更改服务器无需重启，客户端无需重启，自动生效。策划的修改配表、检验游戏循环基本0延时。
