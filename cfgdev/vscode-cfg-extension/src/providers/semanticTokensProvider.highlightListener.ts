/**
 * ANTLR4 Highlighting Listener
 * Extends CfgBaseListener to identify semantic tokens in the parse tree
 * Works with SemanticTokensBuilder to create semantic highlighting
 */

import * as vscode from 'vscode';
import { AbstractParseTreeVisitor } from 'antlr4ts/tree';
import { CfgParser } from '../grammar/CfgParser';
import { CfgListener } from '../grammar/CfgListener';
import { ThemeConfig } from '../services/themeService';

// Import the token types from the provider
const TOKEN_TYPES = {
    STRUCTURE_DEFINITION: 0,
    TYPE_IDENTIFIER: 1,
    FIELD_NAME: 2,
    FOREIGN_KEY: 3,
    COMMENT: 4,
    METADATA: 5,
    PRIMARY_KEY: 6,
    UNIQUE_KEY: 7
};

export class CfgHighlightingListener extends AbstractParseTreeVisitor<void> implements CfgListener {
    private builder: vscode.SemanticTokensBuilder;
    private document: vscode.TextDocument;
    private theme: ThemeConfig;
    private primaryKeyFields: Set<string> = new Set();

    constructor(
        builder: vscode.SemanticTokensBuilder,
        document: vscode.TextDocument,
        theme: ThemeConfig
    ) {
        super();
        this.builder = builder;
        this.document = document;
        this.theme = theme;
    }

    /**
     * Walk the parse tree
     */
    public walk(tree: any): void {
        tree.accept(this);
    }

    /**
     * Default result for visitor
     */
    protected defaultResult(): void {
        // No result needed for semantic token collection
    }

    /**
     * Get range for a token
     */
    private getRange(startToken: any, stopToken?: any): vscode.Range {
        if (!startToken || !startToken.symbol) {
            return new vscode.Range(0, 0, 0, 0);
        }

        const line = startToken.symbol.line - 1;
        const char = startToken.symbol.charPositionInLine;

        const start = new vscode.Position(line, char);
        const end = stopToken && stopToken.symbol
            ? new vscode.Position(
                stopToken.symbol.line - 1,
                stopToken.symbol.charPositionInLine + this.getText(stopToken).length
              )
            : new vscode.Position(
                line,
                char + this.getText(startToken).length
              );

        return new vscode.Range(start, end);
    }

    /**
     * Get text from a context
     */
    private getText(ctx: any): string {
        if (!ctx) return '';
        return ctx.text || ctx.toString() || '';
    }

    /**
     * Check if a type is a base type
     */
    private isBaseType(typeText: string): boolean {
        const baseTypes = ['bool', 'int', 'long', 'float', 'str', 'text', 'list', 'map'];
        return baseTypes.includes(typeText);
    }

    /**
     * Get token type index for a semantic type
     */
    private getTokenTypeIndex(type: keyof typeof TOKEN_TYPES): number {
        return TOKEN_TYPES[type];
    }

    // ============================================================
    // Structure Definitions (struct/interface/table)
    // ============================================================

    /**
     * Highlight struct declaration name
     */
    public visitStructDecl(ctx: any): void {
        const name = ctx.ns_ident();
        if (name && name.symbol) {
            this.builder.push(
                name.symbol.line - 1,
                name.symbol.charPositionInLine,
                this.getText(name).length,
                this.getTokenTypeIndex('STRUCTURE_DEFINITION'),
                0
            );
        }
        this.visitChildren(ctx);
    }

    /**
     * Highlight interface declaration name
     */
    public visitInterfaceDecl(ctx: any): void {
        const name = ctx.ns_ident();
        if (name && name.symbol) {
            this.builder.push(
                name.symbol.line - 1,
                name.symbol.charPositionInLine,
                this.getText(name).length,
                this.getTokenTypeIndex('STRUCTURE_DEFINITION'),
                0
            );
        }
        this.visitChildren(ctx);
    }

    /**
     * Highlight table declaration name
     */
    public visitTableDecl(ctx: any): void {
        const name = ctx.ns_ident();
        if (name && name.symbol) {
            this.builder.push(
                name.symbol.line - 1,
                name.symbol.charPositionInLine,
                this.getText(name).length,
                this.getTokenTypeIndex('STRUCTURE_DEFINITION'),
                0
            );
        }

        // Record primary key fields from the key declaration
        const key = ctx.key();
        if (key && key.identifier()) {
            key.identifier().forEach((id: any) => {
                this.primaryKeyFields.add(this.getText(id));
            });
        }
        this.visitChildren(ctx);
    }

    // ============================================================
    // Field Declarations
    // ============================================================

