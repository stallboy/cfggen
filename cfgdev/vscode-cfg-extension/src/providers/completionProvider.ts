import { TextDocument, Position } from 'vscode-languageserver-textdocument';
import { CompletionItem, CompletionItemKind } from 'vscode-languageserver-protocol';
import { CfgParserService } from '../services/cfgParserService';
import { SymbolTableService } from '../services/symbolTableService';
import { Logger } from '../utils/logger';
import { COMMON_METADATA } from '../models/metadataDefinition';

export class CompletionProvider {
    private parserService: CfgParserService;
    private symbolTableService: SymbolTableService;
    private logger: Logger;

    constructor(parserService: CfgParserService, symbolTableService: SymbolTableService, logger: Logger) {
        this.parserService = parserService;
        this.symbolTableService = symbolTableService;
        this.logger = logger;
    }

    // 提供自动补全
    async provideCompletions(document: TextDocument, position: Position): Promise<CompletionItem[]> {
        this.logger.debug(`Providing completions for ${document.uri} at ${position.line}:${position.character}`);

        const completions: CompletionItem[] = [];

        try {
            const text = document.getText();
            const line = text.split('\n')[position.line] || '';
            const beforeCursor = line.substring(0, position.character);

            // 根据上下文提供不同的补全项
            if (beforeCursor.includes('->') || beforeCursor.includes('=>')) {
                // 外键补全
                completions.push(...this.provideForeignKeyCompletions());
            } else if (beforeCursor.includes('(')) {
                // 元数据补全
                completions.push(...this.provideMetadataCompletions());
            } else {
                // 类型补全
                completions.push(...this.provideTypeCompletions());
            }
        } catch (error) {
            this.logger.error('Failed to provide completions:', error);
        }

        return completions;
    }

    // 解析补全项
    resolveCompletion(item: CompletionItem): CompletionItem {
        // 为补全项添加详细信息
        return item;
    }

    // 提供类型补全
    private provideTypeCompletions(): CompletionItem[] {
        return [
            { label: 'int', kind: CompletionItemKind.Keyword, insertText: 'int' },
            { label: 'long', kind: CompletionItemKind.Keyword, insertText: 'long' },
            { label: 'float', kind: CompletionItemKind.Keyword, insertText: 'float' },
            { label: 'str', kind: CompletionItemKind.Keyword, insertText: 'str' },
            { label: 'text', kind: CompletionItemKind.Keyword, insertText: 'text' },
            { label: 'bool', kind: CompletionItemKind.Keyword, insertText: 'bool' }
        ];
    }

    // 提供外键补全
    private provideForeignKeyCompletions(): CompletionItem[] {
        // TODO: 从符号表中获取所有表定义
        return [
            { label: 'task', kind: CompletionItemKind.Class, insertText: 'task' },
            { label: 'item', kind: CompletionItemKind.Class, insertText: 'item' },
            { label: 'npc', kind: CompletionItemKind.Class, insertText: 'npc' }
        ];
    }

    // 提供元数据补全
    private provideMetadataCompletions(): CompletionItem[] {
        return COMMON_METADATA.map(metadata => ({
            label: metadata,
            kind: CompletionItemKind.Property,
            insertText: metadata
        }));
    }
}
