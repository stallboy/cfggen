package configgen.value;

import configgen.schema.FieldSchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ForeachPrimitiveValue {

    public interface PrimitiveValueVisitor {
        void accept(PrimitiveValue primitiveValue, String table, List<String> fieldChain);
    }

    public static void foreach(PrimitiveValueVisitor visitor, CfgValue cfgValue) {
        for (VTable vTable : cfgValue.tables()) {
            foreachVTable(visitor, vTable);
        }
    }

    public static void foreachVTable(PrimitiveValueVisitor visitor, VTable vTable) {
        for (VStruct vStruct : vTable.valueList()) {
            foreachValue(visitor, vStruct, vTable.name(), List.of());
        }
    }

    public static void foreachValue(PrimitiveValueVisitor visitor, Value value, String table, List<String> fieldChain) {
        switch (value) {
            case PrimitiveValue primitiveValue -> {
                visitor.accept(primitiveValue, table, fieldChain);
            }
            case VStruct vStruct -> {
                int i = 0;
                for (FieldSchema field : vStruct.schema().fields()) {
                    Value fv = vStruct.values().get(i);
                    foreachValue(visitor, fv, table, listAddOf(fieldChain, field.name()));
                    i++;
                }
            }
            case VInterface vInterface -> {
                foreachValue(visitor, vInterface.child(), table, fieldChain);
            }
            case VList vList -> {
                int i = 0;
                for (SimpleValue sv : vList.valueList()) {
                    foreachValue(visitor, sv, table, listAddOf(fieldChain, STR. "\{ i }" ));
                    i++;
                }
            }
            case VMap vMap -> {
                int i = 0;
                for (Map.Entry<SimpleValue, SimpleValue> entry : vMap.valueMap().entrySet()) {
                    foreachValue(visitor, entry.getKey(), table, listAddOf(fieldChain, STR. "\{ i }k" ));
                    foreachValue(visitor, entry.getKey(), table, listAddOf(fieldChain, STR. "\{ i }v" ));
                    i++;
                }
            }
        }
    }

    private static List<String> listAddOf(List<String> old, String e) {
        List<String> res = new ArrayList<>(old.size() + 1);
        res.addAll(old);
        res.add(e);
        return res;
    }

}
