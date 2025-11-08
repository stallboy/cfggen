# Theme System Design: 双层语法高亮主题系统

**Date**: 2025-11-09
**Status**: Design Supplement
**Based on**: Constitution requirements + two-layer highlighting architecture

## Executive Summary

本设计文档详细说明theme系统如何与双层语法高亮（TextMate + Semantic Tokens）关联，实现主题色动态切换和一致性。

## Theme System Architecture

### 1. Theme Configuration Structure

```typescript
interface ThemeConfig {
  name: 'default' | 'chineseClassical';
  scopes: TextMateScopeMapping;      // TextMate层scope映射
  semanticTokens: SemanticTokenMapping; // Semantic层token映射
  metadata: {
    displayName: string;
    description: string;
    isDefault: boolean;
  };
}

interface TextMateScopeMapping {
  keywords: string;          // scope: keyword.control.cfg
  strings: string;           // scope: string.quoted.double.cfg
  numbers: string;           // scope: constant.numeric.cfg
  comments: string;          // scope: comment.line.double-slash.cfg
  operators: string;         // scope: keyword.operator.cfg
  punctuation: string;       // scope: punctuation.cfg
}

interface SemanticTokenMapping {
  structureDefinition: string;  // struct/interface/table名称
  typeIdentifier: string;       // 非基本类型
  fieldName: string;            // 字段名
  foreignKey: string;           // 外键引用
  primaryKey: string;           // 主键
  uniqueKey: string;            // 唯一键
  metadata: string;             // 元数据关键字
}
```

### 2. 双层高亮关联机制

#### TextMate层关联 (Layer 1)

**实现方式**: 通过VSCode的`contributes.configuration`和`tokenColorCustomizations`

```typescript
// 1. package.json中定义主题配置
{
  "contributes": {
    "configuration": {
      "type": "object",
      "properties": {
        "cfg.theme": {
          "type": "string",
          "default": "chineseClassical",
          "enum": ["default", "chineseClassical"],
          "description": "选择语法高亮主题颜色"
        }
      }
    },
    "languages": [{
      "id": "cfg",
      "extensions": [".cfg"],
      "configuration": "./cfg-language-configuration.json"
    }],
    "grammars": [{
      "language": "cfg",
      "scopeName": "source.cfg",
      "path": "./syntaxes/cfg.tmLanguage.json"
    }]
  }
}
```

**2. cfg.tmLanguage.json中的scope定义**:
```json
{
  "name": "CFG",
  "scopeName": "source.cfg",
  "patterns": [
    {
      "name": "keyword.control.cfg",
      "match": "\\b(struct|interface|table|int|str|bool|list|map)\\b"
    },
    {
      "name": "string.quoted.double.cfg",
      "begin": "\"",
      "end": "\"",
      "patterns": [
        {
          "name": "constant.character.escape.cfg",
          "match": "\\\\."
        }
      ]
    },
    {
      "name": "constant.numeric.cfg",
      "match": "\\b\\d+\\b"
    },
    {
      "name": "comment.line.double-slash.cfg",
      "begin": "//",
      "end": "$"
    },
    {
      "name": "keyword.operator.cfg",
      "match": "(\\-\\>|\\=\\>|\\=|\\:)"
    },
    {
      "name": "punctuation.cfg",
      "match": "[{}\\[\\]\\(\\),;]"
    }
  ]
}
```

