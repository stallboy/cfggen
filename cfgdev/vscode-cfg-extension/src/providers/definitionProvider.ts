import * as vscode from 'vscode';
import { FileIndexService } from '../services/fileIndexService';
import { Logger } from '../utils/logger';
import { PerformanceMonitor } from '../utils/performance';

/**
 * CFG定义跳转提供者
 * 提供结构体、接口、表定义的跳转功能
 */
export class CfgDefinitionProvider implements vscode.DefinitionProvider {
  private fileIndexService: FileIndexService;
  private logger: Logger;
  private performance: PerformanceMonitor;

  constructor() {
    this.fileIndexService = FileIndexService.getInstance();
    this.logger = Logger.getInstance();
    this.performance = PerformanceMonitor.getInstance();
  }

  /**
   * 提供定义位置
   */
  public async provideDefinition(
    document: vscode.TextDocument,
    position: vscode.Position,
    _token: vscode.CancellationToken
  ): Promise<vscode.Definition | undefined> {
    const endMeasurement = this.performance.startMeasurement('definition');

    try {
      const wordRange = document.getWordRangeAtPosition(position);
      if (!wordRange) {
        endMeasurement();
        return undefined;
      }

      const word = document.getText(wordRange);
      const lineText = document.lineAt(position.line).text;

      // 分析上下文确定要查找的定义类型
      const context = this.analyzeContext(document, position, word, lineText);

      if (!context) {
        endMeasurement();
        return undefined;
      }

      // 查找定义
      const definition = await this.findDefinition(context, document, position);

      endMeasurement();
      return definition;
    } catch (error) {
      endMeasurement();
      this.logger.error('Definition provider error:', error);
      return undefined;
    }
  }

  /**
   * 分析上下文
   */
  private analyzeContext(
    document: vscode.TextDocument,
    position: vscode.Position,
    word: string,
    lineText: string
  ): DefinitionContext | undefined {
    // 检查是否在注释或字符串中
    if (this.isInCommentOrString(document, position)) {
      return undefined;
    }

    const linePrefix = lineText.substring(0, position.character);
    const lineSuffix = lineText.substring(position.character);

    // 检查是否是类型引用
    if (this.isTypeReference(linePrefix, lineSuffix)) {
      return {
        type: 'type',
        name: word,
        namespace: this.getCurrentNamespace(document, position)
      };
    }

    // 检查是否是外键引用
    const foreignKeyContext = this.getForeignKeyContext(linePrefix, word);
    if (foreignKeyContext) {
      return foreignKeyContext;
    }

    // 检查是否是结构体/接口/表声明
    const declarationContext = this.getDeclarationContext(linePrefix, word);
    if (declarationContext) {
      return declarationContext;
    }

    return undefined;
  }

  /**
   * 查找定义
   */
  private async findDefinition(
    context: DefinitionContext,
    _document: vscode.TextDocument,
    _position: vscode.Position
  ): Promise<vscode.Definition | undefined> {
    switch (context.type) {
      case 'type':
        return this.findTypeDefinition(context.name, context.namespace);
      case 'foreignKey':
        return this.findForeignKeyDefinition(context.targetTable, context.targetKey);
      case 'declaration':
        return this.findDeclarationDefinition(context.declarationType, context.name, context.namespace);
      default:
        return undefined;
    }
  }

  /**
   * 查找类型定义
   */
  private async findTypeDefinition(
    typeName: string,
    currentNamespace?: string | null
  ): Promise<vscode.Definition | undefined> {
    // 尝试查找结构体
    let definition = this.fileIndexService.findStruct(typeName, currentNamespace);
    if (definition) {
      return this.createLocation(definition);
    }

    // 尝试查找接口
    const interfaceDefinition = this.fileIndexService.findInterface(typeName, currentNamespace);
    if (interfaceDefinition) {
      return this.createLocation(interfaceDefinition);
    }

    // 尝试查找表
    definition = this.fileIndexService.findTable(typeName, currentNamespace);
    if (definition) {
      return this.createLocation(definition);
    }

    // 如果当前命名空间没找到，尝试全局查找
    if (currentNamespace) {
      return this.findTypeDefinition(typeName, null);
    }

    return undefined;
  }

  /**
   * 查找外键定义
   */
  private async findForeignKeyDefinition(
    targetTable: string,
    targetKey?: string
  ): Promise<vscode.Definition | undefined> {
    // 查找目标表
    const tableDefinition = this.fileIndexService.findTable(targetTable);
    if (!tableDefinition) {
      return undefined;
    }

    // 如果指定了目标键，查找具体字段
    if (targetKey && tableDefinition) {
      const field = tableDefinition.getFieldByName(targetKey);
      if (field) {
        return this.createLocation(field);
      }
    }

    // 返回表定义
    return this.createLocation(tableDefinition);
  }

