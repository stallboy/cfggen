# VSCode Extension API Contracts

**Date**: 2025-11-08
**Based on**: LSP 3.17 Specification and VSCode Extension API

## Activation Contract

### activate(context: vscode.ExtensionContext)

**Description**: 扩展激活入口

**Input**:
```typescript
context: vscode.ExtensionContext
{
  subscriptions: Disposable[];
  workspaceState: Memento;
  globalState: Memento;
  extensionPath: string;
  asAbsolutePath(relativePath: string): string;
}
```

**Output**: void

**Process**:
1. 注册语言标识符 'cfg'
2. 初始化ANTLR4语法分析器
3. 创建LanguageClient
4. 启动语言服务器
5. 注册命令处理器

**Error Handling**:
- 抛出Error：扩展激活失败，用户需重新加载

## Language Server Protocol (LSP) Implementation

### 1. Completion Provider

**Contract**: textDocument/completion

**Purpose**: 提供代码自动补全

**Request**:
```typescript
{
  textDocument: TextDocumentIdentifier;
  position: Position;
  context?: CompletionContext;
}

interface TextDocumentIdentifier {
  uri: string;  // "file:///path/to/file.cfg"
}

interface Position {
  line: number;
  character: number;
}
```

**Response**:
```typescript
{
  isIncomplete?: boolean;
  items: CompletionItem[];
}

interface CompletionItem {
  label: string;                    // 补全项标签
  kind?: CompletionItemKind;        // 类型（Class, Function, Field等）
  detail?: string;                  // 详细信息
  documentation?: string | MarkupContent;  // 文档
  insertText: string;               // 插入文本
  insertTextFormat?: InsertTextFormat;     // 文本格式（PlainText/Snippet）
  textEdit?: TextEdit;              // 替换文本
  additionalTextEdits?: TextEdit[]; // 附加编辑
  command?: Command;                // 命令
  commitCharacters?: string[];      // 提交字符
  sortText?: string;                // 排序文本
  filterText?: string;              // 过滤文本
  // ... 其他LSP标准属性
}
```

**Completion Contexts**:

**Context 1: 类型补全**
```typescript
// 输入: testField: |
// 触发: 输入冒号后
// 返回: 所有可用类型
{
  items: [
    { label: 'int', kind: CompletionItemKind.Keyword, insertText: 'int' },
    { label: 'str', kind: CompletionItemKind.Keyword, insertText: 'str' },
    { label: 'bool', kind: CompletionItemKind.Keyword, insertText: 'bool' },
    { label: 'Position', kind: CompletionItemKind.Struct, insertText: 'Position' },
    { label: 'Task', kind: CompletionItemKind.Class, insertText: 'task' }
  ]
}
```

**Context 2: 外键补全**
```typescript
// 输入: field:list<int> ->|
// 触发: 输入->后
// 返回: 可引用的表名
{
  items: [
    { label: 'task', kind: CompletionItemKind.Class, insertText: 'task' },
    { label: 'item', kind: CompletionItemKind.Class, insertText: 'item' },
    { label: 'npc', kind: CompletionItemKind.Class, insertText: 'npc' }
  ]
}
```

**Context 3: 元数据补全**
```typescript
// 输入: field:str (|
// 触发: 输入左括号后
// 返回: 可用元数据
{
  items: [
    { label: 'nullable', kind: CompletionItemKind.Property, insertText: 'nullable' },
    { label: 'mustFill', kind: CompletionItemKind.Property, insertText: 'mustFill' },
    { label: 'pack', kind: CompletionItemKind.Property, insertText: 'pack' }
  ]
}
```

### 2. Definition Provider

**Contract**: textDocument/definition

**Purpose**: 提供"跳转到定义"功能

**Request**:
```typescript
{
  textDocument: TextDocumentIdentifier;
  position: Position;
}
```

