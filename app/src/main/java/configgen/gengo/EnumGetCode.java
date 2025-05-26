package configgen.gengo;

import configgen.gen.Generator;

public class EnumGetCode {
    GoName name;
    String enumName;
    public EnumGetCode(GoName name, String enumName) {
        this.name = name;
        this.enumName = enumName;
    }
    public String getEnumGetFunctionCode() {
        return """
                func (t *${ClassName}Mgr) Get${EnumName}() *${ClassName} {
                	return &${enumName}
                }
                """.replace("${ClassName}", name.className).
                replace("${EnumName}", Generator.upper1(enumName)).
                replace("${enumName}", Generator.lower1(enumName));
    }
}
