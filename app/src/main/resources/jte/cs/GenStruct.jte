@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gencs.StructModel
@import java.util.List
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
    @if(model._vTable != null && model._vTable.schema().entry() instanceof EntryType.EntryBase)
        @for(String enumName : model._vTable.enumNames())
            public static ${className} ${model.upper1(enumName)} { get; private set; } = null!;
        @endfor
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


    <%--constructor--%>
    !{boolean hasFields = !model.structural.fields().isEmpty();}

    <%--hash, equal, toString--%>
    @if(!hasFields)
        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as ${className};
            return o != null;
        }

    @else
        !{List<FieldSchema> keys = model._vTable != null ? model._vTable.schema().primaryKey().fieldSchemas() : model.structural.fields();}
        public override int GetHashCode()
        {
            return ${model.hashCodes(keys)};
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as ${className};
            return o != null && ${model.equals(keys)};
        }

        public override string ToString()
        {
            return "(" + ${model.toStrings(model.structural.fields())} + ")";
        }

    @endif

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
