package configgen.gengo;

import configgen.gen.Generator;
import configgen.schema.FieldSchema;
import configgen.schema.ForeignKeySchema;
import configgen.schema.KeySchema;

public class MapRefCode {
    public final String codeGetByDefine;

    public MapRefCode(GoName name, ForeignKeySchema foreignKeySchema) {
        KeySchema keySchema = foreignKeySchema.key();
        GoName refTableName = new GoName(foreignKeySchema.refTableSchema());
        FieldSchema fieldSchema = keySchema.fieldSchemas().getFirst();
        codeGetByDefine = """
                func (t *${className}) GetRef${MapName}() map[int32]*${MapValueType} {
                	if t.ref${MapName} == nil {
                		t.ref${MapName} = make(map[int32]*OtherLoot, len(t.${mapName}))
                		for k, v := range t.${mapName} {
                			t.ref${MapName}[k] = Get${MapValueType}Mgr().Get(v)
                		}
                	}
                	return t.ref${MapName}
                }
                """.replace("${className}", name.className).
                replace("${mapName}", fieldSchema.name()).
                replace("${MapName}", Generator.upper1(fieldSchema.name())).
                replace("${MapValueType}", refTableName.className);


    }
}