**3. 主题色应用 - tokenColorCustomizations**:
```typescript
class ThemeService {
  private getThemeColors(themeName: string): ThemeConfig {
    const themes = {
      default: {
        scopes: {
          keywords: "#0000FF",        // 蓝色关键字
          strings: "#A31515",         // 红色字符串
          numbers: "#098658",         // 绿色数字
          comments: "#008000",        // 绿色注释
          operators: "#795E26",       // 棕色运算符
          punctuation: "#000000"      // 黑色标点
        },
        semanticTokens: {
          structureDefinition: "#0000FF",
          typeIdentifier: "#267F99",
          fieldName: "#001080",
          foreignKey: "#AF00DB",
          primaryKey: "#C586C0",
          uniqueKey: "#C586C0",
          metadata: "#808080"
        }
      },
      chineseClassical: {
        scopes: {
          keywords: "#1E3A8A",        // 黛青关键字
          strings: "#7C2D12",         // 赭石色字符串
          numbers: "#0F766E",         // 苍青数字
          comments: "#166534",        // 竹青注释
          operators: "#7E22CE",       // 紫棠运算符
          punctuation: "#6B7280"      // 墨灰标点
        },
        semanticTokens: {
          structureDefinition: "#1E3A8A",  // 黛青结构
          typeIdentifier: "#0F766E",       // 苍青类型
          fieldName: "#0369A1",            // 天蓝字段
          foreignKey: "#BE185D",           // 桃红外键
          primaryKey: "#7E22CE",           // 紫棠主键
          uniqueKey: "#7E22CE",            // 紫棠唯一键
          metadata: "#6B7280"              // 墨灰元数据
        }
      }
    };
    return themes[themeName];
  }

  // 应用主题到VSCode
  applyTheme(themeName: string) {
    const theme = this.getThemeColors(themeName);
    const tokenColors = this.generateTokenColorCustomizations(theme);

    vscode.workspace.getConfiguration().update(
      'editor.tokenColorCustomizations',
      tokenColors,
      vscode.ConfigurationTarget.Global
    );
  }

  private generateTokenColorCustomizations(theme: ThemeConfig) {
    return {
      "[*]": {
        "textMateRules": [
          {
            "name": "CFG Keywords",
            "scope": "keyword.control.cfg",
            "settings": {
              "foreground": theme.scopes.keywords
            }
          },
          {
            "name": "CFG Strings",
            "scope": "string.quoted.double.cfg",
            "settings": {
              "foreground": theme.scopes.strings
            }
          },
          {
            "name": "CFG Comments",
            "scope": "comment.line.double-slash.cfg",
            "settings": {
              "foreground": theme.scopes.comments
            }
          }
          // ... 其他scope
        ]
      }
    };
  }
}
```

#### Semantic层关联 (Layer 2)

**实现方式**: 通过SemanticTokensLegend动态应用主题色

```typescript
class SemanticTokensProvider implements vscode.DocumentSemanticTokensProvider {
  private legend: vscode.SemanticTokensLegend;
  private themeService: ThemeService;

  constructor(themeService: ThemeService) {
    this.themeService = themeService;
    this.legend = new vscode.SemanticTokensLegend(
      [
        'structureDefinition',    // 0
        'typeIdentifier',         // 1
        'fieldName',              // 2
        'foreignKey',             // 3
        'comment',                // 4
        'metadata',               // 5
        'primaryKey',             // 6
        'uniqueKey'               // 7
      ],
      [] // modifiers
    );
  }

  provideDocumentSemanticTokens(
    document: vscode.TextDocument,
    token: vscode.CancellationToken
  ): vscode.ProviderResult<vscode.SemanticTokens> {
    const builder = new vscode.SemanticTokensBuilder(this.legend);
    const theme = this.themeService.getCurrentTheme();

    // 基于ANTLR4解析树生成tokens
    const parseTree = this.parser.parse(document.getText());
    const walker = new ParseTreeWalker();
    const listener = new CFGHighlightingListener(builder, document, theme);

    walker.walk(listener, parseTree);
    return builder.build();
  }
}

class CFGHighlightingListener extends CfgBaseListener {
  private builder: vscode.SemanticTokensBuilder;
  private document: vscode.TextDocument;
  private theme: ThemeConfig;

  constructor(
    builder: vscode.SemanticTokensBuilder,
    document: vscode.TextDocument,
    theme: ThemeConfig
  ) {
    super();
    this.builder = builder;
    this.document = document;
    this.theme = theme;
  }

  // 结构定义高亮
  enterStructDecl(ctx: CfgParser.StructDeclContext) {
    const name = ctx.ns_ident();
    if (name) {
      this.builder.push(
        name.start.line,
        name.start.character,
        name.text.length,
        this.getTokenType('structureDefinition'),
        0
      );
    }
  }

  // 外键引用高亮
  enterFieldDecl(ctx: CfgParser.FieldDeclContext) {
    const ref = ctx.ref();
    if (ref) {
      const isList = ref.operator().text === '=>';
      const tokenType = isList ? 'uniqueKey' : 'foreignKey';

      this.builder.push(
        ref.start.line,
        ref.start.character,
        ref.text.length,
        this.getTokenType(tokenType),
        0
      );
    }
  }

  // 主键高亮
  enterKeyDecl(ctx: CfgParser.KeyDeclContext) {
    const key = ctx.key();
    if (key) {
      const identifiers = key.identifier();
      for (const id of identifiers) {
        this.builder.push(
          id.start.line,
          id.start.character,
          id.text.length,
          this.getTokenType('primaryKey'),
          0
        );
      }
    }
  }

  private getTokenType(semanticType: string): number {
    const mapping: Record<string, number> = {
      'structureDefinition': 0,
      'typeIdentifier': 1,
      'fieldName': 2,
      'foreignKey': 3,
      'comment': 4,
      'metadata': 5,
      'primaryKey': 6,
      'uniqueKey': 7
    };
    return mapping[semanticType];
  }
}
```

