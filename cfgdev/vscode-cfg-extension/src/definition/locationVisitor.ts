/**
 * Location Visitor
 * Extends HighlightingVisitor to collect symbol definition locations
 * for jump-to-definition functionality
 */

import * as vscode from 'vscode';
import { AbstractParseTreeVisitor, ParseTree, TerminalNode } from 'antlr4ng';
import { CfgVisitor } from '../grammar/CfgVisitor';
import { Struct_declContext } from '../grammar/CfgParser';
import { Interface_declContext } from '../grammar/CfgParser';
import { Table_declContext } from '../grammar/CfgParser';
import { Field_declContext } from '../grammar/CfgParser';
import { Type_Context } from '../grammar/CfgParser';
import { Ns_identContext } from '../grammar/CfgParser';
import { SymbolTableManager, SymbolType, SymbolLocation } from './symbolTableManager';

export class LocationVisitor extends AbstractParseTreeVisitor<void> implements CfgVisitor<void> {
    private document: vscode.TextDocument;
    private symbolTableManager: SymbolTableManager;
    private currentScope: string | undefined;

    constructor(document: vscode.TextDocument) {
        super();
        this.document = document;
        this.symbolTableManager = SymbolTableManager.getInstance();
    }

    public walk(tree: ParseTree): void {
        tree.accept(this);
    }

    protected defaultResult(): void {
        // No result needed for location collection
    }

    private getText(ctx: ParseTree | TerminalNode): string {
        if (!ctx) return '';
        if (ctx instanceof TerminalNode) {
            return ctx.getText();
        }
        const text = ctx.toString();
        return text || '';
    }

    private createRange(startLine: number, startChar: number, endLine?: number, endChar?: number): vscode.Range {
        const startPosition = new vscode.Position(startLine, startChar);
        const endPosition = endLine !== undefined && endChar !== undefined
            ? new vscode.Position(endLine, endChar)
            : new vscode.Position(startLine, startChar + 1); // Default 1 char length
        return new vscode.Range(startPosition, endPosition);
    }

    private extractNameFromNsIdent(nsIdent: Ns_identContext | null | undefined): string | undefined {
        if (!nsIdent || nsIdent.identifier().length === 0) {
            return undefined;
        }

        const identifiers = nsIdent.identifier();
        const names = identifiers.map(id => {
            const terminal = id.IDENT();
            return terminal ? this.getText(terminal) : '';
        }).filter(name => name.length > 0);

        return names.join('.');
    }

    private getRangeFromNsIdent(nsIdent: Ns_identContext | null | undefined): vscode.Range | undefined {
        if (!nsIdent || nsIdent.identifier().length === 0) {
            return undefined;
        }

        const firstIdent = nsIdent.identifier()[0];
        const lastIdent = nsIdent.identifier()[nsIdent.identifier().length - 1];

        const firstTerminal = firstIdent.IDENT();
        const lastTerminal = lastIdent.IDENT();

        if (firstTerminal && lastTerminal && firstTerminal.symbol && lastTerminal.symbol) {
            const startLine = firstTerminal.symbol.line - 1;
            const startChar = firstTerminal.symbol.column;
            const endLine = lastTerminal.symbol.line - 1;
            const endChar = lastTerminal.symbol.column + this.getText(lastTerminal).length;

            return this.createRange(startLine, startChar, endLine, endChar);
        }

        return undefined;
    }

    // ============================================================
    // Structure Definitions
    // ============================================================

    public visitStruct_decl(ctx: Struct_declContext): void {
        const name = this.extractNameFromNsIdent(ctx.ns_ident());
        const range = this.getRangeFromNsIdent(ctx.ns_ident());

        if (name && range) {
            const symbol: Omit<SymbolLocation, 'uri'> = {
                name,
                type: SymbolType.STRUCT,
                range,
                scope: this.currentScope
            };

            this.symbolTableManager.addSymbol(this.document.uri, symbol);
        }

        // Visit children to collect field definitions
        this.visitChildren(ctx);
    }

    public visitInterface_decl(ctx: Interface_declContext): void {
        const name = this.extractNameFromNsIdent(ctx.ns_ident());
        const range = this.getRangeFromNsIdent(ctx.ns_ident());

        if (name && range) {
            const symbol: Omit<SymbolLocation, 'uri'> = {
                name,
                type: SymbolType.INTERFACE,
                range,
                scope: this.currentScope
            };

            this.symbolTableManager.addSymbol(this.document.uri, symbol);

            // Set current scope for nested types within this interface
            const previousScope = this.currentScope;
            this.currentScope = name;

            // Visit children to collect nested type definitions
            this.visitChildren(ctx);

            // Restore previous scope
            this.currentScope = previousScope;
        } else {
            this.visitChildren(ctx);
        }
    }

    public visitTable_decl(ctx: Table_declContext): void {
        const name = this.extractNameFromNsIdent(ctx.ns_ident());
        const range = this.getRangeFromNsIdent(ctx.ns_ident());

        if (name && range) {
            const symbol: Omit<SymbolLocation, 'uri'> = {
                name,
                type: SymbolType.TABLE,
                range,
                scope: this.currentScope
            };

            this.symbolTableManager.addSymbol(this.document.uri, symbol);
        }

        // Visit children to collect field definitions
        this.visitChildren(ctx);
    }

    // ============================================================
    // Field Declarations
    // ============================================================

    public visitField_decl(ctx: Field_declContext): void {
        // Collect field definition
        const identifier = ctx.identifier();
        if (identifier) {
            const terminal = identifier.IDENT();
            if (terminal && terminal.symbol) {
                const fieldName = this.getText(terminal);
                const startLine = terminal.symbol.line - 1;
                const startChar = terminal.symbol.column;
                const range = this.createRange(startLine, startChar);

                const symbol: Omit<SymbolLocation, 'uri'> = {
                    name: fieldName,
                    type: SymbolType.FIELD,
                    range,
                    scope: this.currentScope
                };

                this.symbolTableManager.addSymbol(this.document.uri, symbol);
            }
        }

        // Visit children to collect type references
        this.visitChildren(ctx);
    }

    // ============================================================
    // Type Declarations
    // ============================================================

    public visitType_(ctx: Type_Context): void {
        // IMPORTANT: Do NOT collect type references as symbols
        // Type references (e.g., LvlRank:LevelRank) are USAGES, not definitions
        // We should only collect actual definitions (struct, interface, table)
        // This prevents jumping to usage locations instead of definition locations

        // Simply visit children without collecting type references
        this.visitChildren(ctx);
    }

    /**
     * Visit error nodes (for debugging parsing errors)
     */
    public visitErrorNode(_node: unknown): void {
        console.error('[LocationVisitor] Error node:', _node);
    }
}