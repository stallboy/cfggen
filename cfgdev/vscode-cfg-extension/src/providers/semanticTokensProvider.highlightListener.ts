/**
 * ANTLR4 Highlighting Listener
 * Implements CfgVisitor to identify semantic tokens in the parse tree
 * Implements highlighting rules as per HighlightRule.md
 */

import * as vscode from 'vscode';
import { AbstractParseTreeVisitor, ParseTree, TerminalNode } from 'antlr4ts/tree';
import { CfgVisitor } from '../grammar/CfgVisitor';
import { ThemeConfig } from '../services/themeService';
import { Struct_declContext } from '../grammar/CfgParser';
import { Interface_declContext } from '../grammar/CfgParser';
import { Table_declContext } from '../grammar/CfgParser';
import { Field_declContext } from '../grammar/CfgParser';
import { Foreign_declContext } from '../grammar/CfgParser';
import { RefContext } from '../grammar/CfgParser';
import { Key_declContext } from '../grammar/CfgParser';
import { KeyContext } from '../grammar/CfgParser';
import { MetadataContext } from '../grammar/CfgParser';
import { Type_Context } from '../grammar/CfgParser';
import { Ns_identContext } from '../grammar/CfgParser';

// Token types for semantic highlighting
const TOKEN_TYPES = {
    STRUCTURE_DEFINITION: 0,  // struct/interface/table names
    TYPE_IDENTIFIER: 1,       // custom types and generic types
    FOREIGN_KEY: 2,           // foreign key references (->xxx)
    COMMENT: 3,               // comments
    METADATA: 4,              // metadata keywords
    PRIMARY_KEY: 5,           // primary key field names
    UNIQUE_KEY: 6             // unique key field names
};

export class CfgHighlightingListener extends AbstractParseTreeVisitor<void> implements CfgVisitor<void> {
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

    public walk(tree: ParseTree): void {
        tree.accept(this);
    }

    protected defaultResult(): void {
        // No result needed for semantic token collection
    }

    /**
     * Highlight an ns_ident (namespace identifier) from first to last identifier
     */
    private highlightNsIdent(nsIdent: Ns_identContext | null | undefined): void {
        if (nsIdent && nsIdent.identifier().length > 0) {
            const firstIdent = nsIdent.identifier()[0];
            const lastIdent = nsIdent.identifier()[nsIdent.identifier().length - 1];

            const firstTerminal = firstIdent.IDENT();
            const lastTerminal = lastIdent.IDENT();

            if (firstTerminal && lastTerminal && firstTerminal.symbol && lastTerminal.symbol) {
                const startLine = firstTerminal.symbol.line - 1;
                const startChar = firstTerminal.symbol.charPositionInLine;
                const endChar = lastTerminal.symbol.charPositionInLine + this.getText(lastTerminal).length;

                this.builder.push(
                    startLine,
                    startChar,
                    endChar - startChar,
                    this.getTokenTypeIndex('STRUCTURE_DEFINITION'),
                    0
                );
            }
        }
    }

    private getText(ctx: ParseTree | TerminalNode): string {
        if (!ctx) return '';
        const ctxWithText = ctx as { text?: string };
        return ctxWithText.text || ctx.toString() || '';
    }

    private getTokenTypeIndex(type: keyof typeof TOKEN_TYPES): number {
        return TOKEN_TYPES[type];
    }

    /**
     * Check if a type is a basic type
     */
    private isBaseType(typeText: string): boolean {
        const baseTypes = ['int', 'float', 'long', 'bool', 'str', 'text'];
        return baseTypes.includes(typeText);
    }

    // ============================================================
    // Structure Definitions (struct/interface/table)
    // ============================================================

    public visitStruct_decl(ctx: Struct_declContext): void {
        // Clear previous primary key fields when entering a new struct
        this.primaryKeyFields.clear();

        // Highlight the entire ns_ident, not 'struct' keyword
        this.highlightNsIdent(ctx.ns_ident());
        this.visitChildren(ctx);
    }

    public visitInterface_decl(ctx: Interface_declContext): void {
        // Clear previous primary key fields when entering a new interface
        this.primaryKeyFields.clear();

        // Highlight the entire ns_ident, not 'interface' keyword
        this.highlightNsIdent(ctx.ns_ident());
        this.visitChildren(ctx);
    }

