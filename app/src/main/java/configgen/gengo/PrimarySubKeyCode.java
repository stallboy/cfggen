package configgen.gengo;

import configgen.schema.FieldSchema;

public class PrimarySubKeyCode {
    public final String codeGetByFuncName;
    public final String codeGetByDefine;

    public PrimarySubKeyCode(GoName name, FieldSchema fieldSchema) {
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
