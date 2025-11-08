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

### III. Language Server Protocol Compliance
必须遵循 LSP 规范实现语言功能；提供标准的代码补全、定义跳转、悬停提示等功能；确保与 VSCode 的深度集成，提供流畅的用户体验。

## Technology Stack

扩展开发必须遵循以下技术约束：使用 TypeScript 作为主要开发语言；严格依赖 VSCode Extension API 和 LSP 标准库；ANTLR4 及其相关运行时库用于语法解析；不允许引入未经验证或许可证不兼容的第三方依赖。

## Development Workflow

所有代码变更必须遵循以下流程：特性开发前必须更新相关语法定义；每个功能都需要单元测试覆盖。

**Version**: 1.0.0 | **Ratified**: 2025-11-08 | **Last Amended**: 2025-11-08
