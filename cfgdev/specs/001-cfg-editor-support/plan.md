# Implementation Plan: VSCode CFG Extension

**Branch**: `001-cfg-editor-support` | **Date**: 2025-11-08 | **Spec**: [link](spec.md)
**Input**: Feature specification from `/specs/001-cfg-editor-support/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

开发一个VSCode扩展，为.cfg配置文件提供完整的语言支持功能。基于ANTLR4定义的Cfg.g4语法，实现语法高亮、跳转定义、自动补全三大核心功能。支持跨模块引用和大文件处理，提供两套主题色（中国古典色作为默认），确保在复杂配置环境中的开发效率。

## Technical Context

**Language/Version**: TypeScript 5.x (VSCode Extension API) |
**Primary Dependencies**: ANTLR4 runtime, **仅VSCode Extension API（不使用LSP协议）**, TextMate grammars + Semantic Tokens |
**Storage**: N/A (配置文件解析，无持久化) |
**Testing**: VSCode Extension Test Runner, Jest单元测试 |
**Target Platform**: VSCode编辑器 (跨平台支持) |
**Project Type**: 单项目 (VSCode扩展) |
**Performance Goals**: <2秒响应大文件（>5k行），语法高亮<50ms |
**Constraints**: **必须使用双层语法高亮**（TextMate + Semantic），遵循ANTLR4语法，跨模块引用解析 |
**Scale/Scope**: 支持多模块多文件配置，大文件>10k行无性能问题 |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Core Principles Verification ✅ PASSED

- [x] **Grammar-First Development**: All features are based on ANTLR Cfg.g4 grammar
- [x] **Minimal Dependencies**: Every dependency justified with clear value and license review
- [x] **Direct VS Code API Integration**: Using VSCode Extension API directly (no LSP protocol) for better performance
- [x] **Two-Layer Syntax Highlighting**: TextMate grammars (basic) + Semantic Tokens (semantic) for optimal user experience
- [x] **Performance-Focused**: Large .cfg file handling with incremental parsing and caching (<2s for >5k lines, <50ms highlight)
- [x] **Robust Error Handling**: Clear error messages with real-time diagnostics + hover details + F7 problem list

### Technical Constraints ✅ PASSED

- [x] TypeScript as primary language
- [x] VSCode Extension API only (NO LSP protocol)
- [x] ANTLR4 runtime for syntax parsing
- [x] TextMate grammars for basic syntax highlighting
- [x] Semantic Tokens API for semantic highlighting
- [x] No unverified or incompatible license dependencies
- [x] 两套主题色系统 (中国古典色+默认色)
- [x] 颜色选择配置在插件设置中

### Post-Design Re-evaluation ✅ PASSED

**Phase 0 completed**: research.md generated, all technical decisions documented
**Phase 1 completed**:
  - data-model.md updated (added two-layer highlighting data model)
  - contracts/ updated (added "Why Not LSP" section, VSCode API focus)
  - quickstart.md updated (added two-layer highlighting explanation)
  - Agent context update: Skipped (non-standard workflow)

**All constitution requirements satisfied** - Ready for Phase 2 (task generation)

## Project Structure

### Documentation (this feature)

```text
specs/001-cfg-editor-support/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── theme-system-design.md # Theme system detailed design (双层高亮主题系统)
├── contracts/           # Phase 1 output (/speckit.plan command)
│   └── vscode-extension-api.md # API contracts (VSCode API, NO LSP)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# VSCode Extension项目结构
vscode-cfg-extension/
├── src/
│   ├── extension.ts                 # 扩展入口点
│   ├── grammar/
│   │   └── Cfg.g4                   # ANTLR4语法定义（手动创建）
│   │   └── *.ts                     # 自动生成：CfgLexer.ts, CfgParser.ts, CfgListener.ts, CfgBaseListener.ts
│   ├── models/                      # 数据模型
│   │   ├── configFile.ts            # 配置文件模型
│   │   ├── structDefinition.ts      # 结构定义模型
│   │   ├── tableDefinition.ts       # 表定义模型
│   │   ├── interfaceDefinition.ts   # 接口定义模型
│   │   ├── fieldDefinition.ts       # 字段定义模型
│   │   ├── foreignKeyDefinition.ts  # 外键定义模型
│   │   ├── metadataDefinition.ts    # 元数据定义模型
│   │   └── index.ts                 # 模型导出
│   ├── services/                    # 核心服务
│   │   ├── cacheService.ts          # 缓存服务
│   │   ├── fileIndexService.ts      # 文件索引服务
│   │   ├── symbolTable.ts           # 符号表
│   │   └── moduleResolver.ts        # 模块解析器
│   ├── providers/                   # VSCode API提供器
│   │   ├── completionProvider.ts    # 自动补全 (CompletionItemProvider)
│   │   ├── definitionProvider.ts    # 跳转到定义 (DefinitionProvider)
│   │   ├── hoverProvider.ts         # 悬停提示 (HoverProvider)
│   │   ├── referenceProvider.ts     # 引用查找 (ReferenceProvider)
│   │   ├── semanticTokensProvider.ts # 语义高亮 (DocumentSemanticTokensProvider)
│   │   ├── textmateGrammar.ts       # 基础高亮 (TextMate规则)
│   │   └── themeManager.ts          # 主题管理器
│   ├── services/                    # 核心服务
│   │   ├── themeService.ts          # 主题服务 (双层高亮主题应用)
│   │   ├── cacheService.ts          # 缓存服务
│   │   ├── fileIndexService.ts      # 文件索引服务
│   │   ├── symbolTable.ts           # 符号表
│   │   └── moduleResolver.ts        # 模块解析器
│   └── syntaxes/                    # TextMate语法文件
│       ├── cfg.tmLanguage.json      # TextMate语法定义（基础高亮）
│       └── cfg-language-configuration.json # 语言配置（主题设置）
│   └── utils/                       # 工具函数
│       ├── logger.ts                # 日志工具
│       ├── performance.ts           # 性能监控
│       └── namespaceUtils.ts        # 命名空间工具
├── package.json                     # 扩展配置
├── tsconfig.json                    # TypeScript配置
├── .vscode-test/                    # VSCode测试配置
└── test/                            # 测试文件
    ├── fixtures/                    # 测试用例
    ├── unit/                        # 单元测试
    └── integration/                 # 集成测试
