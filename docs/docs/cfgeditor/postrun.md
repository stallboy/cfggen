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

## 立式生效

作为cfgeditor.exe的服务端，需要额外配置postrun, postrunjavadata

一般postrun设置为bat文件，bat里自动上传```<postrunjavadata>``` 到服务器，服务器立马自动使用，客户端立马生效。
```
-gen server
    own=null             提取部分配置，跟cfg中有tag标记的提取
    port=3456            为cfgeditor.exe提供服务的端口
    note=_note.csv       server.note
    aicfg=null           llm大模型选择，需要兼容openai的api
    postrun=null         可以是个xx.bat，用于自动提交服务器及时生效
    postrunjavadata=configdata.zip 如果设置了postrun，增加或更新json后，会先生成javadata文件，然后运行postrun
```

