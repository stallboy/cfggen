<!--
Sync Impact Report:
- Version change: 1.0.0 → 2.0.0 (MAJOR - 项目方向重大变更)
- Modified principles: 全部重写
  - I. 用户体验优先 → 保持但重新解释
  - II. 性能响应性 → 新增
  - III. 语法高亮准确性 → 新增
  - IV. 智能提示实用性 → 新增
  - V. 最小依赖原则 → 保持但重新解释
- Added sections: VSCode插件特定约束
- Removed sections: 多语言一致性、类型安全优先（不适用于插件）
- Templates requiring updates:
  ✅ .specify/templates/plan-template.md (宪法检查部分)
  ✅ .specify/templates/spec-template.md (范围对齐)
  ✅ .specify/templates/tasks-template.md (任务分类)
- Follow-up TODOs: 无
-->

# cfg-vscode-extension Constitution

## Core Principles

### I. 用户体验优先
所有功能设计必须以提升用户编辑.cfg文件的体验为核心。语法高亮和自动提示必须直观、准确、及时。

**理性**：VSCode插件的价值在于提升开发效率，用户体验是衡量成功的关键指标。

### II. 性能响应性
语法高亮和自动提示必须在毫秒级响应，不能影响用户的编辑流畅度。插件启动和加载时间必须最小化。

**理性**：编辑器插件的性能直接影响用户的工作效率，延迟会破坏用户体验。

### III. 语法高亮准确性
.cfg文件的语法高亮必须准确反映文件结构和语义。支持嵌套结构、注释、字符串和关键字的正确着色。

**理性**：准确的语法高亮帮助用户快速理解配置文件结构，减少错误。

### IV. 智能提示实用性
自动提示必须基于上下文提供有用的建议。包括关键字补全、值建议、结构导航等功能。

**理性**：智能提示是VSCode插件的核心价值，必须实用且准确。

### V. 最小依赖原则
外部依赖必须严格审查，优先使用VSCode API和TypeScript标准库。每个新依赖必须证明其价值超过引入的复杂性。

**理性**：插件依赖过多会影响启动性能和稳定性，保持轻量级是重要目标。

## VSCode插件特定约束

### 技术栈
- **主要语言**: TypeScript
- **构建工具**: npm/yarn + VSCode Extension API
- **测试框架**: Mocha + VSCode Test Runner
- **包管理**: package.json + VSCode Marketplace发布

### 架构约束
- 遵循VSCode Extension API规范
- 模块化设计，清晰的职责分离
- 支持.cfg文件格式的完整解析
- 异步处理语法高亮和提示
- 内存使用优化

### 性能目标
- 语法高亮响应时间 < 50ms
- 自动提示响应时间 < 100ms
- 插件启动时间 < 1秒
- 内存占用 < 50MB

### 兼容性要求
- 支持VSCode 1.60.0+
- 跨平台兼容（Windows、macOS、Linux）
- 支持.cfg文件的标准格式

## 开发工作流

### 代码审查
- 所有更改必须经过代码审查
- 审查重点：符合宪法原则、性能影响、用户体验
- 复杂变更需要额外设计文档

### 质量门禁
- 所有测试必须通过
- 性能指标必须达标
- 无新的编译警告
- 符合VSCode扩展开发规范

### 发布流程
- 版本号遵循语义化版本
- 发布前必须进行完整测试
- 更新CHANGELOG.md
- 提交到VSCode Marketplace

## Governance

本宪法优先于所有其他实践和约定。任何修改都需要：
- 文档记录修改原因和影响
- 团队讨论和批准
- 必要的迁移计划
- 版本号按照语义化版本规则更新

所有PR和代码审查必须验证符合宪法要求。复杂性必须有充分理由，不能无故引入。

**Version**: 2.0.0 | **Ratified**: 2025-11-06 | **Last Amended**: 2025-11-06
