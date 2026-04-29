@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gencs.StructModel
@import configgen.util.StringUtil
@param StructModel model
@if(!model.topPkg.equals("Config"))
using Config;
@endif

namespace ${model.name.pkg};

!{String className = model.name.className;}
!{InterfaceSchema nullableInterface = model.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
!{boolean isImpl = nullableInterface != null;}
!{boolean isEnum = model.isEnum();}

@if(isImpl)
    public partial class ${className} : ${model.fullName(nullableInterface)}
    {
        !{var enumRefTable = nullableInterface.nullableEnumRefTable();}
        @if(enumRefTable != null)
            public ${model.fullName(enumRefTable)} type() {
                return ${model.fullName(enumRefTable)}.${model.upper1(model.structural.name())};
            }

        @endif
@elseif(isEnum)
    public enum ${className}
    {
        @for(String enumName : model._vTable.enumNames())
            ${model.upper1(enumName)},
        @endfor
    }

    public partial class ${className}Info
    {
@else
    public partial class ${className}
    {
    <%--static enum--%>
        !{boolean hasEntry = model.hasEntry();}
        @if(hasEntry)
            @for(String enumName : model._vTable.enumNames())
                public static ${className} ${model.upper1(enumName)} { get; private set; } = null!;
            @endfor
        @endif
        @if(hasEntry)


        @endif
@endif

    <%--field property--%>
    @for(FieldSchema field : model.structural.fields())
        public required ${model.type(field.type())} ${model.upper1(field.name())} { get; init; }@if(!field.comment().isEmpty()) /* ${StringUtil.removeLineSep(field.comment())} */@endif
    @endfor
    @if(isEnum)
        public required ${className} EEnum { get; init; }
    @endif

    <%--ref property--%>
    @for(ForeignKeySchema fk : model.structural.foreignKeys())
        public ${model.refType(fk)} ${model.refName(fk)} { get; private set; }${StructModel.refInit(fk)}
    @endfor

    <%--table static Get,All--%>
    @if(model._vTable != null)
        !{TableSchema table = model._vTable.schema();}
        !{boolean primarySeq = model.isSeqKey(table.primaryKey());}
        @template.cs.GenMapGetBy(model = model, keySchema = table.primaryKey(), isPrimaryKey = true)
        @for(KeySchema uk : table.uniqueKeys())
            @template.cs.GenMapGetBy(model = model, keySchema = uk, isPrimaryKey = false)
        @endfor

        public static IReadOnlyList<${className}${isEnum?"Info" : ""}> All()
        {
            @if(primarySeq)
                return _all;
            @else
                return _all.Values;
            @endif
        }
    @endif
}
@if(isEnum)


public static class ${className}Extensions
{
    internal static readonly ${className}Info[] _infos = new ${className}Info[${model._vTable.enumNames().size()}];

    public static ${className}Info Info(this ${className} e)
    {
        return _infos[(int)e];
    }
}
@endif