**Response**:
```typescript
{
  locations: Location[];
}

interface Location {
  uri: string;           // 目标文件uri
  range: Range;          // 目标位置
}

interface Range {
  start: Position;
  end: Position;
}
```

**Definition Cases**:

**Case 1: 类型定义**
```typescript
// 输入: testField:Position|
// 期望: 跳转到Position struct定义
{
  locations: [
    {
      uri: "file:///path/to/definitions.cfg",
      range: {
        start: { line: 10, character: 7 },
        end: { line: 10, character: 15 }
      }
    }
  ]
}
```

**Case 2: 外键引用**
```typescript
// 输入: taskid:int ->task|
// 期望: 跳转到task表定义
{
  locations: [
    {
      uri: "file:///path/to/task.cfg",
      range: {
        start: { line: 45, character: 0 },
        end: { line: 45, character: 4 }
      }
    }
  ]
}
```

**Case 3: 带键外键**
```typescript
// 输入: itemids:list<int> ->item.item|
// 期望: 跳转到item表的item字段定义
{
  locations: [
    {
      uri: "file:///path/to/item.cfg",
      range: {
        start: { line: 15, character: 2 },
        end: { line: 15, character: 6 }
      }
    }
  ]
}
```

**Case 4: 跨模块引用**
```typescript
// 输入: monsterid:int ->other.monster|
// 期望: 跳转到other模块的monster表
{
  locations: [
    {
      uri: "file:///path/to/other/monster.cfg",
      range: {
        start: { line: 0, character: 0 },
        end: { line: 0, character: 6 }
      }
    }
  ]
}
```

**Error Response** (未找到定义):
```typescript
{
  locations: []  // 空数组
}
```

### 3. Hover Provider

**Contract**: textDocument/hover

**Purpose**: 提供悬停提示信息

**Request**:
```typescript
{
  textDocument: TextDocumentIdentifier;
  position: Position;
}
```

**Response**:
```typescript
{
  contents: MarkupContent | MarkedString[] | MarkupContent[];
  range?: Range;
}

type MarkupContent = {
  kind: MarkupKind;  // 'plaintext' | 'markdown'
  value: string;
}
```

**Hover Cases**:

**Case 1: 字段悬停**
```typescript
// 悬停: testField:Position
{
  contents: {
    kind: 'markdown',
    value: '### Position\n\n**Type**: `struct`\n\n**Fields**:\n- x: `int`\n- y: `int`\n\n**Description**: 2D position coordinates'
  }
}
```

**Case 2: 外键悬停**
```typescript
// 悬停: taskid:int ->task
{
  contents: {
    kind: 'markdown',
    value: '### Foreign Key\n\n**Target**: `task`\n**Type**: 1:N\n**Field**: `taskid`\n**Nullable**: true'
  }
}
```

### 4. Reference Provider

**Contract**: textDocument/references

**Purpose**: 查找所有引用位置

**Request**:
```typescript
{
  textDocument: TextDocumentIdentifier;
  position: Position;
  context: ReferenceContext;
}

interface ReferenceContext {
  includeDeclaration: boolean;
  includeDeclarationComments?: boolean;
}
```

**Response**:
```typescript
{
  locations: Location[];
}
```

### 5. ANTLR-Based Syntax Highlighting

**Contract**: textDocument/documentHighlight

**Purpose**: 基于ANTLR解析树的语法高亮

**Implementation**: 使用CfgParser.parse()生成解析树，通过CfgListener遍历树节点，为每个语法结构应用颜色（比词法token更精确）

**Highlight Process**:
1. 使用CfgParser.parse()生成解析树
2. 通过CfgListener遍历树节点
3. 为每个语法结构映射颜色主题
4. 返回DocumentHighlight数组

