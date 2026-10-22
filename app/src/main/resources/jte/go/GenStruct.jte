@import configgen.gengo.*
@import configgen.gengo.GoName
@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gengo.StructModel
@import configgen.util.StringUtil
@import configgen.value.CfgValue
@import java.util.ArrayList
@import java.util.List
@import static configgen.util.StringUtil.upper1
@import static configgen.util.StringUtil.lower1
@param StructModel model
!{
    Structural structural = model.structural;
    String className = model.name.className;
    GoName name = model.name;
    CfgValue.VTable vTable = model.vTable;
    TableSchema table = vTable != null ? vTable.schema() : null;
}
package ${model.pkg}

@if (!structural.fields().isEmpty())
import "fmt"

@endif
type ${className} struct {
    @if(model.hasRelations() && structural.fields().isEmpty())
    _identity byte
    @endif
    <%--field property--%>
    @for (FieldSchema fieldSchema : structural.fields())
        !{String comment = fieldSchema.comment();}
        ${lower1(fieldSchema.name())} ${model.type(fieldSchema.type())}${comment != null && !comment.isEmpty() ? " //" + StringUtil.removeLineSep(comment) : ""}
    @endfor
    <%--ref property--%>
    @for (ForeignKeySchema fk : structural.foreignKeys())
        ${lower1(StructModel.refName(fk))} ${model.refType(fk)}
    @endfor
}

<%--create instance--%>
!{String streamIf = structural.fields().isEmpty() ? "_" : "stream";}
func create${className}(${streamIf} *Stream) *${className} {
    v := &${className}{}
    @for (FieldSchema fieldSchema : structural.fields())
        !{var t = fieldSchema.type();}
        !{var n = lower1(fieldSchema.name());}
        @if (t instanceof StructRef structRef)
            v.${n} = create${model.ClassName(structRef.obj())}(stream)
        @elseif(t instanceof FList (SimpleType item))
            ${n}Size := stream.ReadCount(0)
            v.${n} = make([]${model.type(item)}, ${n}Size)
            for i := 0; i < ${n}Size; i++ {
                v.${n}[i] = ${model.genReadField(item)}
            }
        @elseif (t instanceof FieldType.FMap (FieldType.SimpleType key, SimpleType value))
            ${n}Size := stream.ReadCount(0)
            v.${n} = make(map[${model.type(key)}]${model.type(value)}, ${n}Size)
            for i := 0; i < ${n}Size; i++ {
                var k = ${model.genReadField(key)}
                @if(model.hasRelations())
                if _, exists := v.${n}[k]; exists { panic("duplicate map key: ${structural.fullName()}.${n}") }
                @endif
                v.${n}[k] = ${model.genReadField(value)}
            }
        @else
            v.${n} = ${model.genReadField(t)}
        @endif
    @endfor
    return v
}

<%--String method--%>
!{java.util.List<FieldSchema> fields = structural.fields();}
func (t *${className}) String() string {
    @if (fields.isEmpty())
    return "${className}{}"
    @else
    return fmt.Sprintf("${className}{${fields.stream().map(f -> lower1(f.name()) + "=%v").collect(java.util.stream.Collectors.joining(", "))}}", ${fields.stream().map(f -> "t." + lower1(f.name())).collect(java.util.stream.Collectors.joining(", "))})
    @endif
}

<%--entry--%>
@if (table != null && table.entry() instanceof EntryType.EntryBase && !model.hasRelations())
//entries
var (
    @for (String e : vTable.enumNames())
    ${lower1(e)} ${name.className}
    @endfor
)

@endif
<%--getters--%>
@if (!structural.fields().isEmpty())
    //getters
    @for (FieldSchema fieldSchema : structural.fields())
    func (t *${name.className}) ${upper1(fieldSchema.name())}() ${model.type(fieldSchema.type())} {
        return t.${lower1(fieldSchema.name())}
    }

    @endfor