### 3. Theme切换机制

```typescript
class ThemeManager {
  private static instance: ThemeManager;
  private currentTheme: string = 'chineseClassical';
  private subscribers: ((theme: string) => void)[] = [];

  private constructor() {
    // 监听VSCode配置变化
    vscode.workspace.onDidChangeConfiguration((e) => {
      if (e.affectsConfiguration('cfg.theme')) {
        this.handleThemeChange();
      }
    });
  }

  public static getInstance(): ThemeManager {
    if (!ThemeManager.instance) {
      ThemeManager.instance = new ThemeManager();
    }
    return ThemeManager.instance;
  }

  private handleThemeChange() {
    const config = vscode.workspace.getConfiguration('cfg');
    const newTheme = config.get<string>('theme', 'chineseClassical');

    if (newTheme !== this.currentTheme) {
      this.currentTheme = newTheme;
      this.notifySubscribers(newTheme);

      // 刷新所有打开的.cfg文件
      this.refreshActiveDocuments();
    }
  }

  private notifySubscribers(theme: string) {
    this.subscribers.forEach(callback => callback(theme));
  }

  public subscribe(callback: (theme: string) => void) {
    this.subscribers.push(callback);
    return () => {
      this.subscribers = this.subscribers.filter(cb => cb !== callback);
    };
  }

  public getCurrentTheme(): string {
    return this.currentTheme;
  }

  private refreshActiveDocuments() {
    vscode.workspace.textDocuments.forEach(doc => {
      if (doc.languageId === 'cfg') {
        // 触发语义tokens刷新
        vscode.commands.executeCommand(
          'vscode.refreshSemanticTokens',
          doc.uri
        );
      }
    });
  }
}
```

### 4. 扩展激活时的Theme初始化

```typescript
export function activate(context: vscode.ExtensionContext) {
  // 1. 初始化ThemeManager
  const themeManager = ThemeManager.getInstance();

  // 2. 创建ThemeService
  const themeService = new ThemeService();

  // 3. 应用当前主题
  const currentTheme = themeManager.getCurrentTheme();
  themeService.applyTheme(currentTheme);

  // 4. 监听主题变化
  themeManager.subscribe((theme) => {
    themeService.applyTheme(theme);

    // 通知所有provider刷新
    refreshAllProviders();
  });

  // 5. 注册语义高亮provider（使用当前theme）
  const semanticTokensProvider = new SemanticTokensProvider(themeService);
  context.subscriptions.push(
    vscode.languages.registerDocumentSemanticTokensProvider(
      { language: 'cfg' },
      semanticTokensProvider,
      semanticTokensProvider.getLegend()
    )
  );
}

function refreshAllProviders() {
  // 强制VSCode刷新语义tokens
  vscode.commands.executeCommand('vscode.refreshSemanticTokens');
}
```

### 5. Theme系统测试

