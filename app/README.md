# 配置系统

## 主要功能

- 通过配置外键，取值范围，使策划可以随时检测数据一致性
- 通过生成代码，来访问类型化数据，生成外键引用，生成entry、enum，支持java、c#、lua、go、typescript
- 支持多态结构、嵌套结构，可在一个单元格里写任意复杂的结构数据，让excel有了xml的灵活性
- 生成java注重热更安全，生成lua注重内存大小


## 前置要求 (Prerequisites)

* jdk21
* gradle
* 设置 git/bin 路径到Path环境变量中

## 构建与测试 (Build & Test)

在根目录下

### 生成 cfggen.jar，cfggen.exe

```bash
genjar.bat  # 生成 cfggen.jar
```

```bash
mkexe.bat  # 生成 cfggen.zip，里面有 exe
```

### 测试

#### 查看使用说明

```bash
cd example
usage.bat  # 打印使用说明
```

#### 测试 Java：生成 Java 代码和数据

```bash
cd example
genjava.bat # sealed 需要 Java 17 或以上才支持，也可以去掉 sealed
```

#### 测试 Java：检验 Java 生成

```bash
gradle build
java -jar build/libs/example.jar
# 进入命令行，输入 q 退出，输入其他比如 "ai" 会打印表名称以 ai 开头的结构定义和数据
```

#### 测试 Lua

```bash
genlua.bat
cd lua
chcp 65001
lua.exe test.lua
```

#### 测试 C#

```bash
gencshape.bat
cd cs
run.bat
```

#### 测试 Go

```bash
gengo.bat
cd go
go run .
```

#### 测试 TypeScript

```bash
gents.bat
cd ts
pnpm i -D tsx
npx tsx main.ts
```

## 相关链接

* [主项目 README](../README.md)
* [详细文档](https://stallboy.github.io/cfggen)
* [VSCode CFG 扩展](../cfgdev/vscode-cfg-extension/README.md)
* [编辑器 cfgeditor 文档](../cfgeditor/README.md)