@endif
@if (!structural.foreignKeys().isEmpty())
    @for(ForeignKeySchema fk : structural.foreignKeys())
        @if(fk.pathReference() != null)
        func (t *${className}) ${model.refName(fk)}() ${model.refType(fk)} {
            return t.${lower1(model.refName(fk))}
        }
        @else
        !{
            var RefName = model.refName(fk);
            var refName = lower1(RefName);
            var refType = model.refType(fk);
            var refTableClassName = new GoName(fk.refTableSchema()).className;
            var fieldType = fk.key().fieldSchemas().getFirst().type();
            var varName = lower1(fk.key().fields().getFirst());
            var getFuncName = model.refGetter(fk);
        }
            func (t *${className}) ${RefName}() ${refType} {
                if t.${refName} == nil {
                    @if (fieldType instanceof FList)
                    t.${refName} = make(${refType}, len(t.${varName}))
                    for i, value := range t.${varName} {
                        t.${refName}[i] = Get${refTableClassName}Mgr().${getFuncName}(value)
                    }
                    @elseif (fieldType instanceof FMap)
                    t.${refName} = make(${refType}, len(t.${varName}))
                    for key, value := range t.${varName} {
                        t.${refName}[key] = Get${refTableClassName}Mgr().${getFuncName}(value)
                    }
                    @else
                    t.${refName} = Get${refTableClassName}Mgr().${getFuncName}(${model.GetParamVarsInV(fk.key(), "t")})
                    @endif
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
        func (t *${tableName.className}Mgr) Get${upper1(e)}() *${tableName.className} {
            @if(model.hasRelations())
            return t.symbols["${e}"]
            @else
        	return &${lower1(e)}
            @endif
        }

        @endfor
    @endif
   !{
        List<KeySchema> keySchemas = new ArrayList<>();
        keySchemas.add(table.primaryKey());
        keySchemas.addAll(table.uniqueKeys());
    }
    @for (KeySchema keySchema : keySchemas)
        @if (keySchema.fieldSchemas().size() > 1 || StructModel.structKey(keySchema) != null)
            type ${model.keyClassName(keySchema)} struct {
                @for (FieldSchema field : StructModel.structKey(keySchema) != null ? StructModel.structKey(keySchema).fields() : keySchema.fieldSchemas())
                    ${lower1(field.name())} ${model.type(field.type())}
                @endfor
            }

        @endif
    @endfor
    type ${className}Mgr struct {
        all []*${className}
        @if(model.hasRelations() && table.entry() instanceof EntryType.EntryBase)
        symbols map[string]*${className}
        @endif
        @for (KeySchema keySchema : keySchemas)
            @if (keySchema.fieldSchemas().size() == 1 && keySchema.fieldSchemas().getFirst().isSeq())
                ${model.mapName(keySchema)}Arr []*${name.className}
            @else
                ${model.mapName(keySchema)}Map map[${model.keyClassName(keySchema)}]*${name.className}
            @endif
        @endfor
        @for (FieldSchema fieldSchema : model.reverseFields)
            ${lower1(fieldSchema.name())}MapList map[${model.reverseKeyType(fieldSchema)}][]*${className}
        @endfor
    }

    func(t *${className}Mgr) GetAll() []*${className} {
        return t.all
    }

    @for (KeySchema keySchema : keySchemas)
    !{
        var isPrimaryKey = keySchema.equals(table.primaryKey());
        var fieldSchemas = keySchema.fieldSchemas();
        var fieldCnt = fieldSchemas.size();
        var paramVars = model.GetParamVars(keySchema);
        var varDefines = model.GetVarDefines(keySchema);
        var getFuncName = model.GetFuncName(keySchema,isPrimaryKey);
        var mapName = model.mapName(keySchema);
        var isSeq = fieldCnt == 1 && fieldSchemas.getFirst().isSeq();
    }
    @if (isSeq)
        func(t *${className}Mgr) ${getFuncName}(${varDefines}) *${className} {
            if ${paramVars} < 0 || int(${paramVars}) >= len(t.${mapName}Arr) {
                return nil
            }
            return t.${mapName}Arr[${paramVars}]
        }

    @else
        func(t *${className}Mgr) ${getFuncName}(${varDefines}) *${className} {
            @if (StructModel.structKey(keySchema) != null)
            if ${paramVars} == nil { return nil }
            @endif
            return t.${mapName}Map[${model.keyExpression(keySchema, "")}]
        }

    @endif

    @endfor
        @for(FieldSchema fieldSchema : model.reverseFields)
            !{
                var mapName = lower1(fieldSchema.name());
                var codeGetByFuncName = "GetAllBy" + upper1(fieldSchema.name());
                var IdType = model.type(fieldSchema.type());
                var keyType = model.reverseKeyType(fieldSchema);
            }
            func (t *${className}Mgr) ${codeGetByFuncName}(${mapName} ${IdType}) []*${className} {
                @if (fieldSchema.type() instanceof StructRef && KeySchema.isKeyType(fieldSchema.type()))
                if ${mapName} == nil { return nil }
                @endif
                if t.${mapName}MapList == nil {
                    t.${mapName}MapList = make(map[${keyType}][]*${className})
                    for _, item := range t.all {
                        key := ${model.reverseKeyExpression(fieldSchema, "item." + mapName)}
                        t.${mapName}MapList[key] = append(t.${mapName}MapList[key], item)
                    }
                }
                return t.${mapName}MapList[${model.reverseKeyExpression(fieldSchema, mapName)}]
            }
        @endfor
    <%--gen Init--%>
    func (t *${className}Mgr) Init(stream *Stream) {
        cnt := int32(stream.ReadCount(1))
        t.all = make([]*${className}, 0, cnt)
        @if(model.hasRelations() && table.entry() instanceof EntryType.EntryBase)
        t.symbols = make(map[string]*${className})
        @endif
        @for (KeySchema keySchema : keySchemas)
        !{
            var mapName = model.mapName(keySchema);
            var IdType = model.keyClassName(keySchema);
            var initIsSeq = keySchema.fieldSchemas().size() == 1 && keySchema.fieldSchemas().getFirst().isSeq();
        }
        @if (initIsSeq)
            t.${mapName}Arr = make([]*${className}, cnt)
        @else
            t.${mapName}Map = make(map[${IdType}]*${className}, cnt)
        @endif
        @endfor
        for i := 0; i < int(cnt); i++ {
            v := create${className}(stream)
            t.all = append(t.all, v)
            @for (KeySchema keySchema : keySchemas)
            !{
                var fieldSchemas = keySchema.fieldSchemas();
                var fieldCnt = fieldSchemas.size();
                var mapName = model.mapName(keySchema);
                var paramVarsInV = model.GetParamVarsInV(keySchema, "v");
                var loopIsSeq = fieldCnt == 1 && fieldSchemas.getFirst().isSeq();
            }
            @if (loopIsSeq)
                t.${mapName}Arr[${paramVarsInV}] = v
            @else
                t.${mapName}Map[${model.keyExpression(keySchema, "v.")}] = v
            @endif
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
            switch v.${lower1(entryVarName)} {
            @for (String enumName : vTable.enumNames())
            case "${upper1(enumName)}":
                @if(model.hasRelations())
                t.symbols["${enumName}"] = v
                @else
                ${StringUtil.lower1(enumName)} = *v
                @endif
            @endfor
            }
            @endif
        }
    }
@endif
