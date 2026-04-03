@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gencs.StructModel
@param StructModel model
@if(!model.topPkg.equals("Config"))
using Config;
@endif

namespace ${model.name.pkg};

!{String className = model.name.className;}
!{InterfaceSchema nullableInterface = model.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
!{boolean isImpl = nullableInterface != null;}
@if(isImpl)
    public partial class ${className} : ${model.fullName(nullableInterface)}
    {
        !{var enumRefTable = nullableInterface.nullableEnumRefTable();}
        @if(enumRefTable != null)
            public ${model.fullName(enumRefTable)} type() {
                return ${model.fullName(enumRefTable)}.${model.upper1(model.structural.name())};
            }

        @endif
@else
    public partial class ${className}
    {
@endif
    <%--static enum--%>
    !{boolean hasEntry = model._vTable != null && model._vTable.schema().entry() instanceof EntryType.EntryBase&& !model._vTable.enumNames().isEmpty();}
    @if(hasEntry)
        @for(String enumName : model._vTable.enumNames())
            public static ${className} ${model.upper1(enumName)} { get; private set; } = null!;
        @endfor
    @endif
    @if(hasEntry)


    @endif

    <%--field property--%>
    @for(FieldSchema field : model.structural.fields())
        public required ${model.type(field.type())} ${model.upper1(field.name())} { get; init; }@if(!field.comment().isEmpty()) /* ${field.comment()} */@endif
    @endfor

    <%--ref property--%>
    @for(ForeignKeySchema fk : model.structural.foreignKeys())
        @if (fk.refKey() instanceof RefKey.RefSimple refSimple && refSimple.nullable())
            public ${model.refType(fk)}? ${model.refName(fk)} { get; private set; }
        @else
            public ${model.refType(fk)} ${model.refName(fk)} { get; private set; } = null!;
        @endif
    @endfor

    <%--table static Get,All--%>
    @if(model._vTable != null)
        !{TableSchema table = model._vTable.schema();}
        @template.cs.GenMapGetBy(model = model, keySchema = table.primaryKey(), isPrimaryKey = true)
        @for(KeySchema uk : table.uniqueKeys())
            @template.cs.GenMapGetBy(model = model, keySchema = uk, isPrimaryKey = false)
        @endfor

        public static IReadOnlyList<${className}> All()
        {
            return _all.Values;
        }
    @endif
}
