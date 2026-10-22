@import configgen.gents.TsCodeGenerator
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@param TsCodeGenerator model
function $resolveRelations$(errors: LoadErrors): void {
    const resolver = new $RelationResolver$(errors, [
        @for(var type : model.relations())
        { name: "${type.owner().fullName()}", ctor: ${model.className(type.owner())}, indexes: [
            @for(var index : type.indexes())
            { field: ${index.field()}, keys: ${index.keys().toString()} },
            @endfor
        ], plans: [
            @for(var plan : type.plans())
            { root: "${plan.root()}", slot: ${plan.slot()}, name: "${plan.reference().name()}", nullable: ${((RefKey.RefSimple) plan.reference().refKey()).nullable()}, sources: [
                @for(var source : plan.sources())
                { scope: "${source.scope()}", fields: ${source.fields().toString()} },
                @endfor
            ], target: "${plan.target()}", recordKey: ${plan.recordKey().toString()}, steps: [
                @for(var step : plan.steps())
                { field: ${step.field()}, kind: ${step.kind()}, keys: ${step.keys().toString()} },
                @endfor
            ] },
            @endfor
        ], bind: (value, refs) => {
            @if(!type.references().isEmpty())
            const row = value as ${model.className(type.owner())};
            @endif
            @for(int i = 0; i < type.references().size(); i++)
            !{var fk = type.references().get(i);}
            row._bind${GenNaming.refFieldName(fk)}$(refs[${i}] as ${model.className(fk.referenceResultType())} | undefined);
            @endfor
        } },
        @endfor
    ]);
    @for(var table : model.cfgSchema.sortedTables())
    resolver.table("${table.name()}", [...${model.className(table)}.All().values()]);
    @endfor
    resolver.resolve();
}

@template.ts.RelationResolver()

