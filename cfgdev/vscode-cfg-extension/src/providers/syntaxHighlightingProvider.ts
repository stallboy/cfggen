import { TextDocument, Position } from 'vscode-languageserver-textdocument';
import { DocumentHighlight, Range } from 'vscode-languageserver-protocol';
import { CharStreams, CommonTokenStream } from 'antlr4ts';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { ParseTreeWalker } from 'antlr4ts/tree/ParseTreeWalker';
import { CfgHighlightListener } from '../grammar/cfgHighlightListener';
import { CfgParserService } from '../services/cfgParserService';
import { Logger } from '../utils/logger';
import { getThemeConfig, ThemeConfig } from './themeConfig';

export class SyntaxHighlightingProvider {
    private parserService: CfgParserService;
    private logger: Logger;
    private currentTheme: ThemeConfig;

    constructor(parserService: CfgParserService, logger: Logger) {
        this.parserService = parserService;
        this.logger = logger;
        this.currentTheme = getThemeConfig('chineseClassical'); // 默认使用中国古典色
    }

    // 提供语法高亮
    provideHighlights(document: TextDocument, position: Position): DocumentHighlight[] {
        this.logger.debug(`Providing highlights for ${document.uri} at ${position.line}:${position.character}`);

        try {
            const text = document.getText();
            const inputStream = CharStreams.fromString(text);
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);
            const parser = new CfgParser(tokenStream);

            // 生成解析树
            const parseTree = parser.schema();

            // 使用ANTLR监听器遍历并收集高亮信息
            const listener = new CfgHighlightListener(document, this.currentTheme);
            ParseTreeWalker.DEFAULT.walk(listener, parseTree);

            this.logger.debug(`Generated ${listener.getHighlights().length} highlights`);

            return listener.getHighlights();
        } catch (error) {
            this.logger.error('Failed to provide highlights:', error);
            return [];
        }
    }

    // 设置主题
    setTheme(themeName: 'default' | 'chineseClassical'): void {
        this.currentTheme = getThemeConfig(themeName);
        this.logger.info(`Theme set to: ${themeName}`);
    }

    // 获取当前主题
    getCurrentTheme(): ThemeConfig {
        return this.currentTheme;
    }
}