**Example Implementation**:
```typescript
class CFGHighlightingProvider implements DocumentHighlightProvider {
  private parser: CfgParser;
  private listener: CfgHighlightListener;

  provideDocumentHighlights(
    document: TextDocument,
    position: Position,
    token: CancellationToken
  ): DocumentHighlight[] {
    // 1. 使用CfgParser生成解析树
    const inputStream = CharStreams.fromString(document.getText());
    const lexer = new CfgLexer(inputStream);
    const tokenStream = new CommonTokenStream(lexer);
    this.parser = new CfgParser(tokenStream);

    // 2. 生成解析树
    const tree = this.parser.schema();

    // 3. 使用CfgListener遍历并收集高亮信息
    this.listener = new CfgHighlightListener(document, position);
    ParseTreeWalker.DEFAULT.walk(this.listener, tree);

    // 4. 返回高亮结果
    return this.listener.getHighlights();
  }
}

class CfgHighlightListener extends CfgBaseListener {
  private highlights: DocumentHighlight[] = [];
  private document: TextDocument;
  private position: Position;
  private theme: ThemeConfig;
  private primaryKeyFields: Set<string> = new Set(); // 记录主键字段名

  constructor(document: TextDocument, position: Position, theme: ThemeConfig) {
    super();
    this.document = document;
    this.position = position;
    this.theme = theme;
  }

  // 1. 结构定义 - 只高亮名称
  enterStructDecl(ctx: CfgParser.StructDeclContext) {
    const name = ctx.ns_ident();
    if (name) {
      this.highlights.push({
        range: this.getRange(name.start, name.stop),
        kind: DocumentHighlightKind.Text,
        color: this.theme.colors.structureDefinition
      });
    }
  }

  enterInterfaceDecl(ctx: CfgParser.InterfaceDeclContext) {
    const name = ctx.ns_ident();
    if (name) {
      this.highlights.push({
        range: this.getRange(name.start, name.stop),
        kind: DocumentHighlightKind.Text,
        color: this.theme.colors.structureDefinition
      });
    }
  }

  enterTableDecl(ctx: CfgParser.TableDeclContext) {
    const name = ctx.ns_ident();
    if (name) {
      this.highlights.push({
        range: this.getRange(name.start, name.stop),
        kind: DocumentHighlightKind.Text,
        color: this.theme.colors.structureDefinition
      });
    }

    // 记录主键字段
    const key = ctx.key();
    if (key) {
      // 根据Cfg.g4: key: '[' identifier (',' identifier)* ']'
      // 获取所有identifier
      const identifiers = key.identifier();
      for (const id of identifiers) {
        const fieldName = this.getText(id);
        this.primaryKeyFields.add(fieldName);
      }
    }
  }

  // 2. 复杂结构类型声明
  enterFieldDecl(ctx: CfgParser.FieldDeclContext) {
    // 高亮非基本类型（自定义类型）
    const type = ctx.type_();
    if (type && !this.isBaseType(type)) {
      this.highlights.push({
        range: this.getRange(type.start, type.stop),
        kind: DocumentHighlightKind.Text,
        color: this.theme.colors.complexType
      });
    }

    // 5. 外键引用
    const ref = ctx.ref();
    if (ref) {
      this.highlights.push({
        range: this.getRange(ref.start, ref.stop),
        kind: DocumentHighlightKind.Text,
        color: this.theme.colors.foreignKey
      });
    }
  }

  // 3. 主键字段名称 & 4. 唯一键字段名称
  enterKeyDecl(ctx: CfgParser.KeyDeclContext) {
    const key = ctx.key();
    if (key) {
      // 根据Cfg.g4: key: '[' identifier (',' identifier)* ']'
      // 获取第一个identifier
      const identifier = key.identifier();
      if (identifier) {
        const fieldName = this.getText(identifier);
        // 判断是主键还是唯一键
        const isPrimaryKey = this.primaryKeyFields.has(fieldName);
        this.highlights.push({
          range: this.getRange(identifier.start, identifier.stop),
          kind: DocumentHighlightKind.Text,
          color: isPrimaryKey ? this.theme.colors.primaryKey
                                : this.theme.colors.uniqueKey
        });
      }
    }
  }

  // 5. 外键引用 (在struct内的外键定义)
  enterForeignDecl(ctx: CfgParser.ForeignDeclContext) {
    const ref = ctx.ref();
    if (ref) {
      this.highlights.push({
        range: this.getRange(ref.start, ref.stop),
        kind: DocumentHighlightKind.Text,
        color: this.theme.colors.foreignKey
      });
    }
  }

  // 6. 注释
  enterComment(ctx: CfgParser.CommentContext) {
    this.highlights.push({
      range: this.getRange(ctx.start, ctx.stop),
      kind: DocumentHighlightKind.Text,
      color: this.theme.colors.comment
    });
  }

  // 7. 特定元数据
  enterMetadata(ctx: CfgParser.MetadataContext) {
    this.highlights.push({
      range: this.getRange(ctx.start, ctx.stop),
      kind: DocumentHighlightKind.Text,
      color: this.theme.colors.metadata
    });
  }

  // ========== 辅助方法 ==========
  getHighlights(): DocumentHighlight[] {
    return this.highlights;
  }

  private getRange(symbol: any, stopSymbol?: any): Range {
    const start = this.document.positionAt(symbol.startIndex);
    const end = stopSymbol
      ? this.document.positionAt(stopSymbol.stopIndex + 1)
      : this.document.positionAt(symbol.stopIndex + 1);
    return new Range(start, end);
  }

  private getText(ctx: any): string {
    return this.document.getText(this.getRange(ctx.start, ctx.stop));
  }

  private isBaseType(type: any): boolean {
    // 检查是否为基本类型 (bool/int/long/float/str/text)
    return false; // 简化实现
  }
}
```

