@import configgen.gents.InterfaceModel
@import configgen.gents.StructModel
@import configgen.gents.GenTs
@import configgen.schema.Fieldable
@import configgen.schema.InterfaceSchema
@import configgen.schema.StructSchema
@import configgen.value.CfgValue
@param GenTs model

export namespace ${model.pkg} {

@for (Fieldable fieldable : model.cfgSchema.sortedFieldables())
    @if(fieldable instanceof StructSchema structSchema)
        @template.ts.GenStruct(model = new StructModel())
    @elseif(fieldable instanceof InterfaceSchema interfaceSchema)
        @template.ts.GenInterface(gen = model, sInterface = interfaceSchema)
        @for (StructSchema impl : interfaceSchema.impls())
            @template.ts.GenStruct(model = new StructModel())
        @endfor
    @endif
@endfor


@for (CfgValue.VTable vTable : model.cfgValue.sortedTables())
    @template.ts.GenStruct(model = new StructModel());
@endfor

@if(model.nullableLanguageSwitch!=null)
    @template.ts.Text(pkg = model.pkg, languages = model.nullableLanguageSwitch.languages());
@endif

@template.ts.Processor(model = model)

}


