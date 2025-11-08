import { TextDocument, Position } from 'vscode-languageserver-textdocument';
import { Location } from 'vscode-languageserver-protocol';
import { CharStreams, CommonTokenStream } from 'antlr4ts';
import { CfgLexer } from '../grammar/CfgLexer';
import { CfgParser } from '../grammar/CfgParser';
import { CfgListener } from '../grammar/CfgListener';
import { ParseTreeWalker } from 'antlr4ts/tree/ParseTreeWalker';
import { CfgParserService } from '../services/cfgParserService';
import { SymbolTableService } from '../services/symbolTableService';
import { ModuleResolverService } from '../services/moduleResolverService';
import { Logger } from '../utils/logger';

// 导入ANTLR上下文类型
import {
    Struct_declContext,
    Interface_declContext,
    Table_declContext,
    Field_declContext,
    Foreign_declContext,
    RefContext,
    Ns_identContext,
    IdentifierContext
} from '../grammar/CfgParser';

export class DefinitionProvider {
    private parserService: CfgParserService;
    private symbolTableService: SymbolTableService;
    private moduleResolver: ModuleResolverService;
    private logger: Logger;

    constructor(
        parserService: CfgParserService,
        symbolTableService: SymbolTableService,
        moduleResolver: ModuleResolverService,
        logger: Logger
    ) {
        this.parserService = parserService;
        this.symbolTableService = symbolTableService;
        this.moduleResolver = moduleResolver;
        this.logger = logger;
    }

    /**
     * 提供跳转到定义功能
     * 根据LSP 3.17规范，支持以下场景：
     * 1. 类型定义（struct/interface/table）
     * 2. 外键引用 (->, =>)
     * 3. 跨模块引用
     * 4. 带键外键引用
     */
    provideDefinitions(document: TextDocument, position: Position): Location[] | null {
        this.logger.debug(`Providing definitions for ${document.uri} at ${position.line}:${position.character}`);

        try {
            // 解析文档获取符号表
            const text = document.getText();
            const inputStream = CharStreams.fromString(text);
            const lexer = new CfgLexer(inputStream);
            const tokenStream = new CommonTokenStream(lexer);
            const parser = new CfgParser(tokenStream);

            // 生成解析树
            const parseTree = parser.schema();

            // 使用监听器查找光标位置的符号
            const definitionListener = new DefinitionSearchListener(
                document,
                position,
                this.symbolTableService,
                this.moduleResolver,
                this.logger
            );

            ParseTreeWalker.DEFAULT.walk(definitionListener, parseTree);

            const locations = definitionListener.getDefinitions();
            this.logger.debug(`Found ${locations.length} definitions`);

            return locations.length > 0 ? locations : null;
        } catch (error) {
            this.logger.error('Failed to provide definitions:', error);
            return null;
        }
    }
}

/**
 * 定义搜索监听器
 * 遍历解析树，查找光标位置处的符号引用
 */
class DefinitionSearchListener implements CfgListener {
    private definitions: Location[] = [];
    private document: TextDocument;
    private position: Position;
    private symbolTableService: SymbolTableService;
    private moduleResolver: ModuleResolverService;
    private logger: Logger;

    constructor(
        document: TextDocument,
        position: Position,
        symbolTableService: SymbolTableService,
        moduleResolver: ModuleResolverService,
        logger: Logger
    ) {
        this.document = document;
        this.position = position;
        this.symbolTableService = symbolTableService;
        this.moduleResolver = moduleResolver;
        this.logger = logger;
    }

    getDefinitions(): Location[] {
        return this.definitions;
    }

    // ========== 1. 查找struct/interface/table定义 ==========
    enterStructDecl(ctx: Struct_declContext) {
        const name = ctx.ns_ident();
        if (this.isPositionInContext(name)) {
            const definition = this.symbolTableService.findDefinition(this.getText(name));
            if (definition) {
                this.definitions.push({
                    uri: this.document.uri,
                    range: this.getRange(name.start, name.stop)
                });
            }
        }
    }

    enterInterfaceDecl(ctx: Interface_declContext) {
        const name = ctx.ns_ident();
        if (this.isPositionInContext(name)) {
            const definition = this.symbolTableService.findDefinition(this.getText(name));
            if (definition) {
                this.definitions.push({
                    uri: this.document.uri,
                    range: this.getRange(name.start, name.stop)
                });
            }
        }
    }

    enterTableDecl(ctx: Table_declContext) {
        // T024: 查找表定义
        // 语法: table TableName[key] { ... }
        // 注意：key是表名的一部分，在ns_ident之后

        const name = ctx.ns_ident();
        if (this.isPositionInContext(name)) {
            const definition = this.symbolTableService.findDefinition(this.getText(name));
            if (definition) {
                this.definitions.push({
                    uri: this.document.uri,
                    range: this.getRange(name.start, name.stop)
                });
            }
        }

        // T024: 也检查key部分（如果光标在[key]上）
        const key = ctx.key();
        if (key && this.isPositionInContext(key)) {
            // 如果光标在key上，可以跳转到key字段的定义
            this.logger.debug(`Table key detected at position: ${this.getText(key)}`);
            // TODO: 查找key字段在表中的定义
        }
    }

    // ========== 2. 查找字段类型定义 ==========
    enterFieldDecl(ctx: Field_declContext) {
        // 检查光标是否在类型名称上
        const type = ctx.type_();
        if (type && this.isPositionInContext(type)) {
            // 获取类型名称（可能是基本类型或自定义类型）
            const typeName = this.extractTypeName(type);
            if (typeName && !this.isBaseType(typeName)) {
                // 查找自定义类型定义
                const moduleInfo = this.parseModuleFromNsIdent(type);
                const definition = this.symbolTableService.findDefinition(typeName, moduleInfo.module);
                if (definition) {
                    this.definitions.push({
                        uri: this.document.uri,
                        range: this.getRange(type.start, type.stop)
                    });
                }
            }
        }

        // 检查光标是否在外键引用上
        const ref = ctx.ref();
        if (ref && this.isPositionInContext(ref)) {
            this.handleForeignKeyReference(ref);
        }
    }

