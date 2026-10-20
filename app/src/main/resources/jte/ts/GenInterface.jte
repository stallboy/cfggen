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
        const typeName = os.ReadStringInPool();
        @if(sInterface.impls().size() == 1)
            !{var impl = sInterface.impls().getFirst();}
            if (typeName === "${impl.name()}") {
                return ${gen.className(impl)}._create(os);
            }
        @elseif(sInterface.impls().size() > 1)
            switch(typeName) {
                @for(StructSchema impl : sInterface.impls())
                    case "${impl.name()}":
                        return ${gen.className(impl)}._create(os);
                @endfor
            }
        @endif
        throw new Error("Unknown type: " + typeName);
    }

    abstract toString() : string;
}

