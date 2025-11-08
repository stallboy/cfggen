# Implementation Plan: VSCode CFG Extension

**Branch**: `001-cfg-editor-support` | **Date**: 2025-11-08 | **Spec**: [link](spec.md)
**Input**: Feature specification from `/specs/001-cfg-editor-support/spec.md`

**Note**: This template is filled in by the `/speckit.plan` command. See `.specify/templates/commands/plan.md` for the execution workflow.

## Summary

开发一个VSCode扩展，为.cfg配置文件提供完整的语言支持功能。基于ANTLR4定义的Cfg.g4语法，实现语法高亮、跳转定义、自动补全三大核心功能。支持跨模块引用和大文件处理，提供两套主题色（中国古典色作为默认），确保在复杂配置环境中的开发效率。

## Technical Context

**Language/Version**: TypeScript 5.x (VSCode Extension API) |
**Primary Dependencies**: ANTLR4 runtime, VSCode Extension API, LSP标准库 |
**Storage**: N/A (配置文件解析，无持久化) |
**Testing**: VSCode Extension Test Runner, Jest单元测试 |
**Target Platform**: VSCode编辑器 (跨平台支持) |
**Project Type**: 单项目 (VSCode扩展) |
**Performance Goals**: <2秒响应跳转和补全 |
**Constraints**: 遵循LSP协议，遵循ANTLR4语法，跨模块引用解析 |
**Scale/Scope**: 支持多模块多文件配置 |

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Core Principles Verification

- [x] **Grammar-First Development**: All features are based on ANTLR Cfg.g4 grammar
- [x] **Minimal Dependencies**: Every dependency justified with clear value and license review
- [x] **LSP Compliance**: Language features follow LSP specification (completion, go-to-definition, hover)
- [x] **Performance-Focused**: Large .cfg file handling with incremental parsing and caching
- [x] **Robust Error Handling**: Clear error messages for syntax errors and edge cases

### Technical Constraints

- [x] TypeScript as primary language
- [x] VSCode Extension API and LSP standard libraries
- [x] ANTLR4 runtime for syntax parsing
- [x] No unverified or incompatible license dependencies
- [x] 两套主题色系统 (中国古典色+默认色)
- [x] 颜色选择配置在插件设置中

## Project Structure

### Documentation (this feature)

```text
specs/001-cfg-editor-support/
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (/speckit.plan command)
├── data-model.md        # Phase 1 output (/speckit.plan command)
├── quickstart.md        # Phase 1 output (/speckit.plan command)
├── contracts/           # Phase 1 output (/speckit.plan command)
└── tasks.md             # Phase 2 output (/speckit.tasks command - NOT created by /speckit.plan)
```

### Source Code (repository root)

```text
# VSCode Extension项目结构
vscode-cfg-extension/
├── src/
│   ├── extension.ts                 # 扩展入口点
│   ├── grammar/
│   │   ├── Cfg.g4                   # ANTLR4语法定义
│   │   ├── CfgLexer.ts             # ANTLR4词法分析器
│   │   ├── CfgParser.ts            # ANTLR4语法分析器
│   │   ├── CfgListener.ts          # ANTLR4语法监听器
│   │   └── CfgBaseListener.ts      # 自定义监听器（用于语法高亮）
│   ├── models/                     # 数据模型
│   │   ├── configFile.ts           # 配置文件模型
│   │   ├── structDefinition.ts     # 结构定义模型
│   │   ├── tableDefinition.ts      # 表定义模型
│   │   ├── interfaceDefinition.ts  # 接口定义模型
│   │   ├── fieldDefinition.ts      # 字段定义模型
│   │   ├── foreignKeyDefinition.ts # 外键定义模型
│   │   ├── metadataDefinition.ts   # 元数据定义模型
│   │   └── index.ts                # 模型导出
│   ├── services/                   # 核心服务
│   │   ├── cacheService.ts         # 缓存服务
│   │   ├── fileIndexService.ts     # 文件索引服务
│   │   ├── symbolTable.ts          # 符号表
│   │   └── moduleResolver.ts       # 模块解析器
│   ├── providers/                  # LSP提供器
│   │   ├── completionProvider.ts   # 自动补全
│   │   ├── definitionProvider.ts   # 跳转定义
│   │   ├── hoverProvider.ts        # 悬停提示
│   │   ├── foreignKeyProvider.ts   # 外键导航
│   │   └── syntaxHighlightingProvider.ts # ANTLR语法高亮
│   └── utils/                      # 工具函数
│       ├── logger.ts               # 日志工具
│       ├── performance.ts          # 性能监控
│       └── namespaceUtils.ts       # 命名空间工具
│       └── tokenHighlighter.ts     # ANTLR token高亮器
├── package.json                    # 扩展配置
├── tsconfig.json                   # TypeScript配置
├── .vscode-test/                   # VSCode测试配置
└── test/                           # 测试文件
    ├── fixtures/                   # 测试用例
    ├── unit/                       # 单元测试
    └── integration/                # 集成测试
```

**Structure Decision**: 选择单项目结构，所有源代码集中在vscode-cfg-extension目录。模块化设计将语法解析、数据模型、核心服务和LSP提供器分离，确保代码清晰和维护性。**统一使用ANTLR4进行语法解析和语法高亮**，无需TextMate语法文件，确保语法规则的一致性。遵循VSCode扩展标准目录结构，集成ANTLR4运行时和LSP协议实现。

## Complexity Tracking

> **Fill ONLY if Constitution Check has violations that must be justified**

| Violation | Why Needed | Simpler Alternative Rejected Because |
|-----------|------------|-------------------------------------|
| 主题色系统 (两套颜色) | 用户个性化需求，中国古典色符合本地化需求 | 单色主题无法满足视觉偏好差异 |
| 符号表缓存 | 大文件性能优化，避免重复解析 | 每次都重新解析性能差，影响用户体验 |
| 跨模块解析 | 游戏配置多模块组织需求 | 单模块限制实际使用场景 |

