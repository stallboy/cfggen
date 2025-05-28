@import configgen.gen.Generator
@import configgen.gengo.GenGo
@import configgen.gengo.GoName
@import configgen.gengo.model.StructModel
@import configgen.schema.FieldSchema
@import configgen.schema.ForeignKeySchema
@import configgen.schema.Structural
@param StructModel model
!{
    Structural structural = model.structural;
    String className = model.name.className;
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
        ${GenGo.refName(fk)} ${GenGo.refType(fk)}
    @endfor
}

!{String streamIf = structural.fields().size() > 0 ? "stream" : "_";}
func create${className}(${streamIf} *Stream) *${className} {
    v := &${className}{}
    @for (FieldSchema fieldSchema : structural.fields())
        readValues.append(genReadValue(fieldSchema));
    @endfor
    return v
}

//getters
func (t *AiAi_action) GetID() int32 {
return t.iD
}

func (t *AiAi_action) GetDesc() string {
return t.desc
}

func (t *AiAi_action) GetFormulaID() int32 {
return t.formulaID
}

func (t *AiAi_action) GetArgIList() []int32 {
return t.argIList
}

func (t *AiAi_action) GetArgSList() []int32 {
return t.argSList
}

type AiAi_actionMgr struct {
all []*AiAi_action
iDMap map[int32]*AiAi_action

}

func(t *AiAi_actionMgr) GetAll() []*AiAi_action {
return t.all
}

func(t *AiAi_actionMgr) Get(iD int32) *AiAi_action {
return t.iDMap[iD]
}



func (t *AiAi_actionMgr) Init(stream *Stream) {
cnt := stream.ReadInt32()
t.all = make([]*AiAi_action, 0, cnt)
t.iDMap = make(map[int32]*AiAi_action, cnt)

for i := 0; i < int(cnt); i++ {
v := createAiAi_action(stream)
t.all = append(t.all, v)
t.iDMap[v.iD] = v

}
}

