package configgen.gengo;

import configgen.schema.FieldSchema;

public class PrimarySubKeyCode {
    public final String codeMapDefine;
    public final String codeGetByDefine;
    public final String codeGetByFuncName;

    public PrimarySubKeyCode(GoName name, FieldSchema fieldSchema) {
        codeMapDefine = """
                    ${mapName}MapList map[${IdType}][]*${className}
                """.replace("${mapName}", fieldSchema.name())
                .replace("${IdType}", GenGo.type(fieldSchema.type()))
                .replace("${className}", name.className);
        codeGetByFuncName = "GetAllBy" + GenGo.upper1(fieldSchema.name());
        codeGetByDefine = """
                func (t *${className}Mgr) ${codeGetByFuncName}(${mapName} ${IdType}) []*${className} {
                	if t.${mapName}MapList == nil {
                		t.${mapName}MapList = make(map[${IdType}][]*${className})
                		for _, item := range t.all {
                			t.${mapName}MapList[item.${mapName}] = append(t.${mapName}MapList[item.${mapName}], item)
                		}
                	}
                	return t.${mapName}MapList[${mapName}]
                }
                """
                .replace("${className}", name.className)
                .replace("${mapName}", fieldSchema.name())
                .replace("${codeGetByFuncName}", codeGetByFuncName)
                .replace("${IdType}", GenGo.type(fieldSchema.type()));
    }
}
