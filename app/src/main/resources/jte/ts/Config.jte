@import configgen.gents.StructModel
@import configgen.gents.TsCodeGenerator
@import configgen.schema.Fieldable
@import configgen.schema.InterfaceSchema
@import configgen.schema.StructSchema
@import configgen.value.CfgValue
@param TsCodeGenerator model
// noinspection UnnecessaryLocalVariableJS,JSUnusedLocalSymbols,JSUnusedGlobalSymbols,DuplicatedCode,SpellCheckingInspection

import {Stream, LoadErrors} from "./ConfigUtil";

export namespace ${model.pkg} {

@for (Fieldable fieldable : model.cfgSchema.sortedFieldables())
    @if(fieldable instanceof StructSchema structSchema)
        @template.ts.GenStruct(model = new StructModel(model, structSchema, null))
    @elseif(fieldable instanceof InterfaceSchema interfaceSchema)
        @template.ts.GenInterface(gen = model, sInterface = interfaceSchema)
        @for (StructSchema impl : interfaceSchema.impls())
            @template.ts.GenStruct(model = new StructModel(model, impl, null))
        @endfor
    @endif
@endfor


@for (CfgValue.VTable vTable : model.cfgValue.sortedTables())
    @template.ts.GenStruct(model = new StructModel(model, vTable.schema(), vTable))
@endfor

@if(model.nullableLanguageSwitch!=null)
    @template.ts.Text(pkg = model.pkg, languages = model.nullableLanguageSwitch.languages())
@endif

@template.ts.Processor(model = model)

}


