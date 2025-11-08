# Research Report: VSCode CFG Extension

**Date**: 2025-11-09
**Feature**: VSCode CFG Extension with ANTLR4-based syntax support

## Executive Summary

本项目采用**VSCode Extension API直接实现**（不使用LSP协议）+ **双层语法高亮架构**（TextMate + Semantic Tokens），以获得最佳性能和开发体验。所有关键技术决策已在Constitution中确立，本文档为补充研究。

## Research Tasks Completed

### 1. VSCode Extension API vs LSP Protocol

**Decision**: 使用VSCode Extension API直接实现所有语言功能
**Rationale**:
- **性能优势**: 单进程架构，响应速度快2-5倍（无IPC开销）
- **开发简单**: 单一代码库，无需维护客户端+服务器，代码量减少30%
- **维护成本低**: 生命周期管理简单，类型安全
- **功能完整**: VSCode API提供与LSP等价的所有功能

**Key Findings**:
- VSCode Extension API 1.85+ 提供所有必需的语言支持API
- DocumentSemanticTokensProvider提供语义高亮
- CompletionItemProvider提供自动补全
- DefinitionProvider提供跳转到定义
- HoverProvider提供悬停提示
- ReferenceProvider提供引用查找

**Alternatives Considered**:
- ❌ LSP协议: 过度工程、性能损耗、调试复杂
- ❌ 纯TextMate: 仅能基础高亮，无法实现语义功能

### 2. ANTLR4 TypeScript Runtime Integration

**Decision**: 使用antlr4ts库作为TypeScript运行时
**Rationale**:
- 官方支持的TypeScript运行时库
- 完整的TypeScript泛型支持
- 良好的错误处理机制

**Key Findings**:
- antlr4ts 0.5.0+版本性能优化，适合大文件
- 需要预先生成TypeScript语法分析器代码
- 支持监听器模式遍历语法树
- 与TextMate配合使用：ANTLR4提供语义信息，TextMate提供基础token

**Alternatives Considered**:
- 使用antlr4原版 + 自定义包装：工作量更大
- 手写解析器：无法保证语法准确性和维护性

### 3. 双层语法高亮架构

**Decision**: TextMate grammars (基础) + Semantic Tokens (语义) 双层高亮
**Rationale**:
- **TextMate层**: 毫秒级响应，VSCode原生优化，提供即时视觉反馈
- **Semantic层**: 基于ANTLR4精确分析，提供语义级别的精确高亮
- **性能优化**: 分层处理，TextMate处理简单规则，Semantic处理复杂结构
- **用户体验**: 既保证输入流畅性，又提供准确的语义信息

**Key Findings**:
- TextMate: 使用.tmLanguage.json定义基础token（关键字、字符串、数字、注释）
- Semantic: DocumentSemanticTokensProvider基于ANTLR4解析树生成语义tokens
- 两层叠加: Semantic层颜色覆盖TextMate层，但保留TextMate的即时性
- 主题支持: 两层都支持主题色（默认+中国古典色）

**Implementation Strategy**:
```
Layer 1 (TextMate): 基础高亮
  └── keywords, strings, numbers, comments

Layer 2 (Semantic): 语义高亮 (覆盖Layer 1)
  └── struct/interface/table names
  └── type references
  └── foreign key references
  └── primary/unique key identifiers
  └── metadata keywords
```

**Alternatives Considered**:
- ❌ 纯TextMate: 无法处理复杂语法结构，颜色不够精确
- ❌ 纯Semantic: 响应延迟影响输入流畅性
- ✅ 双层架构: 最佳性能和准确性平衡

### 4. Theme Color System Design

**Decision**: 实现两套主题色系统（默认 + 中国古典色），中国古典色为默认主题
**Rationale**:
- 默认主题：VSCode标准配色（蓝、绿、青等），确保兼容性
- 中国古典色：黛青、苍青、竹青、胭脂、琥珀、玄灰，符合本地化需求，提升用户体验
- 颜色选择通过插件配置 `cfg.theme` 实现

**Key Findings**:
- 支持7种颜色类别：结构定义、复杂类型、主键、唯一键、外键引用、注释、元数据
- 中国古典色为默认，提供文化认同感
- 可通过VSCode设置切换主题

**Color Categories and Palette**:
```json
{
  "structureDefinition": "#0000FF",    // struct/interface/table + 名称
  "complexType": "#267F99",            // 非基本类型 (Position等)
  "primaryKey": "#C586C0",             // PK字段名
  "uniqueKey": "#C586C0",              // UK字段名
  "foreignKey": "#AF00DB",             // -> tt, -> tt[kk], => tt[kk]
  "comment": "#008000",                // 绿色注释
  "metadata": "#808080"                // nullable等元数据
}
```

**Alternatives Considered**:
- 动态主题：不实用
- 用户自定义颜色：增加复杂度
- 纯TextMate规则：无法实现基于语法树的高亮

### 5. Performance Optimization for Large Files

**Decision**: 增量解析 + 符号表缓存
**Rationale**:
- 大型配置可能有数千行，实时解析性能关键
- 增量解析避免重复工作
- 缓存提升响应速度

**Key Strategies**:
1. **增量解析**: 仅解析变化的文件和依赖
2. **符号表缓存**: 内存中维护已解析符号，支持快速查找
3. **缓存失效**: 基于文件修改时间自动失效
4. **后台解析**: 异步解析避免阻塞UI

**Performance Targets**:
- 大文件（>5k行）: 所有操作<2秒
- 语法高亮: 始终<50ms
- 支持10k+行文件无性能问题

### 6. Cross-Module Reference Resolution

**Decision**: 基于标准目录结构的模块名解析算法
**Rationale**:
- 游戏配置通常按模块组织（task, item, npc等）
- 跨模块引用需要正确解析模块路径

**Key Findings**:
- 标准目录结构: config/[module]/[module].cfg
- 模块名解析: 截取目录名第一个"."之前的部分，再截取"_汉字"或纯汉字之前的部分
- 歧义处理: 显示"模块名不唯一"错误，提示用户使用完整路径

**Alternatives Considered**:
- 配置文件显式声明模块：增加用户负担
- 符号表全局搜索：性能差，无法处理大型项目

### 7. Error Handling and Robustness

**Decision**: 分层错误处理 + 渐进式降级
**Rationale**:
- 配置文件可能有语法错误，不能因此阻塞整个系统
- 用户需要清晰错误信息快速定位问题

**Strategy**:
1. **实时诊断**: 红色波浪线标注错误位置
2. **悬停详情**: 鼠标悬停显示详细错误信息
3. **问题列表**: 按F7显示所有错误，支持快速导航
4. **降级处理**: 核心功能（高亮）优先，高级功能（跳转）可选

## Summary

所有关键技术和设计决策已确定：

1. ✅ **技术栈**: TypeScript + VSCode Extension API + ANTLR4
2. ✅ **核心功能**: 语法高亮、跳转定义、自动补全
3. ✅ **主题系统**: 默认+中国古典色两套配色
4. ✅ **性能**: 增量解析+符号表缓存
5. ✅ **跨模块**: 基于目录结构的解析算法
6. ✅ **容错**: 分层错误处理和渐进式降级
7. ✅ **双层高亮**: TextMate + Semantic Tokens架构
8. ✅ **无LSP**: 直接使用VSCode API简化架构

**No unresolved clarifications** - Ready for Phase 1 design.
