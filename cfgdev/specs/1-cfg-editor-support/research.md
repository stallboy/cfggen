# Research: CFG文件编辑器支持

**Feature**: CFG文件编辑器支持
**Date**: 2025-11-06

## 技术选择研究

### 1. VSCode Extension API模式选择

**Decision**: 采用Language Server Protocol (LSP) + 直接扩展API的混合模式

**Rationale**:
- LSP提供强大的语言特性支持（跳转、补全、诊断）
- 直接扩展API用于轻量级功能（语法高亮、基本提示）
- 混合模式平衡性能和功能完整性
- 支持复杂的struct/interface/table语法结构

**Alternatives considered**:
- 纯LSP模式：功能完整但启动较重
- 纯扩展API模式：轻量但功能有限

### 2. ANTLR4集成策略

**Decision**: 使用ANTLR4 TypeScript运行时，预先生成词法/语法分析器

**Rationale**:
- ANTLR4提供强大的语法解析能力，支持复杂的语法结构
- TypeScript运行时确保与VSCode插件的兼容性
- 预生成分析器避免运行时编译开销
- 支持struct、interface、table三种主要结构
- 支持外键引用（->, =>）和复杂类型系统

**Alternatives considered**:
- 手写解析器：维护成本高，难以处理复杂语法
- 其他解析器库：功能不如ANTLR4完整

### 3. 性能优化策略

**Decision**: 采用增量解析 + 内存缓存 + 延迟加载

**Rationale**:
- 增量解析减少重复工作
- 内存缓存提升响应速度
- 延迟加载避免启动时处理所有文件

**Alternatives considered**:
- 全量解析：启动慢，内存占用高
- 无缓存：每次都需要重新解析

### 4. 跨文件引用解析

**Decision**: 构建全局索引 + 文件监听机制

**Rationale**:
- 全局索引支持跨文件跳转
- 文件监听确保索引实时更新
- 支持大型项目配置管理
- 支持复杂的外键引用网络（->, =>）
- 支持接口多态和enumRef机制

**Alternatives considered**:
- 按需解析：跳转时临时解析，响应慢
- 无索引：无法支持跨文件功能

### 5. CFG语法特性支持

**Decision**: 完整支持struct/interface/table三种结构

**Rationale**:
- struct：支持基本类型、嵌套结构、容器类型（list, map）
- interface：支持多态、enumRef、defaultImpl机制
- table：支持主键、唯一键、enum、entry属性
- 外键引用：支持->（单引用）和=>（列表引用）
- 命名空间：支持点分隔的命名空间引用

**关键特性**:
- 接口多态：interface内定义struct实现类
- enumRef机制：接口与枚举表的关联
- 复杂外键：支持list/map容器内的外键引用
- 递归结构：支持接口内的递归引用

## 架构模式研究

### 1. 插件架构模式

**Decision**: 分层架构 + 事件驱动

**Rationale**:
- 分层架构确保职责分离
- 事件驱动支持异步处理
- 符合VSCode扩展开发最佳实践

**Components**:
- 解析层：ANTLR4语法解析
- 模型层：配置数据模型
- 服务层：索引、验证、缓存
- 提供者层：VSCode API集成

### 2. 错误处理策略

**Decision**: 分级错误处理 + 用户友好提示

**Rationale**:
- 语法错误：实时提示，阻止保存
- 语义错误：警告提示，允许保存
- 系统错误：优雅降级，不影响基本功能

### 3. 配置语义验证

**Decision**: 基于文档的语义规则 + 运行时验证

**Rationale**:
- 语义规则来自项目文档
- 运行时验证确保配置有效性
- 支持自定义验证规则

## 性能目标验证

### 1. 语法高亮性能

**Target**: < 50ms
**Strategy**:
- 增量词法分析
- 语法树缓存
- 异步处理

### 2. 自动提示性能

**Target**: < 100ms
**Strategy**:
- 预计算上下文
- 缓存常用补全
- 限制建议数量

### 3. 内存使用

**Target**: < 50MB
**Strategy**:
- 语法树压缩
- 索引优化
- 定期清理

## 兼容性考虑

### 1. VSCode版本兼容

**Decision**: 支持VSCode 1.60.0+

**Rationale**:
- 覆盖大多数用户版本
- 使用稳定的API特性
- 向后兼容设计

### 2. 平台兼容性

**Decision**: 支持Windows、macOS、Linux

**Strategy**:
- 使用跨平台Node.js API
- 避免平台特定代码
- 跨平台测试

## 总结

所有技术选择都基于VSCode插件开发最佳实践，平衡功能完整性、性能和用户体验。架构设计支持未来的功能扩展，同时确保当前需求的满足。