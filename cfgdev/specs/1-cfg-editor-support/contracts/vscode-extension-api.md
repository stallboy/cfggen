# VSCode Extension API Contracts

**Feature**: CFG文件编辑器支持
**Date**: 2025-11-06

## 扩展激活

### 激活事件
```typescript
// package.json
{
  "activationEvents": [
    "onLanguage:cfg",
    "onCommand:cfg.validate",
    "onCommand:cfg.gotoDefinition"
  ]
}
```

## 语言特性提供者

### 1. 语法高亮提供者 (DocumentSemanticTokensProvider)

**接口**: `vscode.DocumentSemanticTokensProvider`

**方法**:
- `provideDocumentSemanticTokens(document: TextDocument, token: CancellationToken): ProviderResult<SemanticTokens>`
- `provideDocumentSemanticTokensEdits?(document: TextDocument, previousResultId: string, token: CancellationToken): ProviderResult<SemanticTokens | SemanticTokensEdits>`

**语义标记类型**:
- `struct` - 结构体定义
- `interface` - 接口定义
- `table` - 表定义
- `field` - 字段定义
- `keyword` - 关键字（struct, interface, table, list, map等）
- `type` - 类型名称
- `string` - 字符串值
- `number` - 数值
- `comment` - 注释
- `foreignKey` - 外键引用（->, =>）
- `metadata` - 元数据属性

### 2. 自动补全提供者 (CompletionItemProvider)

**接口**: `vscode.CompletionItemProvider`

**方法**:
- `provideCompletionItems(document: TextDocument, position: Position, token: CancellationToken, context: CompletionContext): ProviderResult<CompletionItem[] | CompletionList>`
- `resolveCompletionItem?(item: CompletionItem, token: CancellationToken): ProviderResult<CompletionItem>`

**补全类型**:
- 结构体/接口/表名称补全
- 字段名称补全
- 类型名称补全（基本类型、容器类型）
- 外键表名称补全
- 关键字补全（struct, interface, table, list, map等）
- 元数据属性补全（enumRef, defaultImpl, pack等）

### 3. 定义跳转提供者 (DefinitionProvider)

**接口**: `vscode.DefinitionProvider`

**方法**:
- `provideDefinition(document: TextDocument, position: Position, token: CancellationToken): ProviderResult<Definition | DefinitionLink[]>`

**支持跳转**:
- 结构体/接口/表定义跳转
- 外键引用跳转（->, =>）
- 接口实现类跳转
- 枚举表引用跳转（enumRef）

### 4. 悬停提示提供者 (HoverProvider)

**接口**: `vscode.HoverProvider`

**方法**:
- `provideHover(document: TextDocument, position: Position, token: CancellationToken): ProviderResult<Hover>`

**提示内容**:
- 结构体/接口/表文档
- 字段描述和类型信息
- 外键目标信息（表名称、键字段）
- 接口多态信息（实现类、enumRef）
- 验证错误信息
- 元数据属性说明

### 5. 诊断提供者 (DiagnosticProvider)

**接口**: 通过 `vscode.languages.createDiagnosticCollection`

**诊断类型**:
- 语法错误（不符合ANTLR4语法）
- 语义错误（类型不存在、外键无效）
- 引用错误（表不存在、字段不存在）
- 接口错误（enumRef表不匹配、实现类缺失）
- 验证警告（可空外键、类型不匹配）

## 命令接口

### 1. 验证命令
```typescript
vscode.commands.registerCommand('cfg.validate', (document: TextDocument) => {
  // 执行配置验证
  return ValidationResult;
});
```

### 2. 跳转命令
```typescript
vscode.commands.registerCommand('cfg.gotoDefinition', (position: Position) => {
  // 执行定义跳转
  return Location | Location[];
});
```

### 3. 查找引用命令
```typescript
vscode.commands.registerCommand('cfg.findReferences', (position: Position) => {
  // 查找所有引用
  return Location[];
});
```

## 配置设置

### 1. 插件配置
```json
{
  "cfg.enableSyntaxHighlighting": {
    "type": "boolean",
    "default": true,
    "description": "启用语法高亮"
  },
  "cfg.enableAutoCompletion": {
    "type": "boolean",
    "default": true,
    "description": "启用自动补全"
  },
  "cfg.validationLevel": {
    "type": "string",
    "enum": ["none", "syntax", "semantic", "full"],
    "default": "semantic",
    "description": "验证级别"
  }
}
```

## 事件接口

### 1. 文件变化事件
```typescript
vscode.workspace.onDidChangeTextDocument((event: TextDocumentChangeEvent) => {
  // 处理文件变化
});
```

### 2. 文件保存事件
```typescript
vscode.workspace.onDidSaveTextDocument((document: TextDocument) => {
  // 处理文件保存
});
```

### 3. 配置变化事件
```typescript
vscode.workspace.onDidChangeConfiguration((event: ConfigurationChangeEvent) => {
  // 处理配置变化
});
```

## 性能指标

### 响应时间要求
- 语法高亮: < 50ms
- 自动补全: < 100ms
- 定义跳转: < 200ms
- 悬停提示: < 150ms

### 内存限制
- 插件内存: < 50MB
- 缓存大小: < 20MB
- 索引大小: < 10MB

## 错误处理

### 错误类型
- 语法解析错误
- 语义验证错误
- 引用解析错误
- 系统错误

### 错误报告
- 使用VSCode诊断API显示错误
- 提供详细的错误信息
- 支持快速修复建议