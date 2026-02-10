@import configgen.gen.Generator
@import configgen.gengo.GoName
@import configgen.schema.ForeignKeySchema
@import configgen.schema.KeySchema
@import configgen.schema.FieldSchema
@import configgen.util.StringUtil
@param GoName name
@param ForeignKeySchema foreignKeySchema
!{
    KeySchema keySchema = foreignKeySchema.key();
    GoName refTableName = new GoName(foreignKeySchema.refTableSchema());
    FieldSchema fieldSchema = keySchema.fieldSchemas().getFirst();
    String className = name.className;
    String mapName = fieldSchema.name();
    String MapName = StringUtil.upper1(fieldSchema.name());
    var MapValueType = refTableName.className;
}
//map ref
func (t *${className}) Ref${MapName}() map[int32]*${MapValueType} {
    if t.ref${MapName} == nil {
        t.ref${MapName} = make(map[int32]*OtherLoot, len(t.${mapName}))
        for k, v := range t.${mapName} {
            t.ref${MapName}[k] = Get${MapValueType}Mgr().Get(v)
        }
    }
    return t.ref${MapName}
}