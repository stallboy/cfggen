@import configgen.gen.Generator
@import configgen.gengo.*
@import configgen.gengo.GoName
@import configgen.gengo.KeySchemaCode
@import configgen.gengo.PrimarySubKeyCode
@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gengo.model.StructModel
@import configgen.value.CfgValue
@import java.util.ArrayList
@import java.util.List
@param StructModel model
!{
    Structural structural = model.structural;
    String className = model.name.className;
    GoName name = model.name;
    CfgValue.VTable vTable = model.vTable;
    TableSchema table = vTable != null ? vTable.schema() : null;
}
package ${model.pkg}

type ${className} struct {
    <%--field property--%>
    @for (FieldSchema fieldSchema : structural.fields())
        !{String comment = fieldSchema.comment();}
        ${GenGo.lower1(fieldSchema.name())} ${GenGo.type(fieldSchema.type())}${comment != null && !comment.isEmpty() ? " //" + comment : ""}
    @endfor
    <%--ref property--%>
    @for (ForeignKeySchema fk : structural.foreignKeys())
        ${GenGo.lower1(GenGo.refName(fk))} ${GenGo.refType(fk)}
    @endfor
}

<%--create instance--%>
!{String streamIf = structural.fields().size() > 0 ? "stream" : "_";}
func create${className}(${streamIf} *Stream) *${className} {
    v := &${className}{}
    @for (FieldSchema fieldSchema : structural.fields())
        !{var t = fieldSchema.type();}
        !{var n = GenGo.lower1(fieldSchema.name());}
        @if (t instanceof StructRef structRef)
            v.${n} = create${GenGo.ClassName(structRef.obj())}(stream)
        @elseif(t instanceof FList (SimpleType item))
            ${n}Size := stream.ReadInt32()
            v.${n} = make([]${GenGo.type(item)}, ${n}Size)
            for i := 0; i < int(${n}Size); i++ {
                v.${n}[i] = ${GenGo.genReadField(item)}
            }
        @elseif (t instanceof FieldType.FMap (FieldType.SimpleType key, SimpleType value))
            ${n}Size := stream.ReadInt32()
            v.${n} = make(map[${GenGo.type(key)}]${GenGo.type(value)}, ${n}Size)
            for i := 0; i < int(${n}Size); i++ {
                var k = ${GenGo.genReadField(key)}
                v.${n}[k] = ${GenGo.genReadField(value)}
            }
        @else
            v.${n} = ${GenGo.genReadField(t)}
        @endif
    @endfor
    return v
}

<%--entry--%>
@if (table != null && table.entry() instanceof EntryType.EntryBase)
//entries
var (
    @for (String e : vTable.enumNames())
    ${GenGo.lower1(e)} ${name.className}
    @endfor
)

@endif
<%--getters--%>
@if (!structural.fields().isEmpty())
    //getters
    @for (FieldSchema fieldSchema : structural.fields())
    func (t *${name.className}) Get${GenGo.upper1(fieldSchema.name())}() ${GenGo.type(fieldSchema.type())} {
        return t.${GenGo.lower1(fieldSchema.name())}
    }

    @endfor
@endif
@if (!structural.foreignKeys().isEmpty())
    <%--list ref 和 map ref--%>
    @for(ForeignKeySchema fk : structural.foreignKeys())
        !{FieldSchema keyShema = fk.key().fieldSchemas().getFirst();}
        @if(keyShema.type() instanceof FieldType.FMap)
            @template.go.GenMapRef(name = name, foreignKeySchema = fk)
        @endif
        @if (keyShema.type() instanceof FieldType.FList)
            @template.go.GenListRef(name = name, foreignKeySchema = fk)
        @endif
    @endfor
    @for(ForeignKeySchema fk : structural.foreignKeys())
        !{
           var RefName = GenGo.refName(fk);
           var refName = GenGo.lower1(RefName);
           var varName = GenGo.lower1(fk.name());
           var refType = GenGo.refType(fk);
           GoName refTbName = new GoName(fk.refTableSchema());
           var refTableClassName = refTbName.className;
           var refKey = fk.refKey();
           var fieldType = fk.key().fieldSchemas().getFirst().type();
           String getFuncName = null;
        }
        @if (refKey instanceof RefKey.RefPrimary)
            @if (!(fieldType instanceof FMap || fieldType instanceof FList))
                !{ getFuncName = new KeySchemaCode(fk.key(), name, true).codeGetFuncName; }
            @endif
        @elseif (refKey instanceof RefKey.RefList)
            !{ getFuncName = new PrimarySubKeyCode(name, fk.key().fieldSchemas().getFirst()).codeGetByFuncName; }
        @endif
        @if (getFuncName != null)
            func (t *${className}) Get${RefName}() ${refType} {
                if t.${refName} == nil {
                    t.${refName} = Get${refTableClassName}Mgr().${getFuncName}(t.${varName})
                }
                return t.${refName}
            }

        @endif
    @endfor
