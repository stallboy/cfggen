package configgen.gengo;

import configgen.gen.Generator;
import configgen.schema.KeySchema;

public class KeySchemaCode {

    public final String codeGetBy;
    public final String codeCreateMap;
    public final String codeSetMap;
    public final String codeGetFuncName;

    KeySchemaCode(KeySchema keySchema, GoName name, boolean refPrimary) {
        StringBuilder varDefines = new StringBuilder();
        StringBuilder paramVars = new StringBuilder();
        StringBuilder paramVarsInV = new StringBuilder();//t.id1Id2Map[KeyId1Id2{v.id1, v.id2}] = v 里面的v.id1, v.id2
        var fieldSchemas = keySchema.fieldSchemas();
        var fieldCnt = fieldSchemas.size();
        for (int i = 0; i < fieldCnt; i++) {
            var fieldSchema = fieldSchemas.get(i);
            String varName = Generator.lower1(fieldSchema.name());
            varDefines.append("${varName} ${varType}".
                    replace("${varName}", varName).
                    replace("${varType}", GenGo.type(fieldSchema.type())));
            paramVars.append(varName);
            paramVarsInV.append("v." + varName);
            if (i < fieldCnt - 1) {
                varDefines.append(", ");
                paramVars.append(", ");
                paramVarsInV.append(", ");
            }
        }
        codeGetFuncName = refPrimary ? "Get" :
                (fieldCnt > 1 ? "GetBy${IdType}" : "GetBy${paramVars}").
                        replace("${IdType}", GenGo.keyClassName(keySchema)).
                        replace("${paramVars}", paramVars);

        codeGetBy = ((fieldCnt > 1 ? """
                func(t *${className}Mgr) ${codeGetFuncName}(${varDefines}) *${className} {
                    return t.${mapName}Map[${IdType}{${paramVars}}]
                }
                
                """ : """
                func(t *${className}Mgr) ${codeGetFuncName}(${varDefines}) *${className} {
                    return t.${mapName}Map[${paramVars}]
                }
                
                """).
                replace("${className}", name.className).
                replace("${codeGetFuncName}", codeGetFuncName).
                replace("${IdType}", GenGo.keyClassName(keySchema)).
                replace("${mapName}", GenGo.mapName(keySchema)).
                replace("${varDefines}", varDefines).
                replace("${paramVars}", paramVars));
        codeCreateMap = ("""
                    t.${mapName}Map = make(map[${IdType}]*${className}, cnt)
                """.
                replace("${mapName}", GenGo.mapName(keySchema)).
                replace("${IdType}", GenGo.keyClassName(keySchema)).
                replace("${className}", name.className)
        );
        codeSetMap = ((fieldCnt > 1 ? """
                        t.${mapName}Map[${IdType}{${paramVarsInV}}] = v                        
                """ : """
                        t.${mapName}Map[${paramVarsInV}] = v
                """).
                replace("${mapName}", GenGo.mapName(keySchema)).
                replace("${IdType}", GenGo.keyClassName(keySchema)).
                replace("${paramVarsInV}", paramVarsInV)
        );
    }
}
