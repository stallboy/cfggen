package configgen.gengo;

import configgen.gen.Generator;
import configgen.schema.FieldSchema;
import configgen.schema.ForeignKeySchema;
import configgen.schema.KeySchema;

public class ListRefCode {
    public final String codeGetByDefine;
    public ListRefCode(GoName name, ForeignKeySchema foreignKeySchema) {
        KeySchema keySchema = foreignKeySchema.key();
        GoName refTableName = new GoName(foreignKeySchema.refTableSchema());
        FieldSchema fieldSchema = keySchema.fieldSchemas().getFirst();
        codeGetByDefine = """
                func (t *${className}) GetRef${VarName}() []*${ValueType} {
                	if t.ref${VarName} == nil {
                		t.ref${VarName} = make([]*${ValueType}, len(t.${varName}))
                		for i, v := range t.${varName} {
                			t.ref${VarName}[i] = Get${ValueType}Mgr().Get(v)
                		}
                	}
                	return t.ref${VarName}
                }
                """.replace("${className}", name.className)
                .replace("${varName}", fieldSchema.name())
                .replace("${VarName}", Generator.upper1(fieldSchema.name()))
                .replace("${ValueType}", refTableName.className);
    }
}
