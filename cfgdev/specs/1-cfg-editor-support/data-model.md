# Data Model: CFG文件编辑器支持

**Feature**: CFG文件编辑器支持
**Date**: 2025-11-06

## 核心实体

### 1. ConfigFile (配置文件)

表示一个.cfg配置文件实例。

**Attributes**:
- `uri`: string - 文件URI
- `content`: string - 文件内容
- `ast`: ASTNode - 抽象语法树
- `lastModified`: number - 最后修改时间
- `size`: number - 文件大小

**Relationships**:
- 包含多个 `SchemaElement` (struct/interface/table)
- 被多个 `TypeDefinition` 引用
- 被多个 `ForeignKeyReference` 引用

### 2. SchemaElement (模式元素)

配置文件的模式元素基类。

**Attributes**:
- `name`: string - 元素名称
- `namespace`: string | null - 命名空间
- `position`: Position - 在文件中的位置
- `metadata`: Metadata[] - 元数据属性

**Subtypes**:
- `StructDefinition` - 结构体定义
- `InterfaceDefinition` - 接口定义
- `TableDefinition` - 表定义

### 3. StructDefinition (结构体定义)

定义结构体类型。

**Attributes**:
- `name`: string - 结构体名称
- `fields`: FieldDefinition[] - 字段定义列表
- `foreignKeys`: ForeignKeyDefinition[] - 外键定义列表

**Field Types**:
- 基本类型：bool, int, long, float, str, text
- 容器类型：list<T>, map<K,V>
- 结构类型：struct, interface

### 4. InterfaceDefinition (接口定义)

定义接口类型，支持多态。

**Attributes**:
- `name`: string - 接口名称
- `implementations`: StructDefinition[] - 实现结构体列表
- `enumRef`: string | null - 枚举表引用
- `defaultImpl`: string | null - 默认实现
- `isPack`: boolean - 是否为pack模式

**Special Features**:
- enumRef：关联枚举表，支持Excel配置
- defaultImpl：指定默认实现类
- pack：支持简化配置模式

### 5. TableDefinition (表定义)

定义数据表结构。

**Attributes**:
- `name`: string - 表名称
- `primaryKey`: string[] - 主键字段列表
- `uniqueKeys`: UniqueKeyDefinition[] - 唯一键定义
- `fields`: FieldDefinition[] - 字段定义列表
- `foreignKeys`: ForeignKeyDefinition[] - 外键定义列表
- `enumField`: string | null - 枚举字段
- `entryField`: string | null - 入口字段

**Table Properties**:
- enum：指定枚举字段
- entry：指定入口字段
- json：JSON格式支持
- description：描述信息

### 6. FieldDefinition (字段定义)

定义结构体或表中的字段。

**Attributes**:
- `name`: string - 字段名称
- `type`: FieldType - 字段类型
- `foreignKey`: ForeignKeyDefinition | null - 外键定义
- `metadata`: Metadata[] - 元数据属性
- `comment`: string | null - 注释

**Field Types**:
- 基本类型：bool, int, long, float, str, text
- 容器类型：list<T>, map<K,V>
- 结构类型：struct, interface

### 7. ForeignKeyDefinition (外键定义)

定义字段的外键引用。

**Attributes**:
- `referenceType`: 'single' | 'list' - 引用类型（-> 或 =>）
- `targetTable`: string - 目标表名称
- `targetKey`: string[] - 目标键字段
- `isNullable`: boolean - 是否可为空

**Reference Types**:
- `->`：单引用，指向表的主键或唯一键
- `=>`：列表引用，指向表的任意字段，返回列表

### 8. UniqueKeyDefinition (唯一键定义)

定义表的唯一键。

**Attributes**:
- `fields`: string[] - 唯一键字段列表
- `name`: string | null - 唯一键名称

### 5. ProjectIndex (项目索引)

全局项目索引，支持跨文件功能。

**Attributes**:
- `typeRegistry`: Map<string, TypeDefinition> - 类型注册表
- `fileRegistry`: Map<string, ConfigFile> - 文件注册表
- `referenceRegistry`: Map<string, ForeignKeyReference[]> - 引用注册表
- `lastUpdated`: number - 最后更新时间

## 数据关系

### 文件-元素关系
```
ConfigFile 1:n SchemaElement
SchemaElement 1:1 StructDefinition (当元素是结构体时)
SchemaElement 1:1 InterfaceDefinition (当元素是接口时)
SchemaElement 1:1 TableDefinition (当元素是表时)
```

### 接口-实现关系
```
InterfaceDefinition 1:n StructDefinition (实现类)
InterfaceDefinition 0:1 TableDefinition (enumRef表)
```

### 字段-外键关系
```
FieldDefinition 0:1 ForeignKeyDefinition
ForeignKeyDefinition 1:1 TableDefinition (目标表)
```

### 表-键关系
```
TableDefinition 1:n UniqueKeyDefinition (唯一键)
TableDefinition 1:1 FieldDefinition (主键字段)
```

## 状态管理

### 1. 文件状态
- `PARSING` - 正在解析
- `PARSED` - 解析完成
- `ERROR` - 解析错误
- `MODIFIED` - 已修改

### 2. 索引状态
- `INITIALIZING` - 初始化中
- `READY` - 准备就绪
- `UPDATING` - 更新中
- `STALE` - 已过期

## 验证规则

### 语法验证
- 配置文件必须符合ANTLR4语法定义
- struct/interface/table声明语法正确
- 字段类型声明符合规范
- 外键引用语法正确（->, =>）

### 语义验证
- 结构体/接口/表名称必须唯一
- 外键引用必须指向存在的表
- 接口的enumRef表必须存在且包含所有实现类
- 表的主键字段必须存在
- 唯一键字段必须存在

### 类型系统验证
- 基本类型使用正确（bool, int, long, float, str, text）
- 容器类型参数正确（list<T>, map<K,V>）
- 结构类型引用存在
- 接口多态类型正确

### 外键验证
- 单引用（->）必须指向主键或唯一键
- 列表引用（=>）可以指向任意字段
- 外键类型必须匹配
- 可空外键（nullable）处理正确

## 性能考虑

### 索引策略
- 增量更新：只更新修改的文件
- 懒加载：按需解析文件
- 缓存：缓存解析结果和索引

### 内存管理
- 语法树压缩：移除不必要的信息
- 引用计数：自动清理未使用的数据
- 分页加载：处理大型配置文件

## 扩展性

### 插件架构
- 支持自定义验证规则
- 支持自定义类型系统
- 支持自定义跳转逻辑

### 数据格式
- 支持多种配置格式变体
- 支持配置模板
- 支持配置继承