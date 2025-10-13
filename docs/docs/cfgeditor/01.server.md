---
layout: page
title: cfgeditor服务器
parent: 编辑器
nav_order: 1
---

# cfgeditor服务器
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

## 启动

使用cfgeditor.exe时，要伴随开启server

启动脚本如下
```bash
java -jar ../cfggen.jar -datadir ../example/config  -gen server
```
