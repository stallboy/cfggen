import {AbstractParseTreeVisitor} from 'antlr4ng';
import {CfgVisitor} from '../grammar/CfgVisitor';
import {
    Struct_declContext, Interface_declContext, Table_declContext, Enum_declContext,
    Type_eleContext, RefContext, Source_valueContext, Ns_identContext,
} from '../grammar/CfgParser';
import {FileDefinitionAndRef, DefinitionType} from './fileDefinitionAndRef';
import {SourcePositions} from '../utils/cfgDocument';

/** Collect declarations and navigable names, never internal field paths. */
export class LocationVisitor extends AbstractParseTreeVisitor<void> implements CfgVisitor<void> {
    private currentInterface?: string;
    constructor(private readonly file: FileDefinitionAndRef, private readonly positions: SourcePositions) { super(); }

    private definition(name: Ns_identContext, type: DefinitionType, isEnum = false): void {
        const range = this.positions.range(name);
        if (!range) return;
        if (this.currentInterface && type === 'struct') {
            this.file.definitionsInInterface.get(this.currentInterface)!.set(name.getText(), range);
        } else this.file.definitions.set(name.getText(), {type, range, isEnum});
    }

    private reference(name: Ns_identContext | null, isRefType: boolean): void {
        if (!name) return;
        const range = this.positions.range(name);
        if (range) this.file.references.push({name: name.getText(), isRefType, range, inInterfaceName: this.currentInterface});
    }

    visitStruct_decl(ctx: Struct_declContext): void {
        this.definition(ctx.ns_ident(), 'struct');
        this.visitChildren(ctx);
    }
    visitInterface_decl(ctx: Interface_declContext): void {
        this.definition(ctx.ns_ident(), 'interface');
        const previous = this.currentInterface;
        this.currentInterface = ctx.ns_ident().getText();
        this.file.definitionsInInterface.set(this.currentInterface, new Map());
        this.visitChildren(ctx);
        this.currentInterface = previous;
    }
    visitTable_decl(ctx: Table_declContext): void {
        const isEnum = ctx.metadata().ident_with_opt_single_value().some(meta => meta.identifier()?.getText() === 'enum');
        this.definition(ctx.ns_ident(), 'table', isEnum);
        this.visitChildren(ctx);
    }
    visitEnum_decl(ctx: Enum_declContext): void {
        this.definition(ctx.ns_ident(), 'enum', true);
        this.visitChildren(ctx);
    }
    visitType_ele(ctx: Type_eleContext): void { this.reference(ctx.ns_ident(), true); }
    visitRef(ctx: RefContext): void {
        const names = ctx.ns_ident();
        if (ctx.AT()) this.reference(names[0], true);
        else if (!ctx.SCOPE() || names.length > 1) this.reference(names[0], false);
    }
    visitSource_value(ctx: Source_valueContext): void {
        if (ctx.AT()) this.reference(ctx.ns_ident(0), true);
    }
}
