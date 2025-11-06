import * as vscode from 'vscode';
import { FileIndexService } from '../services/fileIndexService';
import { Logger } from '../utils/logger';
import { PerformanceMonitor } from '../utils/performance';
import { NamespaceUtils } from '../utils/namespaceUtils';

/**
 * CFG智能提示提供者
 * 提供结构体、接口、表、字段等的自动完成
 */
export class CfgCompletionProvider implements vscode.CompletionItemProvider {
  private fileIndexService: FileIndexService;
  private logger: Logger;
  private performance: PerformanceMonitor;

  constructor() {
    this.fileIndexService = FileIndexService.getInstance();
    this.logger = Logger.getInstance();
    this.performance = PerformanceMonitor.getInstance();
  }

  /**
   * 提供自动完成项
   */
  public async provideCompletionItems(
    document: vscode.TextDocument,
    position: vscode.Position,
    _token: vscode.CancellationToken,
    _context: vscode.CompletionContext
  ): Promise<vscode.CompletionItem[]> {
    const endMeasurement = this.performance.startMeasurement('completion');

    try {
      const lineText = document.lineAt(position.line).text;
      const linePrefix = lineText.substring(0, position.character);

      // 分析上下文，确定提供什么类型的提示
      const contextType = this.analyzeContext(linePrefix, document, position);

      let completionItems: vscode.CompletionItem[] = [];

      switch (contextType.type) {
        case 'keyword':
          completionItems = this.provideKeywordCompletions();
          break;
        case 'type':
          completionItems = this.provideTypeCompletions();
          break;
        case 'struct':
          completionItems = this.provideStructCompletions(contextType.namespace);
          break;
        case 'interface':
          completionItems = this.provideInterfaceCompletions(contextType.namespace);
          break;
        case 'table':
          completionItems = this.provideTableCompletions(contextType.namespace);
          break;
        case 'field':
          completionItems = this.provideFieldCompletions(contextType);
          break;
        case 'metadata':
          completionItems = this.provideMetadataCompletions();
          break;
        default:
          completionItems = this.provideDefaultCompletions();
      }

      endMeasurement();
      return completionItems;
    } catch (error) {
      endMeasurement();
      this.logger.error('Completion provider error:', error);
      return [];
    }
  }

