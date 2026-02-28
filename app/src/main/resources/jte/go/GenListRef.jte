@import configgen.schema.FieldSchema
@import configgen.schema.ForeignKeySchema
@import configgen.schema.KeySchema
@import configgen.gengo.GoName
@import configgen.util.StringUtil
@param GoName name
@param ForeignKeySchema foreignKeySchema
!{
    KeySchema keySchema = foreignKeySchema.key();
    GoName refTableName = new GoName(foreignKeySchema.refTableSchema());
    FieldSchema fieldSchema = keySchema.fieldSchemas().getFirst();
    String className = name.className;
    String varName = fieldSchema.name();
    String VarName =StringUtil.upper1(fieldSchema.name());
    var ValueType = refTableName.className;
}
//list ref
func (t *${className}) Ref${VarName}() []*${ValueType} {
    if t.ref${VarName} == nil {
        t.ref${VarName} = make([]*${ValueType}, len(t.${varName}))
        for i, v := range t.${varName} {
            t.ref${VarName}[i] = Get${ValueType}Mgr().Get(v)
        }
    }
    return t.ref${VarName}
}