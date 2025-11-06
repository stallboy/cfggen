import * as vscode from 'vscode';
import { FileIndexService } from '../services/fileIndexService';
import { Logger } from '../utils/logger';
import { PerformanceMonitor } from '../utils/performance';

/**
 * CFG外键跳转提供者
 * 专门处理外键引用跳转 (-> 和 =>)
 */
export class CfgForeignKeyProvider implements vscode.DefinitionProvider, vscode.ReferenceProvider, vscode.HoverProvider {
  private fileIndexService: FileIndexService;
  private logger: Logger;
  private performance: PerformanceMonitor;

  constructor() {
    this.fileIndexService = FileIndexService.getInstance();
    this.logger = Logger.getInstance();
    this.performance = PerformanceMonitor.getInstance();
  }

  /**
   * 提供外键定义位置
   */
  public async provideDefinition(
    document: vscode.TextDocument,
    position: vscode.Position,
    _token: vscode.CancellationToken
  ): Promise<vscode.Definition | undefined> {
    const endMeasurement = this.performance.startMeasurement('foreignKeyDefinition');

    try {
      const lineText = document.lineAt(position.line).text;
      const linePrefix = lineText.substring(0, position.character);

      // 检查是否是外键引用位置
      const foreignKeyInfo = this.parseForeignKey(linePrefix, lineText, position);
      if (!foreignKeyInfo) {
        endMeasurement();
        return undefined;
      }

      // 查找外键目标
      const definition = await this.findForeignKeyTarget(foreignKeyInfo);

      endMeasurement();
      return definition;
    } catch (error) {
      endMeasurement();
      this.logger.error('Foreign key provider error:', error);
      return undefined;
    }
  }

  /**
   * 解析外键引用
   */
  private parseForeignKey(
    linePrefix: string,
    lineText: string,
    position: vscode.Position
  ): ForeignKeyInfo | undefined {
    // 检查是否在外键引用符号附近
    const singleRefIndex = linePrefix.lastIndexOf('->');
    const listRefIndex = linePrefix.lastIndexOf('=>');
    const refIndex = Math.max(singleRefIndex, listRefIndex);

    if (refIndex === -1) {
      return undefined;
    }

    const refType = singleRefIndex > listRefIndex ? 'single' : 'list';
    const refSymbol = refType === 'single' ? '->' : '=>';

    // 提取目标表和键
    const targetPart = lineText.substring(refIndex + refSymbol.length).trim();
    const targetMatch = targetPart.match(/^([\w.]+)(?:\.(\w+))?/);

    if (!targetMatch) {
      return undefined;
    }

    const targetTable = targetMatch[1];
    const targetKey = targetMatch[2];

    // 检查光标是否在目标表或键上
    const targetStart = refIndex + refSymbol.length + targetPart.indexOf(targetMatch[0]);
    const targetEnd = targetStart + targetMatch[0].length;

    if (position.character >= targetStart && position.character <= targetEnd) {
      return {
        refType,
        targetTable,
        targetKey,
        targetRange: new vscode.Range(
          new vscode.Position(position.line, targetStart),
          new vscode.Position(position.line, targetEnd)
        )
      };
    }

    return undefined;
  }

