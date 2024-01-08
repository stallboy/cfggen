[(English Documents Available)](README_EN.md)

# 策划配表系统

一个对象数据库浏览器、编辑器、程序访问代码生成器

1. 定义对象结构
2. 使用excel编辑、或使用基于节点的界面来编辑和浏览所有对象。
3. 生成访问代码


## 主要功能

* 支持多态结构，嵌套结构
* 通过配置外键，取值范围，检测数据一致性
* 通过生成代码，使程序方便访问类型化数据，生成外键引用，生成entry、enum（让程序中没有魔数），支持java、c#、lua
* 结构数据可以在excel中配置，也可以json中配置，提供基于节点的界面来编辑和浏览。 
* 生成java注重热更新的安全，生成lua注重内存大小

## Documentation

请阅读[详细文档](https://stallboy.github.io/cfggen)

## Prerequisites

* jdk21
* gradle
* 设置 git/bin 路径到Path环境变量中


## build & test

### 生成cfggen.jar

```bash
genjar.bat  # 生成cfggen.jar
```

### test

* 查看使用说明

```bash
cd example
usage.bat  # 打印使用说明
```

* 测试java：生成java代码和数据

```bash
cd example
genjava.bat # sealed需要java 17或以上才支持，也可以去掉sealed
```

* 测试java：检验java生成

```bash
gradle build 
java -jar build/libs/example.jar 
# 进入命令行，输入q退出，输入其他比如"ai"会打印表名称以ai开头的结构定义和数据
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

## 编辑器cfgeditor.exe
请参考[(编辑器 cfgeditor 文档)](cfgeditor/README.md)