**Advantages of ANTLR Approach**:
- **一致性**: 语法高亮与解析使用同一语法规则
- **准确性**: 不会出现TextMate与ANTLR规则不一致
- **可维护性**: 只需维护一套语法定义
- **扩展性**: 更容易支持复杂语法特性

## Configuration Contract

### Configuration Schema

**Configuration File**: `package.json` contributes.configuration

```typescript
{
  "title": "CFG Language Support",
  "properties": {
    "cfg.theme": {
      "type": "string",
      "default": "chineseClassical",
      "enum": ["default", "chineseClassical"],
      "description": "选择语法高亮主题颜色"
    },
    "cfg.enableCache": {
      "type": "boolean",
      "default": true,
      "description": "启用符号表缓存（提升大文件性能）"
    },
    "cfg.maxFileSize": {
      "type": "number",
      "default": 10485760,
      "description": "最大文件大小（字节），超过则跳过解析"
    }
  }
}
```

**Theme Configuration Implementation**:

```typescript
interface ThemeConfig {
  name: 'default' | 'chineseClassical';
  colors: {
    // 1. 结构定义
    structureDefinition: string;    // struct xx, interface xx, table xx

    // 2. 复杂结构类型声明
    complexType: string;            // 非基本类型

    // 3. 主键字段名称
    primaryKey: string;             // table xx[PK]里的PK，以及PK:int里的PK

    // 4. 唯一键字段名称
    uniqueKey: string;              // key_decl里的[UK];里的UK，以及UK:int里的UK

    // 5. 外键引用
    foreignKey: string;             // -> tt, -> tt[kk], => tt[kk]

    // 6. 注释
    comment: string;                // 单行注释//

    // 7. 特定元数据
    metadata: string;               // nullable, mustFill, enumRef, enum, entry, sep, pack, fix, block
  };
}
```

**Highlighting Rules**:

1. **结构定义**: `struct xx`, `interface xx`, `table xx` → 整个`struct xx`、`interface xx`、`table xx`统一高亮
2. **复杂结构类型声明**: `field:Position` → `Position` 高亮（非基本类型）
3. **主键字段名称**: `table xx[PK]`里的`PK`，以及`PK:int`里的`PK` → 高亮`PK`
4. **唯一键字段名称**: `key_decl`里的`[UK]`里的`UK`，以及`UK:int`里的`UK` → 高亮`UK`
5. **外键引用**: `-> tt`, `-> tt[kk]`, `=> tt[kk]` → 整个引用高亮
6. **注释**: `// 单行注释` → 高亮注释
7. **特定元数据**: `(nullable)`, `(mustFill)`, `(enumRef='x')` → 元数据名称高亮

