# Data Model: VSCode CFG Extension

**Date**: 2025-11-09
**Based on**: Cfg.g4 grammar, feature specification, and updated technical decisions
**Architecture**: VSCode Extension API (no LSP) + Two-layer highlighting (TextMate + Semantic Tokens)

## Core Entities

### 1. ConfigFile

**Description**: 顶层容器，解析后的.cfg文件内容

```typescript
interface ConfigFile {
  path: string;              // 文件绝对路径
  moduleName: string;        // 模块名（task, item, npc等）
  definitions: Definition[]; // 所有定义（struct/interface/table）
  symbols: SymbolTable;      // 符号表
  errors: ParseError[];      // 解析错误
  lastModified: number;      // 最后修改时间
}
```

**Validation Rules**:
- path必须存在且为绝对路径
- 模块名从目录名自动解析
- 定义不能重复名称（同一文件内）

### 2. Definition (抽象基类)

**Description**: struct、interface、table的共同基类

```typescript
abstract class Definition {
  name: string;              // 定义名称
  namespace: string;         // 命名空间（module.qualifier）
  metadata: Metadata[];      // 元数据列表
  comment?: string;          // 可选的文档注释
  position: TextRange;       // 源码位置
}
```

### 3. StructDefinition

**Description**: 结构体定义（聚合数据结构）

```typescript
interface StructDefinition extends Definition {
  type: 'struct';
  fields: FieldDefinition[];  // 字段列表
  foreignKeys: ForeignKey[];  // 外键定义
  metadata: StructMetadata;   // struct特有元数据
}

interface StructMetadata {
  sep?: string;              // 分隔符（如时间格式':'）
  pack?: boolean;            // 是否压缩
}
```

**Relationships**:
- StructDefinition → FieldDefinition (1:N)
- FieldDefinition.type引用其他Definition

**Validation Rules**:
- 字段名在struct内唯一
- 类型引用必须存在或为基本类型
- 外键目标必须为table

### 4. InterfaceDefinition

**Description**: 接口定义（多态结构）

```typescript
interface InterfaceDefinition extends Definition {
  type: 'interface';
  implementations: StructDefinition[];  // 实现类
  metadata: InterfaceMetadata;          // interface特有元数据

  // 多态访问器
  getImplementation(name: string): StructDefinition | null;
  getAllImplementations(): StructDefinition[];
}
```

**Relationships**:
- InterfaceDefinition → StructDefinition (1:N)
- InterfaceDefinition.metadata.enumRef指向table

**Validation Rules**:
- 实现类名称必须唯一
- enumRef指向的table必须存在且有name字段
- defaultImpl必须为实现类之一

### 5. TableDefinition

**Description**: 表定义（数据容器）

```typescript
interface TableDefinition extends Definition {
  type: 'table';
  primaryKey: string[];       // 主键字段
  uniqueKeys: string[][];     // 唯一键列表
  fields: FieldDefinition[];  // 字段列表
  foreignKeys: ForeignKey[];  // 外键定义
  metadata: TableMetadata;    // table特有元数据
}

interface TableMetadata {
  enumField?: string;         // 枚举字段名
  entryField?: string;        // 入口字段名
  json?: boolean;            // JSON格式
  description?: string;       // 描述
}
```

**Relationships**:
- TableDefinition → FieldDefinition (1:N)
- TableDefinition.primaryKey引用FieldDefinition.name
- 外键引用指向其他TableDefinition

**Validation Rules**:
- 主键必须存在且唯一
- 唯一键字段必须存在
- enumField指向的字段类型必须为str或int

### 6. FieldDefinition

**Description**: 字段定义（基本构建块）

```typescript
interface FieldDefinition {
  name: string;               // 字段名
  type: FieldType;            // 字段类型
  foreignKey?: ForeignKey;    // 可选外键引用
  metadata: FieldMetadata[];  // 元数据列表
  comment?: string;           // 注释
  position: TextRange;        // 位置
}

type FieldType =
  | BaseType                  // bool, int, long, float, str, text
  | ListType                  // list<type>
  | MapType                   // map<key, value>
  | CustomType;               // struct/interface引用

interface BaseType {
  kind: 'base';
  name: 'bool' | 'int' | 'long' | 'float' | 'str' | 'text';
}

interface ListType {
  kind: 'list';
  elementType: FieldType;
}

interface MapType {
  kind: 'map';
  keyType: FieldType;
  valueType: FieldType;
}

interface CustomType {
  kind: 'custom';
  namespace: string;          // 完整类型名
  shortName: string;          // 短名称
  definition?: Definition;    // 解析后的定义引用
}
```

**Relationships**:
- FieldDefinition → ForeignKey (0:1)
- FieldDefinition.type指向其他Definition或基本类型

**Validation Rules**:
- 字段名在同一容器内唯一
- list/map的类型参数必须有效
- 自定义类型引用必须存在

### 7. ForeignKeyDefinition

**Description**: 外键定义（引用关系）

```typescript
interface ForeignKey {
  name?: string;              // 外键名（可省略）
  referenceType: 'single' | 'list';  // 单值或列表引用
  operator: '->' | '=>';      // 引用操作符
  target: ReferenceTarget;    // 引用目标
  metadata: FieldMetadata[];  // 元数据
  position: TextRange;        // 位置
}

interface ReferenceTarget {
  module: string;             // 模块名
  table: string;              // 表名
  field?: string;            // 字段名（可选）
  key?: string;              // 键名（可选）
  targetDefinition?: TableDefinition;  // 解析后的定义
}
```

**Relationships**:
- ForeignKey → TableDefinition (1:1)
- 列表引用需额外解析每个元素

