import * as vscode from 'vscode';
import { Logger } from '../utils/logger';
import { PerformanceMonitor } from '../utils/performance';

/**
 * CFG语法高亮提供者
 * 基于ANTLR4语法定义提供语法高亮
 */
export class CfgSyntaxHighlightingProvider implements vscode.DocumentSemanticTokensProvider {
  private logger: Logger;
  private performance: PerformanceMonitor;

  constructor() {
    this.logger = Logger.getInstance();
    this.performance = PerformanceMonitor.getInstance();
  }

  /**
   * 提供语义标记
   */
  public async provideDocumentSemanticTokens(
    document: vscode.TextDocument,
    _token: vscode.CancellationToken
  ): Promise<vscode.SemanticTokens> {
    const endMeasurement = this.performance.startMeasurement('syntaxHighlighting');

    try {
      const builder = new vscode.SemanticTokensBuilder();
      const text = document.getText();
      const lines = text.split('\n');

      for (let lineIndex = 0; lineIndex < lines.length; lineIndex++) {
        const line = lines[lineIndex];
        this.highlightLine(line, lineIndex, builder);
      }

      const tokens = builder.build();
      endMeasurement();
      return tokens;
    } catch (error) {
      endMeasurement();
      this.logger.error('Syntax highlighting error:', error);
      return new vscode.SemanticTokensBuilder().build();
    }
  }

  /**
   * 高亮单行
   */
  private highlightLine(
    line: string,
    lineIndex: number,
    builder: vscode.SemanticTokensBuilder
  ): void {
    const tokens = this.tokenizeLine(line);

    for (const token of tokens) {
      const tokenType = this.getTokenType(token.value);
      if (tokenType) {
        builder.push(
          lineIndex,
          token.start,
          token.length,
          tokenType,
          0
        );
      }
    }
  }

  /**
   * 分词处理
   */
  private tokenizeLine(line: string): Array<{ value: string; start: number; length: number }> {
    const tokens: Array<{ value: string; start: number; length: number }> = [];
    let currentToken = '';
    let currentStart = 0;
    let inString = false;
    let inComment = false;

    for (let i = 0; i < line.length; i++) {
      const char = line[i];

      // 处理字符串
      if (char === "'" && !inComment) {
        if (inString) {
          // 字符串结束
          if (currentToken) {
            tokens.push({
              value: currentToken + char,
              start: currentStart,
              length: currentToken.length + 1
            });
          }
          currentToken = '';
          inString = false;
        } else {
          // 字符串开始
          if (currentToken) {
            tokens.push({
              value: currentToken,
              start: currentStart,
              length: currentToken.length
            });
          }
          currentToken = char;
          currentStart = i;
          inString = true;
        }
        continue;
      }

      // 处理注释
      if (char === '#' && !inString) {
        if (!inComment) {
          // 注释开始
          if (currentToken) {
            tokens.push({
              value: currentToken,
              start: currentStart,
              length: currentToken.length
            });
          }
          currentToken = line.substring(i);
          currentStart = i;
          inComment = true;
          break; // 注释到行尾
        }
      }

      // 处理分隔符
      if (!inString && !inComment && this.isSeparator(char)) {
        if (currentToken) {
          tokens.push({
            value: currentToken,
            start: currentStart,
            length: currentToken.length
          });
        }
        if (!this.isWhitespace(char)) {
          tokens.push({
            value: char,
            start: i,
            length: 1
          });
        }
        currentToken = '';
        currentStart = i + 1;
        continue;
      }

      // 累积字符
      if (!this.isWhitespace(char) || inString || inComment) {
        if (currentToken === '') {
          currentStart = i;
        }
        currentToken += char;
      } else {
        if (currentToken) {
          tokens.push({
            value: currentToken,
            start: currentStart,
            length: currentToken.length
          });
          currentToken = '';
        }
        currentStart = i + 1;
      }
    }

    // 处理最后一个token
    if (currentToken) {
      tokens.push({
        value: currentToken,
        start: currentStart,
        length: currentToken.length
      });
    }

    return tokens;
  }

  /**
   * 获取token类型
   */
  private getTokenType(token: string): number | undefined {
    // 关键字
    if (this.isKeyword(token)) {
      return 0; // keyword
    }

    // 类型
    if (this.isType(token)) {
      return 1; // type
    }

    // 字符串
    if (token.startsWith("'") && token.endsWith("'")) {
      return 2; // string
    }

    // 数字
    if (this.isNumber(token)) {
      return 3; // number
    }

    // 注释
    if (token.startsWith('#')) {
      return 4; // comment
    }

    // 操作符
    if (this.isOperator(token)) {
      return 5; // operator
    }

    // 元数据
    if (token.startsWith('-')) {
      return 6; // decorator
    }

    return undefined;
  }

  /**
   * 检查是否是关键字
   */
  private isKeyword(token: string): boolean {
    const keywords = [
      'struct', 'interface', 'table', 'key', 'ref', 'nullable',
      'required', 'unique', 'index', 'foreign', 'primary'
    ];
    return keywords.includes(token.toLowerCase());
  }

  /**
   * 检查是否是类型
   */
  private isType(token: string): boolean {
    const types = [
      'bool', 'int', 'long', 'float', 'str', 'text',
      'list', 'map'
    ];
    return types.includes(token.toLowerCase());
  }

  /**
   * 检查是否是数字
   */
  private isNumber(token: string): boolean {
    return /^-?\d+(\.\d+)?$/.test(token);
  }

  /**
   * 检查是否是操作符
   */
  private isOperator(token: string): boolean {
    const operators = ['->', '=>', '=', ':', '<', '>', ',', '.', '{', '}', '(', ')'];
    return operators.includes(token);
  }

  /**
   * 检查是否是分隔符
   */
  private isSeparator(char: string): boolean {
    return /[\s\[\]{}():,.<>=\-]/.test(char);
  }

  /**
   * 检查是否是空白字符
   */
  private isWhitespace(char: string): boolean {
    return /\s/.test(char);
  }
}

/**
 * 语法高亮Legend定义
 */
export const cfgTokenLegend = new vscode.SemanticTokensLegend([
  'keyword',
  'type',
  'string',
  'number',
  'comment',
  'operator',
  'decorator'
]);