    public visitTable_decl(ctx: Table_declContext): void {
        // Clear previous primary key fields when entering a new table
        this.primaryKeyFields.clear();

        // Highlight the entire ns_ident, not 'table' keyword
        this.highlightNsIdent(ctx.ns_ident());

        // Record ONLY the first primary key field name from table declaration
        const key = ctx.key();
        if (key && key.identifier().length > 0) {
            // Only record the first identifier as primary key
            const firstId = key.identifier()[0];
            const terminal = firstId.IDENT();
            if (terminal) {
                this.primaryKeyFields.add(this.getText(terminal));
            }
        }
        this.visitChildren(ctx);
    }

    // ============================================================
    // Field Declarations
    // ============================================================

    public visitField_decl(ctx: Field_declContext): void {
        // Highlight field name
        const identifier = ctx.identifier();
        if (identifier) {
            const terminal = identifier.IDENT();
            if (terminal && terminal.symbol) {
                const fieldName = this.getText(terminal);
                const tokenType = this.primaryKeyFields.has(fieldName)
                    ? 'PRIMARY_KEY'
                    : null;

                // Only highlight if this is a primary key field
                if (tokenType) {
                    this.builder.push(
                        terminal.symbol.line - 1,
                        terminal.symbol.charPositionInLine,
                        this.getText(terminal).length,
                        this.getTokenTypeIndex(tokenType as keyof typeof TOKEN_TYPES),
                        0
                    );
                }
            }
        }

        // Highlight complex type declarations (non-base types)
        const type = ctx.type_();
        if (type) {
            this.highlightType(type);
        }

        // Highlight foreign key reference
        const ref = ctx.ref();
        if (ref) {
            this.highlightForeignKey(ref);
        }

        // Highlight metadata
        const metadata = ctx.metadata();
        if (metadata) {
            this.highlightMetadata(metadata);
        }
    }

    // ============================================================
    // Type Highlighting
    // ============================================================

    private highlightType(type: Type_Context): void {
        // Handle three cases:
        // 1. TLIST '<' type_ele '>'  - e.g., list<int>
        // 2. TMAP '<' type_ele ',' type_ele '>'  - e.g., map<string, int>
        // 3. type_ele  - e.g., Range, RewardItem

        const typeElems = type.type_ele();
        if (typeElems && typeElems.length > 0) {
            // Check if this is a TLIST or TMAP
            const isListOrMap = type.TLIST() !== undefined || type.TMAP() !== undefined;

            if (isListOrMap) {
                // For TLIST/TMAP, highlight the entire expression from the keyword to the last type_ele
                const firstElem = typeElems[0];
                const lastElem = typeElems[typeElems.length - 1];
                const firstNsIdent = firstElem.ns_ident();
                const lastNsIdent = lastElem.ns_ident();

                if (firstNsIdent && lastNsIdent &&
                    firstNsIdent.identifier().length > 0 &&
                    lastNsIdent.identifier().length > 0) {

                    const firstIdent = firstNsIdent.identifier()[0];
                    const lastIdent = lastNsIdent.identifier()[lastNsIdent.identifier().length - 1];
                    const firstTerminal = firstIdent.IDENT();
                    const lastTerminal = lastIdent.IDENT();

                    if (firstTerminal && lastTerminal && firstTerminal.symbol && lastTerminal.symbol) {
                        const startLine = firstTerminal.symbol.line - 1;
                        const startChar = firstTerminal.symbol.charPositionInLine;
                        const endChar = lastTerminal.symbol.charPositionInLine + this.getText(lastTerminal).length;

                        this.builder.push(
                            startLine,
                            startChar,
                            endChar - startChar,
                            this.getTokenTypeIndex('TYPE_IDENTIFIER'),
                            0
                        );
                    }
                }
            } else {
                // For simple types (type_ele), highlight the entire namespace identifier if it's NOT a basic type
                const lastElem = typeElems[typeElems.length - 1];
                const nsIdent = lastElem.ns_ident();

                if (nsIdent && nsIdent.identifier().length > 0) {
                    const firstIdent = nsIdent.identifier()[0];
                    const lastIdent = nsIdent.identifier()[nsIdent.identifier().length - 1];
                    const firstTerminal = firstIdent.IDENT();
                    const lastTerminal = lastIdent.IDENT();

                    if (firstTerminal && lastTerminal && firstTerminal.symbol && lastTerminal.symbol) {
                        // Only highlight if this is NOT a basic type
                        const typeText = this.getText(lastTerminal);
                        if (!this.isBaseType(typeText)) {
                            const startLine = firstTerminal.symbol.line - 1;
                            const startChar = firstTerminal.symbol.charPositionInLine;
                            const endChar = lastTerminal.symbol.charPositionInLine + this.getText(lastTerminal).length;

                            this.builder.push(
                                startLine,
                                startChar,
                                endChar - startChar,
                                this.getTokenTypeIndex('TYPE_IDENTIFIER'),
                                0
                            );
                        }
                    }
                }
            }
        }
    }