  /**
   * 查找声明定义
   */
  private async findDeclarationDefinition(
    declarationType: 'struct' | 'interface' | 'table',
    name: string,
    currentNamespace?: string | null
  ): Promise<vscode.Definition | undefined> {
    switch (declarationType) {
      case 'struct':
        return this.createLocation(this.fileIndexService.findStruct(name, currentNamespace));
      case 'interface':
        return this.createLocation(this.fileIndexService.findInterface(name, currentNamespace));
      case 'table':
        return this.createLocation(this.fileIndexService.findTable(name, currentNamespace));
      default:
        return undefined;
    }
  }

  /**
   * 创建位置信息
   */
  private createLocation(definition: any): vscode.Location | undefined {
    if (!definition || !definition.position) {
      return undefined;
    }

    const uri = definition.uri || vscode.Uri.file(definition.filePath);
    const range = new vscode.Range(
      new vscode.Position(definition.position.start.line, definition.position.start.column),
      new vscode.Position(definition.position.end.line, definition.position.end.column)
    );

    return new vscode.Location(uri, range);
  }

  /**
   * 检查是否在注释或字符串中
   */
  private isInCommentOrString(document: vscode.TextDocument, position: vscode.Position): boolean {
    const textUntilPosition = document.getText(new vscode.Range(new vscode.Position(0, 0), position));

    // 检查注释
    const lines = textUntilPosition.split('\n');
    const currentLine = lines[lines.length - 1];
    if (currentLine.includes('#')) {
      return true;
    }

    // 检查字符串
    const singleQuoteCount = (textUntilPosition.match(/'/g) || []).length;
    return singleQuoteCount % 2 === 1;
  }

  /**
   * 检查是否是类型引用
   */
  private isTypeReference(linePrefix: string, lineSuffix: string): boolean {
    // 类型引用通常出现在字段声明、外键引用等位置
    return (
      linePrefix.endsWith(':') ||
      linePrefix.includes('list<') ||
      linePrefix.includes('map<') ||
      lineSuffix.startsWith('->') ||
      lineSuffix.startsWith('=>')
    );
  }

  /**
   * 获取外键上下文
   */
  private getForeignKeyContext(linePrefix: string, word: string): ForeignKeyContext | undefined {
    // 检查是否是外键引用 (-> 或 =>)
    if (linePrefix.includes('->') || linePrefix.includes('=>')) {
      const parts = linePrefix.split(/[->=>]/);
      if (parts.length >= 2) {
        const targetPart = parts[parts.length - 1].trim();
        const targetParts = targetPart.split('.');

        if (targetParts.length === 1) {
          return {
            type: 'foreignKey',
            targetTable: word
          };
        } else if (targetParts.length === 2) {
          return {
            type: 'foreignKey',
            targetTable: targetParts[0],
            targetKey: targetParts[1]
          };
        }
      }
    }

    return undefined;
  }

  /**
   * 获取声明上下文
   */
  private getDeclarationContext(linePrefix: string, word: string): DeclarationContext | undefined {
    if (linePrefix.includes('struct ')) {
      return {
        type: 'declaration',
        declarationType: 'struct',
        name: word
      };
    }
    if (linePrefix.includes('interface ')) {
      return {
        type: 'declaration',
        declarationType: 'interface',
        name: word
      };
    }
    if (linePrefix.includes('table ')) {
      return {
        type: 'declaration',
        declarationType: 'table',
        name: word
      };
    }
    return undefined;
  }

  /**
   * 获取当前命名空间
   */
  private getCurrentNamespace(_document: vscode.TextDocument, _position: vscode.Position): string | null {
    // 这里需要分析当前文件的结构来确定当前命名空间
    // 目前返回null，后续可以增强
    return null;
  }
}

/**
 * 定义上下文类型定义
 */
type DefinitionContext = TypeContext | ForeignKeyContext | DeclarationContext;

interface TypeContext {
  type: 'type';
  name: string;
  namespace?: string | null;
}

interface ForeignKeyContext {
  type: 'foreignKey';
  targetTable: string;
  targetKey?: string;
}

interface DeclarationContext {
  type: 'declaration';
  declarationType: 'struct' | 'interface' | 'table';
  name: string;
  namespace?: string | null;
}