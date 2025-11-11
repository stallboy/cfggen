# 系统状态文档

## 项目概述

这是一个VSCode扩展，为.cfg配置文件提供语法高亮、语义标记和语言支持。

## 技术栈

- **语言**: TypeScript
- **运行时**: Node.js
- **解析器**: ANTLR4 (antlr4ng)
- **构建工具**: Webpack
- **包管理**: npm

## 核心功能

### 1. 语法高亮
- 支持.cfg文件的语法高亮显示
- 使用VSCode内置主题自动适配

### 2. 语义标记
- 结构定义高亮 (struct/interface/table名称)
- 类型标识符高亮 (非基本类型)
- 外键引用高亮
- 元数据关键字高亮
- 主键字段名称高亮
- 唯一键字段名称高亮

### 3. 跳转到定义
- 支持外键引用跳转 (`->table1`, `=>table1[field2]`)
- 支持类型定义跳转 (类型名称)
- 支持多级包名解析 (`pkg1.pkg2.table`)
- 支持嵌套作用域查找 (interface内优先)
- 支持两种搜索策略 (从当前目录/从根目录)
- 智能缓存机制 (基于文件修改时间和大小)
- 模块解析器 (包名到文件路径映射)

### 4. 解析器
- 使用ANTLR4语法解析器
- 自动生成解析器代码
- 支持复杂类型系统

## 项目结构

```
vscode-cfg-extension/
├── src/
│   ├── definition/               # 跳转功能目录
│   │   ├── definitionProvider.ts # 定义提供者主类
│   │   ├── locationVisitor.ts    # 位置访问者 (收集定义和引用)
│   │   ├── moduleResolver.ts     # 模块解析器 (包名映射)
│   │   ├── fileCache.ts          # 文件缓存管理器
│   │   └── types.ts              # 类型定义
│   ├── highlight/                # 高亮功能目录
│   │   ├── semanticTokensProvider.ts    # 语义标记提供者
│   │   ├── HighlightingVisitor.ts       # 语法高亮访问者
│   │   └── tokenTypes.ts                # 标记类型定义
│   ├── grammar/                 # 语法文件目录 (共享)
│   │   ├── Cfg.g4                # ANTLR4语法定义
│   │   ├── CfgLexer.ts           # 生成的词法分析器
│   │   ├── CfgParser.ts          # 生成的解析器
│   │   └── CfgVisitor.ts         # 生成的访问者接口
│   └── extension.ts              # 扩展入口点
├── syntaxes/
│   └── cfg.tmLanguage.json       # TextMate语法定义
├── language-configuration.json   # 语言配置
└── package.json                  # 扩展配置
```

## 依赖项

### 运行时依赖
- `@types/vscode`: VSCode API类型定义
- `antlr4ng`: ANTLR4 TypeScript运行时

### 开发依赖
- `antlr-ng`: ANTLR4代码生成工具
- `typescript`: TypeScript编译器
- `webpack`: 模块打包工具
- `@vscode/vsce`: VSCode扩展打包工具

## 构建和开发

### 构建命令
- `npm run compile`: 编译TypeScript
- `npm run webpack`: 打包扩展
- `npm run package`: 打包发布版本

### 解析器生成
- `npm run generate-parser`: 从Cfg.g4生成解析器代码
- 安装后自动运行解析器生成

## 配置

### 语言配置
- 语言ID: `cfg`
- 文件扩展名: `.cfg`, `.CFG`, `.Cfg`
- 语法范围: `source.cfg`

## 当前状态

### 已实现功能
- ✅ 语法高亮 (TextMate语法)
- ✅ 语义标记 (6种标记类型)
- ✅ ANTLR4解析器集成
- ✅ 扩展打包和发布
- ✅ 跳转到定义 (类型+外键引用)

### 待实现功能
- 暂无

## 版本信息

- 当前版本: 1.1.0
- VSCode引擎: ^1.85.0
- 发布者: thy