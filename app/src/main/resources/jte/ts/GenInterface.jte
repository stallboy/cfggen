@import configgen.gents.GenTs
@import configgen.schema.HasRef
@import configgen.schema.InterfaceSchema
@import configgen.schema.StructSchema
@param GenTs gen
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
        switch(os.ReadString()) {
            @for(StructSchema impl : sInterface.impls())
                case "${impl.name()}":
                    return ${gen.className(impl)}._create(os);
            @endfor
        }
    }
}

