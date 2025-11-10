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

### 3. 解析器
- 使用ANTLR4语法解析器
- 自动生成解析器代码
- 支持复杂类型系统

## 项目结构

```
vscode-cfg-extension/
├── src/
│   ├── extension.ts              # 扩展入口点
│   ├── providers/
│   │   ├── semanticTokensProvider.ts    # 语义标记提供者
│   │   ├── HighlightingVisitor.ts       # 语法高亮访问者
│   │   └── tokenTypes.ts                # 标记类型定义
│   └── grammar/
│       ├── Cfg.g4                # ANTLR4语法定义
│       ├── CfgLexer.ts           # 生成的词法分析器
│       ├── CfgParser.ts          # 生成的解析器
│       └── CfgVisitor.ts         # 生成的访问者接口
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

### 待实现功能
- ⏳ 跳转到定义 (类型+外键引用) - 已规划但未实现
- ⏳ 自动补全
- ⏳ 错误检查
- ⏳ 代码格式化

## 版本信息

- 当前版本: 1.1.0
- VSCode引擎: ^1.85.0
- 发布者: thy