  /**
   * 分析上下文
   */
  private analyzeContext(
    linePrefix: string,
    document: vscode.TextDocument,
    position: vscode.Position
  ): CompletionContext {
    // 检查是否在注释中
    if (linePrefix.includes('#')) {
      return { type: 'none' };
    }

    // 检查是否在字符串中
    const textUntilPosition = document.getText(new vscode.Range(new vscode.Position(0, 0), position));
    const singleQuoteCount = (textUntilPosition.match(/'/g) || []).length;
    if (singleQuoteCount % 2 === 1) {
      return { type: 'none' }; // 在字符串中
    }

    // 分析行前缀确定上下文
    const trimmedPrefix = linePrefix.trim();

    // 检查关键字上下文
    if (this.isKeywordContext(trimmedPrefix)) {
      return { type: 'keyword' };
    }

    // 检查类型上下文
    if (this.isTypeContext(trimmedPrefix)) {
      return { type: 'type' };
    }

    // 检查结构体/接口/表上下文
    const declarationContext = this.getDeclarationContext(trimmedPrefix);
    if (declarationContext) {
      return declarationContext;
    }

    // 检查字段上下文
    const fieldContext = this.getFieldContext(trimmedPrefix, document, position);
    if (fieldContext) {
      return fieldContext;
    }

    // 检查元数据上下文
    if (this.isMetadataContext(trimmedPrefix)) {
      return { type: 'metadata' };
    }

    return { type: 'default' };
  }

  /**
   * 提供关键字自动完成
   */
  private provideKeywordCompletions(): vscode.CompletionItem[] {
    const keywords = [
      'struct', 'interface', 'table', 'key', 'ref',
      'nullable', 'required', 'unique', 'index'
    ];

    return keywords.map(keyword => {
      const item = new vscode.CompletionItem(keyword, vscode.CompletionItemKind.Keyword);
      item.detail = 'CFG Keyword';
      item.documentation = `CFG language keyword: ${keyword}`;
      return item;
    });
  }

  /**
   * 提供类型自动完成
   */
  private provideTypeCompletions(): vscode.CompletionItem[] {
    const types = [
      'bool', 'int', 'long', 'float', 'str', 'text',
      'list', 'map'
    ];

    return types.map(type => {
      const item = new vscode.CompletionItem(type, vscode.CompletionItemKind.TypeParameter);
      item.detail = 'CFG Type';
      item.documentation = `CFG data type: ${type}`;
      return item;
    });
  }

  /**
   * 提供结构体自动完成
   */
  private provideStructCompletions(namespace?: string | null): vscode.CompletionItem[] {
    const structs = this.fileIndexService.findAllStructs();

    return structs
      .filter(struct => !namespace || NamespaceUtils.namespaceMatches(struct.namespace, namespace))
      .map(struct => {
        const item = new vscode.CompletionItem(struct.fullName, vscode.CompletionItemKind.Struct);
        item.detail = 'CFG Struct';
        item.documentation = `CFG struct definition: ${struct.fullName}`;
        return item;
      });
  }

  /**
   * 提供接口自动完成
   */
  private provideInterfaceCompletions(namespace?: string | null): vscode.CompletionItem[] {
    const interfaces = this.fileIndexService.findAllInterfaces();

    return interfaces
      .filter(interfaceDef => !namespace || NamespaceUtils.namespaceMatches(interfaceDef.namespace, namespace))
      .map(interfaceDef => {
        const item = new vscode.CompletionItem(interfaceDef.fullName, vscode.CompletionItemKind.Interface);
        item.detail = 'CFG Interface';
        item.documentation = `CFG interface definition: ${interfaceDef.fullName}`;
        return item;
      });
  }

  /**
   * 提供表自动完成
   */
  private provideTableCompletions(namespace?: string | null): vscode.CompletionItem[] {
    const tables = this.fileIndexService.findAllTables();

    return tables
      .filter(table => !namespace || NamespaceUtils.namespaceMatches(table.namespace, namespace))
      .map(table => {
        const item = new vscode.CompletionItem(table.fullName, vscode.CompletionItemKind.Class);
        item.detail = 'CFG Table';
        item.documentation = `CFG table definition: ${table.fullName}`;
        return item;
      });
  }

  /**
   * 提供字段自动完成
   */
  private provideFieldCompletions(_context: FieldContext): vscode.CompletionItem[] {
    // 这里需要根据具体的上下文提供字段提示
    // 目前返回空数组，后续可以根据具体结构体/接口/表提供字段提示
    return [];
  }

  /**
   * 提供元数据自动完成
   */
  private provideMetadataCompletions(): vscode.CompletionItem[] {
    const metadata = [
      'nullable', 'required', 'unique', 'index',
      'description', 'default', 'min', 'max', 'length'
    ];

    return metadata.map(meta => {
      const item = new vscode.CompletionItem(meta, vscode.CompletionItemKind.Property);
      item.detail = 'CFG Metadata';
      item.documentation = `CFG metadata attribute: ${meta}`;
      return item;
    });
  }

  /**
   * 提供默认自动完成
   */
  private provideDefaultCompletions(): vscode.CompletionItem[] {
    // 返回所有类型的自动完成
    return [
      ...this.provideKeywordCompletions(),
      ...this.provideTypeCompletions(),
      ...this.provideStructCompletions(),
      ...this.provideInterfaceCompletions(),
      ...this.provideTableCompletions()
    ];
  }

  /**
   * 检查是否是关键字上下文
   */
  private isKeywordContext(linePrefix: string): boolean {
    return linePrefix === '' || linePrefix.endsWith(' ') || linePrefix.endsWith('\n');
  }

  /**
   * 检查是否是类型上下文
   */
  private isTypeContext(linePrefix: string): boolean {
    return linePrefix.endsWith(':') || linePrefix.includes('list<') || linePrefix.includes('map<');
  }

  /**
   * 获取声明上下文
   */
  private getDeclarationContext(linePrefix: string): CompletionContext | null {
    if (linePrefix.includes('struct ')) {
      return { type: 'struct' };
    }
    if (linePrefix.includes('interface ')) {
      return { type: 'interface' };
    }
    if (linePrefix.includes('table ')) {
      return { type: 'table' };
    }
    return null;
  }

  /**
   * 获取字段上下文
   */
  private getFieldContext(
    _linePrefix: string,
    _document: vscode.TextDocument,
    _position: vscode.Position
  ): FieldContext | null {
    // 这里需要更复杂的逻辑来分析当前在哪个结构体/接口/表中
    // 目前返回null，后续可以增强
    return null;
  }

  /**
   * 检查是否是元数据上下文
   */
  private isMetadataContext(linePrefix: string): boolean {
    return linePrefix.endsWith(' ') && !linePrefix.includes(':') && !linePrefix.includes('{');
  }
}

/**
 * 自动完成上下文类型定义
 */
type CompletionContext =
  | { type: 'none' }
  | { type: 'keyword' }
  | { type: 'type' }
  | { type: 'struct'; namespace?: string | null }
  | { type: 'interface'; namespace?: string | null }
  | { type: 'table'; namespace?: string | null }
  | FieldContext
  | { type: 'metadata' }
  | { type: 'default' };

/**
 * 字段上下文
 */
interface FieldContext {
  type: 'field';
  parentType: 'struct' | 'interface' | 'table';
  parentName: string;
  namespace?: string | null;
}