**Theme Color Palettes**:

```typescript
const themes = {
  default: {
    // 1. 结构定义
    structureDefinition: '#0000FF',    // struct/interface/table关键字 + 名称

    // 2. 复杂结构类型声明
    complexType: '#267F99',            // 自定义类型（Position等）

    // 3. 主键字段名称
    primaryKey: '#C586C0',             // PK字段名

    // 4. 唯一键字段名称
    uniqueKey: '#C586C0',              // UK字段名

    // 5. 外键引用
    foreignKey: '#AF00DB',             // -> tt, -> tt[kk], => tt[kk]

    // 6. 注释
    comment: '#008000',                // 绿色注释

    // 7. 特定元数据
    metadata: '#808080'                // nullable等元数据
  },

  chineseClassical: {
    // 1. 结构定义
    structureDefinition: '#1E3A8A',    // 黛青 - struct/interface/table + 名称

    // 2. 复杂结构类型声明
    complexType: '#0F766E',            // 苍青 - 自定义类型

    // 3. 主键字段名称
    primaryKey: '#7E22CE',             // 紫棠 - PK字段

    // 4. 唯一键字段名称
    uniqueKey: '#7E22CE',              // 紫棠 - UK字段

    // 5. 外键引用
    foreignKey: '#BE185D',             // 桃红 - 外键引用

    // 6. 注释
    comment: '#166534',                // 竹青 - 注释

    // 7. 特定元数据
    metadata: '#6B7280'                // 墨灰 - 元数据
  }
};
```

## Command Contracts

### 1. cfg.reload

**Description**: 重新加载当前文件符号表

**Input**: none

**Output**: void

**Process**:
1. 清除缓存
2. 重新解析文件
3. 更新符号表
4. 通知客户端刷新

### 2. cfg.showReferences

**Description**: 显示当前位置的所有引用

**Input**: DocumentPosition (uri, line, character)

**Process**:
1. 解析当前符号
2. 搜索所有引用
3. 在Quick Pick中显示列表
4. 支持跳转

**Output**: void

## Error Handling Contract

### Error Types

```typescript
interface CFGError {
  code: string;              // 错误代码
  message: string;           // 错误消息
  severity: 'error' | 'warning' | 'info';
  range?: Range;            // 错误位置
  source: 'parser' | 'semantic' | 'system';
  data?: any;               // 附加数据
}

// 错误码
const ErrorCodes = {
  SYNTAX_ERROR: 'P001',
  DUPLICATE_DEFINITION: 'P002',
  REFERENCE_NOT_FOUND: 'P003',
  TYPE_MISMATCH: 'P004',
  INVALID_METADATA: 'P005',
  MODULE_NOT_FOUND: 'P006',
  FILE_TOO_LARGE: 'P007'
};
```

### Error Display

**VSCode Diagnostic Collection**:
```typescript
const diagnosticCollection = vscode.languages.createDiagnosticCollection('cfg');

function reportError(error: CFGError) {
  const diagnostic: vscode.Diagnostic = {
    message: error.message,
    severity: error.severity === 'error' ? vscode.DiagnosticSeverity.Error
             : error.severity === 'warning' ? vscode.DiagnosticSeverity.Warning
             : vscode.DiagnosticSeverity.Information,
    range: error.range
  };
  diagnosticCollection.set(vscode.Uri.parse(error.uri), [diagnostic]);
}
```

## Performance Contract

### Response Time Requirements

