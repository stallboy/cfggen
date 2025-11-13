/**
 * 语义标记提供者
 * 使用ANTLR4解析器为CFG文件提供语义高亮
 * 语义标记自动使用VSCode内置主题
 */

import * as vscode from 'vscode';
import { CommonTokenStream } from 'antlr4ng';
import { CharStream } from 'antlr4ng';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { HighlightingVisitor } from './highlightingVisitor';
import { TOKEN_TYPE_NAMES } from './tokenTypes';
import { ErrorHandler } from '../utils/errorHandler';

export class SemanticTokensProvider implements vscode.DocumentSemanticTokensProvider {
    private legend: vscode.SemanticTokensLegend;

    constructor() {
        this.legend = new vscode.SemanticTokensLegend(
            TOKEN_TYPE_NAMES,
            []  // 修饰符
        );
    }

    /**
     * 获取语义标记图例
     */
    public getLegend(): vscode.SemanticTokensLegend {
        return this.legend;
    }

    /**
     * 为文档提供语义标记
     */
    public provideDocumentSemanticTokens(
        document: vscode.TextDocument,
        _token: vscode.CancellationToken
    ): vscode.ProviderResult<vscode.SemanticTokens> {
        try {
            // 从文档创建ANTLR4输入流
            const inputStream = CharStream.fromString(document.getText());
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);

            // 解析文档
            const parser = new CfgParser(tokenStream);
            const parseTree = parser.schema();
            const builder = new vscode.SemanticTokensBuilder(this.legend);
            const visitor = new HighlightingVisitor(builder);
            visitor.walk(parseTree);

            // 构建语义标记
            const tokens = builder.build();

            return tokens;
        } catch (error) {
            ErrorHandler.logError('SemanticTokensProvider.provideDocumentSemanticTokens', error);
            return new vscode.SemanticTokens(new Uint32Array(0), undefined);
        }
    }
}
