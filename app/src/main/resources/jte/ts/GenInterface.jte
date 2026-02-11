@import configgen.gents.TsCodeGenerator
@import configgen.schema.HasRef
@import configgen.schema.InterfaceSchema
@import configgen.schema.StructSchema
@param TsCodeGenerator gen
@param InterfaceSchema sInterface 

!{String className = gen.className(sInterface);}
export abstract class ${className} {
    @if(sInterface.nullableEnumRefTable() != null)
        abstract type() : ${gen.className(sInterface.nullableEnumRefTable())};
    @endif

    @if(HasRef.hasRef(sInterface))
        _resolve(errors: LoadErrors) {
        }
    @endif

    static _create(os: Stream) : ${className} {
        switch(os.ReadStringInPool()) {
            @for(StructSchema impl : sInterface.impls())
                case "${impl.name()}":
                    return ${gen.className(impl)}._create(os);
            @endfor
        }
    }
}

