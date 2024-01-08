---
layout: home
title: 策划配表系统
nav_order: 1
---

# 策划配表系统

一个对象数据库浏览器、编辑器、程序访问代码生成器

1. 定义对象结构
2. 使用excel编辑、或直接编辑json、或使用gui编辑，可以浏览所有对象。
3. 生成访问代码

[View it on GitHub][cfggen repo]{: .btn .fs-5 .mb-4 .mb-md-0 }

## 主要功能

* 通过配置外键，取值范围，使策划可以随时检测数据一致性

* 通过生成代码，来访问类型化数据，生成外键引用，生成entry、enum，支持java、c#、lua

* 支持多态结构、嵌套结构，可在一个单元格里写任意复杂的结构数据，让csv有了xml的灵活性

* 生成java注重安全

* 生成lua注重内存大小

## build & test

* 生成configgen.jar
```bash
gradle fatjar  # 打出app/build/libs/configgen.jar
```

* 测试java：生成java代码和数据
```bash
cd example
genjavasealed.bat # genjava 也可以，sealed需要java 17或以上才支持
```

* 测试java：检验java生成
```bash
gradle build 
java -jar build/libs/example.jar # 进入命令行，输入q退出，输入其他比如ai会打印表名称以ai开头的结构定义和数据
```

* 测试lua
```bash
genlua.bat 
cd lua
chcp 65001
lua.exe test.lua
```

* 测试csharp
```bash
gencshape.bat 
cd cs
dotnet run
```

## build cfgeditor.exe
请参考[(编辑器 cfgeditor 文档)][cfgeditor README]

---

[cfggen repo]: https://github.com/stallboy/cfggen
[cfgeditor README]: https://github.com/stallboy/cfggen/blob/master/cfgeditor/README.md