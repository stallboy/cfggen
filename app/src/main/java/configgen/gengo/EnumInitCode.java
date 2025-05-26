package configgen.gengo;

import configgen.gen.Generator;
import configgen.schema.EntryType;
import configgen.value.CfgValue;

public class EnumInitCode {
    GoName name;
    CfgValue.VTable vTable;

    String templateSwitch = """
                    switch v.${varName} {
            ${casesCode}
                    }
            """;
    String templateCase = """
                    case "${EnumName}":
                        ${enumName} = *v
            """;

    public EnumInitCode(GoName name, CfgValue.VTable vTable) {
        this.name = name;
        this.vTable = vTable;
    }

    public String getEnumInitFunctionCode() {
        if (vTable.enumNames() == null)
            return "";
        StringBuilder cases = new StringBuilder();
        for (String enumName : vTable.enumNames()) {
            cases.append(templateCase.replace("${EnumName}", Generator.upper1(enumName))
                    .replace("${enumName}", Generator.lower1(enumName)));
        }
        var entry = vTable.schema().entry();
        String entryVarName = switch (entry) {
            case EntryType.EEntry eEntry -> eEntry.field();
            case EntryType.EEnum eEnum -> eEnum.field();
            default -> null; // Default case, use a generic name
        };
        return templateSwitch
                .replace("${varName}", Generator.lower1(entryVarName))
                .replace("${casesCode}", cases);
    }
}