  /**
   * 查找外键目标
   */
  private async findForeignKeyTarget(
    foreignKeyInfo: ForeignKeyInfo
  ): Promise<vscode.Definition | undefined> {
    const { targetTable, targetKey } = foreignKeyInfo;

    // 查找目标表
    const tableDefinition = this.fileIndexService.findTable(targetTable);
    if (!tableDefinition) {
      this.logger.debug(`Target table not found: ${targetTable}`);
      return undefined;
    }

    // 如果指定了目标键，查找具体字段
    if (targetKey) {
      const field = tableDefinition.getFieldByName(targetKey);
      if (field) {
        return this.createLocation(field);
      } else {
        this.logger.debug(`Target field not found: ${targetTable}.${targetKey}`);
      }
    }

    // 返回表定义
    return this.createLocation(tableDefinition);
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
   * 查找外键引用
   * 查找引用特定表或字段的所有外键
   */
  public async provideReferences(
    document: vscode.TextDocument,
    position: vscode.Position,
    _context: vscode.ReferenceContext
  ): Promise<vscode.Location[]> {
    const endMeasurement = this.performance.startMeasurement('foreignKeyReferences');

    try {
      const wordRange = document.getWordRangeAtPosition(position);
      if (!wordRange) {
        endMeasurement();
        return [];
      }

      const word = document.getText(wordRange);
      const references: vscode.Location[] = [];

      // 获取所有文件
      const allFiles = Array.from(this.fileIndexService['index'].files.values());

      for (const file of allFiles) {
        // 查找引用当前表或字段的外键
        const fileReferences = this.findReferencesInFile(file, word);
        references.push(...fileReferences);
      }

      endMeasurement();
      return references;
    } catch (error) {
      endMeasurement();
      this.logger.error('Foreign key references error:', error);
      return [];
    }
  }

  /**
   * 在文件中查找引用
   */
  private findReferencesInFile(file: any, target: string): vscode.Location[] {
    const references: vscode.Location[] = [];

    // 检查结构体中的外键引用
    for (const struct of file.structs || []) {
      for (const foreignKey of struct.foreignKeys || []) {
        if (foreignKey.targetTable === target ||
            foreignKey.targetKey.includes(target)) {
          const location = this.createLocation(foreignKey);
          if (location) {
            references.push(location);
          }
        }
      }
    }

    // 检查表中的外键引用
    for (const table of file.tables || []) {
      for (const foreignKey of table.foreignKeys || []) {
        if (foreignKey.targetTable === target ||
            foreignKey.targetKey.includes(target)) {
          const location = this.createLocation(foreignKey);
          if (location) {
            references.push(location);
          }
        }
      }
    }

    return references;
  }

  /**
   * 提供悬停信息
   */
  public async provideHover(
    document: vscode.TextDocument,
    position: vscode.Position,
    _token: vscode.CancellationToken
  ): Promise<vscode.Hover | undefined> {
    const endMeasurement = this.performance.startMeasurement('foreignKeyInfo');

    try {
      const lineText = document.lineAt(position.line).text;
      const linePrefix = lineText.substring(0, position.character);

      // 检查是否是外键引用位置
      const foreignKeyInfo = this.parseForeignKey(linePrefix, lineText, position);
      if (!foreignKeyInfo) {
        endMeasurement();
        return undefined;
      }

      const { refType, targetTable, targetKey } = foreignKeyInfo;

      // 查找目标信息
      const tableDefinition = this.fileIndexService.findTable(targetTable);
      if (!tableDefinition) {
        endMeasurement();
        return undefined;
      }

      let hoverText = `**Foreign Key Reference**\n\n`;
      hoverText += `**Type:** ${refType === 'single' ? 'Single Reference (->)' : 'List Reference (=>)'}\n`;
      hoverText += `**Target Table:** ${targetTable}\n`;

      if (targetKey) {
        const field = tableDefinition.getFieldByName(targetKey);
        if (field) {
          hoverText += `**Target Field:** ${targetKey} (${field.fieldType.toString()})\n`;
        } else {
          hoverText += `**Target Field:** ${targetKey} (not found)\n`;
        }
      }

      hoverText += `\n*Click to navigate to definition*`;

      const hover = new vscode.Hover(hoverText, foreignKeyInfo.targetRange);
      endMeasurement();
      return hover;
    } catch (error) {
      endMeasurement();
      this.logger.error('Foreign key info error:', error);
      return undefined;
    }
  }
}

/**
 * 外键信息接口
 */
interface ForeignKeyInfo {
  refType: 'single' | 'list';
  targetTable: string;
  targetKey?: string;
  targetRange: vscode.Range;
}