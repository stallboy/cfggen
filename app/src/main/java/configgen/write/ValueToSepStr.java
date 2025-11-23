package configgen.write;

import configgen.schema.FieldFormat.Sep;
import configgen.schema.FieldSchema;
import configgen.schema.FieldType.FList;
import configgen.value.CfgValue;
import configgen.value.CfgValue.*;
import org.jetbrains.annotations.NotNull;

import java.util.stream.Collectors;

import static configgen.schema.FieldFormat.AutoOrPack.PACK;

public class ValueToSepStr {

    public static String toSepStr(@NotNull VList vList,
                                  @NotNull FieldSchema field) {
        if (!(field.fmt() instanceof Sep(char sep))) {
            throw new IllegalArgumentException("FieldFormat is not Sep");
        }

        // 不支持在类型为结构体的字段上设置 sep（应在结构体定义上设置）
        if (!(field.type() instanceof FList)) {
            throw new IllegalArgumentException("FieldType is not FList");
        }

        String sepStr = String.valueOf(sep);
        return vList.valueList().stream().map(ValueToSepStr::toStr)
                .collect(Collectors.joining(sepStr));
    }

    public static String toSepStr(@NotNull VStruct vStruct) {
        if (!(vStruct.schema().fmt() instanceof Sep(char sep))) {
            throw new IllegalArgumentException("FieldFormat is not Sep");
        }

        String sepStr = String.valueOf(sep);
        // 结构体中的所有字段必须是基本类型（primitive）
        return vStruct.values().stream().map(v -> ((PrimitiveValue) v).toStr())
                .collect(Collectors.joining(sepStr));
    }

    private static String toStr(SimpleValue value) {
        switch (value) {
            case PrimitiveValue pv -> {
                return pv.toStr();
            }

            case VStruct vStruct -> {
                switch (vStruct.schema().fmt()) {
                    case PACK -> {
                        return vStruct.packStr();
                    }
                    case Sep ignored -> {
                        return toSepStr(vStruct);
                    }
                    default -> {
                        throw new IllegalArgumentException("StructSchema is not Pack or Sep");
                    }
                }
            }


            case CfgValue.VInterface vInterface -> {
                if (vInterface.schema().fmt().equals(PACK)) {
                    return vInterface.packStr();
                }
                throw new IllegalArgumentException("InterfaceSchema is not Pack");
            }
        }
    }

}
