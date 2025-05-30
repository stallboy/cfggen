@import configgen.gen.Generator
@import configgen.gengo.GenGo
@import configgen.gengo.GoName
@import configgen.gengo.model.StructModel
@import configgen.schema.*
@import configgen.schema.FieldType
@import configgen.schema.ForeignKeySchema
@import configgen.schema.Structural
@import configgen.schema.EntryType
@import configgen.value.CfgValue
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
    ${Generator.lower1(fieldSchema.name())} ${GenGo.type(fieldSchema.type())}${comment != null && !comment.isEmpty() ? " //" + comment : ""}
@endfor
<%--ref property--%>
@for (ForeignKeySchema fk : structural.foreignKeys())
    ${Generator.lower1(GenGo.refName(fk))} ${GenGo.refType(fk)}
@endfor
}

!{String streamIf = structural.fields().size() > 0 ? "stream" : "_";}
func create${className}(${streamIf} *Stream) *${className} {
    v := &${className}{}
    @for (FieldSchema fieldSchema : structural.fields())
    @template.go.GenReadSetField(fieldType = fieldSchema.type(), varName = Generator.lower1(fieldSchema.name()))
    @endfor
    return v
}
@if (table != null && table.entry() instanceof EntryType.EntryBase)
    //entries
    var (
    @for (String e : vTable.enumNames())
        ${e} ${name.className}
    @endfor
    )
@endif

@if (!structural.fields().isEmpty())
    //getters
    @for (FieldSchema fieldSchema : structural.fields())
        func (t *${name.className}) Get${Generator.upper1(fieldSchema.name())}() ${GenGo.type(fieldSchema.type())} {
            return t.${Generator.lower1(fieldSchema.name())}
        }
    @endfor
@endif


@if (!structural.foreignKeys().isEmpty())
    //ref properties
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
           var refName = Generator.lower1(RefName);
           var varName = Generator.lower1(fk.name());
           var refType = GenGo.refType(fk);
           GoName refTbName = new GoName(fk.refTableSchema());
           var refTableClassName = refTbName.className;
        }
        func (t *${className}) Get${RefName}() ${refType} {
            if t.${refName} == nil {
                t.${refName} = Get${refTableClassName}Mgr().${codeGetFuncName}(t.${varName})
            }
            return t.${refName}
        }

        @if (fk.refKey() instanceof RefKey.RefPrimary)
            !{var keySchemaType = fk.key().fieldSchemas().getFirst().type();}
            @if(!(keySchemaType instanceof FieldType.FMap)&&(keySchemaType instanceof FieldType.FList))

            @endif
            func (t *${name.className}) Get${GenGo.refName(fk)}() *${GenGo.refType(fk)} {
                return t.${Generator.lower1(GenGo.refName(fk))}
            }
        @endif
    @endfor

@endif



