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
            const lines = text.split('\n');
            const currentLine = lines[position.line] || '';
            const beforeCursor = currentLine.substring(0, position.character);
            const afterCursor = currentLine.substring(position.character);

            // 更精确的上下文检测
            const context = this.detectContext(beforeCursor, afterCursor, lines, position);

            // 根据上下文提供不同的补全项
            if (context === 'foreign_key') {
                // 外键补全
                completions.push(...await this.provideForeignKeyCompletions());
            } else if (context === 'metadata') {
                // 元数据补全
                completions.push(...this.provideMetadataCompletions());
            } else if (context === 'type' || context === 'field_type') {
                // 类型补全（基本类型 + 自定义类型）
                completions.push(...await this.provideTypeCompletions());
            } else if (context === 'struct_field') {
                // 字段名补全
                completions.push(...this.provideFieldNameCompletions());
            }
        } catch (error) {
            this.logger.error('Failed to provide completions:', error);
        }

        return completions;
    }

    // 检测补全上下文
    private detectContext(beforeCursor: string, afterCursor: string, lines: string[], position: Position): string {
        // 检测是否在外键引用中
        if (beforeCursor.includes('->') || beforeCursor.includes('=>')) {
            return 'foreign_key';
        }

        // 检测是否在元数据中
        if (beforeCursor.includes('(') && !afterCursor.includes(')')) {
            return 'metadata';
        }

        // 检测是否在字段类型定义中
        const fieldTypePattern = /:\s*$/;
        if (fieldTypePattern.test(beforeCursor)) {
            return 'field_type';
        }

        // 检测是否在struct内部（字段名补全）
        const inStructPattern = /struct\s+\w+\s*\{$/;
        const prevLines = lines.slice(Math.max(0, position.line - 5), position.line);
        if (prevLines.some(line => inStructPattern.test(line))) {
            return 'struct_field';
        }

        // 默认类型补全
        return 'type';
    }

    // 解析补全项
    resolveCompletion(item: CompletionItem): CompletionItem {
        // 为补全项添加详细信息
        return item;
    }

    // 提供类型补全
    private async provideTypeCompletions(): Promise<CompletionItem[]> {
        const completions: CompletionItem[] = [];

        // 基本类型
        completions.push(
            { label: 'int', kind: CompletionItemKind.Keyword, insertText: 'int' },
            { label: 'long', kind: CompletionItemKind.Keyword, insertText: 'long' },
            { label: 'float', kind: CompletionItemKind.Keyword, insertText: 'float' },
            { label: 'str', kind: CompletionItemKind.Keyword, insertText: 'str' },
            { label: 'text', kind: CompletionItemKind.Keyword, insertText: 'text' },
            { label: 'bool', kind: CompletionItemKind.Keyword, insertText: 'bool' }
        );

        // 复杂类型
        completions.push(
            { label: 'list', kind: CompletionItemKind.Struct, insertText: 'list<' },
            { label: 'map', kind: CompletionItemKind.Struct, insertText: 'map<' }
        );

        try {
            // 获取符号表中的自定义类型
            const symbolTable = this.symbolTableService.getGlobalSymbolTable();
            const customTypes = symbolTable.findAll();

            for (const def of customTypes) {
                const type = def.type;
                const name = def.name;

                if (type === 'struct' || type === 'interface' || type === 'table') {
                    completions.push({
                        label: name,
                        kind: type === 'table' ? CompletionItemKind.Class : CompletionItemKind.Struct,
                        insertText: name,
                        detail: `${type} definition in ${def.namespace}`,
                        documentation: `Type: ${type}\nNamespace: ${def.namespace}`
                    });
                }
            }
        } catch (error) {
            this.logger.error('Failed to load custom types from symbol table:', error);
        }

        return completions;
    }

    // 提供外键补全
    private async provideForeignKeyCompletions(): Promise<CompletionItem[]> {
        const completions: CompletionItem[] = [];

        try {
            // 从符号表中获取所有表定义
            const symbolTable = this.symbolTableService.getGlobalSymbolTable();
            const tables = symbolTable.findAll('table');

            for (const table of tables) {
                const module = table.namespace.split('.')[0];
                const name = table.name;

                completions.push({
                    label: name,
                    kind: CompletionItemKind.Class,
                    insertText: name,
                    detail: `Table in ${module} module`,
                    documentation: `Table: ${name}\nModule: ${module}`,
                    command: {
                        title: 'Complete',
                        command: 'editor.action.triggerSuggest'
                    }
                });

                // 如果不在当前模块，添加模块前缀的补全项
                if (module) {
                    completions.push({
                        label: `${module}.${name}`,
                        kind: CompletionItemKind.Class,
                        insertText: `${module}.${name}`,
                        detail: `Table in ${module} module (with module prefix)`,
                        documentation: `Table: ${name}\nModule: ${module}`
                    });
                }
            }
        } catch (error) {
            this.logger.error('Failed to load tables from symbol table:', error);
            // 降级到默认表名
            completions.push(
                { label: 'task', kind: CompletionItemKind.Class, insertText: 'task' },
                { label: 'item', kind: CompletionItemKind.Class, insertText: 'item' },
                { label: 'npc', kind: CompletionItemKind.Class, insertText: 'npc' }
            );
        }

        return completions;
    }

    // 提供元数据补全
    private provideMetadataCompletions(): CompletionItem[] {
        return COMMON_METADATA.map(metadata => ({
            label: metadata,
            kind: CompletionItemKind.Property,
            insertText: metadata
        }));
    }

    // 提供字段名补全
    private provideFieldNameCompletions(): CompletionItem[] {
        // 返回常见的字段名建议
        return [
            { label: 'id', kind: CompletionItemKind.Field, insertText: 'id:' },
            { label: 'name', kind: CompletionItemKind.Field, insertText: 'name:' },
            { label: 'type', kind: CompletionItemKind.Field, insertText: 'type:' },
            { label: 'value', kind: CompletionItemKind.Field, insertText: 'value:' },
            { label: 'desc', kind: CompletionItemKind.Field, insertText: 'desc:' },
            { label: 'config', kind: CompletionItemKind.Field, insertText: 'config:' },
            { label: 'data', kind: CompletionItemKind.Field, insertText: 'data:' },
            { label: 'count', kind: CompletionItemKind.Field, insertText: 'count:' },
            { label: 'time', kind: CompletionItemKind.Field, insertText: 'time:' },
            { label: 'flag', kind: CompletionItemKind.Field, insertText: 'flag:' }
        ];
    }
}