@endif
@if(table!=null)
    <%--EnumGetter--%>
    @if(vTable.schema().entry() instanceof EntryType.EntryBase)
        !{var tableName = new GoName(vTable.schema());}
        @for (String e : vTable.enumNames())
        func (t *${tableName.className}Mgr) Get${GenGo.upper1(e)}() *${tableName.className} {
        	return &${GenGo.lower1(e)}
        }

        @endfor
    @endif
   !{
        List<KeySchema> keySchemas = new ArrayList<>();
        keySchemas.add(table.primaryKey());
        keySchemas.addAll(table.uniqueKeys());
    }
    @for (KeySchema keySchema : keySchemas)
        @if (keySchema.fieldSchemas().size() > 1)
            type ${GenGo.keyClassName(keySchema)} struct {
                @for (FieldSchema field : keySchema.fieldSchemas())
                    ${GenGo.lower1(field.name())} ${GenGo.type(field.type())}
                @endfor
            }

        @endif
    @endfor
    type ${className}Mgr struct {
        all []*${className}
        @for (KeySchema keySchema : keySchemas)
            ${GenGo.mapName(keySchema)}Map map[${GenGo.keyClassName(keySchema)}]*${name.className}
        @endfor
        @if(table.primaryKey().fieldSchemas().size() > 1)
            @for (FieldSchema fieldSchema : table.primaryKey().fieldSchemas())
                ${fieldSchema.name()}MapList map[${GenGo.type(fieldSchema.type())}][]*${className}
            @endfor
        @endif
    }

    func(t *${className}Mgr) GetAll() []*${className} {
        return t.all
    }

    @for (KeySchema keySchema : keySchemas)
    !{KeySchemaCode keySchemaCode = new KeySchemaCode(keySchema, name, keySchema.equals(table.primaryKey()));}
    ${keySchemaCode.codeGetBy}
    @endfor
    @if(table.primaryKey().fieldSchemas().size() > 1)
        @for(FieldSchema fieldSchema : table.primaryKey().fieldSchemas())
            !{
                var mapName =fieldSchema.name();
                var codeGetByFuncName = "GetAllBy" + GenGo.upper1(fieldSchema.name());
                var IdType= GenGo.type(fieldSchema.type());
            }
            func (t *${className}Mgr) ${codeGetByFuncName}(${mapName} ${IdType}) []*${className} {
                if t.${mapName}MapList == nil {
                    t.${mapName}MapList = make(map[${IdType}][]*${className})
                    for _, item := range t.all {
                        t.${mapName}MapList[item.${mapName}] = append(t.${mapName}MapList[item.${mapName}], item)
                    }
                }
                return t.${mapName}MapList[${mapName}]
            }
        @endfor
    @endif
    <%--gen Init--%>
    func (t *${className}Mgr) Init(stream *Stream) {
        cnt := stream.ReadInt32()
        t.all = make([]*${className}, 0, cnt)
        @for (KeySchema keySchema : keySchemas)
        !{KeySchemaCode keySchemaCode = new KeySchemaCode(keySchema, name, keySchema.equals(table.primaryKey()));}
        ${keySchemaCode.codeCreateMap}
        @endfor
        for i := 0; i < int(cnt); i++ {
            v := create${className}(stream)
            t.all = append(t.all, v)
            @for (KeySchema keySchema : keySchemas)
            !{KeySchemaCode keySchemaCode = new KeySchemaCode(keySchema, name, keySchema.equals(table.primaryKey()));}
            ${keySchemaCode.codeSetMap}
            @endfor
            @if (vTable.enumNames() != null)
            !{
                var entry = vTable.schema().entry();
                String entryVarName = switch (entry) {
                    case EntryType.EEntry eEntry -> eEntry.field();
                    case EntryType.EEnum eEnum -> eEnum.field();
                    default -> null; // Default case, use a generic name
                };
            }
            switch v.${Generator.lower1(entryVarName)} {
            @for (String enumName : vTable.enumNames())
            case "${Generator.upper1(enumName)}":
                ${Generator.lower1(enumName)} = *v
            @endfor
            }
            @endif
        }
    }
@endif