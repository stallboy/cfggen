import * as vscode from 'vscode';
import { CharStreams, CommonTokenStream } from 'antlr4ts';
import { CfgLexer } from './cfgLexer';
import { CfgParser } from './cfgParser';
// import { CfgVisitor } from './cfgVisitor';
import { Logger } from '../utils/logger';
import { PerformanceMonitor } from '../utils/performance';

export interface ParseResult {
  ast: any; // ANTLR4 AST
  errors: ParseError[];
  success: boolean;
}

export interface ParseError {
  line: number;
  column: number;
  message: string;
  severity: 'error' | 'warning';
}

export class ANTLRParser {
  private logger = Logger.getInstance();
  private performance = PerformanceMonitor.getInstance();

  /**
   * 解析CFG文件内容
   * @param content 文件内容
   * @param uri 文件URI（用于错误报告）
   * @returns 解析结果
   */
  public parse(content: string, uri?: vscode.Uri): ParseResult {
    const endMeasurement = this.performance.startMeasurement('parse');

    try {
      const inputStream = CharStreams.fromString(content);
      const lexer = new CfgLexer(inputStream);
      const tokenStream = new CommonTokenStream(lexer);
      const parser = new CfgParser(tokenStream);

      // 移除默认错误监听器，使用自定义错误处理
      parser.removeErrorListeners();

      const errorListener = new CustomErrorListener();
      parser.addErrorListener(errorListener);

      // 解析整个schema
      const tree = parser.schema();

      // 构建AST
      // const visitor = new CfgVisitor();
      // const ast = visitor.visit(tree);
      const ast = tree; // 暂时直接返回解析树

      const errors = errorListener.getErrors();
      const success = errors.length === 0;

      endMeasurement();

      this.logger.debug(`Parsed file ${uri?.toString() || 'unknown'}: ${success ? 'success' : 'failed with errors'}`);

      return {
        ast,
        errors,
        success
      };
    } catch (error) {
      endMeasurement();
      this.logger.error(`Parse error for ${uri?.toString() || 'unknown'}:`, error);

      return {
        ast: null,
        errors: [{
          line: 1,
          column: 1,
          message: `Parse error: ${error instanceof Error ? error.message : String(error)}`,
          severity: 'error'
        }],
        success: false
      };
    }
  }

  /**
   * 验证CFG语法
   * @param content 文件内容
   * @returns 语法错误列表
   */
  public validateSyntax(content: string): ParseError[] {
    const result = this.parse(content);
    return result.errors;
  }

  /**
   * 快速检查语法是否有效
   * @param content 文件内容
   * @returns 是否语法有效
   */
  public isValidSyntax(content: string): boolean {
    const result = this.parse(content);
    return result.success;
  }
}

class CustomErrorListener {
  private errors: ParseError[] = [];

  constructor() {}

  public syntaxError(
    _recognizer: any,
    _offendingSymbol: any,
    line: number,
    charPositionInLine: number,
    msg: string,
    _e: any
  ): void {
    this.errors.push({
      line: line - 1, // ANTLR4行号从1开始，VSCode从0开始
      column: charPositionInLine,
      message: msg,
      severity: 'error'
    });
  }

  public getErrors(): ParseError[] {
    return this.errors;
  }
}