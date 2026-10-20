@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gencs.StructModel
@import configgen.util.StringUtil
@param StructModel model
@if(!model.implicitUsings && model.usesCollections())
    using System.Collections.Generic;
@endif

${model.nsLine()}

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
                public static ${className} ${model.upper1(enumName)} => ${model.upper1(enumName)}In(ConfigMgr.GetMgr());
                public static ${className} ${model.upper1(enumName)}In(ConfigMgr config) => config.${model.tableField("Symbol_" + model.upper1(enumName))}!;
            @endfor
        @endif
        @if(hasEntry)


        @endif
@endif

    <%--field property--%>
    @for(FieldSchema field : model.structural.fields())
        public ${model.requiredKeyword()}${model.type(field.type())} ${model.upper1(field.name())} { get; init; }${model.nullInit(field.type())}@if(!field.comment().isEmpty()) /* ${StringUtil.removeLineSep(field.comment())} */@endif
    @endfor
    @if(isEnum)
        public ${model.requiredKeyword()}${className} EEnum { get; init; }
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
            => All(ConfigMgr.GetMgr());

        public static IReadOnlyList<${className}${isEnum?"Info" : ""}> All(ConfigMgr config)
            => config.${model.tableField("Rows")};
    @endif
}
@if(isEnum)


public static class ${className}Extensions
{
    public static ${className}Info Info(this ${className} e)
        => e.Info(ConfigMgr.GetMgr());

    public static ${className}Info Info(this ${className} e, ConfigMgr config)
        => config.${model.tableField("Symbols")}[(int)e]!;
}
@endif
@if(model.unity)
}
@endif
