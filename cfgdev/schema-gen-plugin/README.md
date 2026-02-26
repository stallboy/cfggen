# cfggen-schema-plugin

根据自然语言描述或设计文档生成 cfggen 配表 schema 定义文件的 Claude Code 插件。

## 安装

将整个 `schema-gen-plugin` 目录复制到 Claude Code 的插件目录：

```bash
# 项目级安装
cp -r schema-gen-plugin .claude/plugins/

# 或全局安装
cp -r schema-gen-plugin ~/.claude/plugins/
```

## 使用方法

### 基本用法

```bash
/gen-schema 创建一个物品表，包含id、名称、类型、价格、描述
```

### 示例 1：简单数据表

```bash
/gen-schema 创建一个物品表，包含id、名称、类型、价格、描述
```

生成 `item.cfg`:

```cfg
// 物品配置表
table item[id] {
    id:int;           // 物品ID
    name:str;         // 物品名称
    type:str;         // 物品类型
    price:int;        // 物品价格
    description:text; // 物品描述
}
```

### 示例 2：复杂系统

```bash
/gen-schema 创建一个任务系统：
1. 任务表包含任务id、名称、描述、完成条件（接口）、奖励列表
2. 完成条件接口支持：击杀怪物、收集物品、与NPC对话
3. 奖励包含经验和物品列表
```

### 示例 3：带关联的配置

```bash
/gen-schema 创建商店系统，包含商店表和商品表，商品引用物品配置
```

## 功能特性

| 特性 | 说明 |
|------|------|
| 🎯 **自然语言输入** | 用中文描述配置需求，自动生成 CFG schema |
| 🏗️ **完整语法支持** | 支持 struct、interface、table、外键、元数据等 |
| 🔗 **智能关联** | 自动识别并生成外键关联关系（`->` 单向、`=>` 多向） |
| 📝 **代码注释** | 自动添加清晰的中文注释 |
| ✨ **多态支持** | 智能识别需要 interface 的多态场景 |
| 📚 **DDD 方法论** | 运用领域驱动设计思想进行建模 |

## 组件说明

### 命令

- **`/gen-schema`** - 主命令，根据自然语言描述生成 schema

### Skills

- **`cfg-grammar`** - CFG 语法规范参考
  - `references/field-types.md` - 完整类型系统
  - `references/metadata.md` - 元数据属性详细说明
  - `references/patterns.md` - 设计模式和最佳实践

## CFG 语法规范

生成的 schema 遵循 cfggen 规范：

- **[结构定义](../../docs/docs/cfggen/03.schema.md)** - struct、interface、table 基础语法
- **[元数据配置](../../docs/docs/cfggen/07.othermetadatas.md)** - @column 等元数据用法

## 设计方法论

本插件采用领域驱动设计（DDD）思想：

1. **实体 (Entities) → `table`**：具有唯一标识的业务对象
2. **多态点 (Polymorphism) → `interface`**：多种实现方式的场景
3. **值对象 (Value Objects) → `struct`**：内聚的字段集合

## 约束说明

为简化学习曲线，本插件：
- **不包含**唯一键定义
- **不包含**复杂主键

如需这些高级特性，请参考 cfggen 完整文档。

## 许可证

MIT
