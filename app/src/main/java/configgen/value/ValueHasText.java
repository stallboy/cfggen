package configgen.value;

import configgen.schema.HasText;

import static configgen.value.CfgValue.*;

public class ValueHasText {

    public static boolean hasText(Value value) {
        switch (value) {
            case PrimitiveValue pv -> {
                return pv instanceof VText;
            }
            case VStruct vStruct -> {
                if (!HasText.hasText(vStruct.schema())) {
                    return false;
                }

                return vStruct.values().stream().anyMatch(ValueHasText::hasText);
            }
            case VInterface vInterface -> {
                if (!HasText.hasText(vInterface.schema())) {
                    return false;
                }
                return vInterface.child().values().stream().anyMatch(ValueHasText::hasText);
            }
            case VList vList -> {
                return vList.valueList().stream().anyMatch(ValueHasText::hasText);
            }
            case VMap vMap -> {
                // Map的key不会有Text，在CfgSchemaResolve时检查
                return vMap.valueMap().values().stream().anyMatch(ValueHasText::hasText);
            }
        }
    }
}
