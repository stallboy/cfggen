import { TextDocument, Position } from 'vscode-languageserver-textdocument';
import { Hover, MarkupContent } from 'vscode-languageserver-protocol';
import { CfgParserService } from '../services/cfgParserService';
import { SymbolTableService } from '../services/symbolTableService';
import { Logger } from '../utils/logger';

export class HoverProvider {
    private parserService: CfgParserService;
    private symbolTableService: SymbolTableService;
    private logger: Logger;

    constructor(parserService: CfgParserService, symbolTableService: SymbolTableService, logger: Logger) {
        this.parserService = parserService;
        this.symbolTableService = symbolTableService;
        this.logger = logger;
    }

    // 提供悬停提示
    provideHover(document: TextDocument, position: Position): Hover | null {
        this.logger.debug(`Providing hover for ${document.uri} at ${position.line}:${position.character}`);

        try {
            const text = document.getText();
            const line = text.split('\n')[position.line] || '';
            const beforeCursor = line.substring(0, position.character);

            // TODO: 使用ANTLR解析当前位置的符号
            // 然后从符号表中获取符号信息

            // 简单的悬停提示
            const match = line.match(/(\w+):/);
            if (match) {
                const typeName = match[1];
                const definition = this.symbolTableService.findDefinition(typeName);
                if (definition) {
                    return {
                        contents: {
                            kind: 'markdown',
                            value: `### ${typeName}\n\n**Type**: \`${definition.type}\`\n\n**Namespace**: \`${definition.namespace}\``
                        },
                        range: {
                            start: { line: position.line, character: match.index || 0 },
                            end: { line: position.line, character: (match.index || 0) + typeName.length }
                        }
                    };
                } else {
                    // 基础类型提示
                    if (['int', 'long', 'float', 'str', 'text', 'bool'].includes(typeName)) {
                        return {
                            contents: {
                                kind: 'markdown',
                                value: `### ${typeName}\n\n**Type**: \`base type\``
                            },
                            range: {
                                start: { line: position.line, character: match.index || 0 },
                                end: { line: position.line, character: (match.index || 0) + typeName.length }
                            }
                        };
                    }
                }
            }
        } catch (error) {
            this.logger.error('Failed to provide hover:', error);
        }

        return null;
    }
}