```typescript
describe('Theme System', () => {
  it('should apply default theme colors', () => {
    const themeService = new ThemeService();
    const theme = themeService.getThemeColors('default');

    expect(theme.scopes.keywords).toBe('#0000FF');
    expect(theme.semanticTokens.structureDefinition).toBe('#0000FF');
  });

  it('should apply chineseClassical theme colors', () => {
    const themeService = new ThemeService();
    const theme = themeService.getThemeColors('chineseClassical');

    expect(theme.scopes.keywords).toBe('#1E3A8A'); // 黛青
    expect(theme.semanticTokens.structureDefinition).toBe('#1E3A8A');
  });

  it('should switch theme dynamically', async () => {
    const themeManager = ThemeManager.getInstance();
    let callbackCalled = false;

    themeManager.subscribe((theme) => {
      expect(theme).toBe('default');
      callbackCalled = true;
    });

    // 模拟配置变更
    await vscode.workspace.getConfiguration().update(
      'cfg.theme',
      'default',
      vscode.ConfigurationTarget.Global
    );

    expect(callbackCalled).toBe(true);
  });

  it('should refresh semantic tokens on theme change', () => {
    const mockRefresh = jest.spyOn(vscode.commands, 'executeCommand');
    themeManager.subscribe('default');

    expect(mockRefresh).toHaveBeenCalledWith(
      'vscode.refreshSemanticTokens',
      expect.anything()
    );
  });
});
```

## Theme Color Palette

### Default Theme (VSCode标准配色)

```json
{
  "scopes": {
    "keywords": "#0000FF",        // 蓝色关键字
    "strings": "#A31515",         // 红色字符串
    "numbers": "#098658",         // 绿色数字
    "comments": "#008000",        // 绿色注释
    "operators": "#795E26",       // 棕色运算符
    "punctuation": "#000000"      // 黑色标点
  },
  "semanticTokens": {
    "structureDefinition": "#0000FF",    // struct/interface/table
    "typeIdentifier": "#267F99",         // 非基本类型
    "fieldName": "#001080",              // 字段名
    "foreignKey": "#AF00DB",             // 外键引用
    "primaryKey": "#C586C0",             // 主键
    "uniqueKey": "#C586C0",              // 唯一键
    "metadata": "#808080"                // 元数据
  }
}
```

### Chinese Classical Theme (中国古典色)

```json
{
  "scopes": {
    "keywords": "#1E3A8A",        // 黛青关键字
    "strings": "#7C2D12",         // 赭石色字符串
    "numbers": "#0F766E",         // 苍青数字
    "comments": "#166534",        // 竹青注释
    "operators": "#7E22CE",       // 紫棠运算符
    "punctuation": "#6B7280"      // 墨灰标点
  },
  "semanticTokens": {
    "structureDefinition": "#1E3A8A",    // 黛青结构
    "typeIdentifier": "#0F766E",         // 苍青类型
    "fieldName": "#0369A1",              // 天蓝字段
    "foreignKey": "#BE185D",             // 桃红外键
    "primaryKey": "#7E22CE",             // 紫棠主键
    "uniqueKey": "#7E22CE",              // 紫棠唯一键
    "metadata": "#6B7280"                // 墨灰元数据
  }
}
```

## 色名由来

- **黛青** (#1E3A8A): 传统青黛染料色，深蓝偏青，象征深邃优雅
- **苍青** (#0F766E): 天空青蓝色，象征广阔与稳重
- **天蓝** (#0369A1): 天空纯净蓝，象征清晰与透明
- **紫棠** (#7E22CE): 紫棠花色，深紫偏粉，用于主键和唯一键
- **桃红** (#BE185D): 桃花粉色，温润柔和，用于外键引用
- **竹青** (#166534): 竹叶青绿色，自然清新，用于注释
- **赭石** (#7C2D12): 传统赭石色，沉稳内敛，用于字符串
- **墨灰** (#6B7280): 墨色配灰，沉稳内敛，用于元数据和标点

## 总结

**Theme与双层高亮的关联**:

1. **TextMate层**: 通过`scopeName`映射主题色，使用`tokenColorCustomizations`应用
2. **Semantic层**: 通过`SemanticTokensLegend`和`tokenType`映射主题色，直接在provider中应用
3. **Theme切换**: 监听配置变化，动态应用新主题，强制刷新所有provider
4. **一致性**: 两层使用相同的主题配置，确保视觉一致性

**需要添加到plan.md**:
- themeService.ts - 主题服务
- themeManager.ts - 主题管理器
- cfg.tmLanguage.json - TextMate语法文件
- tokenColorCustomizations机制
- theme配置监听和动态切换
