import * as vscode from 'vscode';
import { AbstractParseTreeVisitor, ParseTree, TerminalNode } from 'antlr4ng';
import { CfgVisitor } from '../grammar/CfgVisitor';
import { Struct_declContext } from '../grammar/CfgParser';
import { Interface_declContext } from '../grammar/CfgParser';
import { Table_declContext } from '../grammar/CfgParser';
import { Field_declContext } from '../grammar/CfgParser';
import { Foreign_declContext } from '../grammar/CfgParser';
import { RefContext } from '../grammar/CfgParser';
import { Type_Context } from '../grammar/CfgParser';
import { Ns_identContext } from '../grammar/CfgParser';
import { FileDefinitionAndRef, Ref } from './types';
import { TypeUtils } from '../utls/typeUtils';

/**
 * 位置访问者 - 收集定义和引用信息
 */
export class LocationVisitor extends AbstractParseTreeVisitor<void> implements CfgVisitor<void> {
    private fileDef: FileDefinitionAndRef;
    private currentInterface?: string;
    private currentInterfaceDefs?: Map<string, vscode.Range>;

    constructor(fileDef: FileDefinitionAndRef) {
        super();
        this.fileDef = fileDef;
    }

    public walk(tree: ParseTree): void {
        tree.accept(this);
    }

    protected defaultResult(): void {
        // No result needed
    }

    private getText(ctx: ParseTree | TerminalNode): string {
        if (!ctx) return '';
        if (ctx instanceof TerminalNode) {
            return ctx.getText();
        }
        const text = ctx.toString();
        return text || '';
    }

    private createRange(line: number, startChar: number, endChar: number): vscode.Range {
        return new vscode.Range(
            new vscode.Position(line - 1, startChar),
            new vscode.Position(line - 1, endChar)
        );
    }

    private getNsIdentText(nsIdent: Ns_identContext | null | undefined): string {
        if (!nsIdent) return '';
        const identifiers = nsIdent.identifier();
        if (identifiers.length === 0) return '';

        return identifiers.map(id => {
            const terminal = id.IDENT();
            return terminal ? this.getText(terminal) : '';
        }).filter(text => text).join('.');
    }

    private getNsIdentRange(nsIdent: Ns_identContext | null | undefined): vscode.Range | null {
        if (!nsIdent) return null;
        const identifiers = nsIdent.identifier();
        if (identifiers.length === 0) return null;

        const firstIdent = identifiers[0];
        const lastIdent = identifiers[identifiers.length - 1];
        const firstTerminal = firstIdent.IDENT();
        const lastTerminal = lastIdent.IDENT();

        if (firstTerminal && lastTerminal && firstTerminal.symbol && lastTerminal.symbol) {
            const startLine = firstTerminal.symbol.line;
            const startChar = firstTerminal.symbol.column;
            const endChar = lastTerminal.symbol.column + this.getText(lastTerminal).length;
            return this.createRange(startLine, startChar, endChar);
        }
        return null;
    }

    // ============================================================
    // Structure Definitions (struct/interface/table)
    // ============================================================

    public visitStruct_decl(ctx: Struct_declContext): void {
        const nsIdent = ctx.ns_ident();
        const name = this.getNsIdentText(nsIdent);
        const range = this.getNsIdentRange(nsIdent);

        if (name && range) {
            if (this.currentInterfaceDefs) {
                // interface 内的struct
                this.currentInterfaceDefs.set(name, range);
            } else {
                // 全局struct
                this.fileDef.definitions.set(name, range);
            }
        }
        this.visitChildren(ctx);
    }

    public visitInterface_decl(ctx: Interface_declContext): void {
        const nsIdent = ctx.ns_ident();
        const name = this.getNsIdentText(nsIdent);
        const range = this.getNsIdentRange(nsIdent);

        if (name && range) {
            // 全局interface
            this.fileDef.definitions.set(name, range);

            // 进入interface作用域
            const interfaceDefs = new Map();
            this.fileDef.definitionsInInterface.set(name, interfaceDefs);

            this.currentInterface = name;
            this.currentInterfaceDefs = interfaceDefs;

            this.visitChildren(ctx);

            // 退出interface作用域
            this.currentInterface = undefined;
            this.currentInterfaceDefs = undefined;
        } else {
            this.visitChildren(ctx);
        }
    }

    public visitTable_decl(ctx: Table_declContext): void {
        const nsIdent = ctx.ns_ident();
        const name = this.getNsIdentText(nsIdent);
        const range = this.getNsIdentRange(nsIdent);

        if (name && range) {
            // 全局table
            this.fileDef.definitions.set(name, range);
        }
        this.visitChildren(ctx);
    }

    // ============================================================
    // Field Declarations
    // ============================================================

    public visitField_decl(ctx: Field_declContext): void {
        const line = ctx.start?.line;
        if (!line) {
            this.visitChildren(ctx);
            return;
        }

        const ref = new Ref();
        ref.inInterfaceName = this.currentInterface;

        // 处理类型引用
        const type = ctx.type_();
        if (type) {
            this.processType(type, ref);
        }

        // 处理外键引用
        const foreignRef = ctx.ref();
        if (foreignRef) {
            this.processRef(foreignRef, ref);
        }

        // 如果有关键引用信息，记录到lineToRefs
        // VSCode行号从0开始，所以需要减1
        if (ref.refType || ref.refTable) {
            this.fileDef.lineToRefs.set(line - 1, ref);
        }

        this.visitChildren(ctx);
    }

    // ============================================================
    // Foreign Key Declarations
    // ============================================================

    public visitForeign_decl(ctx: Foreign_declContext): void {
        const line = ctx.start?.line;
        if (!line) {
            this.visitChildren(ctx);
            return;
        }

        const ref = new Ref();
        ref.inInterfaceName = this.currentInterface;

        const foreignRef = ctx.ref();
        if (foreignRef) {
            this.processRef(foreignRef, ref);
        }

        // 如果有关键引用信息，记录到lineToRefs
        // VSCode行号从0开始，所以需要减1
        if (ref.refTable) {
            this.fileDef.lineToRefs.set(line - 1, ref);
        }

        this.visitChildren(ctx);
    }

    // ============================================================
    // Type Processing
    // ============================================================

    private processType(type: Type_Context, ref: Ref): void {
        const typeElems = type.type_ele();
        if (!typeElems || typeElems.length === 0) return;

        // 处理最后一个type_ele（可能是自定义类型）
        const lastElem = typeElems[typeElems.length - 1];
        const nsIdent = lastElem.ns_ident();

        if (nsIdent && nsIdent.identifier().length > 0) {
            const typeName = this.getNsIdentText(nsIdent);
            const range = this.getNsIdentRange(nsIdent);

            if (typeName && range && TypeUtils.isCustomType(typeName)) {
                ref.refType = typeName;
                ref.refTypeStart = range.start.character;
                ref.refTypeEnd = range.end.character;
            }
        }
    }

    // ============================================================
    // Foreign Key Reference Processing
    // ============================================================

    private processRef(refCtx: RefContext, ref: Ref): void {
        const nsIdent = refCtx.ns_ident();
        if (!nsIdent) return;

        const tableName = this.getNsIdentText(nsIdent);
        const range = this.getNsIdentRange(nsIdent);

        if (tableName && range) {
            ref.refTable = tableName;
            ref.refTableStart = range.start.character;
            ref.refTableEnd = range.end.character;
        }
    }

    /**
     * Visit error nodes (for debugging parsing errors)
     */
    public visitErrorNode(_node: unknown): void {
        console.error('[LocationVisitor] Error node:', _node);
    }
}