```

**ANTLR4文件生成说明**:
- `CfgLexer.ts`, `CfgParser.ts`, `CfgListener.ts`, `CfgBaseListener.ts` 由ANTLR4工具从`Cfg.g4`自动生成
- 通过`npm run generate-parser`命令生成（见package.json scripts）
- 生成的TypeScript文件使用antlr4ts运行时库
- 无需手动创建或修改这些自动生成的文件

**Structure Decision**: 选择单项目结构，所有源代码集中在vscode-cfg-extension目录。模块化设计将语法解析、数据模型、核心服务和VSCode API提供器分离，确保代码清晰和维护性。

**双层语法高亮架构**:
1. **TextMate grammars (基础高亮)** - 毫秒级响应关键字、字符串、数字等
2. **Semantic Tokens (语义高亮)** - 基于ANTLR4精确高亮struct/interface/table、外键、主键等

**主题系统与双层高亮的关联**:
```
Theme System
├── TextMate Layer (Layer 1)
│   ├── scopeName映射 (keyword.control.cfg, string.quoted.double.cfg等)
│   ├── tokenColorCustomizations应用主题色
│   └── 毫秒级响应，零延迟
│
├── Semantic Layer (Layer 2)
│   ├── SemanticTokensLegend (tokenType索引)
│   ├── 主题色直接应用到SemanticTokensBuilder
│   └── 基于ANTLR4解析树的精确高亮
│
└── ThemeManager
    ├── 监听VSCode配置变化 (cfg.theme)
    ├── 动态应用主题到两层高亮
    └── 强制刷新所有打开的.cfg文件
```

**关键技术组件**:
- **themeService.ts**: 主题配置管理，两层高亮颜色映射
- **themeManager.ts**: 主题切换监听，动态应用机制
- **cfg.tmLanguage.json**: TextMate语法文件，定义基础token的scope
- **cfg-language-configuration.json**: VSCode语言配置，主题选择
- **package.json contributes.configuration**: 主题配置选项

**不使用LSP协议**，直接使用VSCode Extension API获得更好性能（2-5倍速度提升）和更简单的架构。遵循VSCode扩展标准目录结构，集成ANTLR4运行时。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 主题色系统 (两套颜色) | 用户个性化需求，中国古典色符合本地化需求 | 单色主题无法满足视觉偏好差异 |
| 符号表缓存 | 大文件性能优化，避免重复解析 | 每次都重新解析性能差，影响用户体验 |
| 跨模块解析 | 游戏配置多模块组织需求 | 单模块限制实际使用场景 |
| **不使用LSP协议** | 单进程架构性能更好（2-5倍速度），架构更简单 | LSP协议增加IPC开销、调试复杂、内存占用高 |
| **双层语法高亮** | TextMate提供即时反馈，Semantic提供精确语义高亮 | 单层高亮无法兼顾性能和准确性 |
| **复杂Theme系统** | 两层高亮需要独立且同步的主题应用机制 | 简化方案无法同时支持TextMate和Semantic的主题切换 |