**Validation Rules**:
- 引用的table必须存在
- field和key必须同时存在或同时省略
- 列表引用中每个元素都应满足外键约束

### 8. MetadataDefinition

**Description**: 元数据定义（配置属性）

```typescript
interface Metadata {
  name: string;               // 元数据名
  value?: Literal;            // 可选值
  position: TextRange;        // 位置
}

type Literal =
  | IntegerLiteral           // 整型常量
  | HexIntegerLiteral        // 十六进制整数
  | FloatLiteral             // 浮点常量
  | StringLiteral;           // 字符串常量

interface IntegerLiteral {
  kind: 'integer';
  value: number;
  raw: string;
}
```

**Common Metadata**:
- `nullable`: 允许空值
- `mustFill`: 必须填写
- `pack`: 压缩存储
- `enumRef`: 枚举引用
- `defaultImpl`: 默认实现
- `fix`: 固定长度
- `block`: 数据块
- `noserver`: 客户端专用

### 9. SymbolTable

**Description**: 符号表（快速查找已解析符号）

```typescript
class SymbolTable {
  private definitions: Map<string, Definition[]>;        // 定义映射
  private byModule: Map<string, Definition[]>;          // 按模块分组
  private byType: Map<string, Definition[]>;            // 按类型分组

  // 添加定义
  add(definition: Definition): void;

  // 查找定义
  find(name: string, module?: string): Definition | null;
  findAll(type?: DefinitionType): Definition[];

  // 跨模块查找
  findInModule(module: string, name: string): Definition | null;

  // 获取引用关系
  getReferences(target: Definition): Reference[];
}
```

**Indexing Strategy**:
- 主索引：名称
- 二级索引：模块名
- 三级索引：类型（struct/interface/table）

### 10. ModuleResolver

**Description**: 模块解析器（跨模块引用处理）

```typescript
class ModuleResolver {
  private rootPath: string;         // 根路径
  private modulePaths: Map<string, string>;  // 模块路径映射

  // 解析模块名
  parseModuleName(filePath: string): string;

  // 查找模块路径
  resolveModule(moduleName: string): string | null;

  // 加载模块
  loadModule(moduleName: string): Promise<ConfigFile | null>;

  // 获取所有模块
  getAllModules(): string[];
}
```

**Module Name Resolution**:
1. 提取目录名
2. 截取第一个"."之前
3. 截取"_汉字"或纯汉字之前

## State Transitions

### Parse States

```
IDLE → PARSING → READY
  ↓
PARSING → ERROR
  ↓
ERROR → PARSING (on retry)
```

### Symbol Resolution States

```
UNRESOLVED → PARTIALLY_RESOLVED → RESOLVED
  ↓
PARTIALLY_RESOLVED → ERROR (invalid reference)
```

## Validation Rules Summary

1. **唯一性约束**:
   - 同级定义名称唯一
   - 同名字段在同容器内唯一

2. **引用完整性**:
   - 外键引用必须存在
   - 类型引用必须可解析
   - 模块引用必须存在

3. **类型约束**:
   - 主键/唯一键必须为基本类型
   - enum字段必须为str或int
   - list/map类型参数必须有效

4. **元数据约束**:
   - enumRef指向的表必须有name字段
   - defaultImpl必须为接口实现类
   - entry字段必须为str类型

## Error Handling

### Parse Errors

```typescript
interface ParseError {
  code: string;               // 错误码
  message: string;            // 错误消息
  position: TextRange;        // 错误位置
  severity: 'error' | 'warning' | 'info';
  source?: string;            // 错误来源（语法/语义）
}
```

**Error Codes**:
- `P001`: 语法错误
- `P002`: 重复定义
- `P003`: 引用未找到
- `P004`: 类型不匹配
- `P005`: 无效元数据
- `P006`: 模块不存在

## Two-Layer Syntax Highlighting Data Model

### 1. TextMate Grammar Rules

**Description**: 基础语法高亮规则，定义基础token类型

```typescript
interface TextMateRule {
  name: string;                // token名称
  match: string;               // 正则表达式
  captures: Captures;          // 捕获组
}

interface Captures {
  [key: number]: {
    name: string;              // 捕获组名称
  };
}
```

**TextMate规则**:
```json
{
  "keywords": "struct|interface|table|int|str|bool|list|map",
  "strings": "\"[^\"]*\"",
  "numbers": "\d+",
  "comments": "//.*",
  "operators": "->|=>|=|:",
  "punctuation": "{ } [ ] ( ) , ;"
}
```

### 2. Semantic Token Information

**Description**: 语义高亮信息，基于ANTLR4解析树

```typescript
interface SemanticToken {
  line: number;                // 行号
  startCharacter: number;      // 起始字符
  length: number;              // 长度
  tokenType: number;           // token类型索引
  tokenModifiers: number;      // 修饰符
}

enum SemanticTokenTypes {
  structureDefinition = 0,    // struct/interface/table名称
  typeIdentifier = 1,         // 类型名
  fieldName = 2,              // 字段名
  foreignKey = 3,             // 外键引用
  comment = 4,                // 注释
  metadata = 5                // 元数据
}
```

### 3. Combined Highlighting Strategy

**TextMate Layer职责**:
- 快速响应用户输入（毫秒级）
- 基础token识别（关键字、字符串、数字、注释、运算符、标点）
- 零性能开销（VSCode原生优化）

**Semantic Layer职责**:
- 复杂语义结构高亮（struct/interface/table名称、非基本类型、主键/唯一键、外键引用）
- 基于ANTLR4解析树精确分析
- 主题色应用（默认+中国古典色）
- 20-50ms响应时间

**叠加规则**:
- Semantic层颜色覆盖TextMate层
- 但保留TextMate的即时性
- 两者共同作用于同一个视觉结果
