import { TextDocument, Position } from 'vscode-languageserver-textdocument';
import { Location, ReferenceContext } from 'vscode-languageserver-protocol';
import { CfgParserService } from '../services/cfgParserService';
import { SymbolTableService } from '../services/symbolTableService';
import { Logger } from '../utils/logger';

export class ReferenceProvider {
    private parserService: CfgParserService;
    private symbolTableService: SymbolTableService;
    private logger: Logger;

    constructor(parserService: CfgParserService, symbolTableService: SymbolTableService, logger: Logger) {
        this.parserService = parserService;
        this.symbolTableService = symbolTableService;
        this.logger = logger;
    }

    // 提供引用查找
    provideReferences(document: TextDocument, position: Position, context: ReferenceContext): Location[] | null {
        this.logger.debug(`Providing references for ${document.uri} at ${position.line}:${position.character}`);

        try {
            const text = document.getText();
            const line = text.split('\n')[position.line] || '';

            // TODO: 使用ANTLR解析当前位置的符号
            // 然后在所有文档中查找引用

            // 简单的引用查找（当前文档）
            const wordMatch = line.match(/\b(\w+)\b/);
            if (wordMatch) {
                const symbolName = wordMatch[1];
                const definition = this.symbolTableService.findDefinition(symbolName);
                if (definition) {
                    // TODO: 搜索所有引用位置
                    return [{
                        uri: document.uri,
                        range: {
                            start: { line: position.line, character: wordMatch.index || 0 },
                            end: { line: position.line, character: (wordMatch.index || 0) + symbolName.length }
                        }
                    }];
                }
            }
        } catch (error) {
            this.logger.error('Failed to provide references:', error);
        }

        return [];
    }
}
