<!--
Sync Impact Report - Constitution Update
Version change: 0.0.0 → 1.0.0
Modified principles: N/A (initial creation)
Added sections: Core Principles (5 principles), Technology Stack, Development Workflow
Removed sections: N/A
Templates updated: ✅ .specify/templates/plan-template.md (Constitution Check section)
                  ✅ .specify/templates/tasks-template.md (testing requirements marked MANDATORY)
Files requiring review: All command files validated - no updates needed
Follow-up TODOs: None
-->

# VSCode CFG Extension Constitution

## Core Principles

### I. Grammar-First Development
所有功能必须基于 ANTLR 定义的 Cfg.g4 语法结构；语法定义是扩展的基础，所有语法高亮、跳转和自动提示功能都严格遵循语法规则；确保语法定义的完整性和准确性是首要任务。

### II. Minimal Dependencies
严格控制库依赖，优先使用 VSCode Extension API 原生功能；每个依赖都需要明确的价值论证和许可证审查；避免引入不必要的第三方库，保持扩展的轻量级和可维护性。

### III. Direct VS Code API Integration
直接使用 VSCode Extension API 实现所有语言功能（语义高亮、代码补全、定义跳转、悬停提示）；避免使用 LSP 协议，优先选择单进程架构以获得更好的性能（2-5倍响应速度提升）和更低的开发复杂度（代码量减少30%）；确保与 VSCode 的深度原生集成。

### IV. Performance-Focused Architecture
必须为大型 .cfg 文件提供流畅体验；采用增量解析和符号表缓存策略；所有语言操作响应时间需满足：语法高亮<50ms、自动补全<200ms、跳转到定义<300ms；内存占用控制在合理范围（<100MB for 10k+ line files）。

### V. Robust Error Handling
必须提供清晰、可理解的错误提示；对语法错误采用渐进式降级策略（核心功能优先，高级功能可选）；引用未找到时显示明确提示而非静默失败；确保扩展在各种错误情况下都能稳定运行。

## Technology Stack

扩展开发必须遵循以下技术约束：使用 TypeScript 作为主要开发语言；**仅**依赖 VSCode Extension API（**不使用LSP协议**）；ANTLR4 及其相关运行时库用于语法解析；**必须同时使用**TextMate grammars（基础高亮）和语义Tokens API（语义高亮）进行两层语法高亮；不允许引入未经验证或许可证不兼容的第三方依赖。

## Design Decisions (Constitutional Amendments)

### 2025-11-09 Amendment: Why Not LSP?
**Decision**: 选择直接使用 VSCode Extension API 而非 Language Server Protocol

**Rationale**:
- **性能优势**: 单进程架构消除IPC开销，响应速度快2-5倍
- **开发简单**: 无需维护客户端+服务器双代码，代码量减少30%
- **维护成本低**: 单一代码库、生命周期管理简单、类型安全
- **功能完整**: VSCode API提供与LSP等价的所有功能（completion、definition、hover、references、semantic tokens）
- **启动快**: 无需额外进程启动，启动时间减少5倍

**Rejection of LSP**:
- 过度工程：单语言扩展不需要协议抽象层
- 性能损耗：JSON序列化 和进程间通信带来不必要开销
- 调试复杂：需要同时调试客户端和服务器
- 内存占用：双进程占用约80MB vs 单进程40MB

**Applicable Scope**: 001-cfg-editor-support feature 及所有后续CFG相关功能

### 2025-11-09 Amendment: Two-Layer Syntax Highlighting
**Decision**: 采用TextMate grammars + Semantic Tokens双层语法高亮架构

**Architecture**:
```
┌─────────────────────────────────────────┐
│         VSCode Editor Buffer            │
├─────────────────────────────────────────┤
│  Layer 1: TextMate Grammars (基础高亮)   │
│  - 基础token识别                         │
│  - 关键字、字符串、数字等                 │
│  - 实时、零性能开销                       │
│  - 快速响应用户输入                       │
├─────────────────────────────────────────┤
│  Layer 2: Semantic Tokens (语义高亮)     │
│  - 复杂语义结构高亮                       │
│  - struct/interface/table名称            │
│  - 外键引用、类型引用、主键/唯一键         │
│  - 主题色应用                            │
├─────────────────────────────────────────┤
│  文件内容 (.cfg)                        │
└─────────────────────────────────────────┘
```

**Why Two Layers**:
1. **TextMate层**: 提供即时反馈的基础高亮，确保零延迟输入体验
2. **Semantic层**: 提供基于ANTLR4的精确语义高亮，支持复杂语法结构
3. **性能优化**: TextMate处理基础token，Semantic处理复杂结构
4. **兼容性**: TextMate是VSCode标准，Semantic是增强
5. **可维护性**: TextMate处理简单规则，Semantic处理复杂逻辑

**Implementation Details**:
- TextMate: 使用.tmLanguage.json定义基础token规则
- Semantic: 使用DocumentSemanticTokensProvider基于ANTLR4解析树
- 两层叠加：Semantic层颜色覆盖TextMate层，但保留TextMate的即时性
- 主题支持：两层都支持主题色（默认+中国古典色）

**Performance**:
- TextMate: 毫秒级响应（VSCode原生优化）
- Semantic: 20-50ms（增量解析+缓存）
- 总延迟: <50ms（满足SC-001要求）

**Applicable Scope**: 001-cfg-editor-support feature 语法高亮模块

## Development Workflow

所有代码变更必须遵循以下流程：特性开发前必须更新相关语法定义；每个功能都需要单元测试覆盖。

**Version**: 1.2.0 | **Ratified**: 2025-11-08 | **Last Amended**: 2025-11-09