    // ========== 3. 查找外键定义 ==========
    enterForeignDecl(ctx: Foreign_declContext) {
        const ref = ctx.ref();
        if (ref && this.isPositionInContext(ref)) {
            this.handleForeignKeyReference(ref);
        }
    }

    /**
     * 处理外键引用解析
     * 支持以下格式：
     * - -> table
     * - -> table.field
     * - -> table[key]
     * - => table
     * - => table.field
     * - => table[key]
     */
    private handleForeignKeyReference(refCtx: RefContext) {
        try {
            const refText = this.getText(refCtx);
            this.logger.debug(`Processing foreign key reference: ${refText}`);

            // 解析外键引用语法：->/=> ns_ident key?
            // REF或LISTREF + ns_ident + 可选key

            const nsIdent = refCtx.ns_ident();
            if (!nsIdent) {
                return;
            }

            // 提取表名（命名空间）
            const tableName = this.getText(nsIdent);
            this.logger.debug(`Referenced table: ${tableName}`);

            // 检查key部分（如果存在）
            const key = refCtx.key();
            if (key) {
                this.logger.debug(`Referenced with key: ${this.getText(key)}`);
            }

            // 解析模块名（如果存在跨模块引用）
            const parts = tableName.split('.');
            let moduleName: string | undefined;
            let targetName: string;

            if (parts.length > 1) {
                // 跨模块引用：module.table
                moduleName = parts[0];
                targetName = parts[1];
                this.logger.debug(`Cross-module reference: module=${moduleName}, target=${targetName}`);
            } else {
                // 同模块引用
                targetName = parts[0];
                this.logger.debug(`Same-module reference: target=${targetName}`);
            }

            // 在符号表中查找定义
            const definition = this.symbolTableService.findDefinition(targetName, moduleName);

            if (definition) {
                this.logger.debug(`Found definition for ${targetName}`);

                // 如果有key，需要查找字段定义
                if (key) {
                    const keyText = this.getText(key);
                    this.logger.debug(`Looking for key field: ${keyText}`);

                    // T023: 处理带键外键引用的边界情况
                    try {
                        // 解析key内容，提取字段名列表
                        const keyContent = keyText.replace(/[\[\]]/g, '');
                        const fieldNames = keyContent.split(',').map(s => s.trim()).filter(s => s.length > 0);

                        if (fieldNames.length > 0) {
                            this.logger.debug(`Key fields: ${fieldNames.join(', ')}`);
                            // TODO: 验证这些字段在目标定义中是否存在
                        }
                    } catch (error) {
                        this.logger.warn(`Invalid key format: ${keyText}`);
                    }
                }

                // 添加定义位置
                this.definitions.push({
                    uri: this.document.uri,
                    range: this.getRange(refCtx.start, refCtx.stop)
                });
            } else {
                // T023: 处理未找到定义的边界情况
                this.logger.warn(`Definition not found for: ${targetName}`);

                // 记录未解析的引用用于后续诊断
                const errorInfo = {
                    type: 'undefined_reference',
                    target: targetName,
                    module: moduleName,
                    position: {
                        line: this.position.line,
                        character: this.position.character
                    }
                };
                this.logger.debug(`Recording undefined reference: ${JSON.stringify(errorInfo)}`);
            }
        } catch (error) {
            this.logger.error('Error handling foreign key reference:', error);
        }
    }

    // ========== 辅助方法 ==========
    private isPositionInContext(ctx: any): boolean {
        if (!ctx || !ctx.start || !ctx.stop) {
            return false;
        }

        const startLine = ctx.start.line;
        const endLine = ctx.stop.line;

        return this.position.line >= startLine && this.position.line <= endLine;
    }

    private getRange(start: any, stop?: any): any {
        const startPos = this.document.positionAt(start.startIndex);
        const endPos = stop
            ? this.document.positionAt(stop.stopIndex + 1)
            : this.document.positionAt(start.stopIndex + 1);
        return { start: startPos, end: endPos };
    }

    private getText(ctx: any): string {
        return this.document.getText(this.getRange(ctx.start, ctx.stop));
    }

    private extractTypeName(typeCtx: any): string | null {
        // 简化实现：直接从ns_ident提取名称
        const nsIdent = typeCtx.ns_ident?.();
        return nsIdent ? this.getText(nsIdent) : null;
    }

    private isBaseType(typeName: string): boolean {
        const baseTypes = ['bool', 'int', 'long', 'float', 'str', 'text'];
        return baseTypes.includes(typeName);
    }

    private parseModuleFromNsIdent(typeCtx: any): { module?: string; name: string } {
        // 简化实现
        return { name: this.getText(typeCtx) };
    }

    // ========== Exit methods (required by CfgListener) ==========
    exitSchema(ctx: any): void {}
    exitSchema_ele(ctx: any): void {}
    exitStruct_decl(ctx: any): void {}
    exitInterface_decl(ctx: any): void {}
    exitTable_decl(ctx: any): void {}
    exitField_decl(ctx: any): void {}
    exitForeign_decl(ctx: any): void {}

    // ========== Additional enter methods for other rules ==========
    enterEveryRule(ctx: any): void {}

    // Every listener must provide this, uniformly for all rule contexts.
    // ParseTreeListener 需要实现此方法
    visitTree(tree: any): void {}
}