    /**
     * Highlight field name and type
     */
    public visitFieldDecl(ctx: any): void {
        // Highlight field name
        const identifier = ctx.identifier();
        if (identifier && identifier.symbol) {
            this.builder.push(
                identifier.symbol.line - 1,
                identifier.symbol.charPositionInLine,
                this.getText(identifier).length,
                this.getTokenTypeIndex('FIELD_NAME'),
                0
            );
        }

        // Highlight custom type (non-base type)
        const type = ctx.type_();
        if (type) {
            const typeText = this.getText(type);
            if (!this.isBaseType(typeText)) {
                // This is a custom type, highlight it
                const lastChild = type.type_ele()[type.type_ele().length - 1];
                if (lastChild && lastChild.ns_ident) {
                    const nsIdent = lastChild.ns_ident();
                    if (nsIdent && nsIdent.symbol) {
                        this.builder.push(
                            nsIdent.symbol.line - 1,
                            nsIdent.symbol.charPositionInLine,
                            this.getText(nsIdent).length,
                            this.getTokenTypeIndex('TYPE_IDENTIFIER'),
                            0
                        );
                    }
                }
            }
        }

        // Highlight foreign key reference
        const ref = ctx.ref();
        if (ref) {
            this.highlightForeignKey(ref);
        }
        this.visitChildren(ctx);
    }

    // ============================================================
    // Foreign Key Declarations
    // ============================================================

    /**
     * Highlight foreign key in struct
     */
    public visitForeignDecl(ctx: any): void {
        const ref = ctx.ref();
        if (ref) {
            this.highlightForeignKey(ref);
        }
        this.visitChildren(ctx);
    }

    /**
     * Highlight foreign key reference
     */
    private highlightForeignKey(ref: any): void {
        const operator = ref.REF() || ref.LISTREF();
        if (operator && operator.symbol) {
            this.builder.push(
                operator.symbol.line - 1,
                operator.symbol.charPositionInLine,
                this.getText(operator).length,
                this.getTokenTypeIndex('FOREIGN_KEY'),
                0
            );
        }

        const nsIdent = ref.ns_ident();
        if (nsIdent && nsIdent.symbol) {
            this.builder.push(
                nsIdent.symbol.line - 1,
                nsIdent.symbol.charPositionInLine,
                this.getText(nsIdent).length,
                this.getTokenTypeIndex('FOREIGN_KEY'),
                0
            );
        }

        // Highlight key if present
        const key = ref.key();
        if (key) {
            this.highlightKey(key, true);
        }
    }

    // ============================================================
    // Key Declarations
    // ============================================================

    /**
     * Highlight key declarations (primary and unique keys)
     */
    public visitKeyDecl(ctx: any): void {
        const key = ctx.key();
        if (key) {
            this.highlightKey(key, false);
        }
        this.visitChildren(ctx);
    }

    /**
     * Highlight a key and its fields
     */
    private highlightKey(key: any, isForeignKey: boolean): void {
        const identifiers = key.identifier();
        identifiers.forEach((id: any) => {
            const fieldName = this.getText(id);
            const tokenType = this.primaryKeyFields.has(fieldName) && !isForeignKey
                ? 'PRIMARY_KEY'
                : 'UNIQUE_KEY';

            if (id && id.symbol) {
                this.builder.push(
                    id.symbol.line - 1,
                    id.symbol.charPositionInLine,
                    this.getText(id).length,
                    this.getTokenTypeIndex(tokenType as keyof typeof TOKEN_TYPES),
                    0
                );
            }
        });
    }

    // ============================================================
    // Comments
    // ============================================================

    /**
     * Highlight comments
     */
    public visitCOMMENT(ctx: any): void {
        if (ctx && ctx.symbol) {
            this.builder.push(
                ctx.symbol.line - 1,
                ctx.symbol.charPositionInLine,
                this.getText(ctx).length,
                this.getTokenTypeIndex('COMMENT'),
                0
            );
        }
        this.visitChildren(ctx);
    }

    // ============================================================
    // Metadata
    // ============================================================

    /**
     * Highlight metadata
     */
    public visitMetadata(ctx: any): void {
        const identWithOpt = ctx.ident_with_opt_single_value();
        if (identWithOpt) {
            identWithOpt.forEach((identCtx: any) => {
                const identifier = identCtx.identifier();
                if (identifier && identifier.symbol) {
                    this.builder.push(
                        identifier.symbol.line - 1,
                        identifier.symbol.charPositionInLine,
                        this.getText(identifier).length,
                        this.getTokenTypeIndex('METADATA'),
                        0
                    );
                }
            });
        }
        this.visitChildren(ctx);
    }
}
