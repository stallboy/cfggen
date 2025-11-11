[(English Documents Available)](README_EN.md)


# 策划配表系统

![intro](docs/assets/intro.png)

一个对象数据库浏览器、编辑器、程序访问代码生成器

1. 定义对象结构
2. 使用excel编辑、或使用基于节点的界面来编辑和浏览所有对象。
3. 生成访问代码


## 主要功能

* 支持多态结构，嵌套结构
* 通过配置外键，取值范围，检测数据一致性
* 通过生成代码，使程序方便访问类型化数据，生成外键引用，生成entry、enum（让程序中没有魔数），支持java、c#、lua、go、typescript
* 结构数据可以在excel中配置，也可以json中配置，提供基于节点的界面来编辑和浏览。 
* 生成java注重热更新的安全，生成lua注重内存大小

## Documentation

请阅读[详细文档](https://stallboy.github.io/cfggen)

## 快速开始

### 配表系统 cfggen

请参考 [配置系统 文档](app/README.md)。

### 编辑器 cfgeditor.exe

请参考 [编辑器 cfgeditor 文档](cfgeditor/README.md)


### VSCode插件：cfg-support 

我们为 `.cfg` 配置文件提供了专门的 VSCode 插件，包含以下功能：

- **语法高亮**: 结构定义、类型标识符、外键引用等
- **跳转到定义**: Ctrl+点击类型名称或外键引用跳转到定义位置

详细功能和使用说明请参考 [VSCode CFG 扩展文档](cfgdev/vscode-cfg-extension/README.md)。

