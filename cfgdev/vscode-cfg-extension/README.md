# VSCode CFG Language Support

[![Version](https://img.shields.io/badge/version-1.0.0-blue.svg)](https://github.com/your-username/cfg-language-support)
[![VSCode](https://img.shields.io/badge/VSCode-1.85+-green.svg)](https://code.visualstudio.com/)
[![License](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE)
[![Language](https://img.shields.io/badge/language-TypeScript-red.svg)](src/)

一个强大的 VSCode 扩展，为 `.cfg` 配置文件提供完整的语言支持功能。基于 ANTLR4 语法解析，提供语法高亮、跳转到定义、自动补全等 IDE 级功能。

## ✨ 特性

- 🎨 **语法高亮**: 基于 ANTLR4 的精确语法高亮，支持两套主题色（默认 + 中国古典色）
- 🔍 **跳转到定义**: 支持外键引用和类型定义的跨模块导航
- ⚡ **智能补全**: 上下文感知的自动补全，包括类型、外键、元数据
- 🏷️ **悬停提示**: 悬停查看类型信息和文档
- 📊 **引用查找**: 查找符号的所有引用位置
- 🌐 **跨模块支持**: 自动解析跨模块引用关系
- 🚀 **高性能**: 支持大文件处理，增量解析和缓存优化

## 📷 截图

### 语法高亮效果

**默认主题**:
```cfg
struct Position {
    x:int;      // 蓝色关键字，绿色注释
    y:int;
}
```

**中国古典色主题** (默认):
```cfg
struct Position {
    x:int;      // 黛青关键字，竹青注释
    y:int;
}
```

### 功能演示

- **自动补全**: 输入类型时显示候选项
- **跳转到定义**: 按 F12 跳转到定义位置
- **悬停提示**: 悬停显示类型信息

## 🚀 安装

### 方法一：从 VSCode 市场安装（推荐）

1. 打开 VSCode
2. 按 `Ctrl+Shift+X` 打开扩展面板
3. 搜索 "CFG Language Support"
4. 点击"安装"

### 方法二：从 VSIX 文件安装

1. 下载 `cfg-language-support-1.0.0.vsix` 文件
2. 在 VSCode 中按 `Ctrl+Shift+P`
3. 输入 "Extensions: Install from VSIX..."
4. 选择下载的 VSIX 文件

### 方法三：开发模式安装（源码）

```bash
# 1. 克隆仓库
git clone https://github.com/your-username/cfg-language-support.git
cd cfg-language-support

# 2. 安装依赖
npm install

# 3. 编译扩展
npm run compile

# 4. 在 VSCode 中调试
# 按 F5 打开扩展开发主机
```

## 🛠️ 开发环境搭建

### 前置要求

- Node.js 18+
- npm 8+
- VSCode 1.85+
- TypeScript 5.3+

### 环境初始化

```bash
# 1. 安装依赖
npm install

# 2. 生成 ANTLR4 解析器
npm run generate-parser

# 3. 编译 TypeScript
npm run compile

# 4. 监听文件变化（开发模式）
npm run watch
```

### 运行测试

```bash
# 运行所有测试
npm test

# 运行特定测试
npm run test -- --grep "completion"

# 手动运行自动补全测试
node test/runCompletionTests.js

# 手动运行跳转定义测试
node test/runDefinitionTests.js
```

### 代码质量检查

```bash
# ESLint 检查
npm run lint

# 自动修复
npm run lint -- --fix
```

## 📦 打包

### 生成 VSIX 文件

```bash
# 1. 确保代码已编译
npm run compile

# 2. 运行 linting 检查
npm run lint

# 3. 打包为 VSIX
npm run package
```

成功后会生成 `cfg-language-support-1.0.0.vsix` 文件。

### 自定义版本号

编辑 `package.json` 中的 version 字段：

```json
{
  "version": "1.0.0"
}
```

然后重新打包：

```bash
npm run package
```

## 🌐 发布到 VSCode 市场

### 步骤 1：准备发布

1. 更新版本号（package.json）
2. 更新 CHANGELOG.md
3. 确保所有测试通过
4. 编译代码

```bash
npm run compile
npm test
npm run package
```

### 步骤 2：获取发布令牌

1. 登录 [Azure DevOps](https://dev.azure.com)
2. 创建个人访问令牌（Personal Access Token）
3. 记录令牌（格式类似：`xxxxxxxxxxxxxxxxxxxx`）

### 步骤 3：登录并发布

```bash
# 1. 登录（会提示输入令牌）
npx vsce login your-publisher-name

# 2. 发布
npx vsce publish
```

或指定版本发布：

```bash
npx vsce publish 1.0.0
```

### 步骤 4：发布到 Open VSX（可选）

```bash
# 安装 ovsx
npm install -g @ovsx/cli

# 登录
ovsx login your-publisher-name -p <YOUR_PAT>

# 发布
ovsx publish
```

### 常见问题

**Q: 发布失败，提示 "Unauthorized"**
```
A: 确保：
1. 令牌有效且未过期
2. 发布者名称正确（与 package.json 中的 publisher 一致）
3. 令牌有足够的权限
```

**Q: 发布失败，提示 "Extension already exists"**
```
A: 递增版本号后重新发布
```

**Q: 打包失败，TypeScript 错误**
```
A: 修复所有 linting 错误
npm run lint
```

## ⚙️ 配置

### 设置位置

在 VSCode 设置中搜索 "CFG" 或手动编辑 `settings.json`：

```json
{
  "cfg.theme": "chineseClassical",
  "cfg.enableCache": true,
  "cfg.maxFileSize": 10485760
}
```

### 配置项说明

| 配置项 | 类型 | 默认值 | 说明 |
|--------|------|--------|------|
| `cfg.theme` | string | "chineseClassical" | 主题选择：<br/>- "default": VSCode 标准配色<br/>- "chineseClassical": 中国古典色 |
| `cfg.enableCache` | boolean | true | 启用符号表缓存，提升大文件性能 |
| `cfg.maxFileSize` | number | 10485760 | 最大文件大小（字节），默认 10MB |

### 推荐配置

```json
{
  "cfg.theme": "chineseClassical",
  "cfg.enableCache": true,
  "cfg.maxFileSize": 10485760,
  "editor.formatOnSave": true,
  "files.associations": {
    "*.cfg": "cfg"
  }
}
```

## 📖 使用说明

### 基本操作

1. **打开 .cfg 文件**
   - 扩展会自动激活
   - 语法高亮立即生效

2. **跳转到定义**
   - 将光标放在符号上
   - 按 `F12` 或 `Ctrl+Click`
   - 右键菜单选择"转到定义"

3. **自动补全**
   - 输入类型名时自动触发
   - 按 `Ctrl+Space` 手动触发
   - 选择候选项按 `Enter` 或 `Tab`

4. **悬停提示**
   - 将鼠标悬停在符号上
   - 显示类型信息和文档

### 高级功能

**跨模块引用**:
```cfg
// 在 task.cfg 中
struct TaskData {
    itemId: int ->item.item    // 跳转到 item 模块
    monsterId: int ->npc.monster  // 跳转到 npc 模块
}
```

**接口多态**:
```cfg
interface completecondition {
    struct KillMonster {
        monsterid:int ->other.monster;
    }
}
```

## 🏗️ 项目结构

```
vscode-cfg-extension/
├── src/
│   ├── extension.ts              # 扩展入口
│   ├── grammar/
│   │   ├── Cfg.g4               # ANTLR4 语法定义
│   │   └── *.ts                 # 自动生成的解析器
│   ├── models/                   # 数据模型
│   │   ├── configFile.ts
│   │   ├── definition.ts
│   │   ├── structDefinition.ts
│   │   ├── tableDefinition.ts
│   │   ├── fieldDefinition.ts
│   │   ├── foreignKeyDefinition.ts
│   │   ├── metadataDefinition.ts
│   │   ├── symbolTable.ts
│   │   └── index.ts
│   ├── providers/                # LSP 提供器
│   │   ├── completionProvider.ts
│   │   ├── definitionProvider.ts
│   │   ├── hoverProvider.ts
│   │   ├── referenceProvider.ts
│   │   ├── syntaxHighlightingProvider.ts
│   │   └── themeConfig.ts
│   ├── services/                 # 核心服务
│   │   ├── cacheService.ts
│   │   ├── cfgParserService.ts
│   │   ├── moduleResolverService.ts
│   │   └── symbolTableService.ts
│   ├── server/                   # 语言服务器
│   │   └── cfgLanguageServer.ts
│   └── utils/                    # 工具函数
│       ├── logger.ts
│       ├── performance.ts
│       └── namespaceUtils.ts
├── test/                         # 测试文件
│   ├── fixtures/                 # 测试用例
│   ├── runCompletionTests.js     # 自动补全测试
│   └── runDefinitionTests.js     # 跳转定义测试
├── .eslintrc.json
├── .eslintignore
├── .gitignore
├── language-configuration.json
├── package.json
├── tsconfig.json
└── README.md
```

## 🧪 测试

### 单元测试

```bash
# 运行所有单元测试
npm test

# 编译并运行测试
npm run pretest
```

### 手动测试

```bash
# 测试自动补全功能
node test/runCompletionTests.js

# 测试跳转到定义功能
node test/runDefinitionTests.js
```

### 测试用例

测试文件位于 `test/fixtures/` 目录：
- `definitions/`: 跳转定义测试用例
- `completions/`: 自动补全测试用例
- `test-config.cfg`: 主测试文件

## 🤝 贡献

欢迎贡献代码！请遵循以下步骤：

1. Fork 此仓库
2. 创建特性分支：`git checkout -b feature/your-feature`
3. 提交更改：`git commit -m 'Add some feature'`
4. 推送到分支：`git push origin feature/your-feature`
5. 创建 Pull Request

### 贡献指南

- 遵循现有代码风格
- 添加必要的测试
- 更新相关文档
- 确保所有测试通过

```bash
# 开发工作流
git checkout -b feature/new-feature
npm run watch  # 开发模式
# 编辑代码...
npm run lint   # 检查代码
npm test       # 运行测试
git commit -m 'feat: add new feature'
git push origin feature/new-feature
```

### 问题反馈

如果您遇到问题或有任何建议，请在 [GitHub Issues](https://github.com/your-username/cfg-language-support/issues) 中创建 Issue。

## 📄 许可证

本项目基于 [MIT 许可证](LICENSE) 开源。

## 🙏 致谢

- [ANTLR4](https://www.antlr.org/) - 强大的语法分析器生成器
- [VSCode](https://code.visualstudio.com/) - 优秀的代码编辑器
- [VSCode Extension API](https://code.visualstudio.com/api) - 扩展开发平台
- [Language Server Protocol](https://microsoft.github.io/language-server-protocol/) - 语言服务器协议

## 📞 支持

如果您觉得这个扩展有用，请给我们一个 ⭐！

如有问题，请：
- 查看 [FAQ](https://github.com/your-username/cfg-language-support/wiki/FAQ)
- 搜索 [Issues](https://github.com/your-username/cfg-language-support/issues)
- 创建新的 [Issue](https://github.com/your-username/cfg-language-support/issues/new)

---

**CFG Language Support** - 让配置文件开发更简单！🎉