    // ============================================================
    // Foreign Key Highlighting
    // ============================================================

    public visitForeign_decl(ctx: Foreign_declContext): void {
        const ref = ctx.ref();
        if (ref) {
            this.highlightForeignKey(ref);
        }
        this.visitChildren(ctx);
    }

    private highlightForeignKey(ref: RefContext): void {
        // Highlight the entire foreign key reference as one unit
        // This includes ->, module, and key if present
        const operator = ref.REF() || ref.LISTREF();
        if (operator && operator.symbol) {
            // Get the range of the entire foreign key reference
            const nsIdent = ref.ns_ident();
            if (nsIdent && nsIdent.identifier().length > 0) {
                const firstIdent = nsIdent.identifier()[0];
                const firstTerminal = firstIdent.IDENT();

                if (firstTerminal && firstTerminal.symbol) {
                    // Calculate the end position
                    const key = ref.key();
                    let endChar = firstTerminal.symbol.charPositionInLine + this.getText(firstTerminal).length;

                    if (key && key.identifier().length > 0) {
                        const lastKeyId = key.identifier()[key.identifier().length - 1];
                        const lastKeyTerminal = lastKeyId.IDENT();
                        if (lastKeyTerminal && lastKeyTerminal.symbol) {
                            endChar = lastKeyTerminal.symbol.charPositionInLine + this.getText(lastKeyTerminal).length;
                        }
                    }

                    // Highlight from the operator to the end
                    this.builder.push(
                        operator.symbol.line - 1,
                        operator.symbol.charPositionInLine,
                        endChar - operator.symbol.charPositionInLine,
                        this.getTokenTypeIndex('FOREIGN_KEY'),
                        0
                    );
                }
            }
        }
    }

    // ============================================================
    // Key Declarations
    // ============================================================

    public visitKey_decl(ctx: Key_declContext): void {
        const key = ctx.key();
        if (key) {
            this.highlightKey(key, false);
        }
        this.visitChildren(ctx);
    }

    private highlightKey(key: KeyContext, isForeignKey: boolean): void {
        const identifiers = key.identifier();
        identifiers.forEach((id) => {
            const terminal = id.IDENT();
            if (terminal) {
                const fieldName = this.getText(terminal);
                const tokenType = this.primaryKeyFields.has(fieldName) && !isForeignKey
                    ? 'PRIMARY_KEY'
                    : 'UNIQUE_KEY';

                if (terminal.symbol) {
                    this.builder.push(
                        terminal.symbol.line - 1,
                        terminal.symbol.charPositionInLine,
                        this.getText(terminal).length,
                        this.getTokenTypeIndex(tokenType as keyof typeof TOKEN_TYPES),
                        0
                    );
                }
            }
        });
    }

    // ============================================================
    // Comments
    // ============================================================

    public visitCOMMENT(ctx: TerminalNode): void {
        if (ctx && ctx.symbol) {
            this.builder.push(
                ctx.symbol.line - 1,
                ctx.symbol.charPositionInLine,
                this.getText(ctx).length,
                this.getTokenTypeIndex('COMMENT'),
                0
            );
        }
    }

    // ============================================================
    // Metadata
    // ============================================================

    public visitMetadata(ctx: MetadataContext): void {
        this.highlightMetadata(ctx);
        this.visitChildren(ctx);
    }

    private highlightMetadata(ctx: MetadataContext): void {
        const identWithOpt = ctx.ident_with_opt_single_value();
        if (identWithOpt) {
            identWithOpt.forEach((identCtx) => {
                const identifier = identCtx.identifier();
                if (identifier) {
                    const terminal = identifier.IDENT();
                    if (terminal && terminal.symbol) {
                        const metadataName = this.getText(terminal);

                        // Check if this is a special metadata keyword
                        const specialMetadata = [
                            'nullable', 'mustFill', 'enumRef', 'enum',
                            'entry', 'sep', 'pack', 'fix', 'block'
                        ];

                        if (specialMetadata.includes(metadataName)) {
                            this.builder.push(
                                terminal.symbol.line - 1,
                                terminal.symbol.charPositionInLine,
                                this.getText(terminal).length,
                                this.getTokenTypeIndex('METADATA'),
                                0
                            );
                        }
                    }
                }
            });
        }
    }
}