| 操作 | 目标响应时间 | 最大允许时间 |
|------|-------------|-------------|
| 语法高亮 | 实时 | <50ms |
| 自动补全 | <200ms | <500ms |
| 跳转到定义 | <300ms | <1s |
| 悬停提示 | <200ms | <500ms |
| 符号表加载 | <1s | <2s |

### Caching Strategy

**Purpose**: 为.cfg文件解析结果提供缓存，优化重复访问和跨模块引用的性能。

**Cache Key**: 文件路径的绝对URI (e.g., `file:///path/to/file.cfg`)

**Cache Structure**:

```typescript
interface CachedFileData {
  // 解析后的符号
  symbols: Map<string, Definition>;
  symbolsByModule: Map<string, Definition[]>;
  symbolsByFile: Map<string, Definition[]>;

  // 依赖关系
  dependencies: string[];    // 依赖的其他文件路径

  // 元数据
  lastParsed: number;        // 最后解析时间戳
  fileVersion: number;       // 文件内容版本（基于文件修改时间或哈希）
  size: number;              // 缓存大小（字节）
}

interface CacheManager {
  // 获取缓存
  get(uri: string): CachedFileData | null;

  // 设置缓存
  set(uri: string, data: CachedFileData): void;

  // 失效缓存
  invalidate(uri: string): void;

  // 清除所有缓存
  clear(): void;

  // 清除依赖链
  invalidateDependencies(uri: string): void;
}
```

**Cache Invalidation Strategy**:

1. **文件修改**: 当文件被修改时，根据文件修改时间更新版本号，缓存失效
2. **依赖链更新**: 当被依赖的文件修改时，所有依赖它的文件缓存也失效
3. **LRU淘汰**: 当缓存大小超过限制时，优先淘汰最少使用的条目
4. **内存压力**: 在内存不足时，主动清理部分缓存

**Cache Lifecycle**:

```
1. 访问文件
   ↓
2. 检查缓存是否存在且有效
   ↓
3. 有效 → 返回缓存数据
   无效 → 重新解析并存入缓存
   ↓
4. 更新访问统计信息
```

**Performance Optimization**:

- **预加载**: VSCode启动时预加载已打开的workspace中的.cfg文件
- **增量更新**: 只重新解析修改的文件和依赖文件
- **后台清理**: 异步清理过期缓存，不阻塞主线程
- **压缩存储**: 对大型符号表进行序列化优化

**Example Usage**:

```typescript
class CacheService implements CacheManager {
  private cache = new Map<string, CachedFileData>();
  private maxCacheSize = 100; // 最大缓存文件数
  private maxMemorySize = 50 * 1024 * 1024; // 50MB

  get(uri: string): CachedFileData | null {
    const cached = this.cache.get(uri);
    if (!cached) {
      return null;
    }

    // 检查版本号是否匹配
    const currentVersion = this.getFileVersion(uri);
    if (currentVersion !== cached.fileVersion) {
      this.cache.delete(uri);
      return null;
    }

    return cached;
  }

  set(uri: string, data: CachedFileData): void {
    // 检查内存限制
    if (this.cache.size >= this.maxCacheSize) {
      this.evictLRU();
    }

    this.cache.set(uri, data);
  }

  private evictLRU(): void {
    // 实现LRU淘汰策略
    const oldest = this.cache.keys().next().value;
    this.cache.delete(oldest);
  }
}
```

## Testing Contract

### Unit Test Cases

**Completion Tests**:
1. 类型补全测试
2. 外键补全测试
3. 元数据补全测试
4. 跨模块补全测试

**Definition Tests**:
1. 类型定义跳转
2. 外键跳转
3. 跨模块跳转
4. 错误路径测试

**Integration Tests**:
1. 完整文件解析
2. 多文件跨引用
3. 大文件性能测试
4. 主题切换测试

### Test Data

**使用文件**: `test/fixtures/` 下的真实.cfg示例
- 简单结构：base.cfg
- 复杂结构：task.cfg
- 跨模块：item.cfg, npc.cfg
- 大文件：large_config.cfg (10k+行)
