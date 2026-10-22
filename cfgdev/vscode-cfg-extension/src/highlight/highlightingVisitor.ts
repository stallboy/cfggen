import * as vscode from 'vscode';
import {AbstractParseTreeVisitor, ParserRuleContext} from 'antlr4ng';
import {CfgVisitor} from '../grammar/CfgVisitor';
import {
    Struct_declContext, Interface_declContext, Table_declContext, Enum_declContext,
    Field_declContext, Type_eleContext, RefContext, Source_valueContext,
    Key_declContext, MetadataContext, Enum_value_emptyContext, Enum_value_assignedContext, Ns_identContext,
} from '../grammar/CfgParser';
import {TOKEN_TYPES, TokenType} from './tokenTypes';
import {SourcePositions} from '../utils/cfgDocument';

export class HighlightingVisitor extends AbstractParseTreeVisitor<void> implements CfgVisitor<void> {
    private primaryKeyFields = new Set<string>();
    constructor(private readonly builder: vscode.SemanticTokensBuilder, private readonly positions: SourcePositions) { super(); }
    private highlight(context: ParserRuleContext, type: TokenType): void {
        const range = this.positions.range(context);
        if (range && range.start.line === range.end.line && !range.isEmpty) {
            this.builder.push(range.start.line, range.start.character, range.end.character - range.start.character, TOKEN_TYPES[type], 0);
        }
    }
    private names(name: Ns_identContext | undefined, type: TokenType): void {
        name?.identifier().forEach(id => this.highlight(id, type));
    }
    visitStruct_decl(ctx: Struct_declContext): void { this.declaration(ctx); }
    visitInterface_decl(ctx: Interface_declContext): void { this.declaration(ctx); }
    visitEnum_decl(ctx: Enum_declContext): void { this.declaration(ctx); }
    visitTable_decl(ctx: Table_declContext): void { this.declaration(ctx); }
    private declaration(ctx: Struct_declContext | Interface_declContext | Enum_declContext | Table_declContext): void {
        this.names(ctx.ns_ident(), 'STRUCTURE_DEFINITION');
        const previous = this.primaryKeyFields;
        this.primaryKeyFields = new Set();
        if (ctx instanceof Table_declContext) {
            ctx.key().identifier().forEach(id => {
                this.primaryKeyFields.add(id.getText());
                this.highlight(id, 'PRIMARY_KEY');
            });
        }
        this.visitChildren(ctx);
        this.primaryKeyFields = previous;
    }
    visitField_decl(ctx: Field_declContext): void {
        if (this.primaryKeyFields.has(ctx.identifier().getText())) this.highlight(ctx.identifier(), 'PRIMARY_KEY');
        this.visitChildren(ctx);
    }
    visitType_ele(ctx: Type_eleContext): void {
        const name = ctx.ns_ident();
        if (name) this.names(name, 'TYPE_IDENTIFIER');
    }
    visitRef(ctx: RefContext): void {
        const names = ctx.ns_ident();
        if (ctx.AT()) {
            this.names(names[0], 'TYPE_IDENTIFIER');
            this.names(names[1], 'REFERENCE_FIELD');
        } else if (ctx.SCOPE() && names.length === 1) this.names(names[0], 'REFERENCE_FIELD');
        else {
            this.names(names[0], 'FOREIGN_KEY');
            if (names[1]) this.names(names[1], 'REFERENCE_FIELD');
        }
        ctx.key()?.identifier().forEach(id => this.highlight(id, 'UNIQUE_KEY'));
    }
    visitSource_value(ctx: Source_valueContext): void {
        const names = ctx.ns_ident();
        if (ctx.AT()) {
            this.names(names[0], 'TYPE_IDENTIFIER');
            this.names(names[1], 'REFERENCE_FIELD');
        } else this.names(names[0], 'REFERENCE_FIELD');
    }
    visitKey_decl(ctx: Key_declContext): void {
        ctx.key().identifier().forEach(id => this.highlight(id, 'UNIQUE_KEY'));
    }
    visitMetadata(ctx: MetadataContext): void {
        ctx.ident_with_opt_single_value().forEach(meta => {
            const id = meta.identifier() ?? meta.minus_ident()?.identifier();
            if (id) this.highlight(id, 'METADATA');
        });
    }
    visitEnum_value_empty(ctx: Enum_value_emptyContext): void { this.highlight(ctx.identifier(), 'ENUM_MEMBER'); }
    visitEnum_value_assigned(ctx: Enum_value_assignedContext): void { this.highlight(ctx.identifier(), 'ENUM_MEMBER'); }
}
