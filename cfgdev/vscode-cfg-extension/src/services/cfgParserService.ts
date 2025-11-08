import { TextDocument } from 'vscode-languageserver-textdocument';
import { CharStreams, CommonTokenStream } from 'antlr4ts';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { ConfigFile, ParseError, TextRange, Position } from '../models/configFile';
import { StructDefinition } from '../models/structDefinition';
import { InterfaceDefinition } from '../models/interfaceDefinition';
import { TableDefinition } from '../models/tableDefinition';
import { Definition } from '../models/definition';
import { Logger } from '../utils/logger';

export class CfgParserService {
    private logger: Logger;

    constructor(logger: Logger) {
        this.logger = logger;
    }

    // 解析文档
    async parseDocument(document: TextDocument): Promise<ConfigFile | null> {
        const uri = document.uri;
        const text = document.getText();
        this.logger.debug(`Parsing document: ${uri}`);

        try {
            // 创建词法分析器
            const inputStream = CharStreams.fromString(text);
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);

            // 创建语法分析器
            const parser = new CfgParser(tokenStream);
            const parseTree = parser.schema();

            // 构建模型
            const configFile = this.buildModel(document, parseTree);

            this.logger.debug(`Parsed document successfully: ${uri}`);
            return configFile;
        } catch (error) {
            this.logger.error(`Failed to parse document ${uri}:`, error);
            return null;
        }
    }

    // 构建数据模型
    private buildModel(document: TextDocument, parseTree: any): ConfigFile {
        // TODO: 使用ANTLR监听器遍历解析树并构建模型
        // 这是一个简化实现

        const filePath = document.uri;
        const moduleName = this.extractModuleName(filePath);

        return {
            path: filePath,
            moduleName,
            definitions: [],
            symbols: new (require('../models/symbolTable').SymbolTable)(),
            errors: [],
            lastModified: Date.now()
        };
    }

    // 提取模块名
    private extractModuleName(uri: string): string {
        // 简化实现：从文件路径中提取模块名
        const parts = uri.split('/');
        const fileName = parts[parts.length - 1];
        return fileName.replace('.cfg', '');
    }

    // 创建位置信息
    private createPosition(line: number, character: number): Position {
        return { line, character };
    }

    // 创建文本范围
    private createRange(startLine: number, startChar: number, endLine: number, endChar: number): TextRange {
        return {
            start: this.createPosition(startLine, startChar),
            end: this.createPosition(endLine, endChar)
        };
    }

    // 获取文档内容
    private getText(document: TextDocument, range: TextRange): string {
        const lines = document.getText().split('\n');
        const startLine = range.start.line;
        const endLine = range.end.line;

        if (startLine === endLine) {
            const line = lines[startLine];
            return line.substring(range.start.character, range.end.character);
        }

        // 多行文本（简化处理）
        return lines.slice(startLine, endLine + 1).join('\n');
    }
}
